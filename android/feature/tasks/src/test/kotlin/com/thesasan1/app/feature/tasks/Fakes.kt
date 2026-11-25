package com.thesasan1.app.feature.tasks

import com.thesasan1.app.core.common.IdGenerator
import com.thesasan1.app.core.database.TransactionRunner
import com.thesasan1.app.core.database.dao.OutboxDao
import com.thesasan1.app.core.database.dao.TaskDao
import com.thesasan1.app.core.database.entity.OutboxEntity
import com.thesasan1.app.core.database.entity.TaskEntity
import com.thesasan1.app.core.database.entity.TaskHistoryEntity
import com.thesasan1.app.core.database.entity.TaskStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [TaskDao]. Room's own behaviour (SQL, ordering, FTS) is covered by TaskDaoTest in :core:database. */
@Suppress("TooManyFunctions")
class FakeTaskDao : TaskDao {
    val rows = MutableStateFlow<Map<String, TaskEntity>>(emptyMap())
    val history = MutableStateFlow<List<TaskHistoryEntity>>(emptyList())

    private val open: (TaskEntity) -> Boolean = {
        it.status in setOf(TaskStatus.TODO, TaskStatus.IN_PROGRESS) &&
            it.sync.deletedAt == null &&
            it.archivedAt == null
    }

    override suspend fun getById(id: String) = rows.value[id]
    override fun observeOpen() = select { open(it) && it.parentId == null }
    override fun observeDueOn(date: String) = select { open(it) && it.parentId == null && it.dueDate == date }
    override fun observeOverdue(today: String) =
        select { open(it) && it.parentId == null && (it.dueDate ?: "") < today && it.dueDate != null }
    override fun observeByProject(projectId: String) =
        select { open(it) && it.parentId == null && it.projectId == projectId }
    override fun observeSubtasks(parentId: String) = select { it.parentId == parentId && it.sync.deletedAt == null }
    override fun observeCompleted() = select {
        it.status == TaskStatus.DONE && it.parentId == null && it.sync.deletedAt == null && it.archivedAt == null
    }
    override fun observeArchived() = select { it.sync.deletedAt == null && it.archivedAt != null }
    override fun observeTrash() = select { it.sync.deletedAt != null }
    override fun search(matchQuery: String) = select { it.sync.deletedAt == null }

    override suspend fun insertHistory(event: TaskHistoryEntity) {
        history.value += event
    }

    override fun observeHistory(taskId: String): Flow<List<TaskHistoryEntity>> =
        history.map { all -> all.filter { it.taskId == taskId }.sortedBy { it.occurredAt } }

    override suspend fun upsertRow(task: TaskEntity) {
        rows.value += task.id to task
    }

    override suspend fun deleteIndex(taskId: String) = Unit
    override suspend fun insertIndex(taskId: String, title: String, description: String) = Unit
    override suspend fun subtaskIds(parentId: String) =
        rows.value.values.filter { it.parentId == parentId }.map { it.id }
    override suspend fun deletedBeforeIds(cutoffMillis: Long) =
        rows.value.values.filter { (it.sync.deletedAt ?: Long.MAX_VALUE) < cutoffMillis }.map { it.id }
    override suspend fun deleteIndexFor(ids: List<String>) = Unit
    override suspend fun deleteHistoryFor(ids: List<String>) {
        history.value = history.value.filterNot { it.taskId in ids }
    }
    override suspend fun deleteRows(ids: List<String>) {
        rows.value -= ids.toSet()
    }

    private fun select(filter: (TaskEntity) -> Boolean) =
        rows.map { all -> all.values.filter(filter).sortedBy { it.sortOrder } }
}

class FakeOutboxDao : OutboxDao {
    val entries = mutableListOf<OutboxEntity>()

    override suspend fun enqueue(entry: OutboxEntity): Long {
        entries += entry.copy(seq = entries.size + 1L)
        return entries.size.toLong()
    }

    override suspend fun nextBatch(limit: Int) = entries.take(limit)
    override suspend fun recordFailure(seq: Long, errorCode: String) = Unit
    override suspend fun acknowledge(seqs: List<Long>) = 0
    override fun observePendingCount(): Flow<Int> = MutableStateFlow(entries.size)
}

object DirectTransactionRunner : TransactionRunner {
    override suspend fun <R> invoke(block: suspend () -> R): R = block()
}

class SequentialIds : IdGenerator {
    private var next = 0
    override fun newId() = "id-${++next}"
}

class MutableClock(var now: Long) : Clock() {
    override fun instant(): Instant = Instant.ofEpochMilli(now)
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
}
