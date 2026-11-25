package com.thesasan1.app.feature.habits.domain

import com.thesasan1.app.core.database.entity.HabitEntity
import com.thesasan1.app.core.database.entity.HabitEventEntity
import com.thesasan1.app.core.database.entity.HabitEventType
import com.thesasan1.app.core.database.entity.HabitScheduleType
import java.time.LocalDate

/** Consecutive due days completed: the run still going and the longest ever (HAB-03, HAB-06). */
data class HabitStreak(val current: Int, val best: Int)

/** Streaks derived from the event log; nothing is stored or rewritten (HAB-07, ADR-030). */
object HabitStreaks {

    /** Day-based schedules have day streaks; weekly and monthly targets are judged per period later. */
    fun hasDayStreak(habit: HabitEntity): Boolean =
        habit.scheduleType == HabitScheduleType.DAILY || habit.scheduleType == HabitScheduleType.WEEKDAYS

    /**
     * Walks each day from the habit's start to [today]. A completed due day adds one. A day that is
     * not due, was a rest day, used a freeze, or is [today] and not done yet leaves the run as it is.
     * Any other missed due day ends the run. [events] may hold other days and other habits.
     */
    fun compute(habit: HabitEntity, events: List<HabitEventEntity>, today: LocalDate): HabitStreak? {
        if (!hasDayStreak(habit)) return null
        val byDate = events.filter { it.habitId == habit.id }.groupBy { it.localDate }
        var run = 0
        var best = 0
        var date = LocalDate.parse(habit.startDate)
        while (date <= today) {
            val day = HabitRules.day(habit, date, byDate[date.toString()].orEmpty())
            val frozen = HabitRules.effective(byDate[date.toString()].orEmpty())
                .any { it.eventType == HabitEventType.FREEZE_USED }
            when {
                !day.scheduled -> Unit
                day.isComplete -> {
                    run++
                    best = maxOf(best, run)
                }
                day.rested || frozen || date == today -> Unit
                else -> run = 0
            }
            date = date.plusDays(1)
        }
        return HabitStreak(current = run, best = best)
    }
}
