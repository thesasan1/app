package com.thesasan1.app.feature.notes

import androidx.lifecycle.SavedStateHandle
import com.thesasan1.app.core.common.getOrNull
import com.thesasan1.app.feature.notes.data.OfflineNoteRepository
import com.thesasan1.app.feature.notes.ui.NoteEditorViewModel
import com.thesasan1.app.feature.notes.ui.NotesArchiveViewModel
import com.thesasan1.app.feature.notes.ui.NotesTrashViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class NoteShelfViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val day = 24L * 60 * 60 * 1000
    private val clock = MutableClock(1_764_000_000_000)
    private val noteDao = FakeNoteDao()
    private val repository =
        OfflineNoteRepository(noteDao, FakeOutboxDao(), DirectTransactionRunner, SequentialIds(), clock)

    @Test
    fun `archive lists archived notes and unarchive returns them`() = runTest(main.dispatcher) {
        val note = repository.create(title = "Old trip").getOrNull()!!
        repository.setArchived(note.id, true)

        val vm = NotesArchiveViewModel(repository)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(listOf("Old trip"), vm.uiState.value.notes.map { it.title })

        vm.unarchive(vm.uiState.value.notes.single())
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isEmpty)
        assertNull(noteDao.rows.value.getValue(note.id).archivedAt)
    }

    @Test
    fun `restore takes a note out of the trash`() = runTest(main.dispatcher) {
        val note = repository.create(title = "Oops").getOrNull()!!
        repository.moveToTrash(note.id)

        val vm = NotesTrashViewModel(repository)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.restore(vm.uiState.value.notes.single())
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEmpty)
        assertNull(noteDao.rows.value.getValue(note.id).sync.deletedAt)
    }

    @Test
    fun `delete forever needs confirmation`() = runTest(main.dispatcher) {
        val note = repository.create(title = "Secret").getOrNull()!!
        repository.moveToTrash(note.id)
        val vm = NotesTrashViewModel(repository)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val row = vm.uiState.value.notes.single()

        vm.requestDelete(row)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.pendingDelete)
        vm.cancelDelete()
        advanceUntilIdle()
        assertNull(vm.uiState.value.pendingDelete)
        assertTrue(noteDao.rows.value.containsKey(note.id))

        vm.requestDelete(row)
        vm.confirmDelete()
        advanceUntilIdle()
        assertTrue(noteDao.rows.value.isEmpty())
    }

    @Test
    fun `opening the trash purges notes past the retention window`() = runTest(main.dispatcher) {
        val old = repository.create(title = "Old").getOrNull()!!
        repository.moveToTrash(old.id)
        clock.now += 31 * day
        val recent = repository.create(title = "Recent").getOrNull()!!
        repository.moveToTrash(recent.id)

        val vm = NotesTrashViewModel(repository)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(listOf("Recent"), vm.uiState.value.notes.map { it.title })
    }

    @Test
    fun `archiving from the editor closes it and unarchiving keeps it open`() = runTest(main.dispatcher) {
        val note = repository.create(title = "Plan").getOrNull()!!
        val vm = NoteEditorViewModel(SavedStateHandle(mapOf("noteId" to note.id)), repository)
        advanceUntilIdle()

        vm.toggleArchive()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isArchived)
        assertTrue(vm.uiState.value.isClosed)

        val reopened = NoteEditorViewModel(SavedStateHandle(mapOf("noteId" to note.id)), repository)
        advanceUntilIdle()
        reopened.toggleArchive()
        advanceUntilIdle()
        assertTrue(!reopened.uiState.value.isArchived && !reopened.uiState.value.isClosed)
    }
}
