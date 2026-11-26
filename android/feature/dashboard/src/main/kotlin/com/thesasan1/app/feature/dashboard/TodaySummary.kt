package com.thesasan1.app.feature.dashboard

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Today at a glance for Home: open tasks due today or overdue, and today's habits. */
data class TodaySummary(val tasksDue: Int, val tasksOverdue: Int, val habitsDue: Int, val habitsDone: Int)

/**
 * Supplies [TodaySummary]. The app module implements it from the tasks and habits features, so
 * the dashboard never depends on them (ADR-003).
 */
fun interface TodaySummarySource {
    fun observe(today: LocalDate): Flow<TodaySummary>
}
