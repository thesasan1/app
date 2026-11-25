package com.thesasan1.app.feature.habits

import com.thesasan1.app.core.database.entity.HabitMeasurement
import com.thesasan1.app.core.database.entity.HabitScheduleType
import com.thesasan1.app.core.database.entity.WeekdayMask
import com.thesasan1.app.feature.habits.data.OfflineHabitRepository
import com.thesasan1.app.feature.habits.ui.HabitMessage
import com.thesasan1.app.feature.habits.ui.HabitsViewModel
import com.thesasan1.app.feature.habits.ui.NewHabitForm
import java.time.DayOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    // 2026-11-25T12:00Z, a Wednesday in the clock's zone (UTC).
    private val clock = MutableClock(1_795_608_000_000)
    private val dao = FakeHabitDao()
    private val repository =
        OfflineHabitRepository(dao, FakeOutboxDao(), DirectTransactionRunner, SequentialIds(), clock)
    private val vm by lazy { HabitsViewModel(repository, clock) }

    @Test
    fun creatingAHabitClosesTheDialogAndListsIt() = runTest(main.dispatcher) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.openNew()
        vm.create(NewHabitForm(name = "Walk"))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isCreating)
        assertEquals(listOf("Walk"), state.days.map { it.habit.name })
        assertEquals(HabitScheduleType.DAILY, state.days.single().habit.scheduleType)
    }

    @Test
    fun pickingSomeDaysMakesAWeekdayHabitAndNoDaysIsRefused() = runTest(main.dispatcher) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.openNew()
        vm.create(NewHabitForm(name = "Gym", days = emptySet()))
        advanceUntilIdle()
        assertEquals(HabitMessage.DaysRequired, vm.uiState.value.message)
        assertTrue(vm.uiState.value.isCreating)

        vm.create(NewHabitForm(name = "Gym", days = setOf(DayOfWeek.MONDAY)))
        advanceUntilIdle()
        val gym = vm.uiState.value.days.single()
        assertEquals(WeekdayMask.of(DayOfWeek.MONDAY), gym.habit.scheduleDays)
        assertFalse(gym.scheduled, "Monday-only is not due on a Wednesday")
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun checkInAndUndoAYesNoHabit() = runTest(main.dispatcher) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.create(NewHabitForm(name = "Read"))
        advanceUntilIdle()

        vm.checkIn(vm.uiState.value.days.single())
        advanceUntilIdle()
        assertTrue(vm.uiState.value.days.single().isComplete)

        vm.undoLast(vm.uiState.value.days.single())
        advanceUntilIdle()
        assertFalse(vm.uiState.value.days.single().isComplete)
    }

    @Test
    fun anAmountHabitAsksForTheAmountBeforeLogging() = runTest(main.dispatcher) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.create(NewHabitForm(name = "Pages", measurement = HabitMeasurement.NUMERIC, target = "20", unit = "pages"))
        advanceUntilIdle()

        vm.checkIn(vm.uiState.value.days.single())
        advanceUntilIdle()
        assertEquals("Pages", vm.uiState.value.logging?.habit?.name)
        assertTrue(dao.events.value.isEmpty())

        vm.logAmount(25.0)
        advanceUntilIdle()
        val day = vm.uiState.value.days.single()
        assertNull(vm.uiState.value.logging)
        assertEquals(25.0, day.progress)
        assertTrue(day.isComplete)
    }
}
