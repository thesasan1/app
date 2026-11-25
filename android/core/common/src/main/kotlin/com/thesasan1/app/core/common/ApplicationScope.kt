package com.thesasan1.app.core.common

import javax.inject.Qualifier

/**
 * Qualifier for the app-wide [kotlinx.coroutines.CoroutineScope]: work that must outlive a screen,
 * such as rescheduling reminders from a broadcast receiver.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
