package com.thesasan1.app.feature.tasks.data

import com.thesasan1.app.core.common.AppError
import com.thesasan1.app.core.common.IdGenerator
import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.core.common.runCatchingOutcome
import com.thesasan1.app.core.database.SearchText
import com.thesasan1.app.core.database.TransactionRunner
import com.thesasan1.app.core.database.dao.OutboxDao
import com.thesasan1.app.core.database.dao.TaskDao
import com.thesasan1.app.core.database.entity.OutboxEntity
import com.thesasan1.app.core.database.entity.OutboxOp
import com.thesasan1.app.core.database.entity.SyncColumns
import com.thesasan1.app.core.database.entity.TaskEntity
import com.thesasan1.app.core.database.entity.TaskEventType as StoredEventType
import com.thesasan1.app.core.database.entity.TaskHistoryEntity
import com.thesasan1.app.core.database.entity.TaskStatus as StoredStatus
import com.thesasan1.app.feature.tasks.domain.Task
import com.thesasan1.app.feature.tasks.domain.TaskDraft
import com.thesasan1.app.feature.tasks.domain.TaskEvent
import com.thesasan1.app.feature.tasks.domain.TaskEventType
import com.thesasan1.app.feature.tasks.domain.TaskPriority
import com.thesasan1.app.feature.tasks.domain.TaskRepository
import com.thesasan1.app.feature.tasks.domain.TaskStatus
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Room-backed [TaskRepository]. Each change runs in one transaction: the task row (and its search
 * index), an append-only history row, and outbox entries for both (DATABASE.md §5, ADR-023).
 */
