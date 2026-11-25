package com.thesasan1.app.di

import com.thesasan1.app.core.common.AppDispatcher
import com.thesasan1.app.core.common.ApplicationScope
import com.thesasan1.app.core.common.Dispatcher
import com.thesasan1.app.core.common.IdGenerator
import com.thesasan1.app.core.common.UuidV7Generator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** System clock in the device's current zone; tests inject a fixed clock instead. */
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    /** UUIDv7 primary keys and idempotency keys (DATABASE.md §1). */
    @Provides
    fun provideIdGenerator(clock: Clock): IdGenerator = UuidV7Generator(clock)

    @Provides
    @Dispatcher(AppDispatcher.IO)
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Dispatcher(AppDispatcher.Default)
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /** App-wide scope for work that outlives a screen; a failing child does not cancel the rest. */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(@Dispatcher(AppDispatcher.IO) io: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + io)
}
