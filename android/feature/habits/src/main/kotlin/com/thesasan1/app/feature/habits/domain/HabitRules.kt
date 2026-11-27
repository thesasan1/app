package com.thesasan1.app.feature.habits.domain

import com.thesasan1.app.core.database.entity.HabitEntity
import com.thesasan1.app.core.database.entity.HabitEventEntity
import com.thesasan1.app.core.database.entity.HabitEventType
import com.thesasan1.app.core.database.entity.HabitMeasurement
import com.thesasan1.app.core.database.entity.HabitScheduleType
import com.thesasan1.app.core.database.entity.WeekdayMask
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Pure habit logic: schedules, the event log read back, and a day's progress (HAB-01, HAB-02, HAB-07). */
object HabitRules {

    /**
     * Whether [habit] is due on [date]. Weekly and monthly targets and N-days-a-week habits can be
     * done on any day, so every day counts. CUSTOM rules are not read yet and count as every day.
     */
    fun isScheduledOn(habit: HabitEntity, date: LocalDate): Boolean {
        val day = date.dayOfWeek
        val onSchedule = when (habit.scheduleType) {
            HabitScheduleType.WEEKDAYS -> WeekdayMask.contains(habit.scheduleDays, day)
            else -> true
        }
        return onSchedule && date >= LocalDate.parse(habit.startDate) && !WeekdayMask.contains(habit.restDays, day)
    }

    /**
     * The events that still count: UNDOs remove the event they point at (an undone correction no
     * longer applies), and the latest CORRECTION replaces a check-in's value. Returns check-ins,
     * rest days and freezes in their recorded order.
     */
    fun effective(events: List<HabitEventEntity>): List<HabitEventEntity> {
        val undone = events.filter { it.eventType == HabitEventType.UNDO }.mapNotNullTo(mutableSetOf()) {
            it.correctsEventId
        }
        val corrections = events
            .filter { it.eventType == HabitEventType.CORRECTION && it.id !in undone && it.correctsEventId != null }
            .groupBy { checkNotNull(it.correctsEventId) }
            .mapValues { (_, list) -> list.maxBy { it.recordedAt } }
        return events
            .filter { it.eventType in COUNTED && it.id !in undone }
            .map { event -> corrections[event.id]?.let { event.copy(value = it.value) } ?: event }
    }

    /** Check-in count for binary and multi-check-in habits, or the summed amount otherwise. */
    fun progress(habit: HabitEntity, checkIns: List<HabitEventEntity>): Double = when (habit.measurementType) {
        HabitMeasurement.BINARY, HabitMeasurement.MULTI_CHECKIN -> checkIns.size.toDouble()
        HabitMeasurement.NUMERIC, HabitMeasurement.DURATION -> checkIns.sumOf { it.value ?: 0.0 }
    }

    /**
     * Whether a day's [progress] meets the habit. For weekly and monthly targets the target is a
     * period total, so a day counts once anything was done; the period itself is judged elsewhere.
     */
    fun isComplete(habit: HabitEntity, progress: Double): Boolean {
        val periodTarget = habit.scheduleType == HabitScheduleType.WEEKLY_TARGET ||
            habit.scheduleType == HabitScheduleType.MONTHLY_TARGET
        val target = habit.targetValue
        return when {
            periodTarget || habit.measurementType == HabitMeasurement.BINARY -> progress > 0
            target == null -> progress > 0
            else -> progress >= target
        }
    }

    /**
     * For an "N times a week" habit, the completed days from the Monday of [date]'s week up to
     * [date]; null for other schedules. [events] may hold other days and other habits.
     */
    fun weekDone(habit: HabitEntity, events: List<HabitEventEntity>, date: LocalDate): Int? {
        if (habit.scheduleType != HabitScheduleType.X_PER_WEEK) return null
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val byDate = events.filter { it.habitId == habit.id }.groupBy { it.localDate }
        return generateSequence(monday) { it.plusDays(1) }.takeWhile { it <= date }
            .count { day(habit, it, byDate[it.toString()].orEmpty()).isComplete }
    }

    /** One habit's [HabitDay] for [date] from that day's raw [events]. */
    fun day(habit: HabitEntity, date: LocalDate, events: List<HabitEventEntity>): HabitDay {
        val counted = effective(events)
        val checkIns = counted.filter { it.eventType == HabitEventType.CHECK_IN }
        val progress = progress(habit, checkIns)
        return HabitDay(
            habit = habit,
            date = date,
            scheduled = isScheduledOn(habit, date),
            rested = counted.any { it.eventType == HabitEventType.REST },
            checkIns = checkIns,
            progress = progress,
            isComplete = isComplete(habit, progress),
        )
    }

    private val COUNTED = setOf(HabitEventType.CHECK_IN, HabitEventType.REST, HabitEventType.FREEZE_USED)
}
