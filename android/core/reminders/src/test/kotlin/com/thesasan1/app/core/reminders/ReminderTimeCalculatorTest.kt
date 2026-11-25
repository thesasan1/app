package com.thesasan1.app.core.reminders

import com.thesasan1.app.core.database.entity.ReminderEntity
import com.thesasan1.app.core.database.entity.ReminderTargetType
import com.thesasan1.app.core.database.entity.SyncColumns
import com.thesasan1.app.core.database.entity.TimeZoneMode
import com.thesasan1.app.core.reminders.domain.ReminderTarget
import com.thesasan1.app.core.reminders.domain.ReminderTimeCalculator
import java.time.Instant
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReminderTimeCalculatorTest {
    private val tehran = ZoneId.of("Asia/Tehran")
    private val berlin = ZoneId.of("Europe/Berlin")

    private fun reminder(
        offset: Int? = null,
        local: String? = null,
        utc: Long? = null,
        mode: TimeZoneMode = TimeZoneMode.FLOATING,
    ) = ReminderEntity(
        id = "r",
        targetType = ReminderTargetType.TASK,
        targetId = "t",
        offsetMinutes = offset,
        triggerAtLocal = local,
        triggerAtUtc = utc,
        timeZoneMode = mode,
        sync = SyncColumns.new(0),
    )

    @Test
    fun anOffsetCountsFromTheTasksDueTimeInTheDeviceZone() {
        val at = ReminderTimeCalculator.fireAt(
            reminder(offset = -30),
            ReminderTarget("Pay rent", "2026-11-26", "10:00"),
            tehran,
        )
        assertEquals(Instant.parse("2026-11-26T06:00:00Z"), at) // 09:30 at +03:30
    }

    @Test
    fun anAllDayTaskIsDueAtNineAndAnUndatedOrClosedTaskNever() {
        val due = ReminderTarget("Call", "2026-11-26", null)
        assertEquals(
            Instant.parse("2026-11-26T05:30:00Z"),
            ReminderTimeCalculator.fireAt(reminder(offset = 0), due, tehran),
        )
        assertNull(ReminderTimeCalculator.fireAt(reminder(offset = 0), due.copy(dueDate = null), tehran))
        assertNull(ReminderTimeCalculator.fireAt(reminder(offset = 0), due.copy(isOpen = false), tehran))
    }

    @Test
    fun aFixedTaskKeepsItsOwnZone() {
        val at = ReminderTimeCalculator.fireAt(
            reminder(offset = 0),
            ReminderTarget("Standup", "2026-11-26", "09:00", zoneId = "Europe/Berlin"),
            tehran,
        )
        assertEquals(Instant.parse("2026-11-26T08:00:00Z"), at)
    }

    @Test
    fun aFloatingTimeFollowsTheDeviceZone() {
        val r = reminder(local = "2026-11-26T08:00")
        assertEquals(Instant.parse("2026-11-26T04:30:00Z"), ReminderTimeCalculator.fireAt(r, null, tehran))
        assertEquals(Instant.parse("2026-11-26T07:00:00Z"), ReminderTimeCalculator.fireAt(r, null, berlin))
    }

    @Test
    fun aFixedInstantIgnoresTheZone() {
        val r = reminder(utc = 1_000_000L, mode = TimeZoneMode.FIXED)
        assertEquals(Instant.ofEpochMilli(1_000_000L), ReminderTimeCalculator.fireAt(r, null, berlin))
    }

    @Test
    fun aWallTimeSkippedByDstMovesForwardByTheGap() {
        // Berlin skips 02:00–03:00 on 2027-03-28.
        val at = ReminderTimeCalculator.fireAt(reminder(local = "2027-03-28T02:30"), null, berlin)
        assertEquals(Instant.parse("2027-03-28T01:30:00Z"), at) // 03:30 CEST
    }

    @Test
    fun unreadableTimesGiveNoFiring() {
        assertNull(ReminderTimeCalculator.fireAt(reminder(local = "tomorrow"), null, tehran))
        assertNull(ReminderTimeCalculator.fireAt(reminder(offset = 0), ReminderTarget("x", "26/11", null), tehran))
    }
}
