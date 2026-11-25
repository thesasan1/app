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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thesasan1.app.core.designsystem.component.AppCard
import com.thesasan1.app.core.designsystem.component.EmptyState
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.core.designsystem.theme.Spacing
import com.thesasan1.app.feature.tasks.R
import com.thesasan1.app.feature.tasks.domain.Task
import com.thesasan1.app.feature.tasks.domain.TaskPriority
import com.thesasan1.app.feature.tasks.domain.TaskStatus

/** One action button on a shelf row. */
internal data class TaskShelfAction(val labelRes: Int, val isDestructive: Boolean = false, val onClick: (Task) -> Unit)

/** Which shelf a screen shows: its title and empty state. */
internal enum class TaskShelf(val titleRes: Int, val emptyTitleRes: Int, val emptyBodyRes: Int) {
    Completed(
        R.string.feature_tasks_shelf_completed,
        R.string.feature_tasks_completed_empty_title,
        R.string.feature_tasks_completed_empty_body,
    ),
    Archive(
        R.string.feature_tasks_shelf_archive,
        R.string.feature_tasks_archive_empty_title,
        R.string.feature_tasks_archive_empty_body,
    ),
    Trash(
        R.string.feature_tasks_shelf_trash,
        R.string.feature_tasks_trash_empty_title,
        R.string.feature_tasks_trash_empty_body,
    ),
}

@Composable
internal fun TasksCompletedRoute(
    onOpenTask: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TasksCompletedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TaskShelfScreen(
        shelf = TaskShelf.Completed,
        uiState = uiState,
        onOpenTask = onOpenTask,
        actions = listOf(TaskShelfAction(R.string.feature_tasks_reopen, onClick = viewModel::reopen)),
        onBack = onBack,
    )
}

@Composable
internal fun TasksArchiveRoute(
    onOpenTask: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TasksArchiveViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TaskShelfScreen(
        shelf = TaskShelf.Archive,
        uiState = uiState,
        onOpenTask = onOpenTask,
        actions = listOf(TaskShelfAction(R.string.feature_tasks_unarchive, onClick = viewModel::unarchive)),
        onBack = onBack,
    )
}

@Composable
internal fun TasksTrashRoute(onBack: () -> Unit, viewModel: TasksTrashViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TaskShelfScreen(
        shelf = TaskShelf.Trash,
        uiState = uiState,
        onOpenTask = null,
        actions = listOf(
            TaskShelfAction(R.string.feature_tasks_restore, onClick = viewModel::restore),
            TaskShelfAction(
                R.string.feature_tasks_delete_forever,
                isDestructive = true,
                onClick = viewModel::requestDelete,
            ),
        ),
        onBack = onBack,
        header = {
            Text(
                stringResource(R.string.feature_tasks_trash_retention),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary,
            )
        },
    )
    uiState.pendingDelete?.let { task ->
        ConfirmDeleteDialog(task = task, onConfirm = viewModel::confirmDelete, onDismiss = viewModel::cancelDelete)
    }
    if (uiState.blocked) {
        AlertDialog(
            onDismissRequest = viewModel::dismissBlocked,
            text = { Text(stringResource(R.string.feature_tasks_restore_parent_first)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissBlocked) { Text(stringResource(R.string.feature_tasks_ok)) }
            },
        )
    }
}

@Composable
internal fun TaskShelfScreen(
    shelf: TaskShelf,
    uiState: TaskShelfUiState,
    onOpenTask: ((String) -> Unit)?,
    actions: List<TaskShelfAction>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    Scaffold(modifier = modifier, containerColor = AppTheme.colors.canvas) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenHorizontal),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.feature_tasks_back)) }
            Text(
                text = stringResource(shelf.titleRes),
                style = AppTheme.typography.headline,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() }.padding(bottom = Spacing.sm),
            )
            header?.invoke()
            if (uiState.isEmpty) {
                EmptyState(title = stringResource(shelf.emptyTitleRes), body = stringResource(shelf.emptyBodyRes))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(vertical = Spacing.md),
                ) {
                    items(uiState.tasks, key = { it.id }) { task -> ShelfRow(task, onOpenTask, actions) }
                }
            }
        }
    }
}

@Composable
private fun ShelfRow(task: Task, onOpenTask: ((String) -> Unit)?, actions: List<TaskShelfAction>) {
    val clickable = if (onOpenTask != null) Modifier.clickable { onOpenTask(task.id) } else Modifier
    AppCard(modifier = Modifier.fillMaxWidth().then(clickable)) {
        Text(
            text = task.title,
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (task.isSubtask) {
            Text(
                text = stringResource(R.string.feature_tasks_subtask_label),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary,
            )
        }
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            actions.forEach { action ->
                TextButton(onClick = { action.onClick(task) }) {
                    Text(
                        text = stringResource(action.labelRes),
                        color = if (action.isDestructive) AppTheme.colors.danger else AppTheme.colors.accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(task: Task, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_tasks_delete_forever_title)) },
        text = { Text(stringResource(R.string.feature_tasks_delete_forever_body, task.title)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.feature_tasks_delete_forever), color = AppTheme.colors.danger)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_tasks_cancel)) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun TaskTrashPreview() {
    val task = Task(
        id = "1",
        parentId = null,
        projectId = null,
        title = "Old errand",
        description = null,
        status = TaskStatus.TODO,
        priority = TaskPriority.NONE,
        dueDate = null,
        dueTime = null,
        snoozedUntil = null,
        completedAt = null,
        archivedAt = null,
        deletedAt = 0,
        createdAt = 0,
        updatedAt = 0,
    )
    AppTheme {
        TaskShelfScreen(
            shelf = TaskShelf.Trash,
            uiState = TaskShelfUiState(tasks = listOf(task), isLoading = false),
            onOpenTask = null,
            actions = listOf(TaskShelfAction(R.string.feature_tasks_restore) {}),
            onBack = {},
        )
    }
}
