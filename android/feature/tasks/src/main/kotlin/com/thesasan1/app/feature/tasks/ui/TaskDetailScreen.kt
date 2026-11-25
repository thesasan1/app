package com.thesasan1.app.feature.tasks.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thesasan1.app.core.designsystem.component.AppButton
import com.thesasan1.app.core.designsystem.component.AppButtonVariant
import com.thesasan1.app.core.designsystem.component.EmptyState
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.core.designsystem.theme.Spacing
import com.thesasan1.app.feature.tasks.R
import com.thesasan1.app.feature.tasks.domain.Task
import com.thesasan1.app.feature.tasks.domain.TaskEvent
import com.thesasan1.app.feature.tasks.domain.TaskEventType
import com.thesasan1.app.feature.tasks.domain.TaskPriority
import com.thesasan1.app.feature.tasks.domain.TaskStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun TaskDetailRoute(
    onOpenTask: (String) -> Unit,
    onClose: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
    remindersViewModel: TaskRemindersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reminders by remindersViewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.flush() }
    LaunchedEffect(uiState.isClosed) { if (uiState.isClosed) onClose() }
    TaskDetailScreen(
        uiState = uiState,
        actions = TaskDetailActions(
            onBack = {
                viewModel.flush()
                onClose()
            },
            onTitleChange = viewModel::onTitleChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onPriority = viewModel::setPriority,
            onDueDate = viewModel::setDueDate,
            onDueTime = viewModel::setDueTime,
            onToggleDone = viewModel::toggleDone,
            onNewSubtaskChange = viewModel::onNewSubtaskChange,
            onAddSubtask = viewModel::addSubtask,
            onToggleSubtask = viewModel::toggleSubtask,
            onOpenSubtask = { id ->
                viewModel.flush()
                onOpenTask(id)
            },
            onArchive = viewModel::archive,
            onMoveToTrash = viewModel::moveToTrash,
        ),
        reminders = reminders,
        remindersActions = TaskRemindersActions(
            onAdd = remindersViewModel::add,
            onRepeatChange = remindersViewModel::setRepeatUntilDone,
            onRemove = remindersViewModel::remove,
        ),
    )
}

internal data class TaskDetailActions(
    val onBack: () -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onPriority: (TaskPriority) -> Unit = {},
    val onDueDate: (LocalDate?) -> Unit = {},
    val onDueTime: (LocalTime?) -> Unit = {},
    val onToggleDone: () -> Unit = {},
    val onNewSubtaskChange: (String) -> Unit = {},
    val onAddSubtask: () -> Unit = {},
    val onToggleSubtask: (Task) -> Unit = {},
    val onOpenSubtask: (String) -> Unit = {},
    val onArchive: () -> Unit = {},
    val onMoveToTrash: () -> Unit = {},
)

@Composable
internal fun TaskDetailScreen(
    uiState: TaskDetailUiState,
    actions: TaskDetailActions,
    modifier: Modifier = Modifier,
    reminders: TaskRemindersUiState = TaskRemindersUiState(),
    remindersActions: TaskRemindersActions = TaskRemindersActions(),
) {
    Scaffold(modifier = modifier, containerColor = AppTheme.colors.canvas) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenHorizontal)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = actions.onBack) { Text(stringResource(R.string.feature_tasks_back)) }
                Spacer(Modifier.weight(1f))
                if (uiState.task != null) MoreMenu(uiState.task.isSubtask, actions)
            }
            val task = uiState.task
            when {
                uiState.notFound -> EmptyState(
                    title = stringResource(R.string.feature_tasks_not_found_title),
                    body = stringResource(R.string.feature_tasks_not_found_body),
                )
                task != null -> {
                    TaskEditor(task, uiState, actions)
                    if (task.status != TaskStatus.DONE) {
                        SectionHeader(R.string.feature_tasks_section_reminders)
                        TaskReminders(reminders, hasDueDate = task.dueDate != null, actions = remindersActions)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskEditor(task: Task, uiState: TaskDetailUiState, actions: TaskDetailActions) {
    OutlinedTextField(
        value = uiState.title,
        onValueChange = actions.onTitleChange,
        label = { Text(stringResource(R.string.feature_tasks_field_title)) },
        isError = uiState.titleMissing,
        supportingText = if (uiState.titleMissing) {
            { Text(stringResource(R.string.feature_tasks_title_required)) }
        } else {
            null
        },
        textStyle = AppTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = uiState.description,
        onValueChange = actions.onDescriptionChange,
        label = { Text(stringResource(R.string.feature_tasks_field_description)) },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
    AppButton(
        text = stringResource(
            if (task.status == TaskStatus.DONE) R.string.feature_tasks_reopen else R.string.feature_tasks_mark_done,
        ),
        onClick = actions.onToggleDone,
        variant = if (task.status == TaskStatus.DONE) AppButtonVariant.Secondary else AppButtonVariant.Primary,
    )
    SectionHeader(R.string.feature_tasks_section_priority)
    PriorityPicker(task.priority, actions.onPriority)
    SectionHeader(R.string.feature_tasks_section_due)
    DuePicker(task, uiState.today ?: LocalDate.now(), actions)
    if (!task.isSubtask) {
        SectionHeader(R.string.feature_tasks_section_subtasks)
        Subtasks(uiState, actions)
    }
    SectionHeader(R.string.feature_tasks_section_history)
    History(uiState.history)
}

@Composable
private fun SectionHeader(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = AppTheme.typography.labelMedium,
        color = AppTheme.colors.textSecondary,
        modifier = Modifier.padding(top = Spacing.sm).semantics { heading() },
    )
}

@Composable
private fun MoreMenu(isSubtask: Boolean, actions: TaskDetailActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text(stringResource(R.string.feature_tasks_more)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (!isSubtask) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feature_tasks_archive_action)) },
                    onClick = {
                        open = false
                        actions.onArchive()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feature_tasks_move_to_trash)) },
                onClick = {
                    open = false
                    actions.onMoveToTrash()
                },
            )
        }
    }
}

