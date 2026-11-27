package com.thesasan1.app.feature.habits

import com.thesasan1.app.core.database.entity.HabitMeasurement
import com.thesasan1.app.core.database.entity.HabitScheduleType
import com.thesasan1.app.core.database.entity.WeekdayMask
import com.thesasan1.app.feature.habits.data.OfflineHabitRepository
import com.thesasan1.app.feature.habits.ui.HabitMessage
import com.thesasan1.app.feature.habits.ui.HabitsViewModel
import com.thesasan1.app.feature.habits.ui.NewHabitForm
import com.thesasan1.app.feature.habits.ui.of
import java.time.DayOfWeek
import java.time.LocalTime
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
    private val reminders = FakeHabitReminders()
    private val vm by lazy { HabitsViewModel(repository, reminders, clock) }

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

    @Test
    fun editOpensWithTheHabitsValuesAndSavesTheChange() = runTest(main.dispatcher) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.create(NewHabitForm(name = "Walk"))
        advanceUntilIdle()

        val day = vm.uiState.value.days.single()
        vm.openEdit(day)
        advanceUntilIdle()
        assertEquals(NewHabitForm(name = "Walk"), NewHabitForm.of(day.habit))
        vm.saveEdit(NewHabitForm(name = "", days = setOf(DayOfWeek.MONDAY)))
        advanceUntilIdle()
        assertEquals(HabitMessage.NameRequired, vm.uiState.value.message)

        vm.saveEdit(NewHabitForm(name = "Evening walk", days = setOf(DayOfWeek.MONDAY)))
        advanceUntilIdle()
        val edited = vm.uiState.value.days.single().habit
        assertNull(vm.uiState.value.editing)
        assertEquals("Evening walk", edited.name)
        assertEquals(WeekdayMask.of(DayOfWeek.MONDAY), edited.scheduleDays)
    }

    @Test
    fun reminderTimeIsSavedOnCreateAndClearedOnEdit() = runTest(main.dispatcher) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.create(NewHabitForm(name = "Stretch", reminder = LocalTime.of(7, 30)))
        advanceUntilIdle()
        val day = vm.uiState.value.days.single()
        assertEquals(LocalTime.of(7, 30), reminders.times.value[day.habit.id])

        vm.openEdit(day)
        advanceUntilIdle()
        assertEquals(LocalTime.of(7, 30), vm.uiState.value.editingReminder)
        vm.saveEdit(NewHabitForm.of(day.habit).copy(reminder = null))
        advanceUntilIdle()
        assertNull(reminders.times.value[day.habit.id])
        assertNull(vm.uiState.value.editing)
    }

    @Test
    fun aTimesAWeekHabitIsDueEveryDayAndShowsTheWeek() = runTest(main.dispatcher) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.create(NewHabitForm(name = "Gym", perWeek = 3))
        advanceUntilIdle()
        val gym = vm.uiState.value.days.single()
        assertEquals(HabitScheduleType.X_PER_WEEK, gym.habit.scheduleType)
        assertEquals(3, gym.habit.scheduleCount)
        assertTrue(gym.scheduled)
        assertEquals(0, gym.weekDone)
        assertEquals(3, NewHabitForm.of(gym.habit).perWeek)

        vm.checkIn(gym)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.days.single().weekDone)
    }
}
