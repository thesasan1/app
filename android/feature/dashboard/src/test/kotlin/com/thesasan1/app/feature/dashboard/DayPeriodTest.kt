package com.thesasan1.app.feature.dashboard

import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class DayPeriodTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        "00:00, Night",
        "04:59, Night",
        "05:00, Morning",
        "11:59, Morning",
        "12:00, Afternoon",
        "16:59, Afternoon",
        "17:00, Evening",
        "21:59, Evening",
        "22:00, Night",
        "23:59, Night",
    )
    fun `maps time of day to period at boundaries`(time: String, expected: DayPeriod) {
        assertEquals(expected, DayPeriod.from(LocalTime.parse(time)))
    }
}
