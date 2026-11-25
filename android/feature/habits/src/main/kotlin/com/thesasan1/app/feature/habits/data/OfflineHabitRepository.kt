package com.thesasan1.app.feature.habits.data

import com.thesasan1.app.core.common.AppError
import com.thesasan1.app.core.common.IdGenerator
import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.core.common.runCatchingOutcome
import com.thesasan1.app.core.database.TransactionRunner
import com.thesasan1.app.core.database.dao.HabitDao
import com.thesasan1.app.core.database.dao.OutboxDao
import com.thesasan1.app.core.database.entity.HabitEntity
import com.thesasan1.app.core.database.entity.HabitEventEntity
import com.thesasan1.app.core.database.entity.HabitEventType
import com.thesasan1.app.core.database.entity.HabitMeasurement
import com.thesasan1.app.core.database.entity.HabitScheduleType
import com.thesasan1.app.core.database.entity.OutboxEntity
import com.thesasan1.app.core.database.entity.OutboxOp
import com.thesasan1.app.core.database.entity.SyncColumns
import com.thesasan1.app.feature.habits.domain.HabitDay
import com.thesasan1.app.feature.habits.domain.HabitDraft
import com.thesasan1.app.feature.habits.domain.HabitRepository
import com.thesasan1.app.feature.habits.domain.HabitRules
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Room-backed [HabitRepository]. Each change writes its row and an outbox entry in one transaction
 * (DATABASE.md §5). Events are only ever inserted (ADR-028).
 */
