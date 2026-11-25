package com.thesasan1.app.feature.notes.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.feature.notes.data.NoteDocumentCodec
import com.thesasan1.app.feature.notes.domain.Note
import com.thesasan1.app.feature.notes.domain.NoteDocument
import com.thesasan1.app.feature.notes.domain.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class SaveStatus { Idle, Saving, Saved, Failed }

data class NoteEditorUiState(
    val title: String = "",
    val body: String = "",
    val isLoading: Boolean = false,
    val isPinned: Boolean = false,
    val saveStatus: SaveStatus = SaveStatus.Idle,
    val notFound: Boolean = false,
    val isClosed: Boolean = false,
)

/**
 * Plain-text note editor with autosave (NOTE-01). Edits are saved [AUTOSAVE_DEBOUNCE_MS] after
 * typing stops and immediately on [flush] (lifecycle stop, back). The draft lives in
 * [SavedStateHandle] so it survives process death, and saves run [NonCancellable] so leaving the
 * screen never drops one. A new note is only stored once it has content.
 *
 * Rich formatting (NOTE-02) isn't editable yet: when the body text is unchanged, the loaded
 * document is written back as-is, so a title edit never flattens formatting (KI-012).
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository,
) : ViewModel() {

    private var noteId: String? = savedStateHandle[KEY_NOTE_ID]
    private var loadedDocument: NoteDocument? = null
    private var lastSaved: Pair<String, String>? = null
    private val saveLock = Mutex()

    // A counter rather than a SharedFlow: an edit made before the collector starts is never lost.
    private val edits = MutableStateFlow(0L)

    private val restoredTitle: String? = savedStateHandle[KEY_DRAFT_TITLE]
    private val restoredBody: String? = savedStateHandle[KEY_DRAFT_BODY]

    private val _uiState = MutableStateFlow(
        NoteEditorUiState(
            title = restoredTitle.orEmpty(),
            body = restoredBody.orEmpty(),
            isLoading = noteId != null,
        ),
    )
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    init {
        noteId?.let { id -> viewModelScope.launch { load(id) } }
        viewModelScope.launch { edits.debounce(AUTOSAVE_DEBOUNCE_MS).collect { save() } }
    }

    fun onTitleChange(value: String) = edit { it.copy(title = value) }

    fun onBodyChange(value: String) = edit { it.copy(body = value) }

    /** Saves now. Called on lifecycle stop and before leaving the screen. */
    fun flush() {
        viewModelScope.launch { save() }
    }

    fun togglePin() {
        viewModelScope.launch {
            save()
            val id = noteId ?: return@launch
            val pinned = !_uiState.value.isPinned
            if (repository.setPinned(id, pinned) is Outcome.Success) _uiState.update { it.copy(isPinned = pinned) }
        }
    }

    /** Moves the note to the trash (recoverable, NOTE-04) and closes the editor. */
    fun moveToTrash() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                saveLock.withLock { noteId?.let { repository.moveToTrash(it) } }
            }
            _uiState.update { it.copy(isClosed = true) }
        }
    }

    private fun edit(change: (NoteEditorUiState) -> NoteEditorUiState) {
        val state = _uiState.updateAndGet(change)
        savedStateHandle[KEY_DRAFT_TITLE] = state.title
        savedStateHandle[KEY_DRAFT_BODY] = state.body
        edits.update { it + 1 }
    }

    private suspend fun load(id: String) {
        when (val result = repository.get(id)) {
            is Outcome.Success -> {
                val note = result.value
                loadedDocument = note.document
                val stored = note.title.orEmpty() to NoteDocumentCodec.plainText(note.document)
                lastSaved = stored
                _uiState.update {
                    it.copy(
                        // A draft restored after process death wins over what was stored.
                        title = restoredTitle ?: stored.first,
                        body = restoredBody ?: stored.second,
                        isPinned = note.isPinned,
                        isLoading = false,
                    )
                }
                if (restoredTitle != null || restoredBody != null) edits.update { it + 1 }
            }
            is Outcome.Failure -> _uiState.update { it.copy(isLoading = false, notFound = true) }
        }
    }

    private suspend fun save() = withContext(NonCancellable) {
        saveLock.withLock {
            val state = _uiState.value
            if (state.isLoading || state.notFound || state.isClosed) return@withLock
            val content = state.title to state.body
            if (content == lastSaved) return@withLock
            val id = noteId
            if (id == null && state.title.isBlank() && state.body.isBlank()) return@withLock

            _uiState.update { it.copy(saveStatus = SaveStatus.Saving) }
            val title = state.title.takeIf(String::isNotBlank)
            val document = documentFor(state.body)
            val result = if (id == null) {
                repository.create(title = title, document = document)
            } else {
                repository.saveContent(id, title, document)
            }
            when (result) {
                is Outcome.Success -> onSaved(result.value, content)
                is Outcome.Failure -> _uiState.update { it.copy(saveStatus = SaveStatus.Failed) }
            }
        }
    }

    private fun onSaved(note: Note, content: Pair<String, String>) {
        if (noteId == null) {
            noteId = note.id
            savedStateHandle[KEY_NOTE_ID] = note.id
        }
        loadedDocument = note.document
        lastSaved = content
        _uiState.update { it.copy(saveStatus = SaveStatus.Saved) }
    }

    private fun documentFor(body: String): NoteDocument {
        val loaded = loadedDocument
        return if (loaded != null &&
            NoteDocumentCodec.plainText(loaded) == body
        ) {
            loaded
        } else {
            NoteDocument.fromPlainText(body)
        }
    }

    companion object {
        const val AUTOSAVE_DEBOUNCE_MS = 600L

        /** Matches [NoteEditorRoute.noteId], so type-safe navigation fills it in. */
        internal const val KEY_NOTE_ID = "noteId"
        private const val KEY_DRAFT_TITLE = "draftTitle"
        private const val KEY_DRAFT_BODY = "draftBody"
    }
}
