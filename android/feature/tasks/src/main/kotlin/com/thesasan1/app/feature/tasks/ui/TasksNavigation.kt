package com.thesasan1.app.feature.tasks.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Type-safe route for the tasks list (ARCHITECTURE.md §2.4). */
@Serializable
data object TasksListRoute

fun NavController.navigateToTasks() = navigate(TasksListRoute)

fun NavGraphBuilder.tasksScreens(navController: NavController) {
    composable<TasksListRoute> {
        TasksListRoute(onBack = { navController.popBackStack() })
    }
}
