package com.thesasan1.app.feature.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.thesasan1.app.core.database.entity.HabitMeasurement
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.core.designsystem.theme.Spacing
import com.thesasan1.app.feature.habits.R
import com.thesasan1.app.feature.habits.domain.HabitDay
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private const val MAX_AMOUNT_CHARS = 7

/** Name, kind, target and days for a new habit (HAB-01, HAB-02). */
@Composable
internal fun NewHabitDialog(error: HabitMessage?, onCreate: (NewHabitForm) -> Unit, onDismiss: () -> Unit) =
    HabitFormDialog(
        initial = NewHabitForm(),
        isNew = true,
        error = error,
        onSave = onCreate,
        onDismiss = onDismiss,
    )

/** The same fields for an existing habit; how it's tracked is shown but can't change (ADR-031). */
@Composable
internal fun EditHabitDialog(
    day: HabitDay,
    reminder: LocalTime?,
    error: HabitMessage?,
    onSave: (NewHabitForm) -> Unit,
    onDismiss: () -> Unit,
) = HabitFormDialog(
    initial = NewHabitForm.of(day.habit).copy(reminder = reminder),
    isNew = false,
    error = error,
    onSave = onSave,
    onDismiss = onDismiss,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitFormDialog(
    initial: NewHabitForm,
    isNew: Boolean,
    error: HabitMessage?,
    onSave: (NewHabitForm) -> Unit,
    onDismiss: () -> Unit,
) {
    var form by remember { mutableStateOf(initial) }
    val locale = LocalConfiguration.current.locales[0]
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (isNew) R.string.feature_habits_new else R.string.feature_habits_edit_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { form = form.copy(name = it) },
                    label = { Text(stringResource(R.string.feature_habits_name)) },
                    singleLine = true,
                )
                Text(stringResource(R.string.feature_habits_kind), style = AppTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    HabitMeasurement.entries.filter { isNew || it == form.measurement }.forEach { kind ->
                        FilterChip(
                            selected = form.measurement == kind,
                            enabled = isNew,
                            onClick = { form = form.copy(measurement = kind) },
                            label = { Text(stringResource(kind.labelRes())) },
                        )
                    }
                }
                if (form.measurement != HabitMeasurement.BINARY) {
                    OutlinedTextField(
                        value = form.target,
                        onValueChange = { form = form.copy(target = it.amountInput()) },
                        label = { Text(stringResource(form.measurement.targetLabelRes())) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
                if (form.measurement == HabitMeasurement.NUMERIC) {
                    OutlinedTextField(
                        value = form.unit,
                        onValueChange = { form = form.copy(unit = it.take(MAX_UNIT_CHARS)) },
                        label = { Text(stringResource(R.string.feature_habits_unit)) },
                        singleLine = true,
                    )
                }
                Text(stringResource(R.string.feature_habits_days), style = AppTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    FilterChip(
                        selected = form.perWeek == null,
                        onClick = { form = form.copy(perWeek = null) },
                        label = { Text(stringResource(R.string.feature_habits_specific_days)) },
                    )
                    FilterChip(
                        selected = form.perWeek != null,
                        onClick = { form = form.copy(perWeek = form.perWeek ?: DEFAULT_PER_WEEK) },
                        label = { Text(stringResource(R.string.feature_habits_times_a_week)) },
                    )
                }
                form.perWeek?.let { count ->
                    PerWeekStepper(count) { form = form.copy(perWeek = it) }
                }
                if (form.perWeek == null) {
                    DayChips(form.days, locale) { form = form.copy(days = it) }
                }
                ReminderField(time = form.reminder, onChange = { form = form.copy(reminder = it) })
                error?.let { Text(stringResource(it.formTextRes()), color = AppTheme.colors.danger) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(form) }, enabled = form.name.isNotBlank()) {
                Text(stringResource(if (isNew) R.string.feature_habits_create else R.string.feature_habits_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_habits_cancel)) } },
    )
}

/** Asks how much was done for a numeric or duration habit. */
@Composable
internal fun LogAmountDialog(day: HabitDay, onLog: (Double?) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    val value = amount.toDoubleOrNull()?.takeIf { it > 0 }
    val label = if (day.habit.measurementType == HabitMeasurement.DURATION) {
        stringResource(R.string.feature_habits_minutes)
    } else {
        day.habit.unit ?: stringResource(R.string.feature_habits_amount)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(day.habit.name) },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.amountInput() },
                label = { Text(label) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onLog(value) }, enabled = value != null) {
                Text(stringResource(R.string.feature_habits_log))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_habits_cancel)) } },
    )
}

private const val MAX_UNIT_CHARS = 20
private const val DEFAULT_PER_WEEK = 3
private const val MAX_PER_WEEK = 7

/** One chip per weekday; tapping toggles it in [days]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayChips(days: Set<DayOfWeek>, locale: Locale, onChange: (Set<DayOfWeek>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        DayOfWeek.entries.forEach { day ->
            val selected = day in days
            FilterChip(
                selected = selected,
                onClick = { onChange(if (selected) days - day else days + day) },
                label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
            )
        }
    }
}

/** "N times a week": minus, the count, plus (1 to 7). */
@Composable
private fun PerWeekStepper(count: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onChange(count - 1) }, enabled = count > 1) { Text("−") }
        Text(
            text = pluralStringResource(R.plurals.feature_habits_per_week, count, count),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.textPrimary,
        )
        TextButton(onClick = { onChange(count + 1) }, enabled = count < MAX_PER_WEEK) { Text("+") }
    }
}

/** Digits and one decimal point, at most a few characters. */
private fun String.amountInput(): String {
    val kept = filter { it.isDigit() || it == '.' }
    val firstDot = kept.indexOf('.')
    val single = if (firstDot < 0) kept else kept.take(firstDot + 1) + kept.drop(firstDot + 1).replace(".", "")
    return single.take(MAX_AMOUNT_CHARS)
}

private fun HabitMeasurement.labelRes(): Int = when (this) {
    HabitMeasurement.BINARY -> R.string.feature_habits_kind_binary
    HabitMeasurement.NUMERIC -> R.string.feature_habits_kind_numeric
    HabitMeasurement.DURATION -> R.string.feature_habits_kind_duration
    HabitMeasurement.MULTI_CHECKIN -> R.string.feature_habits_kind_multi
}

private fun HabitMeasurement.targetLabelRes(): Int = when (this) {
    HabitMeasurement.DURATION -> R.string.feature_habits_target_minutes
    HabitMeasurement.MULTI_CHECKIN -> R.string.feature_habits_target_times
    else -> R.string.feature_habits_target
}
