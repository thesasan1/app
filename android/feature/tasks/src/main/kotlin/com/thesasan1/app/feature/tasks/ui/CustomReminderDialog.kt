package com.thesasan1.app.feature.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.thesasan1.app.core.designsystem.theme.Spacing
import com.thesasan1.app.core.reminders.domain.ReminderUnit
import com.thesasan1.app.core.reminders.domain.customReminderOffset
import com.thesasan1.app.feature.tasks.R

/** Asks for "N minutes/hours/days before"; [onAdd] gets the offset in (negative) minutes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CustomReminderDialog(onAdd: (Int) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(ReminderUnit.HOURS) }
    val offset = amount.toIntOrNull()?.let { customReminderOffset(it, unit) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_tasks_reminder_custom_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value -> amount = value.filter(Char::isDigit).take(MAX_DIGITS) },
                    label = { Text(stringResource(R.string.feature_tasks_reminder_custom_amount)) },
                    isError = amount.isNotEmpty() && offset == null,
                    supportingText = { Text(stringResource(R.string.feature_tasks_reminder_custom_limit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    ReminderUnit.entries.forEach { option ->
                        FilterChip(
                            selected = option == unit,
                            onClick = { unit = option },
                            label = { Text(stringResource(option.labelRes())) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { offset?.let(onAdd) }, enabled = offset != null) {
                Text(stringResource(R.string.feature_tasks_reminder_custom_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_tasks_cancel)) }
        },
    )
}

private const val MAX_DIGITS = 5

private fun ReminderUnit.labelRes(): Int = when (this) {
    ReminderUnit.MINUTES -> R.string.feature_tasks_reminder_unit_minutes
    ReminderUnit.HOURS -> R.string.feature_tasks_reminder_unit_hours
    ReminderUnit.DAYS -> R.string.feature_tasks_reminder_unit_days
}
