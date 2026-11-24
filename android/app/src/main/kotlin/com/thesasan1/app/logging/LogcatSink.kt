package com.thesasan1.app.logging

import android.util.Log
import com.thesasan1.app.core.common.logging.LogLevel
import com.thesasan1.app.core.common.logging.LogSink

/** Writes already-redacted lines to Logcat. Crash reporting stays off until D-05 is decided. */
class LogcatSink : LogSink {
    override fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
    }
}
