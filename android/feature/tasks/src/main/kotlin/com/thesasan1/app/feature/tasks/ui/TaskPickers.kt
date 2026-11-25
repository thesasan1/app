package com.thesasan1.app.feature.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.core.designsystem.theme.Spacing
import com.thesasan1.app.feature.tasks.R
import com.thesasan1.app.feature.tasks.domain.Task
import com.thesasan1.app.feature.tasks.domain.TaskPriority
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PriorityPicker(selected: TaskPriority, onSelect: (TaskPriority) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        TaskPriority.entries.forEach { priority ->
            FilterChip(
                selected = priority == selected,
                onClick = { onSelect(priority) },
                label = { Text(stringResource(priority.labelRes())) },
            )
        }
    }
}

private fun TaskPriority.labelRes(): Int = when (this) {
    TaskPriority.NONE -> R.string.feature_tasks_priority_none
    TaskPriority.LOW -> R.string.feature_tasks_priority_low
    TaskPriority.MEDIUM -> R.string.feature_tasks_priority_medium
    TaskPriority.HIGH -> R.string.feature_tasks_priority_high
    TaskPriority.URGENT -> R.string.feature_tasks_priority_urgent
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun DuePicker(task: Task, today: LocalDate, actions: TaskDetailActions) {
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    Text(
        text = dueLabel(task, today) ?: stringResource(R.string.feature_tasks_no_due_date),
        style = AppTheme.typography.bodyMedium,
        color = AppTheme.colors.textPrimary,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FilterChip(
            selected = task.dueDate == today,
            onClick = { actions.onDueDate(today) },
            label = { Text(stringResource(R.string.feature_tasks_today)) },
        )
        FilterChip(
            selected = task.dueDate == today.plusDays(1),
            onClick = { actions.onDueDate(today.plusDays(1)) },
            label = { Text(stringResource(R.string.feature_tasks_tomorrow)) },
        )
        TextButton(onClick = { pickingDate = true }) { Text(stringResource(R.string.feature_tasks_pick_date)) }
        TextButton(onClick = { pickingTime = true }) { Text(stringResource(R.string.feature_tasks_pick_time)) }
        if (task.dueTime != null) {
            TextButton(onClick = {
                actions.onDueTime(null)
            }) { Text(stringResource(R.string.feature_tasks_clear_time)) }
        }
        if (task.dueDate != null) {
            TextButton(onClick = {
                actions.onDueDate(null)
            }) { Text(stringResource(R.string.feature_tasks_clear_date)) }
        }
    }
    if (pickingDate) {
        // The picker works in UTC midnight millis, whatever the device zone.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (task.dueDate ?: today).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickingDate = false
                    state.selectedDateMillis?.let {
                        actions.onDueDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                }) { Text(stringResource(R.string.feature_tasks_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.feature_tasks_cancel)) }
            },
        ) { DatePicker(state = state) }
    }
    if (pickingTime) {
        val initial = task.dueTime ?: LocalTime.of(DEFAULT_HOUR, 0)
        val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            confirmButton = {
                TextButton(onClick = {
                    pickingTime = false
                    actions.onDueTime(LocalTime.of(state.hour, state.minute))
                }) { Text(stringResource(R.string.feature_tasks_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingTime = false }) { Text(stringResource(R.string.feature_tasks_cancel)) }
            },
            text = { TimePicker(state = state) },
        )
    }
}

private const val DEFAULT_HOUR = 9
