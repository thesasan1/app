package com.thesasan1.app.feature.tasks.di

import com.thesasan1.app.core.reminders.domain.ReminderTargetActions
import com.thesasan1.app.feature.tasks.data.OfflineTaskRepository
import com.thesasan1.app.feature.tasks.domain.TaskRepository
import com.thesasan1.app.feature.tasks.reminders.TaskReminderActions
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal interface TasksModule {
    @Binds fun bindTaskRepository(repository: OfflineTaskRepository): TaskRepository

    @Binds @IntoSet
    fun bindReminderActions(actions: TaskReminderActions): ReminderTargetActions
}
