package com.thesasan1.app.core.reminders.android

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.thesasan1.app.core.common.ApplicationScope
import com.thesasan1.app.core.common.logging.AppLogger
import com.thesasan1.app.core.common.logging.error
import com.thesasan1.app.core.reminders.domain.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Runs [block] on the app scope and keeps the broadcast alive until it finishes. */
private fun BroadcastReceiver.runAsync(scope: CoroutineScope, logger: AppLogger, block: suspend () -> Unit) {
    val pending = goAsync()
    scope.launch {
        try {
            block()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger.error("Reminders", e) { "reminder broadcast failed" }
        } finally {
            pending.finish()
        }
    }
}

/** Receives a reminder alarm and rings its occurrence (once). */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    @Inject lateinit var logger: AppLogger

    override fun onReceive(context: Context, intent: Intent) {
        val occurrenceId = intent.getStringExtra(EXTRA_OCCURRENCE_ID)
        if (intent.action != ACTION_FIRE || occurrenceId == null) return
        runAsync(scope, logger) { scheduler.deliver(occurrenceId) }
    }

    companion object {
        const val ACTION_FIRE = "com.thesasan1.app.reminders.FIRE"
        const val EXTRA_OCCURRENCE_ID = "occurrence_id"
    }
}

/** The notification's Done and Snooze buttons, and swiping it away (REM-02). */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    @Inject lateinit var logger: AppLogger

    override fun onReceive(context: Context, intent: Intent) {
        val occurrenceId = intent.getStringExtra(EXTRA_OCCURRENCE_ID) ?: return
        when (intent.action) {
            ACTION_COMPLETE -> runAsync(scope, logger) { scheduler.complete(occurrenceId) }
            ACTION_SNOOZE -> runAsync(scope, logger) { scheduler.snooze(occurrenceId) }
            ACTION_DISMISSED -> runAsync(scope, logger) { scheduler.dismissed(occurrenceId) }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.thesasan1.app.reminders.COMPLETE"
        const val ACTION_SNOOZE = "com.thesasan1.app.reminders.SNOOZE"
        const val ACTION_DISMISSED = "com.thesasan1.app.reminders.DISMISSED"
        private const val EXTRA_OCCURRENCE_ID = "occurrence_id"

        /** A unique PendingIntent per action and occurrence (the data URI keeps them apart). */
        fun intent(context: Context, action: String, occurrenceId: String): PendingIntent {
            val intent = Intent(context, ReminderActionReceiver::class.java)
                .setAction(action)
                .setData("personalos://reminder-occurrence/$occurrenceId".toUri())
                .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId)
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}

/**
 * Rebuilds every alarm after a reboot, an app update, a clock or time-zone change, or a change
 * to the exact-alarm permission (REM-06, REM-07). Alarms do not survive a reboot on their own.
 */
@AndroidEntryPoint
class ReminderRescheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    @Inject lateinit var logger: AppLogger

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        runAsync(scope, logger) { scheduler.rebuild() }
    }

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
