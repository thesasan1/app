package com.thesasan1.app.core.common.logging

/** Log severity, lowest first. */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

/**
 * The only logging API app code uses (AGENTS.md §18, SECURITY.md). Messages are lambdas, so
 * nothing is built when the level is off, and every message passes through [LogRedactor].
 *
 * Rule: log ids, counts, codes and states, never user content (note text, task titles, journal
 * entries, transcripts). Redaction is a safety net for mistakes, not permission to log content.
 */
interface AppLogger {
    fun isEnabled(level: LogLevel): Boolean

    fun log(level: LogLevel, tag: String, throwable: Throwable? = null, message: () -> String)
}

fun AppLogger.debug(tag: String, message: () -> String) = log(LogLevel.DEBUG, tag, null, message)

fun AppLogger.info(tag: String, message: () -> String) = log(LogLevel.INFO, tag, null, message)

fun AppLogger.warn(tag: String, throwable: Throwable? = null, message: () -> String) =
    log(LogLevel.WARN, tag, throwable, message)

fun AppLogger.error(tag: String, throwable: Throwable? = null, message: () -> String) =
    log(LogLevel.ERROR, tag, throwable, message)

/** Where redacted log lines go: Logcat on Android, a list in tests. */
fun interface LogSink {
    fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?)
}

/**
 * Filters by [minLevel] and redacts every message before it reaches [sink]. A throwable's own
 * message can hold user content, so the sink gets the throwable's class name and redacted
 * message in the text and receives the throwable itself only when [includeThrowables] is true
 * (debug builds, for stack traces).
 */
class RedactingLogger(
    private val sink: LogSink,
    private val minLevel: LogLevel,
    private val includeThrowables: Boolean = false,
) : AppLogger {

    override fun isEnabled(level: LogLevel): Boolean = level >= minLevel

    override fun log(level: LogLevel, tag: String, throwable: Throwable?, message: () -> String) {
        if (!isEnabled(level)) return
        val text = buildString {
            append(LogRedactor.redact(message()))
            if (throwable != null) {
                append(" | ").append(throwable.javaClass.name)
                throwable.message?.let { append(": ").append(LogRedactor.redact(it)) }
            }
        }
        sink.write(level, tag.take(MAX_TAG_LENGTH), text, throwable.takeIf { includeThrowables })
    }

    private companion object {
        /** Logcat truncates longer tags on older API levels. */
        const val MAX_TAG_LENGTH = 23
    }
}
