package com.thesasan1.app.feature.notes

import com.thesasan1.app.core.common.getOrNull
import com.thesasan1.app.feature.notes.data.OfflineNoteRepository
import com.thesasan1.app.feature.notes.domain.NoteDocument
import com.thesasan1.app.feature.notes.ui.NotesListViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class NotesListViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val clock = MutableClock(1_764_000_000_000)
    private val noteDao = FakeNoteDao()
    private val repository =
        OfflineNoteRepository(noteDao, FakeOutboxDao(), DirectTransactionRunner, SequentialIds(), clock)

    @Test
    fun `pinned notes are listed apart and an untitled note borrows its first line`() = runTest(main.dispatcher) {
        repository.create(document = NoteDocument.fromPlainText("milk\neggs"))
        clock.now += 1
        val pinned = repository.create(title = "Plan").getOrNull()!!
        repository.setPinned(pinned.id, true)

        val vm = NotesListViewModel(repository)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("Plan"), state.pinned.map { it.title })
        assertEquals("milk", state.others.single().title)
        assertEquals("eggs", state.others.single().preview)
    }

    @Test
    fun `search waits for typing to pause, then queries the index`() = runTest(main.dispatcher) {
        val vm = NotesListViewModel(repository)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.onQueryChange("mil")
        advanceTimeBy(NotesListViewModel.SEARCH_DEBOUNCE_MS - 1)
        assertTrue(noteDao.searchQueries.isEmpty())
        advanceUntilIdle()
        assertEquals(1, noteDao.searchQueries.size)
        assertTrue(vm.uiState.value.isSearching)
    }

    @Test
    fun `an empty list reports empty once loaded`() = runTest(main.dispatcher) {
        val vm = NotesListViewModel(repository)
        assertFalse(vm.uiState.value.isEmpty)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isEmpty)
    }
}
