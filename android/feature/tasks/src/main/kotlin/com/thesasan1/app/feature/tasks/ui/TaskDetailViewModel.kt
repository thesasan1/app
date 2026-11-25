package com.thesasan1.app.feature.tasks.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.core.database.entity.TaskEntity
import com.thesasan1.app.feature.tasks.domain.Task
import com.thesasan1.app.feature.tasks.domain.TaskDraft
import com.thesasan1.app.feature.tasks.domain.TaskEvent
import com.thesasan1.app.feature.tasks.domain.TaskPriority
import com.thesasan1.app.feature.tasks.domain.TaskRepository
import com.thesasan1.app.feature.tasks.domain.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class TaskDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val task: Task? = null,
    val title: String = "",
    val description: String = "",
    /** The title is blank, so the last saved title is kept until it isn't. */
    val titleMissing: Boolean = false,
    val subtasks: List<Task> = emptyList(),
    val newSubtask: String = "",
    val history: List<TaskEvent> = emptyList(),
    val today: LocalDate? = null,
    val isClosed: Boolean = false,
)

/**
 * Task detail and editor (TASK-02, TASK-03). Title and description autosave
 * [AUTOSAVE_DEBOUNCE_MS] after typing stops and on [flush]; every other change saves at once.
 * Saves run [NonCancellable] under one lock, so leaving the screen never drops or reorders one.
 */
@OptIn(FlowPreview::class)
@Suppress("TooManyFunctions") // one call per control on the screen
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository,
    private val clock: Clock,
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle[KEY_TASK_ID]) { "TaskDetailRoute needs a taskId" }
    private val saveLock = Mutex()

    // A counter rather than a SharedFlow: an edit made before the collector starts is never lost.
    private val edits = MutableStateFlow(0L)

    private val _uiState = MutableStateFlow(TaskDetailUiState(today = LocalDate.now(clock)))
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
        viewModelScope.launch { edits.drop(1).debounce(AUTOSAVE_DEBOUNCE_MS).collect { saveText() } }
        viewModelScope.launch {
            repository.observeSubtasks(taskId).collect { s -> _uiState.update { it.copy(subtasks = s) } }
        }
        viewModelScope.launch {
            repository.observeHistory(taskId).collect { h -> _uiState.update { it.copy(history = h) } }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value.take(TaskEntity.MAX_TITLE_LENGTH), titleMissing = value.isBlank()) }
        edits.value++
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
        edits.value++
    }

    /** Saves typed text now. Called on lifecycle stop and before leaving the screen. */
    fun flush() {
        viewModelScope.launch { saveText() }
    }

    fun setPriority(priority: TaskPriority) = change { task ->
        repository.edit(task.id, textTitle(task), _uiState.value.description, priority)
    }

    fun setDueDate(date: LocalDate?) = change { task ->
        repository.reschedule(task.id, date, if (date == null) null else task.dueTime)
    }

    /** A time only applies with a date; picking one on an undated task makes it due today. */
    fun setDueTime(time: LocalTime?) = change { task ->
        repository.reschedule(task.id, task.dueDate ?: LocalDate.now(clock), time)
    }

    fun toggleDone() = change { task ->
        repository.setStatus(task.id, if (task.status == TaskStatus.DONE) TaskStatus.TODO else TaskStatus.DONE)
    }

    fun onNewSubtaskChange(value: String) {
        _uiState.update { it.copy(newSubtask = value.take(TaskEntity.MAX_TITLE_LENGTH)) }
    }

    fun addSubtask() {
        val title = _uiState.value.newSubtask
        if (title.isBlank()) return
        viewModelScope.launch {
            if (repository.create(TaskDraft(title = title, parentId = taskId)) is Outcome.Success) {
                _uiState.update { it.copy(newSubtask = "") }
            }
        }
    }

    fun toggleSubtask(subtask: Task) {
        viewModelScope.launch {
            val next = if (subtask.status == TaskStatus.DONE) TaskStatus.TODO else TaskStatus.DONE
            repository.setStatus(subtask.id, next)
        }
    }

    /** Archiving closes the screen: the task leaves the list. */
    fun archive() = change(closeOnSuccess = true) { task -> repository.setArchived(task.id, true) }

    /** Moves the task to the trash (recoverable for 30 days) and closes the screen. */
    fun moveToTrash() = change(closeOnSuccess = true) { task -> repository.moveToTrash(task.id) }

    private suspend fun load() {
        when (val outcome = repository.get(taskId)) {
            is Outcome.Success -> _uiState.update {
                it.copy(
                    isLoading = false,
                    task = outcome.value,
                    title = outcome.value.title,
                    description = outcome.value.description.orEmpty(),
                )
            }
            is Outcome.Failure -> _uiState.update { it.copy(isLoading = false, notFound = true) }
        }
    }

    /** Saves pending text first, then runs [action] on the latest task and keeps its result. */
    private fun change(closeOnSuccess: Boolean = false, action: suspend (Task) -> Outcome<Task>) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                saveText()
                saveLock.withLock {
                    val task = _uiState.value.task ?: return@withLock
                    val outcome = action(task)
                    if (outcome is Outcome.Success) {
                        _uiState.update { it.copy(task = outcome.value, isClosed = it.isClosed || closeOnSuccess) }
                    }
                }
            }
        }
    }

    private suspend fun saveText() = withContext(NonCancellable) {
        saveLock.withLock {
            val state = _uiState.value
            val task = state.task ?: return@withLock
            val title = textTitle(task)
            val description = state.description.trim().ifEmpty { null }
            if (title == task.title && description == task.description) return@withLock
            val outcome = repository.edit(task.id, title, description, task.priority)
            if (outcome is Outcome.Success) _uiState.update { it.copy(task = outcome.value) }
        }
    }

    /** The typed title, or the saved one while the field is blank. */
    private fun textTitle(task: Task): String = _uiState.value.title.trim().ifEmpty { task.title }

    companion object {
        const val AUTOSAVE_DEBOUNCE_MS = 600L

        /** Matches [TaskDetailRoute.taskId], which navigation puts in the SavedStateHandle. */
        internal const val KEY_TASK_ID = "taskId"
    }
}
