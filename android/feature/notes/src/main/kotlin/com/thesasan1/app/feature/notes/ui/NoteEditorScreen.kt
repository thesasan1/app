package com.thesasan1.app.feature.notes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thesasan1.app.core.designsystem.component.EmptyState
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.core.designsystem.theme.Spacing
import com.thesasan1.app.feature.notes.R

@Composable
internal fun NoteEditorRoute(onClose: () -> Unit, viewModel: NoteEditorViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.flush() }
    LaunchedEffect(uiState.isClosed) { if (uiState.isClosed) onClose() }
    val close = {
        viewModel.flush()
        onClose()
    }
    BackHandler(onBack = close)
    NoteEditorScreen(
        uiState = uiState,
        onTitleChange = viewModel::onTitleChange,
        onBodyChange = viewModel::onBodyChange,
        onTogglePin = viewModel::togglePin,
        onToggleArchive = viewModel::toggleArchive,
        onMoveToTrash = viewModel::moveToTrash,
        onBack = close,
    )
}

@Composable
internal fun NoteEditorScreen(
    uiState: NoteEditorUiState,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onMoveToTrash: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, containerColor = AppTheme.colors.canvas) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = Spacing.screenHorizontal),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.feature_notes_back)) }
                Spacer(Modifier.weight(1f))
                SaveStatusLabel(uiState.saveStatus)
                if (!uiState.notFound) {
                    TextButton(onClick = onTogglePin) {
                        Text(
                            stringResource(
                                if (uiState.isPinned) R.string.feature_notes_unpin else R.string.feature_notes_pin,
                            ),
                        )
                    }
                    MoreMenu(
                        isArchived = uiState.isArchived,
                        onToggleArchive = onToggleArchive,
                        onMoveToTrash = onMoveToTrash,
                    )
                }
            }
            when {
                uiState.notFound -> EmptyState(
                    title = stringResource(R.string.feature_notes_not_found_title),
                    body = stringResource(R.string.feature_notes_not_found_body),
                )
                uiState.isLoading -> Unit
                else -> {
                    PlainField(
                        value = uiState.title,
                        onValueChange = onTitleChange,
                        placeholder = stringResource(R.string.feature_notes_title_hint),
                        style = AppTheme.typography.titleLarge,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
                    )
                    PlainField(
                        value = uiState.body,
                        onValueChange = onBodyChange,
                        placeholder = stringResource(R.string.feature_notes_body_hint),
                        style = AppTheme.typography.bodyLarge,
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreMenu(isArchived: Boolean, onToggleArchive: () -> Unit, onMoveToTrash: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text(stringResource(R.string.feature_notes_more)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isArchived) R.string.feature_notes_unarchive else R.string.feature_notes_archive_action,
                        ),
                    )
                },
                onClick = {
                    expanded = false
                    onToggleArchive()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feature_notes_move_to_trash), color = AppTheme.colors.danger) },
                onClick = {
                    expanded = false
                    onMoveToTrash()
                },
            )
        }
    }
}

@Composable
private fun SaveStatusLabel(status: SaveStatus) {
    val res = when (status) {
        SaveStatus.Idle -> return
        SaveStatus.Saving -> R.string.feature_notes_saving
        SaveStatus.Saved -> R.string.feature_notes_saved
        SaveStatus.Failed -> R.string.feature_notes_save_failed
    }
    Text(
        text = stringResource(res),
        style = AppTheme.typography.labelSmall,
        color = if (status == SaveStatus.Failed) AppTheme.colors.danger else AppTheme.colors.textTertiary,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style.copy(color = AppTheme.colors.textPrimary),
        cursorBrush = SolidColor(AppTheme.colors.accent),
        singleLine = singleLine,
        modifier = modifier,
        decorationBox = { inner ->
            if (value.isEmpty()) Text(placeholder, style = style, color = AppTheme.colors.textTertiary)
            inner()
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun NoteEditorScreenPreview() {
    AppTheme {
        NoteEditorScreen(
            uiState = NoteEditorUiState(title = "Groceries", body = "milk\neggs", saveStatus = SaveStatus.Saved),
            onTitleChange = {},
            onBodyChange = {},
            onTogglePin = {},
            onToggleArchive = {},
            onMoveToTrash = {},
            onBack = {},
        )
    }
}
