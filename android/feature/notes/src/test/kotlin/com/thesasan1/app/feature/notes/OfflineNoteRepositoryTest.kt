package com.thesasan1.app.feature.notes

import com.thesasan1.app.core.common.AppError
import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.core.database.entity.OutboxOp
import com.thesasan1.app.core.database.entity.SyncState
import com.thesasan1.app.feature.notes.data.OfflineNoteRepository
import com.thesasan1.app.feature.notes.domain.Note
import com.thesasan1.app.feature.notes.domain.NoteDocument
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OfflineNoteRepositoryTest {

    private val start = 1_764_000_000_000
    private val clock = MutableClock(start)
    private val noteDao = FakeNoteDao()
    private val outbox = FakeOutboxDao()
    private val repository = OfflineNoteRepository(noteDao, outbox, DirectTransactionRunner, SequentialIds(), clock)

    @Test
    fun `create stores the note, derives plain text and queues an upsert`() = runTest {
        val note = repository.create(
            title = "  Groceries ",
            document = NoteDocument.fromPlainText("milk\neggs"),
        ).value()

        assertEquals("Groceries", note.title)
        val row = noteDao.rows.value.getValue(note.id)
        assertEquals("milk\neggs", row.bodyPlain)
        assertEquals(SyncState.PENDING, row.sync.syncState)
        val entry = outbox.entries.single()
        assertEquals(OfflineNoteRepository.ENTITY_TYPE, entry.entityType)
        assertEquals(OutboxOp.UPSERT, entry.op)
        assertEquals(note.id, entry.entityId)
        val payload = Json.parseToJsonElement(entry.payload!!).jsonObject
        assertEquals("Groceries", payload.getValue("title").jsonPrimitive.content)
    }

    @Test
    fun `blank title is stored as null`() = runTest {
        assertNull(repository.create(title = "   ").value().title)
    }

    @Test
    fun `saving unchanged content writes nothing`() = runTest {
        val doc = NoteDocument.fromPlainText("same")
        val note = repository.create(title = "T", document = doc).value()
        clock.now += 1_000

        val saved = repository.saveContent(note.id, "T", doc).value()

        assertEquals(start, saved.updatedAt)
        assertEquals(1, outbox.entries.size)
    }

    @Test
    fun `saving new content bumps updated_at and queues another upsert`() = runTest {
        val note = repository.create(document = NoteDocument.fromPlainText("draft")).value()
        clock.now += 1_000

        val saved = repository.saveContent(note.id, null, NoteDocument.fromPlainText("final")).value()

        assertEquals(start + 1_000, saved.updatedAt)
        assertEquals("final", noteDao.rows.value.getValue(note.id).bodyPlain)
        assertEquals(2, outbox.entries.size)
    }

    @Test
    fun `pin, archive and organize update the note`() = runTest {
        val id = repository.create().value().id

        assertTrue(repository.setPinned(id, true).value().isPinned)
        clock.now += 5
        assertEquals(start + 5, repository.setArchived(id, true).value().archivedAt)
        assertEquals("p1", repository.organize(id, projectId = "p1", categoryId = null).value().projectId)
        assertNull(repository.setArchived(id, false).value().archivedAt)
    }

    @Test
    fun `trash, restore and permanent delete follow NOTE-04`() = runTest {
        val id = repository.create().value().id
        clock.now += 10

        assertEquals(start + 10, repository.moveToTrash(id).value().deletedAt)
        assertEquals(listOf(id), repository.observeTrash().first().map(Note::id))
        assertNull(repository.restoreFromTrash(id).value().deletedAt)

        // Only trashed notes can be deleted for good.
        val refused = repository.deletePermanently(id)
        assertInstanceOf(AppError.Validation::class.java, (refused as Outcome.Failure).error)

        repository.moveToTrash(id)
        assertEquals(Outcome.Success(Unit), repository.deletePermanently(id))
        assertNull(noteDao.rows.value[id])
        val last = outbox.entries.last()
        assertEquals(OutboxOp.DELETE, last.op)
        assertNull(last.payload)
    }

    @Test
    fun `purge removes trash older than 30 days only`() = runTest {
        val old = repository.create().value().id
        repository.moveToTrash(old)
        clock.now += OfflineNoteRepository.TRASH_RETENTION.toMillis()
        val recent = repository.create().value().id
        repository.moveToTrash(recent)
        clock.now += 1

        assertEquals(Outcome.Success(1), repository.purgeExpiredTrash())
        assertEquals(listOf(recent), repository.observeTrash().first().map(Note::id))
    }

    @Test
    fun `unknown ids fail with NotFound`() = runTest {
        assertEquals(Outcome.Failure(AppError.NotFound), repository.get("missing"))
        assertEquals(Outcome.Failure(AppError.NotFound), repository.setPinned("missing", true))
        assertEquals(Outcome.Failure(AppError.NotFound), repository.deletePermanently("missing"))
        assertTrue(outbox.entries.isEmpty())
    }

    @Test
    fun `blank search never reaches the database`() = runTest {
        assertEquals(emptyList<Note>(), repository.search("  ** ").first())
        assertTrue(noteDao.searchQueries.isEmpty())

        repository.search("Milk")
        assertEquals(listOf("\"milk*\""), noteDao.searchQueries)
    }

    @Test
    fun `storage failures become AppError Storage`() = runTest {
        val failing = OfflineNoteRepository(
            noteDao = object : com.thesasan1.app.core.database.dao.NoteDao by noteDao {
                override suspend fun getById(id: String) = error("disk I/O")
            },
            outboxDao = outbox,
            transaction = DirectTransactionRunner,
            ids = SequentialIds(),
            clock = clock,
        )

        assertInstanceOf(AppError.Storage::class.java, (failing.get("x") as Outcome.Failure).error)
    }

    private fun <T> Outcome<T>.value(): T = (this as Outcome.Success).value
}
