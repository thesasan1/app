package com.thesasan1.app.feature.habits

import com.thesasan1.app.core.database.entity.HabitEntity
import com.thesasan1.app.core.database.entity.HabitEventEntity
import com.thesasan1.app.core.database.entity.HabitEventType
import com.thesasan1.app.core.database.entity.HabitScheduleType
import com.thesasan1.app.core.database.entity.SyncColumns
import com.thesasan1.app.core.database.entity.WeekdayMask
import com.thesasan1.app.feature.habits.domain.HabitStreak
import com.thesasan1.app.feature.habits.domain.HabitStreaks
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HabitStreaksTest {

    // 2026-11-23 is a Monday; the habit starts then.
    private val start = LocalDate.of(2026, 11, 23)
    private var next = 0

    private fun habit(type: HabitScheduleType = HabitScheduleType.DAILY, days: Int? = null) = HabitEntity(
        id = "h",
        name = "h",
        scheduleType = type,
        scheduleDays = days,
        startDate = start.toString(),
        sync = SyncColumns.new(0),
    )

    private fun on(offset: Long, type: HabitEventType = HabitEventType.CHECK_IN) = HabitEventEntity(
        id = "e${next++}",
        habitId = "h",
        localDate = start.plusDays(offset).toString(),
        eventType = type,
        recordedAt = next.toLong(),
    )

    @Test
    fun aMissedDayEndsTheRunAndBestRemembersTheLongest() {
        val events = listOf(on(0), on(1), on(2), on(4))

        assertEquals(HabitStreak(current = 1, best = 3), HabitStreaks.compute(habit(), events, start.plusDays(4)))
    }

    @Test
    fun todayNotDoneYetKeepsTheRun() {
        val events = listOf(on(0), on(1))

        assertEquals(HabitStreak(current = 2, best = 2), HabitStreaks.compute(habit(), events, start.plusDays(2)))
    }

    @Test
    fun restDaysFreezesAndDaysNotDueDoNotBreakIt() {
        val mwf = habit(
            HabitScheduleType.WEEKDAYS,
            WeekdayMask.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        )
        // Mon done, Wed rest, Fri freeze, next Mon done; Tue, Thu, weekend not due.
        val events = listOf(on(0), on(2, HabitEventType.REST), on(4, HabitEventType.FREEZE_USED), on(7))

        assertEquals(HabitStreak(current = 2, best = 2), HabitStreaks.compute(mwf, events, start.plusDays(8)))
    }

    @Test
    fun anUndoneCheckInDoesNotCountAndWeeklyTargetsHaveNoDayStreak() {
        val checkIn = on(0)
        val undo = on(0, HabitEventType.UNDO).copy(correctsEventId = checkIn.id)

        assertEquals(HabitStreak(0, 0), HabitStreaks.compute(habit(), listOf(checkIn, undo), start.plusDays(1)))
        assertNull(HabitStreaks.compute(habit(HabitScheduleType.WEEKLY_TARGET), emptyList(), start))
    }
}