@Suppress("TooManyFunctions") // mirrors TaskRepository
class OfflineTaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val outboxDao: OutboxDao,
    private val transaction: TransactionRunner,
    private val ids: IdGenerator,
    private val clock: Clock,
) : TaskRepository {

    override fun observeOpen(): Flow<List<Task>> = taskDao.observeOpen().toTasks()

    override fun observeDueOn(date: LocalDate): Flow<List<Task>> = taskDao.observeDueOn(date.toString()).toTasks()

    override fun observeOverdue(today: LocalDate): Flow<List<Task>> = taskDao.observeOverdue(today.toString()).toTasks()

    override fun observeByProject(projectId: String): Flow<List<Task>> = taskDao.observeByProject(projectId).toTasks()

    override fun observeSubtasks(parentId: String): Flow<List<Task>> = taskDao.observeSubtasks(parentId).toTasks()

    override fun observeCompleted(): Flow<List<Task>> = taskDao.observeCompleted().toTasks()

    override fun observeArchived(): Flow<List<Task>> = taskDao.observeArchived().toTasks()

    override fun observeTrash(): Flow<List<Task>> = taskDao.observeTrash().toTasks()

    override fun search(query: String): Flow<List<Task>> {
        val match = SearchText.matchQuery(query) ?: return flowOf(emptyList())
        return taskDao.search(match).toTasks()
    }

    override fun observeHistory(taskId: String): Flow<List<TaskEvent>> =
        taskDao.observeHistory(taskId).map { rows -> rows.map { it.toEvent() } }

    override suspend fun get(id: String): Outcome<Task> = storage {
        taskDao.getById(id)?.toTask()?.let { Outcome.Success(it) } ?: Outcome.Failure(AppError.NotFound)
    }

    override suspend fun create(draft: TaskDraft): Outcome<Task> = storage {
        transaction tx@{
            val title = draft.title.trim()
            validateTitle(title)?.let { return@tx Outcome.Failure(it) }
            if (draft.dueTime != null && draft.dueDate == null) return@tx Outcome.Failure(TIME_WITHOUT_DATE)
            var projectId = draft.projectId
            if (draft.parentId != null) {
                val parent = taskDao.getById(draft.parentId)
                    ?: return@tx Outcome.Failure(AppError.Validation("parent_id", "parent task not found"))
                if (parent.parentId != null) {
                    return@tx Outcome.Failure(AppError.Validation("parent_id", "subtasks can't have subtasks"))
                }
                if (parent.sync.deletedAt != null) {
                    return@tx Outcome.Failure(AppError.Validation("parent_id", "parent task is in the trash"))
                }
                projectId = projectId ?: parent.projectId
            }
            val now = clock.millis()
            val entity = TaskEntity(
                id = ids.newId(),
                parentId = draft.parentId,
                projectId = projectId,
                title = title,
                description = draft.description.cleanText(),
                priority = draft.priority.level,
                dueDate = draft.dueDate?.toString(),
                dueTime = draft.dueTime?.toString(),
                sortOrder = now.toString().padStart(SORT_KEY_WIDTH, '0'),
                sync = SyncColumns.new(now),
            )
            writeTask(entity)
            record(entity.id, StoredEventType.CREATED, from = null, to = snapshot(entity))
            Outcome.Success(entity.toTask())
        }
    }

    override suspend fun edit(id: String, title: String, description: String?, priority: TaskPriority): Outcome<Task> {
        val cleanTitle = title.trim()
        validateTitle(cleanTitle)?.let { return Outcome.Failure(it) }
        return update(id) {
            Change(
                copy(title = cleanTitle, description = description.cleanText(), priority = priority.level),
                StoredEventType.EDITED,
            )
        }
    }

    override suspend fun reschedule(id: String, dueDate: LocalDate?, dueTime: LocalTime?): Outcome<Task> {
        if (dueTime != null && dueDate == null) return Outcome.Failure(TIME_WITHOUT_DATE)
        return update(id) {
            Change(
                copy(dueDate = dueDate?.toString(), dueTime = dueTime?.toString(), snoozedUntil = null),
                StoredEventType.RESCHEDULED,
            )
        }
    }

    override suspend fun snooze(id: String, untilMillis: Long?): Outcome<Task> =
        update(id) { Change(copy(snoozedUntil = untilMillis), StoredEventType.SNOOZED) }

    override suspend fun setStatus(id: String, status: TaskStatus): Outcome<Task> = update(id) {
        val target = StoredStatus.valueOf(status.name)
        when {
            target == this.status -> Change(this, StoredEventType.STATUS_CHANGED)
            target == StoredStatus.DONE ->
                Change(copy(status = target, completedAt = clock.millis()), StoredEventType.COMPLETED)
            this.status == StoredStatus.DONE ->
                Change(copy(status = target, completedAt = null), StoredEventType.REOPENED)
            else -> Change(copy(status = target), StoredEventType.STATUS_CHANGED)
        }
    }

    override suspend fun setArchived(id: String, archived: Boolean): Outcome<Task> = update(id) {
        when {
            archived && archivedAt == null -> Change(copy(archivedAt = clock.millis()), StoredEventType.ARCHIVED)
            !archived && archivedAt != null -> Change(copy(archivedAt = null), StoredEventType.RESTORED)
            else -> Change(this, StoredEventType.EDITED)
        }
    }

    override suspend fun moveToTrash(id: String): Outcome<Task> = update(id, touch = false) {
        if (sync.deletedAt != null) {
            Change(this, StoredEventType.DELETED)
        } else {
            Change(copy(sync = sync.deleted(clock.millis())), StoredEventType.DELETED)
        }
    }

    override suspend fun restoreFromTrash(id: String): Outcome<Task> = update(id) {
        val parent = parentId?.let { taskDao.getById(it) }
        if (parent?.sync?.deletedAt != null) {
            Reject(AppError.Validation("parent_id", "restore the parent task first"))
        } else {
            Change(copy(sync = sync.copy(deletedAt = null)), StoredEventType.RESTORED)
        }
    }

    override suspend fun deletePermanently(id: String): Outcome<Unit> = storage {
        transaction tx@{
            val task = taskDao.getById(id) ?: return@tx Outcome.Failure(AppError.NotFound)
            if (task.sync.deletedAt == null) {
                return@tx Outcome.Failure(
                    AppError.Validation(field = "deleted_at", reason = "only tasks in the trash can be deleted"),
                )
            }
            val removed = taskDao.subtaskIds(id) + id
            taskDao.deletePermanently(id)
            removed.forEach { outboxDao.enqueue(outboxEntry(ENTITY_TYPE, it, OutboxOp.DELETE, payload = null)) }
            Outcome.Success(Unit)
        }
    }

    override suspend fun purgeExpiredTrash(): Outcome<Int> = storage {
        Outcome.Success(taskDao.purgeDeletedBefore(clock.millis() - TRASH_RETENTION.toMillis()))
    }

    /** The result of a change function: the task as it should be, with its event, or a rejection. */
    private sealed interface Step

    private data class Change(val entity: TaskEntity, val event: StoredEventType) : Step

    private data class Reject(val error: AppError) : Step

    /**
     * Applies [change] to the stored task. A change that leaves the task as it was writes nothing,
     * records no history and queues nothing. [touch] bumps `updated_at` and marks the row pending.
     */
    private suspend fun update(
        id: String,
        touch: Boolean = true,
        change: suspend TaskEntity.() -> Step,
    ): Outcome<Task> = storage {
        transaction tx@{
            val current = taskDao.getById(id) ?: return@tx Outcome.Failure(AppError.NotFound)
            when (val step = current.change()) {
                is Reject -> Outcome.Failure(step.error)
                is Change -> {
                    if (step.entity == current) return@tx Outcome.Success(current.toTask())
                    val updated = if (touch) {
                        step.entity.copy(sync = step.entity.sync.touched(clock.millis()))
                    } else {
                        step.entity
                    }
                    writeTask(updated)
                    val (from, to) = diff(snapshot(current), snapshot(updated))
                    record(id, step.event, from, to)
                    Outcome.Success(updated.toTask())
                }
            }
        }
    }

    private suspend fun writeTask(entity: TaskEntity) {
        taskDao.upsert(entity)
        outboxDao.enqueue(outboxEntry(ENTITY_TYPE, entity.id, OutboxOp.UPSERT, payload = syncPayload(entity)))
    }

    private suspend fun record(taskId: String, type: StoredEventType, from: JsonObject?, to: JsonObject?) {
        val event = TaskHistoryEntity(
            id = ids.newId(),
            taskId = taskId,
            eventType = type,
            fromValue = from?.toString(),
            toValue = to?.toString(),
            occurredAt = clock.millis(),
        )
        taskDao.insertHistory(event)
        outboxDao.enqueue(outboxEntry(HISTORY_ENTITY_TYPE, event.id, OutboxOp.UPSERT, payload = historyPayload(event)))
    }

    private fun outboxEntry(type: String, entityId: String, op: OutboxOp, payload: String?) = OutboxEntity(
        entityType = type,
        entityId = entityId,
        op = op,
        payload = payload,
        idempotencyKey = ids.newId(),
        createdAt = clock.millis(),
    )

    private inline fun <T> storage(block: () -> Outcome<T>): Outcome<T> =
        when (val outcome = runCatchingOutcome({ AppError.Storage(it) }, block)) {
            is Outcome.Success -> outcome.value
            is Outcome.Failure -> outcome
        }

    private fun Flow<List<TaskEntity>>.toTasks(): Flow<List<Task>> = map { rows -> rows.map { it.toTask() } }

    companion object {
        /** `sync_outbox.entity_type` values; they match the server's resource names. */
        const val ENTITY_TYPE = "task"
        const val HISTORY_ENTITY_TYPE = "task_event"

        /** Trash retention (DATABASE.md §6), the same as notes. */
        val TRASH_RETENTION: Duration = Duration.ofDays(30)

        private const val SORT_KEY_WIDTH = 16
        private val TIME_WITHOUT_DATE = AppError.Validation("due_time", "a due time needs a due date")

        internal fun validateTitle(title: String): AppError.Validation? = when {
            title.isEmpty() -> AppError.Validation("title", "title is required")
            title.length > TaskEntity.MAX_TITLE_LENGTH -> AppError.Validation("title", "title is too long")
            else -> null
        }

        /** The fields history tracks, as JSON. */
        internal fun snapshot(entity: TaskEntity): JsonObject = buildJsonObject {
            put("title", entity.title)
            put("description", entity.description.json())
            put("status", entity.status.name)
            put("priority", entity.priority)
            put("due_date", entity.dueDate.json())
            put("due_time", entity.dueTime.json())
            put("snoozed_until", entity.snoozedUntil.json())
            put("completed_at", entity.completedAt.json())
            put("archived_at", entity.archivedAt.json())
            put("deleted_at", entity.sync.deletedAt.json())
            put("parent_id", entity.parentId.json())
            put("project_id", entity.projectId.json())
        }

        /** Only the fields that differ, before and after. */
        internal fun diff(before: JsonObject, after: JsonObject): Pair<JsonObject, JsonObject> {
            val keys = after.keys.filter { before[it] != after[it] }
            return JsonObject(keys.associateWith { before.getValue(it) }) to
                JsonObject(keys.associateWith { after.getValue(it) })
        }

        /** The body the sync engine pushes for a task. Content is never logged (SECURITY.md). */
        internal fun syncPayload(entity: TaskEntity): String = JsonObject(
            snapshot(entity) + mapOf(
                "id" to JsonPrimitive(entity.id),
                "time_zone_mode" to JsonPrimitive(entity.timeZoneMode.name),
                "time_zone_id" to entity.timeZoneId.json(),
                "recurrence_rule" to entity.recurrenceRule.json(),
                "recurrence_parent_id" to entity.recurrenceParentId.json(),
                "sort_order" to JsonPrimitive(entity.sortOrder),
                "created_at" to JsonPrimitive(entity.sync.createdAt),
                "updated_at" to JsonPrimitive(entity.sync.updatedAt),
                "base_version" to entity.sync.version.json(),
            ),
        ).toString()

        internal fun historyPayload(event: TaskHistoryEntity): String = buildJsonObject {
            put("id", event.id)
            put("task_id", event.taskId)
            put("event_type", event.eventType.name)
            put("from_value", event.fromValue.json())
            put("to_value", event.toValue.json())
            put("occurred_at", event.occurredAt)
        }.toString()
    }
}

private fun String?.json(): JsonElement = this?.let(::JsonPrimitive) ?: JsonNull

private fun Long?.json(): JsonElement = this?.let(::JsonPrimitive) ?: JsonNull

/** Trimmed; blank is stored as null. */
private fun String?.cleanText(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

internal fun TaskEntity.toTask() = Task(
    id = id,
    parentId = parentId,
    projectId = projectId,
    title = title,
    description = description,
    status = TaskStatus.valueOf(status.name),
    priority = TaskPriority.of(priority),
    dueDate = dueDate?.let(LocalDate::parse),
    dueTime = dueTime?.let(LocalTime::parse),
    snoozedUntil = snoozedUntil,
    completedAt = completedAt,
    archivedAt = archivedAt,
    deletedAt = sync.deletedAt,
    createdAt = sync.createdAt,
    updatedAt = sync.updatedAt,
)

internal fun TaskHistoryEntity.toEvent() = TaskEvent(
    id = id,
    taskId = taskId,
    type = TaskEventType.valueOf(eventType.name),
    from = fromValue,
    to = toValue,
    occurredAt = occurredAt,
)
