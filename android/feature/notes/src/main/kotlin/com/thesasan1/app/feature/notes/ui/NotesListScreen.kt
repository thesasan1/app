package com.thesasan1.app.feature.notes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.OutlinedTextField
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

@Composable
internal fun NotesListRoute(
    onOpenNote: (String?) -> Unit,
    onOpenArchive: () -> Unit,
    onOpenTrash: () -> Unit,
    onBack: () -> Unit,
    viewModel: NotesListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NotesListScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onOpenNote = onOpenNote,
        onTogglePin = viewModel::togglePin,
        onOpenArchive = onOpenArchive,
        onOpenTrash = onOpenTrash,
        onBack = onBack,
    )
}

@Composable
internal fun NotesListScreen(
    uiState: NotesListUiState,
    onQueryChange: (String) -> Unit,
    onOpenNote: (String?) -> Unit,
    onTogglePin: (NoteSummary) -> Unit,
    onOpenArchive: () -> Unit,
    onOpenTrash: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = AppTheme.colors.canvas,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onOpenNote(null) },
                containerColor = AppTheme.colors.accent,
                contentColor = AppTheme.colors.onAccent,
            ) { Text(stringResource(R.string.feature_notes_new)) }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenHorizontal),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.feature_notes_back)) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOpenArchive) { Text(stringResource(R.string.feature_notes_archive)) }
                TextButton(onClick = onOpenTrash) { Text(stringResource(R.string.feature_notes_trash)) }
            }
            Text(
                text = stringResource(R.string.feature_notes_title),
                style = AppTheme.typography.headline,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.feature_notes_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
            )
            when {
                uiState.isEmpty && uiState.isSearching -> EmptyState(
                    title = stringResource(R.string.feature_notes_no_results_title),
                    body = stringResource(R.string.feature_notes_no_results_body),
                )
                uiState.isEmpty -> EmptyState(
                    title = stringResource(R.string.feature_notes_empty_title),
                    body = stringResource(R.string.feature_notes_empty_body),
                    actionLabel = stringResource(R.string.feature_notes_new),
                    onAction = { onOpenNote(null) },
                )
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(bottom = Spacing.colossal + Spacing.xxl),
                ) {
                    section(R.string.feature_notes_section_pinned, uiState.pinned, onOpenNote, onTogglePin)
                    val othersHeader = if (uiState.pinned.isEmpty()) null else R.string.feature_notes_section_others
                    section(othersHeader, uiState.others, onOpenNote, onTogglePin)
                }
            }
        }
    }
}

private fun LazyListScope.section(
    headerRes: Int?,
    notes: List<NoteSummary>,
    onOpenNote: (String?) -> Unit,
    onTogglePin: (NoteSummary) -> Unit,
) {
    if (notes.isEmpty()) return
    if (headerRes != null) {
        item(key = "header-$headerRes") {
            Text(
                text = stringResource(headerRes),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(top = Spacing.md).semantics { heading() },
            )
        }
    }
    items(notes, key = { it.id }) { note ->
        NoteRow(note = note, onClick = { onOpenNote(note.id) }, onTogglePin = { onTogglePin(note) })
    }
}

@Composable
private fun NoteRow(note: NoteSummary, onClick: () -> Unit, onTogglePin: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
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
            }
            TextButton(onClick = onTogglePin) {
                Text(stringResource(if (note.isPinned) R.string.feature_notes_unpin else R.string.feature_notes_pin))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotesListScreenPreview() {
    AppTheme {
        NotesListScreen(
            uiState = NotesListUiState(
                pinned = listOf(NoteSummary("1", "Groceries", "milk eggs bread", isPinned = true, updatedAt = 0)),
                others = listOf(NoteSummary("2", "Trip ideas", "Isfahan in spring", isPinned = false, updatedAt = 0)),
                isLoading = false,
            ),
            onQueryChange = {},
            onOpenNote = {},
            onTogglePin = {},
            onOpenArchive = {},
            onOpenTrash = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
private fun NotesListEmptyPersianPreview() {
    AppTheme {
        NotesListScreen(NotesListUiState(isLoading = false), {}, {}, {}, {}, {}, {})
    }
}
