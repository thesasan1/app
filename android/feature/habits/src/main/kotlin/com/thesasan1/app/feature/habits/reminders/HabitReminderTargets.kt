package com.thesasan1.app.feature.habits.reminders

import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.core.database.dao.HabitDao
import com.thesasan1.app.core.database.entity.HabitMeasurement
import com.thesasan1.app.core.database.entity.ReminderTargetType
import com.thesasan1.app.core.reminders.domain.ReminderTarget
import com.thesasan1.app.core.reminders.domain.ReminderTargetActions
import com.thesasan1.app.core.reminders.domain.ReminderTargets
import com.thesasan1.app.feature.habits.domain.HabitRepository
import com.thesasan1.app.feature.habits.domain.HabitRules
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * What the reminder scheduler needs about a habit (ADR-032): its name and the coming days it is
 * due and not yet done or rested, today first. Archived and deleted habits stop reminding.
 */
class HabitReminderTargets @Inject constructor(private val dao: HabitDao, private val clock: Clock) :
    ReminderTargets {
    override suspend fun find(type: ReminderTargetType, id: String): ReminderTarget? {
        val habit = if (type == ReminderTargetType.HABIT) dao.getById(id) else null
        return habit?.let {
            val today = LocalDate.now(clock)
            val due = (0L until LOOKAHEAD_DAYS).map(today::plusDays).filter { date ->
                val day = HabitRules.day(habit, date, dao.getEventsOn(habit.id, date.toString()))
                day.scheduled && !day.rested && !day.isComplete
            }
            ReminderTarget(
                title = habit.name,
                dueDate = null,
                dueTime = null,
                isOpen = habit.archivedAt == null && habit.sync.deletedAt == null,
                dueDates = due.map(LocalDate::toString),
            )
        }
    }

    private companion object {
        /** Enough to reach next week's day for a once-a-week habit done today. */
        const val LOOKAHEAD_DAYS = 8L
    }
}

/**
 * "Done" on a habit reminder checks the habit in for today through the repository (same rules and
 * outbox entry as the app). Amount and minutes habits need a number, so Done only clears those.
 */
class HabitReminderActions @Inject constructor(
    private val dao: HabitDao,
    private val habits: HabitRepository,
    private val clock: Clock,
) : ReminderTargetActions {
    override suspend fun complete(type: ReminderTargetType, id: String): Boolean {
        val habit = if (type == ReminderTargetType.HABIT) dao.getById(id) else null
        val oneTap = habit?.measurementType == HabitMeasurement.BINARY ||
            habit?.measurementType == HabitMeasurement.MULTI_CHECKIN
        return habit != null && (!oneTap || habits.checkIn(id, LocalDate.now(clock)) is Outcome.Success)
    }
}
