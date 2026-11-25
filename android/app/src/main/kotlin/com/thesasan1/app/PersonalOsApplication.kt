package com.thesasan1.app

import android.app.Application
import com.thesasan1.app.core.common.ApplicationScope
import com.thesasan1.app.core.reminders.domain.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class PersonalOsApplication : Application() {
    @Inject lateinit var reminderScheduler: ReminderScheduler

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Alarms can be lost (force stop, restore, a missed broadcast); rebuilding is idempotent (ADR-025).
        applicationScope.launch { reminderScheduler.rebuild() }
    }
}
