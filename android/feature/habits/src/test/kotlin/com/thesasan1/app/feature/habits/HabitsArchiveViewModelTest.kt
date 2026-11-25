package com.thesasan1.app.feature.habits

import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.feature.habits.data.OfflineHabitRepository
import com.thesasan1.app.feature.habits.domain.HabitDraft
import com.thesasan1.app.feature.habits.ui.HabitsArchiveViewModel
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsArchiveViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val clock = MutableClock(1_795_608_000_000)
    private val today = LocalDate.of(2026, 11, 25)
    private val dao = FakeHabitDao()
    private val repository =
        OfflineHabitRepository(dao, FakeOutboxDao(), DirectTransactionRunner, SequentialIds(), clock)
    private val vm by lazy { HabitsArchiveViewModel(repository) }

    private suspend fun archived(name: String): String {
        val id = (repository.create(HabitDraft(name, today)) as Outcome.Success).value.id
        repository.archive(id)
        return id
    }

    @Test
    fun restoringPutsTheHabitBackOnTodaysList() = runTest(main.dispatcher) {
        val id = archived("Read")
        val state = vm.uiState
        backgroundScope.launch { state.collect {} }
        advanceUntilIdle()
        assertEquals(listOf("Read"), vm.uiState.value.habits.map { it.name })

        vm.restore(vm.uiState.value.habits.single())
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEmpty)
        assertEquals(listOf(id), repository.observeActive().first().map { it.id })
    }

    @Test
    fun deleteWaitsForTheConfirmation() = runTest(main.dispatcher) {
        val id = archived("Run")
        val state = vm.uiState
        backgroundScope.launch { state.collect {} }
        advanceUntilIdle()

        vm.askDelete(vm.uiState.value.habits.single())
        vm.cancelDelete()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.habits.size)

        vm.askDelete(vm.uiState.value.habits.single())
        vm.confirmDelete()
        advanceUntilIdle()
        assertNull(vm.uiState.value.confirmingDelete)
        assertTrue(vm.uiState.value.isEmpty)
        assertNotNull(dao.habits.value.getValue(id).sync.deletedAt)
    }
}
