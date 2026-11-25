package com.thesasan1.app.feature.tasks

import com.thesasan1.app.core.common.getOrNull
import com.thesasan1.app.feature.tasks.data.OfflineTaskRepository
import com.thesasan1.app.feature.tasks.domain.TaskDraft
import com.thesasan1.app.feature.tasks.domain.TaskStatus
import com.thesasan1.app.feature.tasks.ui.TasksArchiveViewModel
import com.thesasan1.app.feature.tasks.ui.TasksCompletedViewModel
import com.thesasan1.app.feature.tasks.ui.TasksTrashViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class TaskShelfViewModelsTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val clock = MutableClock(1_764_000_000_000)
    private val repository =
        OfflineTaskRepository(FakeTaskDao(), FakeOutboxDao(), DirectTransactionRunner, SequentialIds(), clock)

    private suspend fun task(title: String, parentId: String? = null) =
        repository.create(TaskDraft(title = title, parentId = parentId)).getOrNull()!!

    @Test
    fun `completed tasks can be reopened`() = runTest(main.dispatcher) {
        val done = task("Done one")
        repository.setStatus(done.id, TaskStatus.DONE)
        val vm = TasksCompletedViewModel(repository)
        vm.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        assertEquals(listOf("Done one"), vm.uiState.value.tasks.map { it.title })

        vm.reopen(vm.uiState.value.tasks.single())
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isEmpty)
        assertEquals(TaskStatus.TODO, repository.get(done.id).getOrNull()!!.status)
    }

    @Test
    fun `archived tasks can be unarchived`() = runTest(main.dispatcher) {
        val archived = task("Kept")
        repository.setArchived(archived.id, true)
        val vm = TasksArchiveViewModel(repository)
        vm.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.tasks.size)

        vm.unarchive(vm.uiState.value.tasks.single())
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun `trash restores, refuses an orphaned subtask, and deletes only after confirming`() = runTest(main.dispatcher) {
        val parent = task("Parent")
        val child = task("Child", parentId = parent.id)
        repository.moveToTrash(child.id)
        repository.moveToTrash(parent.id)
        val vm = TasksTrashViewModel(repository)
        vm.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.tasks.size)

        vm.restore(vm.uiState.value.tasks.first { it.id == child.id })
        advanceUntilIdle()
        assertTrue(vm.uiState.value.blocked, "a subtask waits for its parent")
        vm.dismissBlocked()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.blocked)

        vm.requestDelete(vm.uiState.value.tasks.first { it.id == parent.id })
        advanceUntilIdle()
        assertEquals(parent.id, vm.uiState.value.pendingDelete?.id)
        vm.cancelDelete()
        advanceUntilIdle()
        assertNull(vm.uiState.value.pendingDelete)
        assertEquals(2, vm.uiState.value.tasks.size)

        vm.requestDelete(vm.uiState.value.tasks.first { it.id == parent.id })
        vm.confirmDelete()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isEmpty, "deleting the parent removes its subtask too")
    }
}
