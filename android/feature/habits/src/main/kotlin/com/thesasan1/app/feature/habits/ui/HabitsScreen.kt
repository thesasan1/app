package com.thesasan1.app.feature.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thesasan1.app.core.database.entity.HabitMeasurement
import com.thesasan1.app.core.designsystem.component.AppButton
import com.thesasan1.app.core.designsystem.component.AppButtonSize
import com.thesasan1.app.core.designsystem.component.AppButtonVariant
import com.thesasan1.app.core.designsystem.component.AppCard
import com.thesasan1.app.core.designsystem.component.EmptyState
import com.thesasan1.app.core.designsystem.theme.AppTheme
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
)

@Composable
internal fun HabitsTodayRoute(
    onBack: () -> Unit,
    onOpenArchive: () -> Unit,
    viewModel: HabitsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshDay() }
    HabitsTodayScreen(
        uiState = uiState,
        actions = HabitActions(viewModel::checkIn, viewModel::undoLast, viewModel::rest, viewModel::archive),
        onNew = viewModel::openNew,
        onOpenArchive = onOpenArchive,
        onBack = onBack,
    )
    if (uiState.isCreating) {
        NewHabitDialog(error = uiState.message, onCreate = viewModel::create, onDismiss = viewModel::dismissNew)
    }
    uiState.logging?.let { day ->
        LogAmountDialog(day = day, onLog = viewModel::logAmount, onDismiss = viewModel::dismissLog)
    }
    val message = uiState.message
    if (message != null && !uiState.isCreating) {
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, containerColor = AppTheme.colors.canvas) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenHorizontal),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.feature_habits_back)) }
            Text(
                text = stringResource(R.string.feature_habits_title),
                style = AppTheme.typography.headline,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() }.padding(bottom = Spacing.sm),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppButton(
                    text = stringResource(R.string.feature_habits_new),
                    onClick = onNew,
                    size = AppButtonSize.Small,
                )
                AppButton(
                    text = stringResource(R.string.feature_habits_archived),
                    onClick = onOpenArchive,
                    size = AppButtonSize.Small,
                    variant = AppButtonVariant.Tertiary,
                )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitRow(day: HabitDay, actions: HabitActions) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = day.habit.name,
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = statusText(day),
            style = AppTheme.typography.bodySmall,
            color = if (day.isComplete) AppTheme.colors.success else AppTheme.colors.textSecondary,
        )
        day.streak?.takeIf { it.best > 0 }?.let { streak ->
            Text(
                text = streakText(streak),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.fillMaxWidth()) {
            val canAdd = !(day.isComplete && day.habit.measurementType == HabitMeasurement.BINARY)
            if (canAdd && !day.rested) {
                AppButton(
                    text = stringResource(checkInRes(day)),
                    onClick = { actions.onCheckIn(day) },
                    size = AppButtonSize.Small,
                )
            }
            if (day.checkIns.isNotEmpty()) {
                AppButton(
                    text = stringResource(R.string.feature_habits_undo),
                    onClick = { actions.onUndo(day) },
                    variant = AppButtonVariant.Tertiary,
                    size = AppButtonSize.Small,
                )
            }
            if (day.scheduled && !day.rested && !day.isComplete) {
                AppButton(
                    text = stringResource(R.string.feature_habits_rest),
                    onClick = { actions.onRest(day) },
                    variant = AppButtonVariant.Tertiary,
                    size = AppButtonSize.Small,
                )
            }
            AppButton(
                text = stringResource(R.string.feature_habits_archive),
                onClick = { actions.onArchive(day) },
                variant = AppButtonVariant.Tertiary,
                size = AppButtonSize.Small,
            )
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
