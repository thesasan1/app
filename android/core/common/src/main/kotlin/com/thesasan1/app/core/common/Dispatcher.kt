package com.thesasan1.app.core.common

import javax.inject.Qualifier

/**
 * Qualifier for injected [kotlinx.coroutines.CoroutineDispatcher]s.
 * Code never references `Dispatchers.IO` directly, so tests can substitute a test dispatcher.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: AppDispatcher)

enum class AppDispatcher {
    Default,
    IO,
}
