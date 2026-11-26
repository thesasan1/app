package com.thesasan1.app.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thesasan1.app.R
import com.thesasan1.app.core.designsystem.component.AppIcons
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.feature.dashboard.DashboardRoute
import com.thesasan1.app.feature.dashboard.dashboardScreen
import com.thesasan1.app.feature.habits.ui.HabitsTodayRoute
import com.thesasan1.app.feature.habits.ui.habitsScreens
import com.thesasan1.app.feature.notes.ui.NotesListRoute
import com.thesasan1.app.feature.notes.ui.notesScreens
import com.thesasan1.app.feature.tasks.ui.TasksListRoute
import com.thesasan1.app.feature.tasks.ui.tasksScreens
import kotlin.reflect.KClass

/** The four tabs of the bottom bar (ADR-033). */
private enum class TopLevel(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Home(DashboardRoute, DashboardRoute::class, R.string.app_tab_home, AppIcons.Home),
    Tasks(TasksListRoute, TasksListRoute::class, R.string.app_tab_tasks, AppIcons.Tasks),
    Habits(HabitsTodayRoute, HabitsTodayRoute::class, R.string.app_tab_habits, AppIcons.Habits),
    Notes(NotesListRoute, NotesListRoute::class, R.string.app_tab_notes, AppIcons.Notes),
}

/**
 * The app shell: a bottom navigation bar over the root graph. The bar shows on the four tab
 * screens and hides on detail screens (editors, shelves). Each feature contributes its destinations
 * through a `NavGraphBuilder` extension so features never reference each other (ADR-003).
 */
@Composable
fun AppNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val current = TopLevel.entries.firstOrNull { destination.isOn(it) }
    val openTab: (TopLevel) -> Unit = { tab -> navController.openTab(tab) }

    Scaffold(
        modifier = modifier,
        containerColor = AppTheme.colors.canvas,
        contentWindowInsets = WindowInsets(0),
        bottomBar = { if (current != null) BottomBar(current, openTab) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DashboardRoute,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            dashboardScreen(
                onOpenNotes = { openTab(TopLevel.Notes) },
                onOpenTasks = { openTab(TopLevel.Tasks) },
                onOpenHabits = { openTab(TopLevel.Habits) },
            )
            notesScreens(navController)
            tasksScreens(navController)
            habitsScreens(navController)
        }
    }
}

@Composable
private fun BottomBar(current: TopLevel, onOpen: (TopLevel) -> Unit) {
    val colors = AppTheme.colors
    NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
        TopLevel.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == current,
                onClick = { if (tab != current) onOpen(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.accent,
                    selectedTextColor = colors.textPrimary,
                    indicatorColor = colors.accent.copy(alpha = INDICATOR_ALPHA),
                    unselectedIconColor = colors.textTertiary,
                    unselectedTextColor = colors.textSecondary,
                ),
            )
        }
    }
}

private const val INDICATOR_ALPHA = 0.14f

private fun NavDestination?.isOn(tab: TopLevel): Boolean = this?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true

/** Switches tabs the standard way: one copy of each tab, each keeping its own back stack. */
private fun NavHostController.openTab(tab: TopLevel) = navigate(tab.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
