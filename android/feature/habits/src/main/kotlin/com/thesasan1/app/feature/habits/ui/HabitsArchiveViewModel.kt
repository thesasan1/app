package com.thesasan1.app.feature.habits.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.core.database.entity.HabitEntity
import com.thesasan1.app.feature.habits.domain.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitsArchiveUiState(
    val habits: List<HabitEntity> = emptyList(),
    val isLoading: Boolean = true,
    /** The habit waiting on the delete confirmation. */
    val confirmingDelete: HabitEntity? = null,
    val failed: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && habits.isEmpty()
}

private data class ArchiveDialogs(val confirmingDelete: HabitEntity? = null, val failed: Boolean = false)

/** Archived habits: restore one to today's list, or delete it after a confirmation. */
@HiltViewModel
class HabitsArchiveViewModel @Inject constructor(private val repository: HabitRepository) : ViewModel() {
    private val dialogs = MutableStateFlow(ArchiveDialogs())

    val uiState: StateFlow<HabitsArchiveUiState> =
        combine(repository.observeArchived(), dialogs) { habits, d ->
            HabitsArchiveUiState(habits, isLoading = false, confirmingDelete = d.confirmingDelete, failed = d.failed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HabitsArchiveUiState())

    fun restore(habit: HabitEntity) = act { repository.unarchive(habit.id) }

    fun askDelete(habit: HabitEntity) {
        dialogs.value = dialogs.value.copy(confirmingDelete = habit)
    }

    fun cancelDelete() {
        dialogs.value = dialogs.value.copy(confirmingDelete = null)
    }

    fun confirmDelete() {
        val habit = dialogs.value.confirmingDelete ?: return
        dialogs.value = dialogs.value.copy(confirmingDelete = null)
        act { repository.delete(habit.id) }
    }

    fun dismissFailure() {
        dialogs.value = dialogs.value.copy(failed = false)
    }

    private fun act(action: suspend () -> Outcome<*>) {
        viewModelScope.launch {
            if (action() is Outcome.Failure) dialogs.value = dialogs.value.copy(failed = true)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
