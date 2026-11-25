package com.thesasan1.app.core.reminders.di

import com.thesasan1.app.core.reminders.android.AndroidAlarmScheduler
import com.thesasan1.app.core.reminders.android.AndroidReminderNotifier
import com.thesasan1.app.core.reminders.domain.AlarmScheduler
import com.thesasan1.app.core.reminders.domain.AllReminderTargetActions
import com.thesasan1.app.core.reminders.domain.AllReminderTargets
import com.thesasan1.app.core.reminders.domain.HabitReminderRepository
import com.thesasan1.app.core.reminders.domain.OfflineHabitReminderRepository
import com.thesasan1.app.core.reminders.domain.OfflineTaskReminderRepository
import com.thesasan1.app.core.reminders.domain.ReminderNotifier
import com.thesasan1.app.core.reminders.domain.ReminderTargetActions
import com.thesasan1.app.core.reminders.domain.ReminderTargets
import com.thesasan1.app.core.reminders.domain.TaskReminderRepository
import com.thesasan1.app.core.reminders.domain.TaskReminderTargets
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
internal interface RemindersModule {
    @Binds fun bindAlarmScheduler(scheduler: AndroidAlarmScheduler): AlarmScheduler

    @Binds fun bindNotifier(notifier: AndroidReminderNotifier): ReminderNotifier

    @Binds fun bindTargets(targets: AllReminderTargets): ReminderTargets

    @Binds fun bindTargetActions(actions: AllReminderTargetActions): ReminderTargetActions

    @Binds @IntoSet
    fun bindTaskTargets(targets: TaskReminderTargets): ReminderTargets

    @Multibinds fun targetLookups(): Set<ReminderTargets>

    @Multibinds fun targetHandlers(): Set<ReminderTargetActions>

    @Binds fun bindHabitReminders(repository: OfflineHabitReminderRepository): HabitReminderRepository

    @Binds fun bindTaskReminders(repository: OfflineTaskReminderRepository): TaskReminderRepository
}
