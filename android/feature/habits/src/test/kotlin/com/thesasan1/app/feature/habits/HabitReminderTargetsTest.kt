package com.thesasan1.app.feature.habits

import com.thesasan1.app.core.common.getOrNull
import com.thesasan1.app.core.database.entity.HabitEventType
import com.thesasan1.app.core.database.entity.HabitMeasurement
import com.thesasan1.app.core.database.entity.HabitScheduleType
import com.thesasan1.app.core.database.entity.ReminderTargetType
import com.thesasan1.app.core.database.entity.WeekdayMask
import com.thesasan1.app.feature.habits.data.OfflineHabitRepository
import com.thesasan1.app.feature.habits.domain.HabitDraft
import com.thesasan1.app.feature.habits.reminders.HabitReminderActions
import com.thesasan1.app.feature.habits.reminders.HabitReminderTargets
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HabitReminderTargetsTest {
    // Wed 2026-11-25 in the clock's zone (UTC).
    private val clock = MutableClock(1_795_608_000_000)
    private val today = LocalDate.of(2026, 11, 25)
    private val dao = FakeHabitDao()
    private val repository =
        OfflineHabitRepository(dao, FakeOutboxDao(), DirectTransactionRunner, SequentialIds(), clock)
    private val targets = HabitReminderTargets(dao, clock)
    private val actions = HabitReminderActions(dao, repository, clock)

    private suspend fun create(draft: HabitDraft) = repository.create(draft).getOrNull()!!

    @Test
    fun dueDatesSkipTodayOnceDoneAndDaysNotDue() = runTest {
        val mwf = create(
            HabitDraft(
                "Gym",
                today,
                scheduleType = HabitScheduleType.WEEKDAYS,
                scheduleDays = WeekdayMask.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            ),
        )
        assertEquals(
            listOf("2026-11-25", "2026-11-27", "2026-11-30", "2026-12-02"),
            targets.find(ReminderTargetType.HABIT, mwf.id)?.dueDates,
        )

        repository.checkIn(mwf.id, today)
        val target = targets.find(ReminderTargetType.HABIT, mwf.id)
        assertEquals("2026-11-27", target?.dueDates?.first())
        assertEquals("Gym", target?.title)
        assertTrue(target?.isOpen == true)

        repository.archive(mwf.id)
        assertFalse(targets.find(ReminderTargetType.HABIT, mwf.id)?.isOpen ?: true)
        assertNull(targets.find(ReminderTargetType.TASK, mwf.id))
    }

    @Test
    fun doneChecksInAYesNoHabitButOnlyClearsAnAmountHabit() = runTest {
        val walk = create(HabitDraft("Walk", today))
        val pages = create(HabitDraft("Read", today, measurement = HabitMeasurement.NUMERIC))

        assertTrue(actions.complete(ReminderTargetType.HABIT, walk.id))
        assertTrue(actions.complete(ReminderTargetType.HABIT, pages.id))
        assertFalse(actions.complete(ReminderTargetType.TASK, walk.id))

        val checkIns = dao.events.value.filter { it.eventType == HabitEventType.CHECK_IN }
        assertEquals(listOf(walk.id), checkIns.map { it.habitId })
    }
}