@Composable
private fun Subtasks(uiState: TaskDetailUiState, actions: TaskDetailActions) {
    uiState.subtasks.forEach { subtask ->
        val done = subtask.status == TaskStatus.DONE
        val label = stringResource(R.string.feature_tasks_complete, subtask.title)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = done,
                onCheckedChange = { actions.onToggleSubtask(subtask) },
                modifier = Modifier.semantics { contentDescription = label },
            )
            Text(
                text = subtask.title,
                style = AppTheme.typography.bodyMedium,
                color = if (done) AppTheme.colors.textTertiary else AppTheme.colors.textPrimary,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f).clickable { actions.onOpenSubtask(subtask.id) },
            )
        }
    }
    OutlinedTextField(
        value = uiState.newSubtask,
        onValueChange = actions.onNewSubtaskChange,
        placeholder = { Text(stringResource(R.string.feature_tasks_add_subtask_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { actions.onAddSubtask() }),
        trailingIcon = {
            TextButton(onClick = actions.onAddSubtask, enabled = uiState.newSubtask.isNotBlank()) {
                Text(stringResource(R.string.feature_tasks_add))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun History(events: List<TaskEvent>) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT) }
    val zone = remember { ZoneId.systemDefault() }
    // Newest first.
    events.asReversed().forEach { event ->
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs)) {
            Text(
                text = stringResource(event.type.labelRes()),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Instant.ofEpochMilli(event.occurredAt).atZone(zone).format(formatter),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}

private fun TaskEventType.labelRes(): Int = when (this) {
    TaskEventType.CREATED -> R.string.feature_tasks_event_created
    TaskEventType.STATUS_CHANGED -> R.string.feature_tasks_event_status_changed
    TaskEventType.RESCHEDULED -> R.string.feature_tasks_event_rescheduled
    TaskEventType.SNOOZED -> R.string.feature_tasks_event_snoozed
    TaskEventType.EDITED -> R.string.feature_tasks_event_edited
    TaskEventType.COMPLETED -> R.string.feature_tasks_event_completed
    TaskEventType.REOPENED -> R.string.feature_tasks_event_reopened
    TaskEventType.ARCHIVED -> R.string.feature_tasks_event_archived
    TaskEventType.DELETED -> R.string.feature_tasks_event_deleted
    TaskEventType.RESTORED -> R.string.feature_tasks_event_restored
}

@Preview(showBackground = true)
@Composable
private fun TaskDetailScreenPreview() {
    val today = LocalDate.of(2026, 11, 25)
    val task = Task(
        id = "1",
        parentId = null,
        projectId = null,
        title = "Plan the Isfahan trip",
        description = "Book the train and a guesthouse near Naqsh-e Jahan.",
        status = TaskStatus.TODO,
        priority = TaskPriority.HIGH,
        dueDate = today,
        dueTime = LocalTime.of(18, 0),
        snoozedUntil = null,
        completedAt = null,
        archivedAt = null,
        deletedAt = null,
        createdAt = 0,
        updatedAt = 0,
    )
    AppTheme {
        TaskDetailScreen(
            uiState = TaskDetailUiState(
                isLoading = false,
                task = task,
                title = task.title,
                description = task.description.orEmpty(),
                subtasks = listOf(
                    task.copy(id = "2", parentId = "1", title = "Train tickets", status = TaskStatus.DONE),
                ),
                history = listOf(TaskEvent("e1", "1", TaskEventType.CREATED, null, null, 1_764_000_000_000)),
                today = today,
            ),
            actions = TaskDetailActions(),
        )
    }
}
