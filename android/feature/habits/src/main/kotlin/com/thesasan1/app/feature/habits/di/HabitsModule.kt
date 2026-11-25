package com.thesasan1.app.feature.habits.di

import com.thesasan1.app.core.reminders.domain.ReminderTargetActions
import com.thesasan1.app.core.reminders.domain.ReminderTargets
import com.thesasan1.app.feature.habits.data.OfflineHabitRepository
import com.thesasan1.app.feature.habits.domain.HabitRepository
import com.thesasan1.app.feature.habits.reminders.HabitReminderActions
import com.thesasan1.app.feature.habits.reminders.HabitReminderTargets
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal interface HabitsModule {
    @Binds fun bindHabitRepository(repository: OfflineHabitRepository): HabitRepository

    @Binds @IntoSet
    fun bindReminderTargets(targets: HabitReminderTargets): ReminderTargets

    @Binds @IntoSet
    fun bindReminderActions(actions: HabitReminderActions): ReminderTargetActions
}
