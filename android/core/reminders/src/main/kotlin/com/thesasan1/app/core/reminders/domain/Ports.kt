package com.thesasan1.app.core.reminders.domain

import com.thesasan1.app.core.database.entity.ReminderTargetType

/** Looks up the task (later: habit, note) a reminder belongs to. */
fun interface ReminderTargets {
    suspend fun find(type: ReminderTargetType, id: String): ReminderTarget?
}

/** Sets and cancels the OS alarm for one occurrence. */
interface AlarmScheduler {
    /** True when alarms ring at the exact minute; false means Android may delay them (REM-07). */
    val canScheduleExact: Boolean

    /** Sets (or replaces) the alarm for [occurrenceId] at [atMillis]. */
    fun schedule(occurrenceId: String, atMillis: Long)

    fun cancel(occurrenceId: String)
}

/** Shows a reminder notification. */
interface ReminderNotifier {
    /** Returns false when notifications are blocked, so the occurrence is not marked delivered. */
    fun show(occurrenceId: String, title: String, text: String?): Boolean
}
