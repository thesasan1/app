package com.thesasan1.app.feature.tasks.domain

import com.thesasan1.app.core.common.Outcome
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

/** Task lifecycle (TASK-05). Completion is a status, never a deletion. */
enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    CANCELLED,
    ;

    val isOpen: Boolean get() = this == TODO || this == IN_PROGRESS
}

/** Priority levels, stored as 0–4 (DATABASE.md §3.2). */
@Suppress("MagicNumber") // the levels are the stored values
enum class TaskPriority(val level: Int) {
    NONE(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    URGENT(4),
    ;

    companion object {
        fun of(level: Int): TaskPriority = entries.firstOrNull { it.level == level } ?: NONE
    }
}

/**
 * A task or subtask as the rest of the app sees it. Instants are UTC epoch milliseconds; [dueDate]
 * and [dueTime] are wall-clock values (floating by default, ADR-023).
 */
data class Task(
    val id: String,
    val parentId: String?,
    val projectId: String?,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val priority: TaskPriority,
    val dueDate: LocalDate?,
    val dueTime: LocalTime?,
    val snoozedUntil: Long?,
    val completedAt: Long?,
    val archivedAt: Long?,
    val deletedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val isSubtask: Boolean get() = parentId != null
    val isArchived: Boolean get() = archivedAt != null
    val isInTrash: Boolean get() = deletedAt != null
}

/** What a new task starts with. Only [title] is required. */
data class TaskDraft(
    val title: String,
    val description: String? = null,
    val priority: TaskPriority = TaskPriority.NONE,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val projectId: String? = null,
    val parentId: String? = null,
)

/** One entry of a task's history (TASK-08). [from] and [to] hold the fields that changed, as JSON. */
data class TaskEvent(
    val id: String,
    val taskId: String,
    val type: TaskEventType,
    val from: String?,
    val to: String?,
    val occurredAt: Long,
)

enum class TaskEventType {
    CREATED,
    STATUS_CHANGED,
    RESCHEDULED,
    SNOOZED,
    EDITED,
    COMPLETED,
    REOPENED,
    ARCHIVED,
    DELETED,
    RESTORED,
}

/**
 * Offline-first task storage (ARCHITECTURE.md §2.5). Every change commits the task, a history
 * entry and the sync outbox entries together. Writes fail with `AppError.NotFound` for unknown ids
 * and `AppError.Validation` for invalid input; a change that alters nothing writes nothing.
 */
@Suppress("TooManyFunctions") // one call per user action (TASK-05, TASK-07)
interface TaskRepository {
    /** Open top-level tasks: by due date (undated last), due time, priority, then manual order. */
    fun observeOpen(): Flow<List<Task>>
    fun observeDueOn(date: LocalDate): Flow<List<Task>>

    /** Open top-level tasks due before [today]. */
    fun observeOverdue(today: LocalDate): Flow<List<Task>>
    fun observeByProject(projectId: String): Flow<List<Task>>
    fun observeSubtasks(parentId: String): Flow<List<Task>>
    fun observeCompleted(): Flow<List<Task>>
    fun observeArchived(): Flow<List<Task>>
    fun observeTrash(): Flow<List<Task>>

    /** Non-trashed tasks matching every word of [query] by prefix. Blank queries match nothing. */
    fun search(query: String): Flow<List<Task>>
    fun observeHistory(taskId: String): Flow<List<TaskEvent>>

    suspend fun get(id: String): Outcome<Task>

    /**
     * Creates a task, or a subtask when [TaskDraft.parentId] is set. Subtasks are one level deep:
     * the parent must be a live top-level task.
     */
    suspend fun create(draft: TaskDraft): Outcome<Task>

    /** Changes the title, description and priority (an EDITED event). */
    suspend fun edit(id: String, title: String, description: String?, priority: TaskPriority): Outcome<Task>

    /** Sets or clears the due date and time (RESCHEDULED). A time needs a date. Clears any snooze. */
    suspend fun reschedule(id: String, dueDate: LocalDate?, dueTime: LocalTime?): Outcome<Task>

    /** Hides the task's reminders until [untilMillis] (SNOOZED); null clears the snooze. */
    suspend fun snooze(id: String, untilMillis: Long?): Outcome<Task>

    /**
     * Moves the task to [status]. DONE records COMPLETED and the completion time; leaving DONE
     * records REOPENED and clears it; anything else records STATUS_CHANGED.
     */
    suspend fun setStatus(id: String, status: TaskStatus): Outcome<Task>

    /** Archives (ARCHIVED) or unarchives (RESTORED) the task. */
    suspend fun setArchived(id: String, archived: Boolean): Outcome<Task>

    /** Soft-deletes the task (DELETED). Its subtasks stay with it and come back when it's restored. */
    suspend fun moveToTrash(id: String): Outcome<Task>

    /** Brings a task back from the trash (RESTORED). A subtask whose parent is in the trash can't be restored. */
    suspend fun restoreFromTrash(id: String): Outcome<Task>

    /** Deletes a trashed task, its subtasks and its history for good. The UI confirms first. */
    suspend fun deletePermanently(id: String): Outcome<Unit>

    /** Removes tasks that have been in the trash longer than the retention window. Returns the count. */
    suspend fun purgeExpiredTrash(): Outcome<Int>
}
