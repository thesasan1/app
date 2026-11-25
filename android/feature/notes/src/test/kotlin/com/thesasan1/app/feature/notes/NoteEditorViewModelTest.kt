package com.thesasan1.app.feature.notes

import androidx.lifecycle.SavedStateHandle
import com.thesasan1.app.core.common.getOrNull
import com.thesasan1.app.feature.notes.data.NoteDocumentCodec
import com.thesasan1.app.feature.notes.data.OfflineNoteRepository
import com.thesasan1.app.feature.notes.domain.NoteBlock
import com.thesasan1.app.feature.notes.domain.NoteDocument
import com.thesasan1.app.feature.notes.domain.Span
import com.thesasan1.app.feature.notes.ui.NoteEditorViewModel
import com.thesasan1.app.feature.notes.ui.SaveStatus
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class NoteEditorViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val noteDao = FakeNoteDao()
    private val outbox = FakeOutboxDao()
    private val repository =
        OfflineNoteRepository(
            noteDao,
            outbox,
            DirectTransactionRunner,
            SequentialIds(),
            MutableClock(1_764_000_000_000),
        )

    private fun editor(handle: SavedStateHandle = SavedStateHandle()) = NoteEditorViewModel(handle, repository)

    @Test
    fun `a new note is stored only after typing stops`() = runTest(main.dispatcher) {
        val vm = editor()
        vm.onBodyChange("milk")
        advanceTimeBy(NoteEditorViewModel.AUTOSAVE_DEBOUNCE_MS - 1)
        assertTrue(noteDao.rows.value.isEmpty())

        advanceUntilIdle()
        val row = noteDao.rows.value.values.single()
        assertEquals("milk", row.bodyPlain)
        assertEquals(SaveStatus.Saved, vm.uiState.value.saveStatus)
    }

    @Test
    fun `an untouched new note is never stored`() = runTest(main.dispatcher) {
        val vm = editor()
        vm.flush()
        advanceUntilIdle()
        assertTrue(noteDao.rows.value.isEmpty())
        assertTrue(outbox.entries.isEmpty())
    }

    @Test
    fun `flush saves at once and later edits update the same note`() = runTest(main.dispatcher) {
        val handle = SavedStateHandle()
        val vm = editor(handle)
        vm.onTitleChange("Plan")
        vm.flush()
        runCurrent()
        val id = noteDao.rows.value.keys.single()
        assertEquals(id, handle.get<String>("noteId"))

        vm.onBodyChange("call Sara")
        advanceUntilIdle()
        assertEquals(1, noteDao.rows.value.size)
        assertEquals("call Sara", noteDao.rows.value.getValue(id).bodyPlain)
    }

    @Test
    fun `opening an existing note loads it and repeated flushes queue nothing`() = runTest(main.dispatcher) {
        val note = repository.create(title = "Groceries", document = NoteDocument.fromPlainText("milk")).getOrNull()!!
        val queued = outbox.entries.size

        val vm = editor(SavedStateHandle(mapOf("noteId" to note.id)))
        advanceUntilIdle()
        assertEquals("Groceries", vm.uiState.value.title)
        assertEquals("milk", vm.uiState.value.body)

        vm.flush()
        vm.flush()
        advanceUntilIdle()
        assertEquals(queued, outbox.entries.size)
    }

    @Test
    fun `a title edit keeps rich formatting in the body`() = runTest(main.dispatcher) {
        val rich = NoteDocument(listOf(NoteBlock.ChecklistItem(checked = true, spans = listOf(Span("eggs")))))
        val note = repository.create(title = "Shop", document = rich).getOrNull()!!

        val vm = editor(SavedStateHandle(mapOf("noteId" to note.id)))
        advanceUntilIdle()
        vm.onTitleChange("Shopping")
        advanceUntilIdle()

        val saved = repository.get(note.id).getOrNull()!!
        assertEquals("Shopping", saved.title)
        assertEquals(rich, saved.document)
    }

    @Test
    fun `a draft restored after process death is saved over the stored note`() = runTest(main.dispatcher) {
        val note = repository.create(title = "Old", document = NoteDocument.fromPlainText("old")).getOrNull()!!
        val handle = SavedStateHandle(mapOf("noteId" to note.id, "draftTitle" to "New", "draftBody" to "new"))

        val vm = editor(handle)
        advanceUntilIdle()

        assertEquals("New", vm.uiState.value.title)
        val saved = repository.get(note.id).getOrNull()!!
        assertEquals("new", NoteDocumentCodec.plainText(saved.document))
    }

    @Test
    fun `move to trash closes the editor and keeps the note recoverable`() = runTest(main.dispatcher) {
        val vm = editor()
        vm.onBodyChange("draft")
        vm.flush()
        runCurrent()
        vm.moveToTrash()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isClosed)
        val row = noteDao.rows.value.values.single()
        assertTrue(row.sync.deletedAt != null)
    }

    @Test
    fun `an unknown id shows not found and saves nothing`() = runTest(main.dispatcher) {
        val vm = editor(SavedStateHandle(mapOf("noteId" to "missing")))
        advanceUntilIdle()
        vm.onBodyChange("text")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.notFound)
        assertTrue(noteDao.rows.value.isEmpty())
        assertEquals(SaveStatus.Idle, vm.uiState.value.saveStatus)
    }
}
