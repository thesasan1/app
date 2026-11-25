package com.thesasan1.app.core.reminders.android

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.thesasan1.app.core.reminders.domain.AlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * [AlarmScheduler] on AlarmManager (ARCHITECTURE.md, reminders). Exact and allowed while idle
 * when the app may set exact alarms; otherwise inexact while idle, which Android may delay
 * (REM-07). Each occurrence has its own intent data URI, so its PendingIntent is unique and
 * setting it again replaces the old alarm instead of adding one.
 */
class AndroidAlarmScheduler @Inject constructor(@ApplicationContext private val context: Context) : AlarmScheduler {
    private val alarmManager: AlarmManager? = context.getSystemService()

    override val canScheduleExact: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager?.canScheduleExactAlarms() == true

    override fun schedule(occurrenceId: String, atMillis: Long) {
        val manager = alarmManager ?: return
        val operation = pendingIntent(occurrenceId, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        if (canScheduleExact) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, operation)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, operation)
        }
    }

    override fun cancel(occurrenceId: String) {
        val operation = pendingIntent(occurrenceId, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager?.cancel(operation)
        operation.cancel()
    }

    private fun pendingIntent(occurrenceId: String, flag: Int): PendingIntent? {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
            .setAction(ReminderAlarmReceiver.ACTION_FIRE)
            .setData("personalos://reminder-occurrence/$occurrenceId".toUri())
            .putExtra(ReminderAlarmReceiver.EXTRA_OCCURRENCE_ID, occurrenceId)
        return PendingIntent.getBroadcast(context, 0, intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }
}
