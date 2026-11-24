package com.thesasan1.app.core.common

import kotlinx.coroutines.CancellationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OutcomeTest {

    @Test
    fun `map transforms success value`() {
        val result = Outcome.Success(2).map { it * 21 }
        assertEquals(Outcome.Success(42), result)
    }

    @Test
    fun `map keeps failure untouched`() {
        val failure: Outcome<Int> = Outcome.Failure(AppError.NotFound)
        assertEquals(failure, failure.map { it * 2 })
    }

    @Test
    fun `getOrNull returns null on failure`() {
        assertNull(Outcome.Failure(AppError.Offline).getOrNull())
    }

    @Test
    fun `runCatchingOutcome wraps thrown exceptions as Unexpected by default`() {
        val boom = IllegalStateException("boom")
        val result = runCatchingOutcome { throw boom }
        assertEquals(Outcome.Failure(AppError.Unexpected(boom)), result)
    }

    @Test
    fun `runCatchingOutcome uses custom error mapping`() {
        val result = runCatchingOutcome(mapError = { AppError.Storage(it) }) { error("disk full") }
        assertTrue((result as Outcome.Failure).error is AppError.Storage)
    }

    @Test
    fun `runCatchingOutcome rethrows cancellation`() {
        assertThrows<CancellationException> {
            runCatchingOutcome { throw CancellationException("cancelled") }
        }
    }

    @Test
    fun `onSuccess and onFailure run only for matching branch`() {
        var successCalls = 0
        var failureCalls = 0
        Outcome.Success(1).onSuccess { successCalls++ }.onFailure { failureCalls++ }
        Outcome.Failure(AppError.Offline).onSuccess { successCalls++ }.onFailure { failureCalls++ }
        assertEquals(1, successCalls)
        assertEquals(1, failureCalls)
    }
}
