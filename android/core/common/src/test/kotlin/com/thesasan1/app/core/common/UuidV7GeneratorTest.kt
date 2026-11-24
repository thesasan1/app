package com.thesasan1.app.core.common

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Random
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UuidV7GeneratorTest {

    @Test
    fun `version and variant follow RFC 9562`() {
        val uuid = UuidV7Generator.generate(1_700_000_000_000, Random(1))
        assertEquals(7, uuid.version())
        assertEquals(2, uuid.variant())
    }

    @Test
    fun `timestamp is stored in the top 48 bits`() {
        val millis = 1_764_000_000_123
        val uuid = UuidV7Generator.generate(millis, Random(2))
        assertEquals(millis, uuid.mostSignificantBits ushr 16)
    }

    @Test
    fun `ids from later milliseconds sort after earlier ones as strings`() {
        val random = Random(3)
        val ids = (0 until 50).map { UuidV7Generator.generate(1_700_000_000_000 + it, random).toString() }
        assertEquals(ids, ids.sorted())
    }

    @Test
    fun `generator uses the injected clock and produces distinct ids`() {
        val clock = Clock.fixed(Instant.ofEpochMilli(1_764_000_000_000), ZoneOffset.UTC)
        val generator = UuidV7Generator(clock)
        val first = generator.newId()
        val second = generator.newId()
        assertNotEquals(first, second)
        assertEquals(1_764_000_000_000, UUID.fromString(first).mostSignificantBits ushr 16)
        assertTrue(first.length == 36)
    }

    @Test
    fun `negative timestamps are rejected`() {
        assertThrows<IllegalArgumentException> { UuidV7Generator.generate(-1, Random(4)) }
    }
}
