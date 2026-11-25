package com.thesasan1.app.core.reminders.domain

import com.thesasan1.app.core.common.logging.AppLogger
import com.thesasan1.app.core.common.logging.info
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
 */
@Singleton
class ReminderScheduler @Inject constructor(
    private val dao: ReminderDao,
    private val targets: ReminderTargets,
    private val alarms: AlarmScheduler,
    private val notifier: ReminderNotifier,
    private val clock: Clock,
    private val logger: AppLogger,
) {
    private val mutex = Mutex()

    suspend fun rebuild() = mutex.withLock { rebuildLocked() }

    suspend fun deliver(occurrenceId: String) = mutex.withLock {
        val occurrence = dao.getOccurrence(occurrenceId)
        if (occurrence != null && occurrence.state in PENDING) {
            val reminder = dao.getById(occurrence.reminderId)?.takeIf { it.isEnabled && it.sync.deletedAt == null }
            val target = reminder?.targetId?.let { targets.find(reminder.targetType, it) }
            val title = target?.title ?: reminder?.title
            val state = when {
                reminder == null || title == null || target?.isOpen == false -> OccurrenceState.DISMISSED
                notifier.show(occurrence.id, title, null) -> OccurrenceState.DELIVERED
                else -> OccurrenceState.MISSED
            }
            val now = clock.millis()
            dao.updateOccurrence(
                occurrence.copy(state = state, deliveredAt = now.takeIf { state == OccurrenceState.DELIVERED }),
            )
            // The alarm has fired; cancelling drops any duplicate still queued for this occurrence.
            alarms.cancel(occurrence.id)
            logger.info(TAG) { "occurrence ${occurrence.id} -> $state" }
        }
        rebuildLocked()
    }

    private suspend fun rebuildLocked() {
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

    private suspend fun nextFireMillis(reminder: ReminderEntity): Long? {
        val target = reminder.targetId?.let { targets.find(reminder.targetType, it) }
        if (reminder.targetId != null && target == null) return null
        return ReminderTimeCalculator.fireAt(reminder, target, clock.zone)?.toEpochMilli()
    }

    companion object {
        private const val TAG = "Reminders"
        private val PENDING = setOf(OccurrenceState.SCHEDULED, OccurrenceState.SNOOZED)

        /** How late a firing may still ring, e.g. after the phone was off. */
        val LATE_GRACE: Duration = Duration.ofHours(12)

        /** How long delivered, dismissed and missed occurrences are kept. */
        val KEEP_FINISHED: Duration = Duration.ofDays(30)
    }
}
