package com.thesasan1.app

import android.app.Application
import com.thesasan1.app.core.common.ApplicationScope
import com.thesasan1.app.core.reminders.domain.ReminderRebuildTrigger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

@HiltAndroidApp
class PersonalOsApplication : Application() {
    @Inject lateinit var reminderRebuild: ReminderRebuildTrigger

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Rebuilds now (alarms can be lost to a force stop or restore; ADR-025) and on every task or
        // reminder change (ADR-027). Rebuilding is idempotent.
        reminderRebuild.start(applicationScope)
    }
}
