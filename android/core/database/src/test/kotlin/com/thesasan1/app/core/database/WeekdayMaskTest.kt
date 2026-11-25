package com.thesasan1.app.core.database

import com.thesasan1.app.core.database.entity.WeekdayMask
import java.time.DayOfWeek
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeekdayMaskTest {

    @Test
    fun mondayIsOneAndSundayIsSixtyFour() {
        assertEquals(1, WeekdayMask.of(DayOfWeek.MONDAY))
        assertEquals(64, WeekdayMask.of(DayOfWeek.SUNDAY))
        assertEquals(WeekdayMask.EVERY_DAY, WeekdayMask.of(*DayOfWeek.entries.toTypedArray()))
    }

    @Test
    fun aMaskReadsBackItsDays() {
        val weekend = WeekdayMask.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

        assertTrue(WeekdayMask.contains(weekend, DayOfWeek.SATURDAY))
        assertFalse(WeekdayMask.contains(weekend, DayOfWeek.FRIDAY))
        assertFalse(WeekdayMask.contains(null, DayOfWeek.MONDAY))
        assertEquals(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), WeekdayMask.days(weekend))
    }
}
