package com.thesasan1.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey

/** Task lifecycle states (DATABASE.md §3.2). Completion is a status, never a deletion (TASK-05). */
enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    CANCELLED,
}

/** How a task's due time is read across time zones (ARCHITECTURE.md, time handling). */
enum class TimeZoneMode {
    /** Wall-clock time wherever the user is ("9:00 wherever I am"). */
    FLOATING,

    /** A fixed instant in [TaskEntity.timeZoneId]. */
    FIXED,
}

/**
 * A task or subtask (DATABASE.md §3.2, TASK-01).
 *
 * - [dueDate] is an ISO-8601 local date (`2026-11-25`) and [dueTime] an ISO local time (`09:30`).
 *   Both are text, so they sort correctly and stay readable in exports.
 * - [priority] runs 0 (none) to 4 (urgent); see [PRIORITY_NONE] … [PRIORITY_URGENT].
 * - [parentId], [projectId] and [recurrenceParentId] are plain columns, not foreign keys: sync can
 *   deliver a subtask before its parent, and deletes are soft anyway.
 * - [sortOrder] is a fractional index for manual ordering (LexoRank-style text).
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["status", "due_date"]),
        Index("project_id"),
        Index("parent_id"),
        Index("recurrence_parent_id"),
        Index("updated_at"),
    ],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "parent_id") val parentId: String? = null,
    @ColumnInfo(name = "project_id") val projectId: String? = null,
    val title: String,
    val description: String? = null,
    @ColumnInfo(defaultValue = "TODO") val status: TaskStatus = TaskStatus.TODO,
    @ColumnInfo(defaultValue = "0") val priority: Int = PRIORITY_NONE,
    @ColumnInfo(name = "due_date") val dueDate: String? = null,
    @ColumnInfo(name = "due_time") val dueTime: String? = null,
    @ColumnInfo(
        name = "time_zone_mode",
        defaultValue = "FLOATING",
    ) val timeZoneMode: TimeZoneMode = TimeZoneMode.FLOATING,
    @ColumnInfo(name = "time_zone_id") val timeZoneId: String? = null,
    @ColumnInfo(name = "recurrence_rule") val recurrenceRule: String? = null,
    @ColumnInfo(name = "recurrence_parent_id") val recurrenceParentId: String? = null,
    @ColumnInfo(name = "snoozed_until") val snoozedUntil: Long? = null,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    @ColumnInfo(name = "archived_at") val archivedAt: Long? = null,
    @ColumnInfo(name = "sort_order", defaultValue = "''") val sortOrder: String = "",
    @Embedded val sync: SyncColumns,
) {
    companion object {
        const val PRIORITY_NONE = 0
        const val PRIORITY_LOW = 1
        const val PRIORITY_MEDIUM = 2
        const val PRIORITY_HIGH = 3
        const val PRIORITY_URGENT = 4
        const val MAX_TITLE_LENGTH = 500
    }
}

/** Full-text index over tasks, kept in step by [com.thesasan1.app.core.database.dao.TaskDao] (like notes, ADR-022). */
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61, notIndexed = ["task_id"])
@Entity(tableName = "tasks_fts")
data class TaskFtsEntity(@ColumnInfo(name = "task_id") val taskId: String, val title: String, val description: String)

/** What a [TaskHistoryEntity] row records (DATABASE.md §3.3, TASK-08). */
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
 * Append-only audit of a task's changes. [fromValue] and [toValue] are small JSON objects with the
 * fields that changed. History is local-first and syncs like any other row; it's never edited.
 */
@Entity(
    tableName = "task_history",
    indices = [Index(value = ["task_id", "occurred_at"])],
)
data class TaskHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "task_id") val taskId: String,
    @ColumnInfo(name = "event_type") val eventType: TaskEventType,
    @ColumnInfo(name = "from_value") val fromValue: String? = null,
    @ColumnInfo(name = "to_value") val toValue: String? = null,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "device_id") val deviceId: String? = null,
)
