package com.thesasan1.app.feature.dashboard

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Type-safe navigation route for the dashboard (ARCHITECTURE.md §2.4). */
@Serializable
data object DashboardRoute

/** The open callbacks are supplied by the app so this feature never references other features (ADR-003). */
fun NavGraphBuilder.dashboardScreen(onOpenNotes: () -> Unit, onOpenTasks: () -> Unit) {
    composable<DashboardRoute> {
        DashboardRoute(onOpenNotes = onOpenNotes, onOpenTasks = onOpenTasks)
    }
}
