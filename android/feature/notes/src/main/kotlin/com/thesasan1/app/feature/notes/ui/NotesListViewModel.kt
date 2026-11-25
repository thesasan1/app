package com.thesasan1.app.feature.notes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesasan1.app.feature.notes.domain.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotesListUiState(
    val query: String = "",
    val pinned: List<NoteSummary> = emptyList(),
    val others: List<NoteSummary> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isSearching: Boolean get() = query.isNotBlank()
    val isEmpty: Boolean get() = !isLoading && pinned.isEmpty() && others.isEmpty()
}

/** Notes list (NOTE-03 pinning, NOTE-06 search). Pinned notes come first, newest edit first. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesListViewModel @Inject constructor(private val repository: NoteRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    private val results = query
        .debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .flatMapLatest { q -> if (q.isBlank()) repository.observeActive() else repository.search(q) }
        .map { notes -> notes.map { it.toSummary() } }

    val uiState: StateFlow<NotesListUiState> = combine(query, results) { q, notes ->
        // In search results, pinning doesn't split the list: relevance order is kept.
        val (pinned, others) = if (q.isBlank()) notes.partition { it.isPinned } else emptyList<NoteSummary>() to notes
        NotesListUiState(query = q, pinned = pinned, others = others, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), NotesListUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun togglePin(note: NoteSummary) {
        viewModelScope.launch { repository.setPinned(note.id, !note.isPinned) }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 200L
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
