package com.thesasan1.app.feature.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.feature.tasks.domain.Task
import com.thesasan1.app.feature.tasks.domain.TaskRepository
import com.thesasan1.app.feature.tasks.domain.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskShelfUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    /** A task waiting for the user to confirm permanent deletion (trash only). */
    val pendingDelete: Task? = null,
    /** The last action was refused, e.g. restoring a subtask whose parent is still in the trash. */
    val blocked: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && tasks.isEmpty()
}

/** A read-mostly list of tasks kept out of the main list: completed, archived or in the trash. */
abstract class TaskShelfViewModel(source: Flow<List<Task>>) : ViewModel() {
    private val pendingDelete = MutableStateFlow<Task?>(null)
    private val blocked = MutableStateFlow(false)

    val uiState: StateFlow<TaskShelfUiState> = combine(source, pendingDelete, blocked) { tasks, pending, isBlocked ->
        TaskShelfUiState(tasks = tasks, isLoading = false, pendingDelete = pending, blocked = isBlocked)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TaskShelfUiState())

    protected fun setPendingDelete(task: Task?) {
        pendingDelete.value = task
    }

    protected val pending: Task? get() = pendingDelete.value

    /** Runs [action] and records whether the repository refused it. */
    protected fun act(action: suspend () -> Outcome<*>) {
        viewModelScope.launch { blocked.value = action() is Outcome.Failure }
    }

    fun dismissBlocked() {
        blocked.value = false
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Completed tasks, newest first. Reopening one returns it to the main list. */
@HiltViewModel
class TasksCompletedViewModel @Inject constructor(private val repository: TaskRepository) :
    TaskShelfViewModel(repository.observeCompleted()) {

    fun reopen(task: Task) = act { repository.setStatus(task.id, TaskStatus.TODO) }
}

/** Archived tasks (TASK-07). Unarchiving returns one to the main list. */
@HiltViewModel
class TasksArchiveViewModel @Inject constructor(private val repository: TaskRepository) :
    TaskShelfViewModel(repository.observeArchived()) {

    fun unarchive(task: Task) = act { repository.setArchived(task.id, false) }
}

/**
 * The trash. Tasks can be restored, or deleted for good after the user confirms. Tasks past the
 * retention window are purged whenever the trash is opened, as for notes.
 */
@HiltViewModel
class TasksTrashViewModel @Inject constructor(private val repository: TaskRepository) :
    TaskShelfViewModel(repository.observeTrash()) {

    init {
        viewModelScope.launch { repository.purgeExpiredTrash() }
    }

    fun restore(task: Task) = act { repository.restoreFromTrash(task.id) }

    fun requestDelete(task: Task) = setPendingDelete(task)

    fun cancelDelete() = setPendingDelete(null)

    fun confirmDelete() {
        val task = pending ?: return
        setPendingDelete(null)
        act { repository.deletePermanently(task.id) }
    }
}
