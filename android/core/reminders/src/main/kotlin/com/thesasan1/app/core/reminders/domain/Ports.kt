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

/** Acts on what a reminder belongs to. Implemented by the feature that owns the target. */
fun interface ReminderTargetActions {
    /** Marks the target done ("Done" on a task reminder). Returns false if it can't be found. */
    suspend fun complete(type: ReminderTargetType, id: String): Boolean
}

/** One reminder notification. Keyed by [occurrenceId], so showing it again replaces it. */
data class ReminderNotification(
    val occurrenceId: String,
    val title: String,
    /** Repeat-until-done: says so on the notification, since swiping it away won't stop it. */
    val repeats: Boolean = false,
)

/** Shows and clears reminder notifications. */
interface ReminderNotifier {
    /** Returns false when notifications are blocked, so the occurrence is not marked delivered. */
    fun show(notification: ReminderNotification): Boolean

    fun cancel(occurrenceId: String)
}
