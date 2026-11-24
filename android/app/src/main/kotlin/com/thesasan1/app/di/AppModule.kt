package com.thesasan1.app.di

import com.thesasan1.app.core.common.AppDispatcher
import com.thesasan1.app.core.common.Dispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** System clock in the device's current zone; tests inject a fixed clock instead. */
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Dispatcher(AppDispatcher.IO)
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Dispatcher(AppDispatcher.Default)
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
