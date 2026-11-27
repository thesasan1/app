@file:Suppress("TooManyFunctions") // The screen, its row, and the row's small parts belong together.

package com.thesasan1.app.feature.habits.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thesasan1.app.core.database.entity.HabitMeasurement
import com.thesasan1.app.core.designsystem.component.AppCard
import com.thesasan1.app.core.designsystem.component.AppIcons
import com.thesasan1.app.core.designsystem.component.EmptyState
import com.thesasan1.app.core.designsystem.component.ScreenHeader
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.core.designsystem.theme.Radii
import com.thesasan1.app.core.designsystem.theme.Spacing
import com.thesasan1.app.feature.habits.R
import com.thesasan1.app.feature.habits.domain.HabitDay
import com.thesasan1.app.feature.habits.domain.HabitStreak

/** The user's actions on one habit row. */
internal data class HabitActions(
    val onCheckIn: (HabitDay) -> Unit,
    val onUndo: (HabitDay) -> Unit,
    val onRest: (HabitDay) -> Unit,
    val onArchive: (HabitDay) -> Unit,
    val onEdit: (HabitDay) -> Unit,
)

@Composable
internal fun HabitsTodayRoute(onOpenArchive: () -> Unit, viewModel: HabitsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshDay() }
    HabitsTodayScreen(
        uiState = uiState,
        actions = HabitActions(
            onCheckIn = viewModel::checkIn,
            onUndo = viewModel::undoLast,
            onRest = viewModel::rest,
            onArchive = viewModel::archive,
            onEdit = viewModel::openEdit,
        ),
        onNew = viewModel::openNew,
        onOpenArchive = onOpenArchive,
    )
    if (uiState.isCreating) {
        NewHabitDialog(error = uiState.message, onCreate = viewModel::create, onDismiss = viewModel::dismissNew)
    }
    uiState.editing?.let { day ->
        EditHabitDialog(
            day = day,
            reminder = uiState.editingReminder,
            error = uiState.message,
            onSave = viewModel::saveEdit,
            onDismiss = viewModel::dismissEdit,
        )
    }
    uiState.logging?.let { day ->
        LogAmountDialog(day = day, onLog = viewModel::logAmount, onDismiss = viewModel::dismissLog)
    }
    val message = uiState.message
    if (message != null && !uiState.isCreating && uiState.editing == null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            text = { Text(stringResource(message.textRes())) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.feature_habits_ok)) }
            },
        )
    }
}

@Composable
internal fun HabitsTodayScreen(
    uiState: HabitsUiState,
    actions: HabitActions,
    onNew: () -> Unit,
    onOpenArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = AppTheme.colors.canvas,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(AppIcons.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.feature_habits_new)) },
                containerColor = AppTheme.colors.accent,
                contentColor = AppTheme.colors.onAccent,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenHorizontal),
        ) {
            ScreenHeader(title = stringResource(R.string.feature_habits_title)) {
                TextButton(onClick = onOpenArchive) { Text(stringResource(R.string.feature_habits_archived)) }
            }
            if (uiState.isEmpty) {
                EmptyState(
                    title = stringResource(R.string.feature_habits_empty_title),
                    body = stringResource(R.string.feature_habits_empty_body),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(vertical = Spacing.md),
                ) {
                    items(uiState.days, key = { it.habit.id }) { day -> HabitRow(day, actions) }
                }
            }
        }
    }
}

@Composable
private fun HabitRow(day: HabitDay, actions: HabitActions) {
    val colors = AppTheme.colors
    val canAdd = !day.rested && !(day.isComplete && day.habit.measurementType == HabitMeasurement.BINARY)
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CheckInButton(day, enabled = canAdd, onClick = { actions.onCheckIn(day) })
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = day.habit.name,
                    style = AppTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val streak = day.streak?.takeIf { it.best > 0 }?.let { streakText(it) }
                val week = day.weekDone?.let { done ->
                    stringResource(R.string.feature_habits_week_progress, done, day.habit.scheduleCount ?: 0)
                }
                Text(
                    text = listOfNotNull(statusText(day), week, streak).joinToString("  ·  "),
                    style = AppTheme.typography.bodySmall,
                    color = if (day.isComplete) colors.success else colors.textSecondary,
                )
            }
            HabitMenu(day, actions)
        }
    }
}

