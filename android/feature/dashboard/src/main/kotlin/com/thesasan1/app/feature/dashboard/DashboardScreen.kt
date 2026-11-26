package com.thesasan1.app.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thesasan1.app.core.designsystem.component.AppIcons
import com.thesasan1.app.core.designsystem.component.AppTile
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.core.designsystem.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun DashboardRoute(
    onOpenNotes: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenHabits: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle(initialValue = null)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    DashboardScreen(
        uiState = uiState,
        summary = summary,
        onOpenNotes = onOpenNotes,
        onOpenTasks = onOpenTasks,
        onOpenHabits = onOpenHabits,
    )
}

@Composable
internal fun DashboardScreen(
    uiState: DashboardUiState,
    summary: TodaySummary?,
    onOpenNotes: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenHabits: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xxxl),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = stringResource(uiState.period.greetingRes()),
                style = AppTheme.typography.headline,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = uiState.today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
            )
            Spacer(Modifier.height(Spacing.xxxl))
            Text(
                text = stringResource(R.string.feature_dashboard_today),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Spacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                AppTile(
                    icon = AppIcons.Tasks,
                    title = stringResource(R.string.feature_dashboard_open_tasks),
                    onClick = onOpenTasks,
                    subtitle = summary?.let { tasksLine(it) },
                )
                AppTile(
                    icon = AppIcons.Habits,
                    title = stringResource(R.string.feature_dashboard_open_habits),
                    onClick = onOpenHabits,
                    subtitle = summary?.let { habitsLine(it) },
                )
                AppTile(AppIcons.Notes, stringResource(R.string.feature_dashboard_open_notes), onOpenNotes)
            }
        }
    }
}

@Composable
private fun tasksLine(summary: TodaySummary): String {
    val due = if (summary.tasksDue == 0) {
        stringResource(R.string.feature_dashboard_tasks_none)
    } else {
        pluralStringResource(R.plurals.feature_dashboard_tasks_due, summary.tasksDue, summary.tasksDue)
    }
    if (summary.tasksOverdue == 0) return due
    val overdue =
        pluralStringResource(R.plurals.feature_dashboard_tasks_overdue, summary.tasksOverdue, summary.tasksOverdue)
    return "$due  ·  $overdue"
}

@Composable
private fun habitsLine(summary: TodaySummary): String = when {
    summary.habitsDue == 0 -> stringResource(R.string.feature_dashboard_habits_none)
    summary.habitsDone >= summary.habitsDue -> stringResource(R.string.feature_dashboard_habits_all_done)
    else -> stringResource(R.string.feature_dashboard_habits_progress, summary.habitsDone, summary.habitsDue)
}

private fun DayPeriod.greetingRes(): Int = when (this) {
    DayPeriod.Morning -> R.string.feature_dashboard_greeting_morning
    DayPeriod.Afternoon -> R.string.feature_dashboard_greeting_afternoon
    DayPeriod.Evening -> R.string.feature_dashboard_greeting_evening
    DayPeriod.Night -> R.string.feature_dashboard_greeting_night
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    AppTheme {
        DashboardScreen(
            DashboardUiState(
                period = DayPeriod.Morning,
                today = LocalDate.of(2026, 11, 24),
            ),
            summary = TodaySummary(tasksDue = 3, tasksOverdue = 1, habitsDue = 4, habitsDone = 2),
            onOpenNotes = {},
            onOpenTasks = {},
            onOpenHabits = {},
        )
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
private fun DashboardScreenPersianPreview() {
    AppTheme {
        DashboardScreen(
            DashboardUiState(
                period = DayPeriod.Evening,
                today = LocalDate.of(2026, 11, 24),
            ),
            summary = null,
            onOpenNotes = {},
            onOpenTasks = {},
            onOpenHabits = {},
        )
    }
}
