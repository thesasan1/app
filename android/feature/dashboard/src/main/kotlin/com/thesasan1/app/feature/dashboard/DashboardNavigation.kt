package com.thesasan1.app.feature.dashboard

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Type-safe navigation route for the dashboard (ARCHITECTURE.md §2.4). */
@Serializable
data object DashboardRoute

/** [onOpenNotes] is supplied by the app so this feature never references :feature:notes (ADR-003). */
fun NavGraphBuilder.dashboardScreen(onOpenNotes: () -> Unit) {
    composable<DashboardRoute> {
        DashboardRoute(onOpenNotes = onOpenNotes)
    }
}
