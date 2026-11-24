package com.thesasan1.app.core.common.logging

import com.thesasan1.app.core.common.AppError
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RedactingLoggerTest {

    private data class Line(val level: LogLevel, val tag: String, val message: String, val throwable: Throwable?)

    private val lines = mutableListOf<Line>()
    private val sink = LogSink { level, tag, message, throwable -> lines += Line(level, tag, message, throwable) }

    @Test
    fun `levels below the minimum are dropped without building the message`() {
        val logger = RedactingLogger(sink, minLevel = LogLevel.WARN)
        var built = false
        logger.debug("Sync") {
            built = true
            "noisy"
        }
        logger.warn("Sync") { "slow" }

        assertFalse(built)
        assertEquals(listOf(LogLevel.WARN), lines.map { it.level })
        assertTrue(logger.isEnabled(LogLevel.ERROR))
    }

    @Test
    fun `messages and throwable messages are redacted`() {
        val logger = RedactingLogger(sink, minLevel = LogLevel.DEBUG)
        logger.error("Auth", IOException("no route for a@b.co")) { "login failed for a@b.co" }

        val line = lines.single()
        assertEquals("login failed for [email] | java.io.IOException: no route for [email]", line.message)
        assertNull(line.throwable)
    }

    @Test
    fun `throwables are passed through only when allowed and tags are capped`() {
        val failure = IllegalStateException("boom")
        RedactingLogger(sink, LogLevel.DEBUG, includeThrowables = true)
            .error("AVeryLongTagNameThatExceedsTheLimit", failure) { "x" }

        assertSame(failure, lines.single().throwable)
        assertEquals(23, lines.single().tag.length)
    }

    @Test
    fun `app errors map to stable content-free codes`() {
        assertEquals("OFFLINE", AppError.Offline.code)
        assertEquals("NOT_FOUND", AppError.NotFound.code)
        assertEquals("HTTP_503", AppError.Network(httpStatus = 503).code)
        assertEquals("NETWORK:IOException", AppError.Network(cause = IOException("secret text")).code)
        assertEquals("STORAGE", AppError.Storage().code)
        assertEquals("VALIDATION:title", AppError.Validation(field = "title", reason = "user text").code)
        assertEquals("UNEXPECTED:IllegalStateException", AppError.Unexpected(IllegalStateException()).code)
    }
}
