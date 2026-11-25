package com.thesasan1.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.thesasan1.app.feature.dashboard.DashboardRoute
import com.thesasan1.app.feature.dashboard.dashboardScreen
import com.thesasan1.app.feature.notes.ui.navigateToNotes
import com.thesasan1.app.feature.notes.ui.notesScreens

/**
 * Root navigation graph. Each feature contributes its destinations through a
 * `NavGraphBuilder` extension so features never reference each other (ADR-003).
 */
@Composable
fun AppNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        modifier = modifier,
    ) {
        dashboardScreen(onOpenNotes = navController::navigateToNotes)
        notesScreens(navController)
    }
}
