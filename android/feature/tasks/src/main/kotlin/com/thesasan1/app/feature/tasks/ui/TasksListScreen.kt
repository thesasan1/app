package com.thesasan1.app.feature.tasks.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thesasan1.app.core.designsystem.component.AppButton
import com.thesasan1.app.core.designsystem.component.AppCard
import com.thesasan1.app.core.designsystem.component.EmptyState
import com.thesasan1.app.core.designsystem.component.ScreenHeader
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.core.designsystem.theme.Spacing
import com.thesasan1.app.feature.tasks.R
import com.thesasan1.app.feature.tasks.domain.Task
import com.thesasan1.app.feature.tasks.domain.TaskPriority
import com.thesasan1.app.feature.tasks.domain.TaskStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun TasksListRoute(
    onOpenTask: (String) -> Unit,
    onOpenShelf: (TaskShelf) -> Unit,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    TasksListScreen(
        uiState = uiState,
        actions = TasksListActions(
            onOpenTask = onOpenTask,
            onOpenShelf = onOpenShelf,
            onQuickAddChange = viewModel::onQuickAddChange,
            onQuickAddDueTodayChange = viewModel::onQuickAddDueTodayChange,
            onAdd = viewModel::addTask,
            onComplete = viewModel::complete,
            onUndoComplete = viewModel::undoComplete,
            onDismissCompleted = viewModel::dismissCompleted,
        ),
    )
}

/** Everything the screen can ask for, kept together so the screen stays previewable. */
internal data class TasksListActions(
    val onOpenTask: (String) -> Unit = {},
    val onOpenShelf: (TaskShelf) -> Unit = {},
    val onQuickAddChange: (String) -> Unit = {},
    val onQuickAddDueTodayChange: (Boolean) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onComplete: (Task) -> Unit = {},
    val onUndoComplete: () -> Unit = {},
    val onDismissCompleted: () -> Unit = {},
)

@Composable
internal fun TasksListScreen(uiState: TasksUiState, actions: TasksListActions, modifier: Modifier = Modifier) {
    val snackbar = remember { SnackbarHostState() }
    val completedMessage = stringResource(R.string.feature_tasks_completed)
    val undoLabel = stringResource(R.string.feature_tasks_undo)
    LaunchedEffect(uiState.justCompleted?.id) {
        if (uiState.justCompleted == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(completedMessage, undoLabel, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) actions.onUndoComplete() else actions.onDismissCompleted()
    }

    Scaffold(
        modifier = modifier,
        containerColor = AppTheme.colors.canvas,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenHorizontal),
        ) {
            ScreenHeader(title = stringResource(R.string.feature_tasks_title)) {
                TaskShelf.entries.forEach { shelf ->
                    TextButton(onClick = { actions.onOpenShelf(shelf) }) { Text(stringResource(shelf.titleRes)) }
                }
            }
            QuickAddBar(uiState.quickAdd, actions)
            if (uiState.isEmpty) {
                EmptyState(
                    title = stringResource(R.string.feature_tasks_empty_title),
                    body = stringResource(R.string.feature_tasks_empty_body),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(bottom = Spacing.xxl),
                ) {
                    val today = uiState.today
                    section(R.string.feature_tasks_section_overdue, uiState.sections.overdue, today, actions)
                    section(R.string.feature_tasks_section_today, uiState.sections.today, today, actions)
                    section(R.string.feature_tasks_section_upcoming, uiState.sections.upcoming, today, actions)
                    section(R.string.feature_tasks_section_someday, uiState.sections.someday, today, actions)
                }
            }
        }
    }
}

@Composable
private fun QuickAddBar(state: QuickAddState, actions: TasksListActions) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md)) {
        OutlinedTextField(
            value = state.title,
            onValueChange = actions.onQuickAddChange,
            placeholder = { Text(stringResource(R.string.feature_tasks_quick_add_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { actions.onAdd() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterChip(
                selected = state.dueToday,
                onClick = { actions.onQuickAddDueTodayChange(!state.dueToday) },
                label = { Text(stringResource(R.string.feature_tasks_due_today)) },
            )
            AppButton(
                text = stringResource(R.string.feature_tasks_add),
                onClick = actions.onAdd,
                enabled = state.canAdd,
            )
        }
    }
}

private fun LazyListScope.section(headerRes: Int, tasks: List<Task>, today: LocalDate, actions: TasksListActions) {
    if (tasks.isEmpty()) return
    item(key = "header-$headerRes") {
        Text(
            text = stringResource(headerRes),
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.textSecondary,
            modifier = Modifier.padding(top = Spacing.md).semantics { heading() },
        )
    }
    items(tasks, key = { it.id }) { task ->
        TaskRow(
            task = task,
            today = today,
            onClick = { actions.onOpenTask(task.id) },
            onComplete = { actions.onComplete(task) },
        )
    }
}

@Composable
private fun TaskRow(task: Task, today: LocalDate, onClick: () -> Unit, onComplete: () -> Unit) {
    val completeLabel = stringResource(R.string.feature_tasks_complete, task.title)
    AppCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = false,
                onCheckedChange = { onComplete() },
                modifier = Modifier.semantics { contentDescription = completeLabel },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val details = listOfNotNull(dueLabel(task, today), priorityLabel(task.priority))
                if (details.isNotEmpty()) {
                    Text(
                        text = details.joinToString(" · "),
                        style = AppTheme.typography.bodyMedium,
                        color = if (task.dueDate?.isBefore(today) == true) {
                            AppTheme.colors.danger
                        } else {
                            AppTheme.colors.textSecondary
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun dueLabel(task: Task, today: LocalDate): String? {
    val date = task.dueDate ?: return null
    val time = task.dueTime?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    val day = when (date) {
        today -> stringResource(R.string.feature_tasks_today)
        today.plusDays(1) -> stringResource(R.string.feature_tasks_tomorrow)
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
    return if (time == null) day else "$day $time"
}

@Composable
internal fun priorityLabel(priority: TaskPriority): String? = when (priority) {
    TaskPriority.HIGH -> stringResource(R.string.feature_tasks_priority_high)
    TaskPriority.URGENT -> stringResource(R.string.feature_tasks_priority_urgent)
    else -> null
}

private fun previewTask(
    id: String,
    title: String,
    due: LocalDate?,
    time: LocalTime? = null,
    priority: TaskPriority = TaskPriority.NONE,
) = Task(
    id = id,
    parentId = null,
    projectId = null,
    title = title,
    description = null,
    status = TaskStatus.TODO,
    priority = priority,
    dueDate = due,
    dueTime = time,
    snoozedUntil = null,
    completedAt = null,
    archivedAt = null,
    deletedAt = null,
    createdAt = 0,
    updatedAt = 0,
)

@Preview(showBackground = true)
@Composable
private fun TasksListScreenPreview() {
    val today = LocalDate.of(2026, 11, 25)
    AppTheme {
        TasksListScreen(
            uiState = TasksUiState(
                today = today,
                sections = TaskSections(
                    overdue = listOf(previewTask("1", "Pay the phone bill", today.minusDays(2))),
                    today = listOf(previewTask("2", "Call the clinic", today, LocalTime.of(9, 30), TaskPriority.HIGH)),
                    someday = listOf(previewTask("3", "Read Shahnameh", null)),
                ),
                isLoading = false,
            ),
            actions = TasksListActions(),
        )
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
private fun TasksListEmptyPersianPreview() {
    AppTheme {
        TasksListScreen(TasksUiState(today = LocalDate.of(2026, 11, 25), isLoading = false), TasksListActions())
    }
}
