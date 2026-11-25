package com.thesasan1.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.thesasan1.app.core.database.SearchText
import com.thesasan1.app.core.database.entity.TaskEntity
import com.thesasan1.app.core.database.entity.TaskHistoryEntity
import kotlinx.coroutines.flow.Flow

private const val OPEN =
    "status IN ('TODO', 'IN_PROGRESS') AND deleted_at IS NULL AND archived_at IS NULL"
private const val OPEN_ORDER =
    "due_date IS NULL, due_date, due_time IS NULL, due_time, priority DESC, sort_order, created_at"

/**
 * Tasks, their full-text index and their history. Always write through [upsert] so `tasks_fts`
 * stays in step. Deleting is soft (`sync.deleted(now)`) until [deletePermanently] or the purge.
 *
 * "Open" means TODO or IN_PROGRESS, not archived and not trashed. Open lists sort by due date
 * (undated last), then due time (untimed last), then priority (highest first), then manual order.
 */
@Dao
@Suppress("TooManyFunctions") // one query per list the task screens show
interface TaskDao {
    /** Writes the task and re-indexes it, in one transaction. */
    @Transaction
    suspend fun upsert(task: TaskEntity) {
        upsertRow(task)
        deleteIndex(task.id)
        insertIndex(
            taskId = task.id,
            title = SearchText.normalize(task.title),
            description = SearchText.normalize(task.description.orEmpty()),
        )
    }

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    /** Open top-level tasks (subtasks appear under their parent). */
    @Query("SELECT * FROM tasks WHERE $OPEN AND parent_id IS NULL ORDER BY $OPEN_ORDER")
    fun observeOpen(): Flow<List<TaskEntity>>

    /** Open top-level tasks due on [date] (ISO local date). */
    @Query("SELECT * FROM tasks WHERE $OPEN AND parent_id IS NULL AND due_date = :date ORDER BY $OPEN_ORDER")
    fun observeDueOn(date: String): Flow<List<TaskEntity>>

    /** Open top-level tasks due before [today] (ISO local date), oldest first. */
    @Query("SELECT * FROM tasks WHERE $OPEN AND parent_id IS NULL AND due_date < :today ORDER BY $OPEN_ORDER")
    fun observeOverdue(today: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE $OPEN AND parent_id IS NULL AND project_id = :projectId ORDER BY $OPEN_ORDER")
    fun observeByProject(projectId: String): Flow<List<TaskEntity>>

    /** Subtasks of [parentId] in any status (not trashed), in manual order. */
    @Query("SELECT * FROM tasks WHERE parent_id = :parentId AND deleted_at IS NULL ORDER BY sort_order, created_at")
    fun observeSubtasks(parentId: String): Flow<List<TaskEntity>>

    /** Completed top-level tasks, most recently completed first. */
    @Query(
        "SELECT * FROM tasks WHERE status = 'DONE' AND parent_id IS NULL AND deleted_at IS NULL " +
            "AND archived_at IS NULL ORDER BY completed_at DESC",
    )
    fun observeCompleted(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deleted_at IS NULL AND archived_at IS NOT NULL ORDER BY archived_at DESC")
    fun observeArchived(): Flow<List<TaskEntity>>

    /** The trash, most recently deleted first. */
    @Query("SELECT * FROM tasks WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun observeTrash(): Flow<List<TaskEntity>>

    /** Non-trashed tasks matching [matchQuery], which must come from [SearchText.matchQuery]. */
    @Query(
        "SELECT tasks.* FROM tasks JOIN tasks_fts ON tasks_fts.task_id = tasks.id " +
            "WHERE tasks_fts MATCH :matchQuery AND tasks.deleted_at IS NULL ORDER BY tasks.updated_at DESC",
    )
    fun search(matchQuery: String): Flow<List<TaskEntity>>

    /** Appends a history row. History is never updated or deleted on its own. */
    @Insert
    suspend fun insertHistory(event: TaskHistoryEntity)

    @Query("SELECT * FROM task_history WHERE task_id = :taskId ORDER BY occurred_at, id")
    fun observeHistory(taskId: String): Flow<List<TaskHistoryEntity>>

    /** Hard-deletes a task with its subtasks, their index rows and their history. */
    @Transaction
    suspend fun deletePermanently(id: String) {
        val ids = subtaskIds(id) + id
        deleteIndexFor(ids)
        deleteHistoryFor(ids)
        deleteRows(ids)
    }

    /** Hard-deletes tasks trashed before [cutoffMillis], with their subtasks, index and history. */
    @Transaction
    suspend fun purgeDeletedBefore(cutoffMillis: Long): Int {
        val expired = deletedBeforeIds(cutoffMillis)
        expired.forEach { deletePermanently(it) }
        return expired.size
    }

    @Upsert
    suspend fun upsertRow(task: TaskEntity)

    @Query("DELETE FROM tasks_fts WHERE task_id = :taskId")
    suspend fun deleteIndex(taskId: String)

    @Query("INSERT INTO tasks_fts (task_id, title, description) VALUES (:taskId, :title, :description)")
    suspend fun insertIndex(taskId: String, title: String, description: String)

    @Query("SELECT id FROM tasks WHERE parent_id = :parentId")
    suspend fun subtaskIds(parentId: String): List<String>

    @Query("SELECT id FROM tasks WHERE deleted_at IS NOT NULL AND deleted_at < :cutoffMillis")
    suspend fun deletedBeforeIds(cutoffMillis: Long): List<String>

    @Query("DELETE FROM tasks_fts WHERE task_id IN (:ids)")
    suspend fun deleteIndexFor(ids: List<String>)

    @Query("DELETE FROM task_history WHERE task_id IN (:ids)")
    suspend fun deleteHistoryFor(ids: List<String>)

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteRows(ids: List<String>)
}