@Suppress("TooManyFunctions") // mirrors HabitRepository
class OfflineHabitRepository @Inject constructor(
    private val dao: HabitDao,
    private val outbox: OutboxDao,
    private val transaction: TransactionRunner,
    private val ids: IdGenerator,
    private val clock: Clock,
) : HabitRepository {

    override fun observeActive(): Flow<List<HabitEntity>> = dao.observeActive()

    override fun observeArchived(): Flow<List<HabitEntity>> = dao.observeArchived()

    override fun observeDay(date: LocalDate): Flow<List<HabitDay>> =
        combine(dao.observeActive(), dao.observeEventsOn(date.toString())) { habits, events ->
            val byHabit = events.groupBy { it.habitId }
            habits.map { HabitRules.day(it, date, byHabit[it.id].orEmpty()) }
        }

    override suspend fun create(draft: HabitDraft): Outcome<HabitEntity> = storage {
        val name = draft.name.trim()
        when (val error = validate(draft, name)) {
            null -> transaction {
                val now = clock.millis()
                val habit = HabitEntity(
                    id = ids.newId(),
                    name = name,
                    description = draft.description?.trim()?.takeIf { it.isNotEmpty() },
                    measurementType = draft.measurement,
                    targetValue = draft.targetValue,
                    unit = draft.unit?.trim()?.takeIf { it.isNotEmpty() },
                    scheduleType = draft.scheduleType,
                    scheduleDays = draft.scheduleDays,
                    scheduleCount = draft.scheduleCount,
                    restDays = draft.restDays,
                    startDate = draft.startDate.toString(),
                    sortOrder = 0,
                    sync = SyncColumns.new(now),
                )
                writeHabit(habit)
                Outcome.Success(habit)
            }
            else -> Outcome.Failure(error)
        }
    }

    override suspend fun archive(habitId: String) = changeHabit(habitId) { it.copy(archivedAt = clock.millis()) }

    override suspend fun unarchive(habitId: String) = changeHabit(habitId) { it.copy(archivedAt = null) }

    override suspend fun delete(habitId: String) =
        changeHabit(habitId) { it.copy(sync = it.sync.deleted(clock.millis())) }

    override suspend fun checkIn(habitId: String, date: LocalDate, value: Double?): Outcome<HabitEventEntity> =
        storage {
            transaction {
                val habit = dao.getById(habitId)?.takeIf { it.sync.deletedAt == null && it.archivedAt == null }
                val error = when {
                    habit == null -> AppError.NotFound
                    else -> checkInError(habit, date, value)
                }
                when {
                    habit == null || error != null -> Outcome.Failure(error ?: AppError.NotFound)
                    else -> {
                        val amount = value.takeIf { habit.measurementType.hasAmount() }
                        Outcome.Success(append(habitId, date.toString(), HabitEventType.CHECK_IN, amount))
                    }
                }
            }
        }

    override suspend fun undo(eventId: String): Outcome<Unit> = storage {
        transaction {
            val event = dao.getEvent(eventId)
            when {
                event == null || event.eventType == HabitEventType.UNDO || isUndone(event) ->
                    Outcome.Failure(AppError.Validation("event", "nothing to undo"))
                else -> {
                    append(event.habitId, event.localDate, HabitEventType.UNDO, null, correcting = event.id)
                    Outcome.Success(Unit)
                }
            }
        }
    }

    override suspend fun correct(eventId: String, value: Double): Outcome<Unit> = storage {
        transaction {
            val event = dao.getEvent(eventId)
            when {
                event == null || event.eventType != HabitEventType.CHECK_IN || isUndone(event) ->
                    Outcome.Failure(AppError.Validation("event", "only a check-in can be corrected"))
                value <= 0 -> Outcome.Failure(AMOUNT_REQUIRED)
                else -> {
                    append(event.habitId, event.localDate, HabitEventType.CORRECTION, value, correcting = event.id)
                    Outcome.Success(Unit)
                }
            }
        }
    }

    override suspend fun markRest(habitId: String, date: LocalDate): Outcome<Unit> = storage {
        transaction {
            val habit = dao.getById(habitId)?.takeIf { it.sync.deletedAt == null }
            val rested = HabitRules.effective(dao.getEventsOn(habitId, date.toString()))
                .any { it.eventType == HabitEventType.REST }
            when {
                habit == null -> Outcome.Failure(AppError.NotFound)
                rested -> Outcome.Success(Unit)
                else -> {
                    append(habitId, date.toString(), HabitEventType.REST, null)
                    Outcome.Success(Unit)
                }
            }
        }
    }

    private suspend fun checkInError(habit: HabitEntity, date: LocalDate, value: Double?): AppError? {
        val today = LocalDate.now(clock)
        val counted = HabitRules.effective(dao.getEventsOn(habit.id, date.toString()))
        return when {
            date > today -> AppError.Validation("date", "can't check in on a future day")
            date < LocalDate.parse(habit.startDate) -> AppError.Validation("date", "before the habit started")
            habit.measurementType.hasAmount() && (value == null || value <= 0) -> AMOUNT_REQUIRED
            habit.measurementType == HabitMeasurement.BINARY &&
                counted.any { it.eventType == HabitEventType.CHECK_IN } ->
                AppError.Validation("date", "already checked in")
            else -> null
        }
    }

    private suspend fun isUndone(event: HabitEventEntity): Boolean = dao.getEventsOn(event.habitId, event.localDate)
        .any { it.eventType == HabitEventType.UNDO && it.correctsEventId == event.id }

    private suspend fun changeHabit(habitId: String, change: (HabitEntity) -> HabitEntity): Outcome<Unit> = storage {
        transaction {
            val habit = dao.getById(habitId)?.takeIf { it.sync.deletedAt == null }
            if (habit == null) {
                Outcome.Failure(AppError.NotFound)
            } else {
                val changed = change(habit)
                writeHabit(changed.copy(sync = changed.sync.touched(clock.millis())))
                Outcome.Success(Unit)
            }
        }
    }

    private suspend fun writeHabit(habit: HabitEntity) {
        dao.upsert(habit)
        enqueue(ENTITY_TYPE, habit.id, habitPayload(habit))
    }

    private suspend fun append(
        habitId: String,
        date: String,
        type: HabitEventType,
        value: Double?,
        correcting: String? = null,
    ): HabitEventEntity {
        val event = HabitEventEntity(
            id = ids.newId(),
            habitId = habitId,
            localDate = date,
            eventType = type,
            value = value,
            correctsEventId = correcting,
            recordedAt = clock.millis(),
        )
        dao.insertEvent(event)
        enqueue(EVENT_ENTITY_TYPE, event.id, eventPayload(event))
        return event
    }

    private suspend fun enqueue(type: String, id: String, payload: String) {
        outbox.enqueue(
            OutboxEntity(
                entityType = type,
                entityId = id,
                op = OutboxOp.UPSERT,
                payload = payload,
                idempotencyKey = ids.newId(),
                createdAt = clock.millis(),
            ),
        )
    }

    private inline fun <T> storage(block: () -> Outcome<T>): Outcome<T> =
        when (val outcome = runCatchingOutcome({ AppError.Storage(it) }, block)) {
            is Outcome.Success -> outcome.value
            is Outcome.Failure -> outcome
        }

    companion object {
        /** `sync_outbox.entity_type` values; they match the server's resource names. */
        const val ENTITY_TYPE = "habit"
        const val EVENT_ENTITY_TYPE = "habit_event"

        const val MAX_NAME_LENGTH = 200
        private const val DAYS_PER_WEEK = 7
        private val AMOUNT_REQUIRED = AppError.Validation("value", "enter an amount above zero")

        internal fun validate(draft: HabitDraft, name: String): AppError.Validation? = when {
            name.isEmpty() -> AppError.Validation("name", "name is required")
            name.length > MAX_NAME_LENGTH -> AppError.Validation("name", "name is too long")
            (draft.targetValue ?: 1.0) <= 0 -> AppError.Validation("target_value", "target must be above zero")
            else -> scheduleError(draft)
        }

        private fun scheduleError(draft: HabitDraft): AppError.Validation? = when (draft.scheduleType) {
            HabitScheduleType.WEEKDAYS ->
                if ((draft.scheduleDays ?: 0) ==
                    0
                ) {
                    AppError.Validation("schedule_days", "pick at least one day")
                } else {
                    null
                }
            HabitScheduleType.X_PER_WEEK ->
                if (draft.scheduleCount !in 1..DAYS_PER_WEEK) {
                    AppError.Validation("schedule_count", "pick 1 to 7 days a week")
                } else {
                    null
                }
            HabitScheduleType.WEEKLY_TARGET, HabitScheduleType.MONTHLY_TARGET ->
                if (draft.targetValue == null) AppError.Validation("target_value", "a target is required") else null
            HabitScheduleType.CUSTOM -> AppError.Validation("schedule_type", "custom schedules aren't supported yet")
            HabitScheduleType.DAILY -> null
        }

        /** The body the sync engine pushes for a habit. Content is never logged (SECURITY.md). */
        internal fun habitPayload(h: HabitEntity): String = buildJsonObject {
            put("id", JsonPrimitive(h.id))
            put("name", JsonPrimitive(h.name))
            put("description", h.description.json())
            put("measurement_type", JsonPrimitive(h.measurementType.name))
            put("target_value", h.targetValue?.let(::JsonPrimitive) ?: JsonNull)
            put("unit", h.unit.json())
            put("schedule_type", JsonPrimitive(h.scheduleType.name))
            put("schedule_days", h.scheduleDays?.let(::JsonPrimitive) ?: JsonNull)
            put("schedule_count", h.scheduleCount?.let(::JsonPrimitive) ?: JsonNull)
            put("rest_days", h.restDays?.let(::JsonPrimitive) ?: JsonNull)
            put("start_date", JsonPrimitive(h.startDate))
            put("archived_at", h.archivedAt?.let(::JsonPrimitive) ?: JsonNull)
            put("updated_at", JsonPrimitive(h.sync.updatedAt))
            put("deleted_at", h.sync.deletedAt?.let(::JsonPrimitive) ?: JsonNull)
            put("base_version", h.sync.version?.let(::JsonPrimitive) ?: JsonNull)
        }.toString()

        internal fun eventPayload(e: HabitEventEntity): String = buildJsonObject {
            put("id", JsonPrimitive(e.id))
            put("habit_id", JsonPrimitive(e.habitId))
            put("local_date", JsonPrimitive(e.localDate))
            put("event_type", JsonPrimitive(e.eventType.name))
            put("value", e.value?.let(::JsonPrimitive) ?: JsonNull)
            put("corrects_event_id", e.correctsEventId.json())
            put("recorded_at", JsonPrimitive(e.recordedAt))
        }.toString()
    }
}

private fun HabitMeasurement.hasAmount(): Boolean =
    this == HabitMeasurement.NUMERIC || this == HabitMeasurement.DURATION

private fun String?.json(): JsonElement = this?.let(::JsonPrimitive) ?: JsonNull
