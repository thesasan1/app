package com.thesasan1.app.core.common.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class LogRedactorTest {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "sync failed for sasan@example.com|sync failed for [email]",
            "Authorization: Bearer abc.def-123|Authorization: Bearer [token]",
            "jwt eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.c2lnbmF0dXJl here|jwt [token] here",
            "password=hunter2&next=1|password=[redacted]&next=1",
            "{\"api_key\": \"k-123\"}|{\"api_key\": [redacted]}",
            "call +1 (415) 555-0100 now|call [number] now",
            "شماره ۰۹۱۲۳۴۵۶۷۸۹ است|شماره [number] است",
            "key opaque0123456789abcdefghijklmnopqrstuv leaked|key [secret] leaked",
        ],
    )
    fun `masks personal data and secrets`(input: String, expected: String) {
        assertEquals(expected, LogRedactor.redact(input))
    }

    @Test
    fun `keeps ids, counts and codes`() {
        val line = "pushed 12 items, entity 018f4d2e-8a1b-7c3d-9e4f-0123456789ab, error HTTP_503"
        assertEquals(line, LogRedactor.redact(line))
    }

    @Test
    fun `short numbers such as counts and years are kept`() {
        assertEquals("retry 3 of 5 in 2026", LogRedactor.redact("retry 3 of 5 in 2026"))
        assertFalse(LogRedactor.redact("attempt 123456").contains("[number]"))
    }
}
