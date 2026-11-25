package com.thesasan1.app.feature.tasks

import com.thesasan1.app.core.common.getOrNull
import com.thesasan1.app.feature.tasks.data.OfflineTaskRepository
import com.thesasan1.app.feature.tasks.domain.TaskDraft
import com.thesasan1.app.feature.tasks.domain.TaskStatus
import com.thesasan1.app.feature.tasks.ui.TasksViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class TasksViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    // 2025-11-24T16:00:00Z, so "today" in the clock's zone (UTC) is 2025-11-24.
    private val clock = MutableClock(1_764_000_000_000)
    private val today = LocalDate.of(2025, 11, 24)
    private val repository =
        OfflineTaskRepository(FakeTaskDao(), FakeOutboxDao(), DirectTransactionRunner, SequentialIds(), clock)

    private fun viewModel() = TasksViewModel(repository, clock)

    @Test
    fun `open tasks are grouped into overdue, today, upcoming and no date`() = runTest(main.dispatcher) {
        repository.create(TaskDraft("Late", dueDate = today.minusDays(1)))
        repository.create(TaskDraft("Now", dueDate = today))
        repository.create(TaskDraft("Soon", dueDate = today.plusDays(3)))
        repository.create(TaskDraft("Whenever"))

        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        val sections = vm.uiState.value.sections
        assertEquals(listOf("Late"), sections.overdue.map { it.title })
        assertEquals(listOf("Now"), sections.today.map { it.title })
        assertEquals(listOf("Soon"), sections.upcoming.map { it.title })
        assertEquals(listOf("Whenever"), sections.someday.map { it.title })
        assertFalse(vm.uiState.value.isEmpty)
    }

    @Test
    fun `quick add creates the task, optionally due today, and clears the field`() = runTest(main.dispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.onQuickAddChange("  Call the clinic ")
        vm.onQuickAddDueTodayChange(true)
        vm.addTask()
        advanceUntilIdle()

        val task = repository.observeOpen().first().single()
        assertEquals("Call the clinic", task.title)
        assertEquals(today, task.dueDate)
        assertEquals("", vm.uiState.value.quickAdd.title)
        assertTrue(vm.uiState.value.quickAdd.dueToday, "the due-today choice sticks for the next task")
    }

    @Test
    fun `blank quick add does nothing and long titles are capped`() = runTest(main.dispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.onQuickAddChange("   ")
        vm.addTask()
        advanceUntilIdle()
        assertTrue(repository.observeOpen().first().isEmpty())

        vm.onQuickAddChange("x".repeat(600))
        advanceUntilIdle()
        assertEquals(500, vm.uiState.value.quickAdd.title.length)
    }

    @Test
    fun `completing hides the task and undo brings it back`() = runTest(main.dispatcher) {
        val task = repository.create(TaskDraft("Pay rent", dueDate = today)).getOrNull()!!
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.complete(vm.uiState.value.sections.today.single())
        advanceUntilIdle()
        assertTrue(vm.uiState.value.sections.today.isEmpty())
        assertEquals(task.id, vm.uiState.value.justCompleted?.id)

        vm.undoComplete()
        advanceUntilIdle()
        assertNull(vm.uiState.value.justCompleted)
        assertEquals(TaskStatus.TODO, repository.get(task.id).getOrNull()!!.status)
        assertEquals(listOf(task.id), vm.uiState.value.sections.today.map { it.id })
    }

    @Test
    fun `refresh regroups after midnight`() = runTest(main.dispatcher) {
        repository.create(TaskDraft("Tomorrow's", dueDate = today.plusDays(1)))
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.sections.upcoming.size)

        clock.now += 24L * 60 * 60 * 1000
        vm.refresh()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.sections.today.size)
    }
}
