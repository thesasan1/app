package com.thesasan1.app.feature.habits.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thesasan1.app.core.database.entity.HabitEntity
import com.thesasan1.app.core.designsystem.component.AppButton
import com.thesasan1.app.core.designsystem.component.AppButtonSize
import com.thesasan1.app.core.designsystem.component.AppButtonVariant
import com.thesasan1.app.core.designsystem.component.AppCard
import com.thesasan1.app.core.designsystem.component.EmptyState
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.core.designsystem.theme.Spacing
import com.thesasan1.app.feature.habits.R

@Composable
internal fun HabitsArchiveRoute(onBack: () -> Unit, viewModel: HabitsArchiveViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HabitsArchiveScreen(
        uiState = uiState,
        onRestore = viewModel::restore,
        onDelete = viewModel::askDelete,
        onBack = onBack,
    )
    uiState.confirmingDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.feature_habits_delete_title, habit.name)) },
            text = { Text(stringResource(R.string.feature_habits_delete_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.feature_habits_delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text(stringResource(R.string.feature_habits_cancel)) }
            },
        )
    }
    if (uiState.failed) {
        AlertDialog(
            onDismissRequest = viewModel::dismissFailure,
            text = { Text(stringResource(R.string.feature_habits_error_failed)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissFailure) { Text(stringResource(R.string.feature_habits_ok)) }
            },
        )
    }
}

@Composable
internal fun HabitsArchiveScreen(
    uiState: HabitsArchiveUiState,
    onRestore: (HabitEntity) -> Unit,
    onDelete: (HabitEntity) -> Unit,
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
                text = stringResource(R.string.feature_habits_archived_title),
                style = AppTheme.typography.headline,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() }.padding(bottom = Spacing.sm),
            )
            if (uiState.isEmpty) {
                EmptyState(
                    title = stringResource(R.string.feature_habits_archived_empty_title),
                    body = stringResource(R.string.feature_habits_archived_empty_body),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(vertical = Spacing.md),
                ) {
                    items(uiState.habits, key = { it.id }) { habit ->
                        ArchivedHabitRow(habit, onRestore = { onRestore(habit) }, onDelete = { onDelete(habit) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedHabitRow(habit: HabitEntity, onRestore: () -> Unit, onDelete: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = habit.name,
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.padding(top = Spacing.sm)) {
            AppButton(
                text = stringResource(R.string.feature_habits_restore),
                onClick = onRestore,
                size = AppButtonSize.Small,
            )
            AppButton(
                text = stringResource(R.string.feature_habits_delete),
                onClick = onDelete,
                size = AppButtonSize.Small,
                variant = AppButtonVariant.Destructive,
            )
        }
    }
}
