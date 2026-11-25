package com.thesasan1.app.feature.tasks.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesasan1.app.core.reminders.domain.REMINDER_PRESETS
import com.thesasan1.app.core.reminders.domain.TaskReminder
import com.thesasan1.app.core.reminders.domain.TaskReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskRemindersUiState(
    val reminders: List<TaskReminder> = emptyList(),
    /** Presets not yet on the task, in the order the picker offers them. */
    val available: List<Int> = REMINDER_PRESETS,
)

/** The reminders section of the task editor (REM-01, REM-02). Alarms follow the data on their own. */
@HiltViewModel
class TaskRemindersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskReminderRepository,
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle[TaskDetailViewModel.KEY_TASK_ID])

    val uiState: StateFlow<TaskRemindersUiState> = repository.observe(taskId)
        .map { reminders ->
            val used = reminders.map { it.offsetMinutes }.toSet()
            TaskRemindersUiState(reminders, REMINDER_PRESETS.filterNot { it in used })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TaskRemindersUiState())

    fun add(offsetMinutes: Int) {
        viewModelScope.launch { repository.add(taskId, offsetMinutes) }
    }

    fun setRepeatUntilDone(reminder: TaskReminder, repeat: Boolean) {
        viewModelScope.launch { repository.setRepeatUntilDone(reminder.id, repeat) }
    }

    fun remove(reminder: TaskReminder) {
        viewModelScope.launch { repository.remove(reminder.id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
