package com.thesasan1.app.feature.notes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesasan1.app.feature.notes.domain.Note
import com.thesasan1.app.feature.notes.domain.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NoteShelfUiState(
    val notes: List<NoteSummary> = emptyList(),
    val isLoading: Boolean = true,
    /** A note waiting for the user to confirm permanent deletion (trash only). */
    val pendingDelete: NoteSummary? = null,
) {
    val isEmpty: Boolean get() = !isLoading && notes.isEmpty()
}

/** A read-mostly list of notes kept out of the main list: the archive or the trash. */
abstract class NoteShelfViewModel(source: Flow<List<Note>>) : ViewModel() {
    private val pendingDelete = MutableStateFlow<NoteSummary?>(null)

    val uiState: StateFlow<NoteShelfUiState> = combine(source, pendingDelete) { notes, pending ->
        NoteShelfUiState(notes = notes.map { it.toSummary() }, isLoading = false, pendingDelete = pending)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), NoteShelfUiState())

    protected fun setPendingDelete(note: NoteSummary?) {
        pendingDelete.value = note
    }

    protected val pending: NoteSummary? get() = pendingDelete.value

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Archived notes (NOTE-03). Tapping one opens it; unarchiving returns it to the main list. */
@HiltViewModel
class NotesArchiveViewModel @Inject constructor(private val repository: NoteRepository) :
    NoteShelfViewModel(repository.observeArchived()) {

    fun unarchive(note: NoteSummary) {
        viewModelScope.launch { repository.setArchived(note.id, false) }
    }
}

/**
 * The trash (NOTE-04). Notes can be restored, or deleted for good after the user confirms. Notes
 * past the retention window are purged whenever the trash is opened.
 */
@HiltViewModel
class NotesTrashViewModel @Inject constructor(private val repository: NoteRepository) :
    NoteShelfViewModel(repository.observeTrash()) {

    init {
        viewModelScope.launch { repository.purgeExpiredTrash() }
    }

    fun restore(note: NoteSummary) {
        viewModelScope.launch { repository.restoreFromTrash(note.id) }
    }

    fun requestDelete(note: NoteSummary) = setPendingDelete(note)

    fun cancelDelete() = setPendingDelete(null)

    fun confirmDelete() {
        val note = pending ?: return
        setPendingDelete(null)
        viewModelScope.launch { repository.deletePermanently(note.id) }
    }
}
