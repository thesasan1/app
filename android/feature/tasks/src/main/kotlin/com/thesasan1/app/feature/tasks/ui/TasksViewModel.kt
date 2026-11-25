package com.thesasan1.app.feature.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.core.database.entity.TaskEntity
import com.thesasan1.app.feature.tasks.domain.Task
import com.thesasan1.app.feature.tasks.domain.TaskDraft
import com.thesasan1.app.feature.tasks.domain.TaskRepository
import com.thesasan1.app.feature.tasks.domain.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the quick-add bar holds (TASK-06). */
data class QuickAddState(val title: String = "", val dueToday: Boolean = false, val isSaving: Boolean = false) {
    val canAdd: Boolean get() = title.isNotBlank() && !isSaving
}

/** Open tasks grouped by when they're due. Each group keeps the repository's order. */
data class TaskSections(
    val overdue: List<Task> = emptyList(),
    val today: List<Task> = emptyList(),
    val upcoming: List<Task> = emptyList(),
    val someday: List<Task> = emptyList(),
) {
    val isEmpty: Boolean get() = overdue.isEmpty() && today.isEmpty() && upcoming.isEmpty() && someday.isEmpty()
}

data class TasksUiState(
    val today: LocalDate,
    val sections: TaskSections = TaskSections(),
    val quickAdd: QuickAddState = QuickAddState(),
    /** The task just completed, for the Undo snackbar; null once it's dismissed or undone. */
    val justCompleted: Task? = null,
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && sections.isEmpty
}

/** Tasks list with quick add (TASK-01, TASK-06). Completing a task can be undone from the snackbar. */
@HiltViewModel
class TasksViewModel @Inject constructor(private val repository: TaskRepository, private val clock: Clock) :
    ViewModel() {

    private val today = MutableStateFlow(LocalDate.now(clock))
    private val quickAdd = MutableStateFlow(QuickAddState())
    private val justCompleted = MutableStateFlow<Task?>(null)

    val uiState: StateFlow<TasksUiState> =
        combine(today, repository.observeOpen(), quickAdd, justCompleted) { day, open, draft, completed ->
            TasksUiState(
                today = day,
                sections = group(open, day),
                quickAdd = draft,
                justCompleted = completed,
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TasksUiState(LocalDate.now(clock)))

    /** Re-reads the date, so a list left open past midnight regroups (called on resume). */
    fun refresh() {
        today.value = LocalDate.now(clock)
    }

    fun onQuickAddChange(title: String) {
        quickAdd.update { it.copy(title = title.take(TaskEntity.MAX_TITLE_LENGTH)) }
    }

    fun onQuickAddDueTodayChange(dueToday: Boolean) {
        quickAdd.update { it.copy(dueToday = dueToday) }
    }

    fun addTask() {
        val draft = quickAdd.value
        if (!draft.canAdd) return
        quickAdd.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val outcome = repository.create(
                TaskDraft(title = draft.title, dueDate = if (draft.dueToday) LocalDate.now(clock) else null),
            )
            // The text is kept on failure so nothing typed is lost.
            quickAdd.update {
                if (outcome is Outcome.Success) it.copy(title = "", isSaving = false) else it.copy(isSaving = false)
            }
        }
    }

    fun complete(task: Task) {
        viewModelScope.launch {
            if (repository.setStatus(task.id, TaskStatus.DONE) is Outcome.Success) justCompleted.value = task
        }
    }

    /** Puts the just-completed task back to the status it had. */
    fun undoComplete() {
        val task = justCompleted.value ?: return
        justCompleted.value = null
        viewModelScope.launch { repository.setStatus(task.id, task.status) }
    }

    fun dismissCompleted() {
        justCompleted.value = null
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        internal fun group(tasks: List<Task>, today: LocalDate): TaskSections {
            val (dated, someday) = tasks.partition { it.dueDate != null }
            return TaskSections(
                overdue = dated.filter { it.dueDate!! < today },
                today = dated.filter { it.dueDate == today },
                upcoming = dated.filter { it.dueDate!! > today },
                someday = someday,
            )
        }
    }
}
