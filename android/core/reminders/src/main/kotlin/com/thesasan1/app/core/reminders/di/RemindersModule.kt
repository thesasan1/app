package com.thesasan1.app.core.reminders.di

import com.thesasan1.app.core.reminders.android.AndroidAlarmScheduler
import com.thesasan1.app.core.reminders.android.AndroidReminderNotifier
import com.thesasan1.app.core.reminders.domain.AlarmScheduler
import com.thesasan1.app.core.reminders.domain.OfflineTaskReminderRepository
import com.thesasan1.app.core.reminders.domain.ReminderNotifier
import com.thesasan1.app.core.reminders.domain.ReminderTargets
import com.thesasan1.app.core.reminders.domain.TaskReminderRepository
import com.thesasan1.app.core.reminders.domain.TaskReminderTargets
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface RemindersModule {
    @Binds fun bindAlarmScheduler(scheduler: AndroidAlarmScheduler): AlarmScheduler

    @Binds fun bindNotifier(notifier: AndroidReminderNotifier): ReminderNotifier

    @Binds fun bindTargets(targets: TaskReminderTargets): ReminderTargets

    @Binds fun bindTaskReminders(repository: OfflineTaskReminderRepository): TaskReminderRepository
}
