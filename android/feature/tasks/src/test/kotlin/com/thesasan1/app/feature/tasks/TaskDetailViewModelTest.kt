package com.thesasan1.app.feature.tasks

import androidx.lifecycle.SavedStateHandle
import com.thesasan1.app.core.common.getOrNull
import com.thesasan1.app.feature.tasks.data.OfflineTaskRepository
import com.thesasan1.app.feature.tasks.domain.TaskDraft
import com.thesasan1.app.feature.tasks.domain.TaskEventType
import com.thesasan1.app.feature.tasks.domain.TaskPriority
import com.thesasan1.app.feature.tasks.domain.TaskStatus
import com.thesasan1.app.feature.tasks.ui.TaskDetailViewModel
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class TaskDetailViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val clock = MutableClock(1_764_000_000_000) // 2025-11-24 in UTC
    private val today = LocalDate.of(2025, 11, 24)
    private val repository =
        OfflineTaskRepository(FakeTaskDao(), FakeOutboxDao(), DirectTransactionRunner, SequentialIds(), clock)

    private suspend fun task(title: String = "Plan trip") = repository.create(TaskDraft(title)).getOrNull()!!

    private fun detail(id: String) = TaskDetailViewModel(SavedStateHandle(mapOf("taskId" to id)), repository, clock)

    private suspend fun stored(id: String) = repository.get(id).getOrNull()!!

    @Test
    fun `text autosaves after typing pauses and a blank title keeps the saved one`() = runTest(main.dispatcher) {
        val id = task().id
        val vm = detail(id)
        advanceUntilIdle()

        vm.onTitleChange("Plan the trip")
        vm.onDescriptionChange("  train first ")
        advanceTimeBy(TaskDetailViewModel.AUTOSAVE_DEBOUNCE_MS - 1)
        assertEquals("Plan trip", stored(id).title)
        advanceUntilIdle()
        assertEquals("Plan the trip", stored(id).title)
        assertEquals("train first", stored(id).description)

        vm.onTitleChange("  ")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.titleMissing)
        assertEquals("Plan the trip", stored(id).title)
    }

    @Test
    fun `flush saves at once`() = runTest(main.dispatcher) {
        val id = task().id
        val vm = detail(id)
        advanceUntilIdle()

        vm.onTitleChange("Now")
        vm.flush()
        advanceTimeBy(1)
        assertEquals("Now", stored(id).title)
    }

    @Test
    fun `priority, due date and time save immediately`() = runTest(main.dispatcher) {
        val id = task().id
        val vm = detail(id)
        advanceUntilIdle()

        vm.setPriority(TaskPriority.URGENT)
        vm.setDueTime(LocalTime.of(9, 30))
        advanceUntilIdle()
        assertEquals(TaskPriority.URGENT, stored(id).priority)
        assertEquals(today, stored(id).dueDate, "a time on an undated task makes it due today")
        assertEquals(LocalTime.of(9, 30), stored(id).dueTime)

        vm.setDueDate(null)
        advanceUntilIdle()
        assertNull(stored(id).dueDate)
        assertNull(stored(id).dueTime)
        assertNull(vm.uiState.value.task!!.dueTime)
    }

    @Test
    fun `done toggles, and history and subtasks are shown`() = runTest(main.dispatcher) {
        val id = task().id
        val vm = detail(id)
        advanceUntilIdle()

        vm.toggleDone()
        advanceUntilIdle()
        assertEquals(TaskStatus.DONE, vm.uiState.value.task!!.status)
        vm.toggleDone()
        vm.onNewSubtaskChange("Tickets")
        vm.addSubtask()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TaskStatus.TODO, state.task!!.status)
        assertEquals(listOf("Tickets"), state.subtasks.map { it.title })
        assertEquals("", state.newSubtask)
        assertEquals(
            listOf(TaskEventType.CREATED, TaskEventType.COMPLETED, TaskEventType.REOPENED),
            state.history.map { it.type },
        )

        vm.toggleSubtask(state.subtasks.single())
        advanceUntilIdle()
        assertEquals(TaskStatus.DONE, vm.uiState.value.subtasks.single().status)
    }

    @Test
    fun `archive and trash close the screen, and unknown ids show not found`() = runTest(main.dispatcher) {
        val id = task().id
        val vm = detail(id)
        advanceUntilIdle()
        vm.moveToTrash()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isClosed)
        assertTrue(stored(id).isInTrash)

        val other = task("Other").id
        val archiving = detail(other)
        advanceUntilIdle()
        archiving.onTitleChange("Other, renamed")
        archiving.archive()
        advanceUntilIdle()
        assertTrue(archiving.uiState.value.isClosed)
        assertEquals("Other, renamed", stored(other).title, "pending text is saved before archiving")
        assertTrue(stored(other).isArchived)

        val missing = detail("nope")
        advanceUntilIdle()
        assertTrue(missing.uiState.value.notFound)
    }
}
