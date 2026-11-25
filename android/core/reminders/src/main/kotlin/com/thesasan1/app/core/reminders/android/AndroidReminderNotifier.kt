package com.thesasan1.app.core.reminders.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.thesasan1.app.core.reminders.R
import com.thesasan1.app.core.reminders.domain.ReminderNotification
import com.thesasan1.app.core.reminders.domain.ReminderNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * [ReminderNotifier] that posts to the "Reminders" channel. Returns false when notifications are
 * off or the permission is missing (Android 13+), so the scheduler records the firing as missed
 * rather than delivered (REM-06). Asking for the permission belongs to the reminder UI.
 */
class AndroidReminderNotifier @Inject constructor(@ApplicationContext private val context: Context) :
    ReminderNotifier {

    override fun show(notification: ReminderNotification): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!canPost(manager)) return false
        ensureChannel()
        val id = notification.occurrenceId
        val text = when {
            notification.repeats -> R.string.reminder_notification_repeats
            else -> R.string.reminder_notification_text
        }
        val built = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(notification.title)
            .setContentText(context.getString(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(id))
            .setDeleteIntent(ReminderActionReceiver.intent(context, ReminderActionReceiver.ACTION_DISMISSED, id))
            .addAction(
                0,
                context.getString(R.string.reminder_action_done),
                ReminderActionReceiver.intent(context, ReminderActionReceiver.ACTION_COMPLETE, id),
            )
            .addAction(
                0,
                context.getString(R.string.reminder_action_snooze),
                ReminderActionReceiver.intent(context, ReminderActionReceiver.ACTION_SNOOZE, id),
            )
            .addAction(
                0,
                context.getString(R.string.reminder_action_snooze_long),
                ReminderActionReceiver.intent(context, ReminderActionReceiver.ACTION_SNOOZE_LONG, id),
            )
            .build()
        @Suppress("MissingPermission") // checked in canPost
        manager.notify(id, NOTIFICATION_ID, built)
        return true
    }

    override fun cancel(occurrenceId: String) {
        NotificationManagerCompat.from(context).cancel(occurrenceId, NOTIFICATION_ID)
    }

    private fun canPost(manager: NotificationManagerCompat): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return granted && manager.areNotificationsEnabled()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun openAppIntent(occurrenceId: String): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(
            context,
            occurrenceId.hashCode(),
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "reminders"

        /** Notifications are keyed by occurrence id (the tag); the numeric id is constant. */
        private const val NOTIFICATION_ID = 1
    }
}
