package com.thesasan1.app.feature.notes

import com.thesasan1.app.core.common.IdGenerator
import com.thesasan1.app.core.database.TransactionRunner
import com.thesasan1.app.core.database.dao.NoteDao
import com.thesasan1.app.core.database.dao.OutboxDao
import com.thesasan1.app.core.database.entity.NoteEntity
import com.thesasan1.app.core.database.entity.OutboxEntity
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [NoteDao]. Room's own behaviour is covered by NoteDaoTest in :core:database. */
class FakeNoteDao : NoteDao {
    val rows = MutableStateFlow<Map<String, NoteEntity>>(emptyMap())
    val searchQueries = mutableListOf<String>()

    private val sorted = compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.sync.updatedAt }

    override suspend fun getById(id: String) = rows.value[id]

    override fun observeActive() = select { it.sync.deletedAt == null && it.archivedAt == null }

    override fun observeByProject(projectId: String) =
        select { it.sync.deletedAt == null && it.archivedAt == null && it.projectId == projectId }

    override fun observeArchived() = select { it.sync.deletedAt == null && it.archivedAt != null }

    override fun observeTrash() = select { it.sync.deletedAt != null }

    override fun search(matchQuery: String): Flow<List<NoteEntity>> {
        searchQueries += matchQuery
        return select { it.sync.deletedAt == null }
    }

    override suspend fun upsertRow(note: NoteEntity) {
        rows.value += note.id to note
    }

    override suspend fun deleteIndex(noteId: String) = Unit
    override suspend fun insertIndex(noteId: String, title: String, bodyPlain: String) = Unit
    override suspend fun deleteIndexForDeletedBefore(cutoffMillis: Long) = Unit

    override suspend fun deleteRow(id: String) {
        rows.value -= id
    }

    override suspend fun deleteRowsDeletedBefore(cutoffMillis: Long): Int {
        val expired = rows.value.values.filter { (it.sync.deletedAt ?: Long.MAX_VALUE) < cutoffMillis }
        rows.value -= expired.map { it.id }.toSet()
        return expired.size
    }

    private fun select(filter: (NoteEntity) -> Boolean) =
        rows.map { all -> all.values.filter(filter).sortedWith(sorted) }
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
