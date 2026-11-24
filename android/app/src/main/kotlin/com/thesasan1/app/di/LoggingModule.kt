package com.thesasan1.app.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.thesasan1.app.core.common.logging.AppLogger
import com.thesasan1.app.core.common.logging.LogLevel
import com.thesasan1.app.core.common.logging.RedactingLogger
import com.thesasan1.app.logging.LogcatSink
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {

    /**
     * Debug builds log everything with stack traces; release builds log warnings and errors only,
     * without throwables, so exception messages never reach Logcat unredacted.
     */
    @Provides
    @Singleton
    fun provideLogger(@ApplicationContext context: Context): AppLogger {
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        return RedactingLogger(
            sink = LogcatSink(),
            minLevel = if (debuggable) LogLevel.DEBUG else LogLevel.WARN,
            includeThrowables = debuggable,
        )
    }
}
