package com.thesasan1.app.feature.habits.di

import com.thesasan1.app.feature.habits.data.OfflineHabitRepository
import com.thesasan1.app.feature.habits.domain.HabitRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface HabitsModule {
    @Binds fun bindHabitRepository(repository: OfflineHabitRepository): HabitRepository
}
