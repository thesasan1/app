package com.thesasan1.app.core.reminders.domain

import com.thesasan1.app.core.common.IdGenerator
import com.thesasan1.app.core.database.TransactionRunner
import com.thesasan1.app.core.database.dao.OutboxDao
import com.thesasan1.app.core.database.dao.ReminderDao
import com.thesasan1.app.core.database.entity.OutboxEntity
import com.thesasan1.app.core.database.entity.OutboxOp
import com.thesasan1.app.core.database.entity.ReminderEntity
import com.thesasan1.app.core.database.entity.ReminderTargetType
import com.thesasan1.app.core.database.entity.SyncColumns
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Minutes in a day: a habit reminder's time of day is 0 until this. */
const val MINUTES_PER_DAY = 24 * 60

/**
 * A habit's daily reminder (ADR-032): rings at [time] on each day the habit is due and not yet done.
 * Stored as an offset reminder, [ReminderEntity.offsetMinutes] minutes after midnight.
 */
data class HabitReminder(val id: String, val time: LocalTime)

/** One reminder per habit, set or cleared from the habit's editor. Alarms follow on their own. */
interface HabitReminderRepository {
    fun observe(habitId: String): Flow<HabitReminder?>

    /** Sets the habit's reminder to [time], or removes it when [time] is null. */
    suspend fun set(habitId: String, time: LocalTime?)
}

class OfflineHabitReminderRepository @Inject constructor(
    private val dao: ReminderDao,
    private val outbox: OutboxDao,
    private val transaction: TransactionRunner,
    private val ids: IdGenerator,
    private val clock: Clock,
) : HabitReminderRepository {

    override fun observe(habitId: String): Flow<HabitReminder?> =
        dao.observeForTarget(ReminderTargetType.HABIT, habitId).map { rows ->
            rows.live().firstOrNull()?.let { HabitReminder(it.id, LocalTime.MIN.plusMinutes(it.minutes().toLong())) }
        }

    override suspend fun set(habitId: String, time: LocalTime?) = transaction {
        val now = clock.millis()
        val live = dao.getForTarget(ReminderTargetType.HABIT, habitId).live()
        val minutes = time?.let { it.hour * MINUTES_IN_HOUR + it.minute }
        val keep = live.firstOrNull()
        // Extra rows (from a future sync merge, say) go, so a habit has one reminder.
        live.drop(1).forEach { write(it.copy(sync = it.sync.deleted(now))) }
        when {
            minutes == null -> keep?.let { write(it.copy(sync = it.sync.deleted(now))) }
            keep == null -> write(
                ReminderEntity(
                    id = ids.newId(),
                    targetType = ReminderTargetType.HABIT,
                    targetId = habitId,
                    offsetMinutes = minutes,
                    sync = SyncColumns.new(now),
                ),
            )
            keep.offsetMinutes != minutes -> write(keep.copy(offsetMinutes = minutes, sync = keep.sync.touched(now)))
        }
        Unit
    }

    private fun List<ReminderEntity>.live() =
        filter { it.sync.deletedAt == null && it.offsetMinutes != null }.sortedBy { it.sync.createdAt }

    private fun ReminderEntity.minutes(): Int = (offsetMinutes ?: 0).coerceIn(0, MINUTES_PER_DAY - 1)

    private suspend fun write(reminder: ReminderEntity) {
        dao.upsert(reminder)
        outbox.enqueue(
            OutboxEntity(
                entityType = OfflineTaskReminderRepository.ENTITY_TYPE,
                entityId = reminder.id,
                op = OutboxOp.UPSERT,
                payload = OfflineTaskReminderRepository.syncPayload(reminder),
                idempotencyKey = ids.newId(),
                createdAt = clock.millis(),
            ),
        )
    }

    private companion object {
        const val MINUTES_IN_HOUR = 60
    }
}
