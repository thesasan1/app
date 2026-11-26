package com.thesasan1.app.di

import com.thesasan1.app.feature.dashboard.TodaySummary
import com.thesasan1.app.feature.dashboard.TodaySummarySource
import com.thesasan1.app.feature.habits.domain.HabitRepository
import com.thesasan1.app.feature.tasks.domain.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.combine

/** Feeds Home from the tasks and habits features, which the dashboard may not depend on (ADR-003). */
@Module
@InstallIn(SingletonComponent::class)
object TodaySummaryModule {

    @Provides
    fun provideTodaySummary(tasks: TaskRepository, habits: HabitRepository): TodaySummarySource =
        TodaySummarySource { today ->
            combine(tasks.observeDueOn(today), tasks.observeOverdue(today), habits.observeDay(today)) {
                    due,
                    late,
                    days,
                ->
                val dueHabits = days.filter { it.scheduled && !it.rested }
                TodaySummary(
                    tasksDue = due.size,
                    tasksOverdue = late.size,
                    habitsDue = dueHabits.size,
                    habitsDone = dueHabits.count { it.isComplete },
                )
            }
        }
}
