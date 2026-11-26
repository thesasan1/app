package com.thesasan1.app.feature.habits.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.feature.habits.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val DEFAULT_REMINDER_HOUR = 9

/** The habit form's daily reminder: shows the time (or Off), opens a time picker, and can be cleared. */
@Composable
internal fun ReminderField(time: LocalTime?, onChange: (LocalTime?) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.feature_habits_reminder),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { picking = true }) {
            val label = time?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
            Text(label ?: stringResource(R.string.feature_habits_reminder_off))
        }
        if (time != null) {
            TextButton(onClick = { onChange(null) }) { Text(stringResource(R.string.feature_habits_reminder_clear)) }
        }
    }
    if (picking) {
        ReminderTimeDialog(
            initial = time ?: LocalTime.of(DEFAULT_REMINDER_HOUR, 0),
            onPick = {
                onChange(it)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(initial: LocalTime, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = is24h)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_habits_reminder)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.feature_habits_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_habits_cancel)) } },
    )
}
