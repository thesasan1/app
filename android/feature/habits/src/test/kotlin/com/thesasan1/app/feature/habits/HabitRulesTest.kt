package com.thesasan1.app.feature.habits

import com.thesasan1.app.core.database.entity.HabitEntity
import com.thesasan1.app.core.database.entity.HabitEventEntity
import com.thesasan1.app.core.database.entity.HabitEventType
import com.thesasan1.app.core.database.entity.HabitMeasurement
import com.thesasan1.app.core.database.entity.HabitScheduleType
import com.thesasan1.app.core.database.entity.SyncColumns
import com.thesasan1.app.core.database.entity.WeekdayMask
import com.thesasan1.app.feature.habits.domain.HabitRules
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HabitRulesTest {

    // 2026-11-23 is a Monday.
    private val monday = LocalDate.of(2026, 11, 23)

    private fun habit(
        type: HabitScheduleType = HabitScheduleType.DAILY,
        measurement: HabitMeasurement = HabitMeasurement.BINARY,
        target: Double? = null,
        days: Int? = null,
        rest: Int? = null,
    ) = HabitEntity(
        id = "h",
        name = "h",
        measurementType = measurement,
        targetValue = target,
        scheduleType = type,
        scheduleDays = days,
        restDays = rest,
        startDate = "2026-11-01",
        sync = SyncColumns.new(0),
    )

    private fun event(id: String, type: HabitEventType, value: Double? = null, of: String? = null, at: Long = 0) =
        HabitEventEntity(
            id = id,
            habitId = "h",
            localDate = monday.toString(),
            eventType = type,
            value = value,
            correctsEventId = of,
            recordedAt = at,
        )

    @Test
    fun weekdayHabitsAreDueOnlyOnTheirDaysAndNeverOnRestDaysOrBeforeTheStart() {
        val mwf = habit(HabitScheduleType.WEEKDAYS, days = WeekdayMask.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))

        assertTrue(HabitRules.isScheduledOn(mwf, monday))
        assertFalse(HabitRules.isScheduledOn(mwf, monday.plusDays(1)))
        assertFalse(HabitRules.isScheduledOn(habit(rest = WeekdayMask.of(DayOfWeek.MONDAY)), monday))
        assertFalse(HabitRules.isScheduledOn(habit(), LocalDate.of(2026, 10, 31)))
        assertTrue(HabitRules.isScheduledOn(habit(HabitScheduleType.X_PER_WEEK), monday.plusDays(2)))
    }

    @Test
    fun undoRemovesAnEventAndAnUndoneCorrectionNoLongerApplies() {
        val events = listOf(
            event("a", HabitEventType.CHECK_IN, 5.0),
            event("b", HabitEventType.CHECK_IN, 3.0),
            event("u", HabitEventType.UNDO, of = "b"),
            event("c1", HabitEventType.CORRECTION, 8.0, of = "a", at = 1),
            event("c2", HabitEventType.CORRECTION, 9.0, of = "a", at = 2),
            event("u2", HabitEventType.UNDO, of = "c2"),
        )

        val counted = HabitRules.effective(events)

        assertEquals(listOf("a"), counted.map { it.id })
        assertEquals(8.0, counted.single().value)
    }

    @Test
    fun numericHabitsCompleteAtTheirTargetAndMultiCheckInsCountCheckIns() {
        val pages = habit(measurement = HabitMeasurement.NUMERIC, target = 20.0)
        val day = HabitRules.day(
            pages,
            monday,
            listOf(event("a", HabitEventType.CHECK_IN, 12.0), event("b", HabitEventType.CHECK_IN, 8.0)),
        )
        assertEquals(20.0, day.progress)
        assertTrue(day.isComplete)

        val water = habit(measurement = HabitMeasurement.MULTI_CHECKIN, target = 3.0)
        val twice = HabitRules.day(
            water,
            monday,
            listOf(event("a", HabitEventType.CHECK_IN), event("b", HabitEventType.CHECK_IN)),
        )
        assertEquals(2.0, twice.progress)
        assertFalse(twice.isComplete)
    }

    @Test
    fun aWeeklyTargetDayCountsOnceAnythingIsDoneAndRestIsReported() {
        val weekly = habit(HabitScheduleType.WEEKLY_TARGET, HabitMeasurement.NUMERIC, target = 100.0)

        assertTrue(HabitRules.day(weekly, monday, listOf(event("a", HabitEventType.CHECK_IN, 10.0))).isComplete)
        assertTrue(HabitRules.day(habit(), monday, listOf(event("r", HabitEventType.REST))).rested)
    }

    @Test
    fun timesAWeekCountsCompletedDaysFromMonday() {
        val gym = habit(HabitScheduleType.X_PER_WEEK).copy(scheduleCount = 3)
        val sunday = monday.minusDays(1)
        fun checkIn(id: String, date: LocalDate) = event(id, HabitEventType.CHECK_IN).copy(localDate = date.toString())
        val events = listOf(checkIn("a", sunday), checkIn("b", monday), checkIn("c", monday.plusDays(2)))

        assertEquals(1, HabitRules.weekDone(gym, events, monday.plusDays(1)))
        assertEquals(2, HabitRules.weekDone(gym, events, monday.plusDays(2)))
        assertEquals(null, HabitRules.weekDone(habit(), events, monday))
    }
}
