package com.thesasan1.app.feature.notes.ui

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
import com.thesasan1.app.feature.notes.R

/** One action button on a shelf row. */
internal data class ShelfAction(
    val labelRes: Int,
    val isDestructive: Boolean = false,
    val onClick: (NoteSummary) -> Unit,
)

@Composable
internal fun NotesArchiveRoute(
    onOpenNote: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: NotesArchiveViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NoteShelfScreen(
        titleRes = R.string.feature_notes_archive,
        emptyTitleRes = R.string.feature_notes_archive_empty_title,
        emptyBodyRes = R.string.feature_notes_archive_empty_body,
        uiState = uiState,
        onOpenNote = onOpenNote,
        actions = listOf(ShelfAction(R.string.feature_notes_unarchive, onClick = viewModel::unarchive)),
        onBack = onBack,
    )
}

@Composable
internal fun NotesTrashRoute(onBack: () -> Unit, viewModel: NotesTrashViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NoteShelfScreen(
        titleRes = R.string.feature_notes_trash,
        emptyTitleRes = R.string.feature_notes_trash_empty_title,
        emptyBodyRes = R.string.feature_notes_trash_empty_body,
        uiState = uiState,
        onOpenNote = null,
        actions = listOf(
            ShelfAction(R.string.feature_notes_restore, onClick = viewModel::restore),
            ShelfAction(
                R.string.feature_notes_delete_forever,
                isDestructive = true,
                onClick = viewModel::requestDelete,
            ),
        ),
        onBack = onBack,
        header = {
            Text(
                stringResource(R.string.feature_notes_trash_retention),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary,
            )
        },
    )
    uiState.pendingDelete?.let { note ->
        ConfirmDeleteDialog(note = note, onConfirm = viewModel::confirmDelete, onDismiss = viewModel::cancelDelete)
    }
}

@Composable
internal fun NoteShelfScreen(
    titleRes: Int,
    emptyTitleRes: Int,
    emptyBodyRes: Int,
    uiState: NoteShelfUiState,
    onOpenNote: ((String) -> Unit)?,
    actions: List<ShelfAction>,
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
            TextButton(onClick = onBack) { Text(stringResource(R.string.feature_notes_back)) }
            Text(
                text = stringResource(titleRes),
                style = AppTheme.typography.headline,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() }.padding(bottom = Spacing.sm),
            )
            header?.invoke()
            if (uiState.isEmpty) {
                EmptyState(title = stringResource(emptyTitleRes), body = stringResource(emptyBodyRes))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(vertical = Spacing.md),
                ) {
                    items(uiState.notes, key = { it.id }) { note -> ShelfRow(note, onOpenNote, actions) }
                }
            }
        }
    }
}

@Composable
private fun ShelfRow(note: NoteSummary, onOpenNote: ((String) -> Unit)?, actions: List<ShelfAction>) {
    val clickable = if (onOpenNote != null) Modifier.clickable { onOpenNote(note.id) } else Modifier
    AppCard(modifier = Modifier.fillMaxWidth().then(clickable)) {
        Text(
            text = note.title.ifEmpty { stringResource(R.string.feature_notes_untitled) },
            style = AppTheme.typography.titleMedium,
            color = if (note.title.isEmpty()) AppTheme.colors.textTertiary else AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (note.preview.isNotEmpty()) {
            Text(
                text = note.preview,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            actions.forEach { action ->
                TextButton(onClick = { action.onClick(note) }) {
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
private fun ConfirmDeleteDialog(note: NoteSummary, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val name = note.title.ifEmpty { stringResource(R.string.feature_notes_untitled) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_notes_delete_forever_title)) },
        text = { Text(stringResource(R.string.feature_notes_delete_forever_body, name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.feature_notes_delete_forever), color = AppTheme.colors.danger)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_notes_cancel)) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun TrashPreview() {
    AppTheme {
        NoteShelfScreen(
            titleRes = R.string.feature_notes_trash,
            emptyTitleRes = R.string.feature_notes_trash_empty_title,
            emptyBodyRes = R.string.feature_notes_trash_empty_body,
            uiState = NoteShelfUiState(
                notes = listOf(NoteSummary("1", "Old plan", "draft", isPinned = false, updatedAt = 0)),
                isLoading = false,
            ),
            onOpenNote = null,
            actions = listOf(ShelfAction(R.string.feature_notes_restore) {}),
            onBack = {},
        )
    }
}
