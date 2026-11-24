package com.thesasan1.app.core.common

import java.security.SecureRandom
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/** Creates primary keys and idempotency keys on the client (DATABASE.md §1). */
fun interface IdGenerator {
    fun newId(): String
}

/**
 * RFC 9562 UUID version 7: a 48-bit Unix-millisecond timestamp followed by random bits, so ids
 * sort by creation time (good for SQLite B-tree locality and for sync ordering).
 *
 * Ids made within the same millisecond are not guaranteed to be ordered among themselves;
 * nothing in the app may rely on that.
 */
class UuidV7Generator @Inject constructor(private val clock: Clock) : IdGenerator {

    private val random = SecureRandom()

    override fun newId(): String = generate(clock.millis(), random).toString()

    companion object {
        private const val TIMESTAMP_SHIFT = 16
        private const val TIMESTAMP_MASK = 0xFFFF_FFFF_FFFFL
        private const val VERSION_7 = 0x7000L
        private const val RAND_A_MASK = 0x0FFFL
        private const val VARIANT_MASK = 0x3FFF_FFFF_FFFF_FFFFL
        private const val VARIANT_RFC = Long.MIN_VALUE // binary 10 in the two top bits

        internal fun generate(epochMillis: Long, random: java.util.Random): UUID {
            require(epochMillis >= 0) { "UUIDv7 needs a non-negative timestamp" }
            val msb = ((epochMillis and TIMESTAMP_MASK) shl TIMESTAMP_SHIFT) or
                VERSION_7 or
                (random.nextLong() and RAND_A_MASK)
            val lsb = (random.nextLong() and VARIANT_MASK) or VARIANT_RFC
            return UUID(msb, lsb)
        }
    }
}
