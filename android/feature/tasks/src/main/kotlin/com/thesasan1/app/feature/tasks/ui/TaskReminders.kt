package com.thesasan1.app.feature.tasks.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.core.designsystem.theme.Spacing
import com.thesasan1.app.core.reminders.domain.TaskReminder
import com.thesasan1.app.feature.tasks.R

internal data class TaskRemindersActions(
    val onAdd: (Int) -> Unit = {},
    val onRepeatChange: (TaskReminder, Boolean) -> Unit = { _, _ -> },
    val onRemove: (TaskReminder) -> Unit = {},
)

/** Reminders on a task: one row per reminder, chips to add the presets, and permission notices. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TaskReminders(state: TaskRemindersUiState, hasDueDate: Boolean, actions: TaskRemindersActions) {
    val permissions = rememberReminderPermissions()
    if (!hasDueDate) {
        Text(
            text = stringResource(R.string.feature_tasks_reminders_need_due),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
        )
        return
    }
    state.reminders.forEach { reminder ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(reminderLabel(reminder.offsetMinutes), style = AppTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = reminder.repeatUntilDone,
                        onCheckedChange = { actions.onRepeatChange(reminder, it) },
                    )
                    Text(stringResource(R.string.feature_tasks_reminder_repeat), style = AppTheme.typography.bodyMedium)
                }
            }
            TextButton(onClick = { actions.onRemove(reminder) }) {
                Text(stringResource(R.string.feature_tasks_reminder_remove))
            }
        }
    }
    var customOpen by remember { mutableStateOf(false) }
    val add: (Int) -> Unit = { offset ->
        actions.onAdd(offset)
        permissions.requestNotificationsIfNeeded()
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        state.available.forEach { offset ->
            AssistChip(
                onClick = { add(offset) },
                label = { Text(stringResource(R.string.feature_tasks_reminder_add, reminderLabel(offset))) },
            )
        }
        AssistChip(
            onClick = { customOpen = true },
            label = { Text(stringResource(R.string.feature_tasks_reminder_custom)) },
        )
    }
    if (customOpen) {
        CustomReminderDialog(
            onAdd = { offset ->
                add(offset)
                customOpen = false
            },
            onDismiss = { customOpen = false },
        )
    }
    if (state.reminders.isNotEmpty()) ReminderPermissionNotices(permissions)
}

@Composable
private fun ReminderPermissionNotices(permissions: ReminderPermissions) {
    if (!permissions.notificationsAllowed) {
        Notice(R.string.feature_tasks_reminders_notifications_off, R.string.feature_tasks_reminders_turn_on) {
            permissions.fixNotifications()
        }
    }
    // REM-07: without exact-alarm access, say that reminders may be late rather than promise the minute.
    if (!permissions.exactAlarmsAllowed) {
        Notice(R.string.feature_tasks_reminders_inexact, R.string.feature_tasks_reminders_allow_exact) {
            permissions.requestExactAlarms()
        }
    }
}

@Composable
private fun Notice(textRes: Int, buttonRes: Int, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(textRes),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier)
        TextButton(onClick = onClick) { Text(stringResource(buttonRes)) }
    }
}

@Composable
private fun reminderLabel(offsetMinutes: Int): String {
    val before = -offsetMinutes
    return when {
        offsetMinutes == 0 -> stringResource(R.string.feature_tasks_reminder_at_due)
        before % MINUTES_PER_DAY == 0 ->
            pluralStringResource(
                R.plurals.feature_tasks_reminder_days_before,
                before / MINUTES_PER_DAY,
                before / MINUTES_PER_DAY,
            )
        before % MINUTES_PER_HOUR == 0 ->
            pluralStringResource(
                R.plurals.feature_tasks_reminder_hours_before,
                before / MINUTES_PER_HOUR,
                before / MINUTES_PER_HOUR,
            )
        else -> pluralStringResource(R.plurals.feature_tasks_reminder_minutes_before, before, before)
    }
}

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * 60

/** What the reminders section needs to know and do about permissions. Re-read on every resume. */
private class ReminderPermissions(
    val notificationsAllowed: Boolean,
    val exactAlarmsAllowed: Boolean,
    val requestNotificationsIfNeeded: () -> Unit,
    val fixNotifications: () -> Unit,
    val requestExactAlarms: () -> Unit,
)

@Composable
private fun rememberReminderPermissions(): ReminderPermissions {
    val context = LocalContext.current
    var checks by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { checks++ }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { checks++ }
    val notificationsAllowed = remember(checks) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val exactAlarmsAllowed = remember(checks) { canScheduleExact(context) }
    val canAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostPermission(context)
    return ReminderPermissions(
        notificationsAllowed = notificationsAllowed,
        exactAlarmsAllowed = exactAlarmsAllowed,
        requestNotificationsIfNeeded = {
            if (!notificationsAllowed && canAsk) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        fixNotifications = {
            // Once asked and refused, Android won't show the prompt again; settings is the only way back.
            if (canAsk) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }
        },
        requestExactAlarms = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()),
                )
            }
        },
    )
}

private fun hasPostPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    val state = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    return state == PackageManager.PERMISSION_GRANTED
}

private fun canScheduleExact(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
}
