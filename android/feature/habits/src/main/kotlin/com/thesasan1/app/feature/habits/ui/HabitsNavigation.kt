package com.thesasan1.app.feature.habits.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Type-safe route for today's habits (ARCHITECTURE.md §2.4). */
@Serializable
data object HabitsTodayRoute

fun NavController.navigateToHabits() = navigate(HabitsTodayRoute)

fun NavGraphBuilder.habitsScreens(navController: NavController) {
    composable<HabitsTodayRoute> { HabitsTodayRoute(onBack = { navController.popBackStack() }) }
}
