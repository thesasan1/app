package com.thesasan1.app.feature.habits

import com.thesasan1.app.core.common.AppError
import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.core.common.getOrNull
import com.thesasan1.app.core.database.entity.HabitEventType
import com.thesasan1.app.core.database.entity.HabitMeasurement
import com.thesasan1.app.core.database.entity.HabitScheduleType
import com.thesasan1.app.feature.habits.data.OfflineHabitRepository
import com.thesasan1.app.feature.habits.domain.HabitDraft
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OfflineHabitRepositoryTest {

    // 2026-11-25T12:00Z
    private val clock = MutableClock(1_795_608_000_000)
    private val today = LocalDate.of(2026, 11, 25)
    private val dao = FakeHabitDao()
    private val outbox = FakeOutboxDao()
    private val repository = OfflineHabitRepository(dao, outbox, DirectTransactionRunner, SequentialIds(), clock)

    private suspend fun create(draft: HabitDraft = HabitDraft(name = "  Read  ", startDate = today.minusDays(7))) =
        repository.create(draft).getOrNull()!!

    private fun Outcome<*>.validationField() = ((this as Outcome.Failure).error as AppError.Validation).field

    @Test
    fun createTrimsTheNameAndQueuesTheHabit() = runTest {
        val habit = create()

        assertEquals("Read", habit.name)
        assertEquals(listOf("habit"), outbox.entries.map { it.entityType })
        assertTrue(outbox.entries.single().payload!!.contains("\"name\":\"Read\""))
    }

    @Test
    fun createRejectsAnEmptyNameAWeekdayHabitWithNoDaysAndCustomRules() = runTest {
        assertEquals("name", repository.create(HabitDraft("  ", today)).validationField())
        assertEquals(
            "schedule_days",
            repository.create(HabitDraft("Gym", today, scheduleType = HabitScheduleType.WEEKDAYS)).validationField(),
        )
        assertEquals(
            "schedule_type",
            repository.create(HabitDraft("Gym", today, scheduleType = HabitScheduleType.CUSTOM)).validationField(),
        )
        assertTrue(outbox.entries.isEmpty())
    }

    @Test
    fun aBinaryHabitChecksInOnceADayAndNotInTheFuture() = runTest {
        val habit = create()

        assertTrue(repository.checkIn(habit.id, today) is Outcome.Success)
        assertEquals("date", repository.checkIn(habit.id, today).validationField())
        assertEquals("date", repository.checkIn(habit.id, today.plusDays(1)).validationField())

        val day = repository.observeDay(today).first().single()
        assertTrue(day.isComplete)
    }

    @Test
    fun undoAppendsAnEventAndTheDayIsOpenAgain() = runTest {
        val habit = create()
        val checkIn = repository.checkIn(habit.id, today).getOrNull()!!

        assertTrue(repository.undo(checkIn.id) is Outcome.Success)
        assertTrue(repository.undo(checkIn.id) is Outcome.Failure)

        assertEquals(listOf(HabitEventType.CHECK_IN, HabitEventType.UNDO), dao.events.value.map { it.eventType })
        assertFalse(repository.observeDay(today).first().single().isComplete)
        assertTrue(repository.checkIn(habit.id, today) is Outcome.Success)
    }

    @Test
    fun aNumericCheckInNeedsAnAmountAndCanBeCorrected() = runTest {
        val pages = create(
            HabitDraft("Pages", today, measurement = HabitMeasurement.NUMERIC, targetValue = 20.0, unit = "pages"),
        )
        assertEquals("value", repository.checkIn(pages.id, today).validationField())

        val event = repository.checkIn(pages.id, today, 12.0).getOrNull()!!
        repository.correct(event.id, 25.0)

        val day = repository.observeDay(today).first().single()
        assertEquals(25.0, day.progress)
        assertTrue(day.isComplete)
        assertEquals(1, dao.events.value.count { it.eventType == HabitEventType.CHECK_IN })
    }

    @Test
    fun archivedHabitsLeaveTheDayAndCannotBeCheckedIn() = runTest {
        val habit = create()

        repository.archive(habit.id)

        assertTrue(repository.observeDay(today).first().isEmpty())
        assertEquals(listOf(habit.id), repository.observeArchived().first().map { it.id })
        assertEquals(AppError.NotFound, (repository.checkIn(habit.id, today) as Outcome.Failure).error)
    }

    @Test
    fun restIsRecordedOncePerDay() = runTest {
        val habit = create()

        repository.markRest(habit.id, today)
        repository.markRest(habit.id, today)

        assertEquals(1, dao.events.value.count { it.eventType == HabitEventType.REST })
        assertTrue(repository.observeDay(today).first().single().rested)
    }
}