/** The one primary action: a round button that fills with the accent once the day is done. */
@Composable
private fun CheckInButton(day: HabitDay, enabled: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val done = day.isComplete
    val label = stringResource(checkInRes(day))
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = Radii.full,
        color = if (done) colors.accent else colors.surface,
        contentColor = if (done) colors.onAccent else colors.accent,
        border = if (done) null else BorderStroke(2.dp, if (enabled) colors.accent else colors.borderSubtle),
        modifier = Modifier.size(Spacing.minTouchTarget).semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) {
            val icon = if (done ||
                day.habit.measurementType == HabitMeasurement.BINARY
            ) {
                AppIcons.Check
            } else {
                AppIcons.Add
            }
            if (done || enabled) Icon(icon, contentDescription = null)
        }
    }
}

/** Secondary actions live in an overflow menu so each row shows one clear action. */
@Composable
private fun HabitMenu(day: HabitDay, actions: HabitActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                AppIcons.More,
                contentDescription = stringResource(R.string.feature_habits_more),
                tint = AppTheme.colors.textSecondary,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val item: @Composable (Int, () -> Unit) -> Unit = { res, action ->
                DropdownMenuItem(
                    text = { Text(stringResource(res)) },
                    onClick = {
                        open = false
                        action()
                    },
                )
            }
            if (day.checkIns.isNotEmpty()) item(R.string.feature_habits_undo) { actions.onUndo(day) }
            if (day.scheduled &&
                !day.rested &&
                !day.isComplete
            ) {
                item(R.string.feature_habits_rest) { actions.onRest(day) }
            }
            item(R.string.feature_habits_edit) { actions.onEdit(day) }
            item(R.string.feature_habits_archive) { actions.onArchive(day) }
        }
    }
}

@Composable
private fun streakText(streak: HabitStreak): String {
    val current = pluralStringResource(R.plurals.feature_habits_streak, streak.current, streak.current)
    return if (streak.best > streak.current) {
        stringResource(R.string.feature_habits_streak_with_best, current, streak.best)
    } else {
        current
    }
}

private fun checkInRes(day: HabitDay): Int = when (day.habit.measurementType) {
    HabitMeasurement.BINARY -> R.string.feature_habits_mark_done
    HabitMeasurement.MULTI_CHECKIN -> R.string.feature_habits_add_one
    HabitMeasurement.NUMERIC, HabitMeasurement.DURATION -> R.string.feature_habits_log
}

@Composable
private fun statusText(day: HabitDay): String {
    val habit = day.habit
    val progress = formatAmount(day.progress)
    val target = habit.targetValue?.let(::formatAmount)
    return when {
        day.rested -> stringResource(R.string.feature_habits_status_rest)
        !day.scheduled -> stringResource(R.string.feature_habits_status_not_due)
        habit.measurementType == HabitMeasurement.BINARY -> stringResource(
            if (day.isComplete) R.string.feature_habits_status_done else R.string.feature_habits_status_todo,
        )
        habit.measurementType == HabitMeasurement.DURATION ->
            stringResource(R.string.feature_habits_status_minutes, progress, target ?: "—")
        target == null -> listOfNotNull(progress, habit.unit).joinToString(" ")
        else -> stringResource(R.string.feature_habits_status_of, progress, target, habit.unit.orEmpty()).trim()
    }
}

/** Whole numbers without a decimal point; otherwise up to two decimals. */
internal fun formatAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')

private fun HabitMessage.textRes(): Int = when (this) {
    HabitMessage.NameRequired -> R.string.feature_habits_error_name
    HabitMessage.TargetRequired -> R.string.feature_habits_error_target
    HabitMessage.DaysRequired -> R.string.feature_habits_error_days
    HabitMessage.AmountRequired -> R.string.feature_habits_error_amount
    HabitMessage.AlreadyDone -> R.string.feature_habits_error_done
    HabitMessage.Failed -> R.string.feature_habits_error_failed
}

internal fun HabitMessage.formTextRes(): Int = textRes()
