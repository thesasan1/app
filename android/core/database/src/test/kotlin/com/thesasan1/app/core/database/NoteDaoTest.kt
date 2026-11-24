package com.thesasan1.app.core.database

import com.thesasan1.app.core.database.entity.NoteEntity
import com.thesasan1.app.core.database.entity.SyncColumns
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NoteDaoTest {

    @get:Rule
    val database = DatabaseTestRule()

    private val now = 1_764_000_000_000
    private val dao get() = database.db.noteDao()

    @Test
    fun activeNotesArePinnedFirstThenMostRecentlyEdited() = runTest {
        dao.upsert(note("old", updatedAt = now))
        dao.upsert(note("new", updatedAt = now + 2))
        dao.upsert(note("pinned", updatedAt = now - 5).copy(isPinned = true))

        assertEquals(listOf("pinned", "new", "old"), dao.observeActive().first().map { it.id })
    }

    @Test
    fun archivedAndTrashedNotesLeaveTheActiveList() = runTest {
        dao.upsert(note("live"))
        dao.upsert(note("archived").copy(archivedAt = now))
        dao.upsert(note("trashed").let { it.copy(sync = it.sync.deleted(now + 1)) })

        assertEquals(listOf("live"), dao.observeActive().first().map { it.id })
        assertEquals(listOf("archived"), dao.observeArchived().first().map { it.id })
        assertEquals(listOf("trashed"), dao.observeTrash().first().map { it.id })
    }

    @Test
    fun restoringFromTrashBringsTheNoteBack() = runTest {
        val note = note("n")
        dao.upsert(note.copy(sync = note.sync.deleted(now + 1)))
        dao.upsert(note.copy(sync = note.sync.touched(now + 2).copy(deletedAt = null)))

        assertEquals(listOf("n"), dao.observeActive().first().map { it.id })
        assertEquals(emptyList<NoteEntity>(), dao.observeTrash().first())
    }

    @Test
    fun projectFilterShowsOnlyThatProjectsActiveNotes() = runTest {
        dao.upsert(note("a").copy(projectId = "p1"))
        dao.upsert(note("b").copy(projectId = "p2"))
        dao.upsert(note("c").copy(projectId = "p1", archivedAt = now))

        assertEquals(listOf("a"), dao.observeByProject("p1").first().map { it.id })
    }

    @Test
    fun searchMatchesTitleAndBodyByWordPrefix() = runTest {
        dao.upsert(note("groceries", title = "Groceries", body = "milk and eggs"))
        dao.upsert(note("meeting", title = "Standup", body = "Discuss the milestone"))

        assertEquals(listOf("groceries"), search("groc"))
        assertEquals(listOf("meeting"), search("MILE"))
        assertEquals(setOf("groceries", "meeting"), search("mil").toSet())
        assertEquals(listOf("groceries"), search("milk eggs"))
        assertEquals(emptyList<String>(), search("milk standup"))
    }

    @Test
    fun searchFindsPersianTextWrittenWithArabicLettersOrDigits() = runTest {
        // Stored with Persian ی/ک, ZWNJ and Persian digits; searched with Arabic ي/ك and ASCII digits.
        dao.upsert(note("fa", title = "کتاب\u200Cهای ۱۴۰۳", body = "یادداشت روزانه"))

        assertEquals(listOf("fa"), search("كتاب"))
        assertEquals(listOf("fa"), search("1403"))
        assertEquals(listOf("fa"), search("يادداشت"))
        assertEquals(listOf("fa"), search("ها"))
    }

    @Test
    fun editingANoteReplacesItsIndexEntry() = runTest {
        dao.upsert(note("n", body = "first draft"))
        dao.upsert(note("n", body = "second version", updatedAt = now + 1))

        assertEquals(emptyList<String>(), search("draft"))
        assertEquals(listOf("n"), search("second"))
        assertEquals(1, indexRows())
    }

    @Test
    fun searchSkipsTrashedNotesButKeepsArchivedOnes() = runTest {
        dao.upsert(note("archived", body = "keep me").copy(archivedAt = now))
        dao.upsert(note("trashed", body = "keep me").let { it.copy(sync = it.sync.deleted(now)) })

        assertEquals(listOf("archived"), search("keep"))
    }

    @Test
    fun queryMadeOnlyOfPunctuationYieldsNoMatchExpression() {
        assertNull(SearchText.matchQuery(" \"*-() "))
    }

    @Test
    fun purgeRemovesOldTrashAndItsIndexRows() = runTest {
        dao.upsert(note("old", body = "alpha").let { it.copy(sync = it.sync.deleted(now)) })
        dao.upsert(note("recent", body = "alpha").let { it.copy(sync = it.sync.deleted(now + 10)) })
        dao.upsert(note("live", body = "alpha"))

        assertEquals(1, dao.purgeDeletedBefore(now + 5))
        assertNull(dao.getById("old"))
        assertEquals(2, indexRows())
    }

    private suspend fun search(text: String): List<String> =
        dao.search(requireNotNull(SearchText.matchQuery(text))).first().map { it.id }

    private fun indexRows(): Int = database.db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM notes_fts").use {
        it.moveToFirst()
        it.getInt(0)
    }

    private fun note(id: String, title: String? = null, body: String = "", updatedAt: Long = now) = NoteEntity(
        id = id,
        title = title,
        body = """{"v":1,"blocks":[]}""",
        bodyPlain = body,
        sync = SyncColumns.new(updatedAt),
    )
}
