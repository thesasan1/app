package com.thesasan1.app.feature.tasks

import androidx.lifecycle.SavedStateHandle
import com.thesasan1.app.core.reminders.domain.TaskReminder
import com.thesasan1.app.core.reminders.domain.TaskReminderRepository
import com.thesasan1.app.feature.tasks.ui.TaskRemindersViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class TaskRemindersViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repository = object : TaskReminderRepository {
        val rows = MutableStateFlow<List<Pair<String, TaskReminder>>>(emptyList())
        private var next = 0

        override fun observe(taskId: String): Flow<List<TaskReminder>> =
            rows.map { all -> all.filter { it.first == taskId }.map { it.second }.sortedBy { it.offsetMinutes } }

        override suspend fun add(taskId: String, offsetMinutes: Int) {
            rows.value = rows.value + (taskId to TaskReminder("r${next++}", offsetMinutes, false))
        }

        override suspend fun setRepeatUntilDone(reminderId: String, repeat: Boolean) {
            rows.value =
                rows.value.map { (t, r) -> t to if (r.id == reminderId) r.copy(repeatUntilDone = repeat) else r }
        }

        override suspend fun remove(reminderId: String) {
            rows.value = rows.value.filterNot { it.second.id == reminderId }
        }
    }

    private val viewModel by lazy { TaskRemindersViewModel(SavedStateHandle(mapOf("taskId" to "t")), repository) }

    @Test
    fun addedPresetsLeaveThePickerAndComeBackWhenRemoved() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.add(-60)
        advanceUntilIdle()
        assertEquals(listOf(-60), viewModel.uiState.value.reminders.map { it.offsetMinutes })
        assertEquals(listOf(0, -10, -24 * 60), viewModel.uiState.value.available)

        viewModel.setRepeatUntilDone(viewModel.uiState.value.reminders.single(), true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.reminders.single().repeatUntilDone)

        viewModel.remove(viewModel.uiState.value.reminders.single())
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.reminders.isEmpty())
        assertEquals(4, viewModel.uiState.value.available.size)
    }
}
