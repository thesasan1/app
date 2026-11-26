package com.thesasan1.app.feature.notes.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Type-safe route for the notes list (ARCHITECTURE.md §2.4). */
@Serializable
data object NotesListRoute

/** Editor route. A null [noteId] opens a new note, which is only stored once it has content. */
@Serializable
data class NoteEditorRoute(val noteId: String? = null)

@Serializable
data object NotesArchiveRoute

@Serializable
data object NotesTrashRoute

fun NavController.navigateToNotes() = navigate(NotesListRoute)

fun NavGraphBuilder.notesScreens(navController: NavController) {
    composable<NotesListRoute> {
        NotesListRoute(
            onOpenNote = { id -> navController.navigate(NoteEditorRoute(id)) },
            onOpenArchive = { navController.navigate(NotesArchiveRoute) },
            onOpenTrash = { navController.navigate(NotesTrashRoute) },
        )
    }
    composable<NotesArchiveRoute> {
        NotesArchiveRoute(
            onOpenNote = { id -> navController.navigate(NoteEditorRoute(id)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<NotesTrashRoute> {
        NotesTrashRoute(onBack = { navController.popBackStack() })
    }
    composable<NoteEditorRoute> {
        NoteEditorRoute(onClose = { navController.popBackStack() })
    }
}
