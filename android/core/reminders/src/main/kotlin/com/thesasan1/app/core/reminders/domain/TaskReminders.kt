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
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A reminder on a task, set relative to its due date and time (REM-01). */
data class TaskReminder(val id: String, val offsetMinutes: Int, val repeatUntilDone: Boolean)

/** The reminders the task editor offers (minutes relative to the due time; negative is before). */
@Suppress("MagicNumber") // the presets are the data
val REMINDER_PRESETS: List<Int> = listOf(0, -10, -60, -24 * 60)

/** Units for a custom reminder offset, in minutes. */
@Suppress("MagicNumber") // minutes per unit
enum class ReminderUnit(val minutes: Int) {
    MINUTES(1),
    HOURS(60),
    DAYS(24 * 60),
}

/** The longest a reminder may come before its task is due. */
const val MAX_REMINDER_LEAD_MINUTES = 30 * 24 * 60

/** [amount] [unit]s before the due time as an offset (negative minutes), or null when out of range. */
fun customReminderOffset(amount: Int, unit: ReminderUnit): Int? {
    val minutes = amount.toLong() * unit.minutes
    return if (minutes in 1..MAX_REMINDER_LEAD_MINUTES) -minutes.toInt() else null
}

/** Task reminders as the editor sees them. Alarms follow on their own (see [ReminderRebuildTrigger]). */
interface TaskReminderRepository {
    fun observe(taskId: String): Flow<List<TaskReminder>>

    /** Adds a reminder at [offsetMinutes]; does nothing if the task already has one there. */
    suspend fun add(taskId: String, offsetMinutes: Int)

    suspend fun setRepeatUntilDone(reminderId: String, repeat: Boolean)

    suspend fun remove(reminderId: String)
}

/**
 * Room-backed [TaskReminderRepository]. Each change writes the reminder and its outbox entry in one
 * transaction (DATABASE.md §5); removal is a soft delete so it can sync.
 */
class OfflineTaskReminderRepository @Inject constructor(
    private val dao: ReminderDao,
    private val outbox: OutboxDao,
    private val transaction: TransactionRunner,
    private val ids: IdGenerator,
    private val clock: Clock,
) : TaskReminderRepository {

    override fun observe(taskId: String): Flow<List<TaskReminder>> =
        dao.observeForTarget(ReminderTargetType.TASK, taskId).map { rows ->
            rows.filter { it.sync.deletedAt == null && it.offsetMinutes != null }
                .map { TaskReminder(it.id, checkNotNull(it.offsetMinutes), it.repeatUntilDone) }
                .sortedBy { it.offsetMinutes }
        }

    override suspend fun add(taskId: String, offsetMinutes: Int) = transaction {
        val existing = dao.getForTarget(ReminderTargetType.TASK, taskId)
        if (existing.none { it.sync.deletedAt == null && it.offsetMinutes == offsetMinutes }) {
            write(
                ReminderEntity(
                    id = ids.newId(),
                    targetType = ReminderTargetType.TASK,
                    targetId = taskId,
                    offsetMinutes = offsetMinutes,
                    sync = SyncColumns.new(clock.millis()),
                ),
            )
        }
    }

    override suspend fun setRepeatUntilDone(reminderId: String, repeat: Boolean) = transaction {
        val reminder = dao.getById(reminderId)
        if (reminder != null && reminder.repeatUntilDone != repeat) {
            write(reminder.copy(repeatUntilDone = repeat, sync = reminder.sync.touched(clock.millis())))
        }
    }

    override suspend fun remove(reminderId: String) = transaction {
        val reminder = dao.getById(reminderId)
        if (reminder != null && reminder.sync.deletedAt == null) {
            write(reminder.copy(sync = reminder.sync.deleted(clock.millis())))
        }
    }

    private suspend fun write(reminder: ReminderEntity) {
        dao.upsert(reminder)
        outbox.enqueue(
            OutboxEntity(
                entityType = ENTITY_TYPE,
                entityId = reminder.id,
                op = OutboxOp.UPSERT,
                payload = syncPayload(reminder),
                idempotencyKey = ids.newId(),
                createdAt = clock.millis(),
            ),
        )
    }

    companion object {
        /** `sync_outbox.entity_type`; matches the server's resource name. */
        const val ENTITY_TYPE = "reminder"

        /** Ids, numbers and enums only, so it is built by hand without escaping (no free text). */
        internal fun syncPayload(r: ReminderEntity): String = buildString {
            append("{\"id\":\"").append(r.id).append('"')
            append(",\"targetType\":\"").append(r.targetType.name).append('"')
            r.targetId?.let { append(",\"targetId\":\"").append(it).append('"') }
            append(",\"triggerType\":\"").append(r.triggerType.name).append('"')
            r.offsetMinutes?.let { append(",\"offsetMinutes\":").append(it) }
            append(",\"repeatUntilDone\":").append(r.repeatUntilDone)
            append(",\"isEnabled\":").append(r.isEnabled)
            append(",\"updatedAt\":").append(r.sync.updatedAt)
            r.sync.deletedAt?.let { append(",\"deletedAt\":").append(it) }
            append('}')
        }
    }
}
