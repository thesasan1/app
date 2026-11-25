package com.thesasan1.app.feature.tasks.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Type-safe route for the tasks list (ARCHITECTURE.md §2.4). */
@Serializable
data object TasksListRoute

/** One task's detail and editor. */
@Serializable
data class TaskDetailRoute(val taskId: String)

@Serializable
data object TasksCompletedRoute

@Serializable
data object TasksArchiveRoute

@Serializable
data object TasksTrashRoute

fun NavController.navigateToTasks() = navigate(TasksListRoute)

fun NavGraphBuilder.tasksScreens(navController: NavController) {
    val openTask: (String) -> Unit = { id -> navController.navigate(TaskDetailRoute(id)) }
    val back: () -> Unit = { navController.popBackStack() }
    composable<TasksListRoute> {
        TasksListRoute(
            onOpenTask = openTask,
            onOpenShelf = { shelf ->
                when (shelf) {
                    TaskShelf.Completed -> navController.navigate(TasksCompletedRoute)
                    TaskShelf.Archive -> navController.navigate(TasksArchiveRoute)
                    TaskShelf.Trash -> navController.navigate(TasksTrashRoute)
                }
            },
            onBack = back,
        )
    }
    composable<TaskDetailRoute> { TaskDetailRoute(onOpenTask = openTask, onClose = back) }
    composable<TasksCompletedRoute> { TasksCompletedRoute(onOpenTask = openTask, onBack = back) }
    composable<TasksArchiveRoute> { TasksArchiveRoute(onOpenTask = openTask, onBack = back) }
    composable<TasksTrashRoute> { TasksTrashRoute(onBack = back) }
}
