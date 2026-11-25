package com.thesasan1.app.core.reminders.android

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
