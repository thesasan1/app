package com.thesasan1.app.core.reminders.domain

import com.thesasan1.app.core.database.dao.ReminderDao
import com.thesasan1.app.core.database.entity.OccurrenceState
import com.thesasan1.app.core.database.entity.ReminderEntity
import com.thesasan1.app.core.database.entity.ReminderOccurrenceEntity
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps OS alarms in step with the `reminders` table (ADR-025, REM-01, REM-06).
 *
 * [rebuild] is idempotent: run it on app start, after boot, after time or zone changes and
 * whenever a reminder or its task changes. Each reminder's next firing becomes one occurrence
 * (deterministic id, so never twice); occurrences that no longer match are dropped with their
 * alarms; every pending occurrence gets its alarm (re)set. A firing missed by more than
 * [LATE_GRACE] (phone off for a day, say) is recorded as MISSED instead of ringing late.
 *
 * [deliver] is what the alarm calls. It rings an occurrence once: anything not pending is ignored.
 * [complete], [snooze] and [dismissed] are the notification's actions (REM-02). A rebuild also takes
 * down a notification still on screen once its task is closed in the app or its reminder is turned
 * off or removed.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    private val dao: ReminderDao,
    private val targets: ReminderTargets,
    private val actions: ReminderTargetActions,
    private val alarms: AlarmScheduler,
    private val notifier: ReminderNotifier,
    private val clock: Clock,
) {
    private val mutex = Mutex()

    suspend fun rebuild() = mutex.withLock { rebuildLocked() }

    suspend fun deliver(occurrenceId: String) = mutex.withLock {
        val occurrence = dao.getOccurrence(occurrenceId)
        if (occurrence != null && occurrence.state in PENDING) {
            dao.updateOccurrence(ring(occurrence))
            // The alarm has fired; cancelling drops any duplicate still queued for this occurrence.
            alarms.cancel(occurrence.id)
        }
        rebuildLocked()
    }

    /** Shows [occurrence] if it should still ring, and returns its new state. */
    private suspend fun ring(occurrence: ReminderOccurrenceEntity): ReminderOccurrenceEntity {
        val reminder = dao.getById(occurrence.reminderId)?.takeIf { it.isEnabled && it.sync.deletedAt == null }
        val target = reminder?.targetId?.let { targets.find(reminder.targetType, it) }
        val title = target?.title ?: reminder?.title
        if (reminder == null || title == null || target?.isOpen == false) {
            return occurrence.copy(state = OccurrenceState.DISMISSED)
        }
        val now = clock.millis()
        val repeats = reminder.repeatUntilDone && now - occurrence.scheduledForUtc < REPEAT_WINDOW.toMillis()
        return when {
            !notifier.show(ReminderNotification(occurrence.id, title, repeats = repeats)) ->
                occurrence.copy(state = OccurrenceState.MISSED)
            // Repeat until done (REM-02): ring again later unless the user completes it.
            repeats -> occurrence.copy(
                state = OccurrenceState.SNOOZED,
                deliveredAt = now,
                snoozeUntil = now + REPEAT_INTERVAL.toMillis(),
            )
            else -> occurrence.copy(state = OccurrenceState.DELIVERED, deliveredAt = now)
        }
    }

    /** "Done" on the notification: completes the occurrence and its task (REM-02). */
    suspend fun complete(occurrenceId: String) = mutex.withLock {
        val occurrence = dao.getOccurrence(occurrenceId)
        if (occurrence != null && occurrence.state != OccurrenceState.COMPLETED) {
            dao.updateOccurrence(occurrence.copy(state = OccurrenceState.COMPLETED, snoozeUntil = null))
            dao.getById(occurrence.reminderId)?.let { reminder ->
                reminder.targetId?.let { actions.complete(reminder.targetType, it) }
            }
        }
        alarms.cancel(occurrenceId)
        notifier.cancel(occurrenceId)
        rebuildLocked()
    }

    /** "Snooze" on the notification: rings again after [duration]. */
    suspend fun snooze(occurrenceId: String, duration: Duration = SNOOZE) = mutex.withLock {
        val occurrence = dao.getOccurrence(occurrenceId)
        if (occurrence != null && occurrence.state in RINGING) {
            val until = clock.millis() + duration.toMillis()
            dao.updateOccurrence(occurrence.copy(state = OccurrenceState.SNOOZED, snoozeUntil = until))
        }
        notifier.cancel(occurrenceId)
        rebuildLocked()
    }

    /**
     * The notification was swiped away. A delivered one is dismissed; a repeat-until-done one keeps
     * its next repeat, since only "Done" (or turning the reminder off) stops it.
     */
    suspend fun dismissed(occurrenceId: String) = mutex.withLock {
        val occurrence = dao.getOccurrence(occurrenceId)
        if (occurrence?.state == OccurrenceState.DELIVERED) {
            dao.updateOccurrence(occurrence.copy(state = OccurrenceState.DISMISSED))
        }
    }

    private suspend fun rebuildLocked() {
        clearStale()
        val now = clock.millis()
        val wanted = dao.getEnabledTimeReminders().associate { it.id to nextFireMillis(it) }

        dao.getPendingOccurrences().groupBy { it.reminderId }.forEach { (reminderId, pending) ->
            if (pending.any { it.scheduledForUtc != wanted[reminderId] }) {
                pending.forEach { alarms.cancel(it.id) }
                dao.deletePendingOccurrencesFor(reminderId)
            }
        }

        wanted.forEach { (reminderId, at) ->
            if (at != null) {
                val occurrence = ReminderOccurrenceEntity.scheduled(reminderId, at)
                val late = now - at > LATE_GRACE.toMillis()
                dao.insertOccurrence(if (late) occurrence.copy(state = OccurrenceState.MISSED) else occurrence)
            }
        }

        dao.getPendingOccurrences().forEach { alarms.schedule(it.id, maxOf(it.nextFireAt, now)) }
        dao.pruneFinishedOccurrences(now - KEEP_FINISHED.toMillis())
    }

    /** Dismisses shown notifications that no longer apply (task done elsewhere, reminder off or gone). */
    private suspend fun clearStale() {
        dao.getRingingOccurrences().forEach { occurrence ->
            if (!stillApplies(occurrence)) {
                dao.updateOccurrence(occurrence.copy(state = OccurrenceState.DISMISSED, snoozeUntil = null))
                alarms.cancel(occurrence.id)
                notifier.cancel(occurrence.id)
            }
        }
    }

    private suspend fun stillApplies(occurrence: ReminderOccurrenceEntity): Boolean {
        val reminder = dao.getById(occurrence.reminderId)?.takeIf { it.isEnabled && it.sync.deletedAt == null }
        val targetId = reminder?.targetId ?: return reminder != null
        return targets.find(reminder.targetType, targetId)?.isOpen == true
    }

    private suspend fun nextFireMillis(reminder: ReminderEntity): Long? {
        val target = reminder.targetId?.let { targets.find(reminder.targetType, it) }
        if (reminder.targetId != null && target == null) return null
        return ReminderTimeCalculator.fireAt(reminder, target, clock.zone)?.toEpochMilli()
    }

    companion object {
        private val PENDING = setOf(OccurrenceState.SCHEDULED, OccurrenceState.SNOOZED)
        private val RINGING = setOf(OccurrenceState.DELIVERED, OccurrenceState.SNOOZED)

        /** The notification's Snooze button. */
        val SNOOZE: Duration = Duration.ofMinutes(10)

        /** How often a repeat-until-done reminder rings again, and for how long it keeps trying. */
        val REPEAT_INTERVAL: Duration = Duration.ofMinutes(15)
        val REPEAT_WINDOW: Duration = Duration.ofHours(24)

        /** How late a firing may still ring, e.g. after the phone was off. */
        val LATE_GRACE: Duration = Duration.ofHours(12)

        /** How long delivered, dismissed and missed occurrences are kept. */
        val KEEP_FINISHED: Duration = Duration.ofDays(30)
    }
}
