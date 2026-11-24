package com.thesasan1.app.feature.dashboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class DashboardViewModelTest {

    private val tehran = ZoneId.of("Asia/Tehran")

    @Test
    fun `initial state uses the injected clock and zone`() {
        // 2026-11-24T05:00Z is 08:30 in Tehran (UTC+03:30).
        val clock = Clock.fixed(Instant.parse("2026-11-24T05:00:00Z"), tehran)
        val state = DashboardViewModel(clock).uiState.value
        assertEquals(DashboardUiState(DayPeriod.Morning, LocalDate.of(2026, 11, 24)), state)
    }

    @Test
    fun `local date follows the device zone, not UTC`() {
        // 22:00Z on Nov 24 is already 01:30 on Nov 25 in Tehran.
        val clock = Clock.fixed(Instant.parse("2026-11-24T22:00:00Z"), tehran)
        val state = DashboardViewModel(clock).uiState.value
        assertEquals(LocalDate.of(2026, 11, 25), state.today)
        assertEquals(DayPeriod.Night, state.period)
    }

    @Test
    fun `refresh recomputes time-dependent state`() {
        val mutableClock = MutableClock(Instant.parse("2026-11-24T09:00:00Z"))
        val viewModel = DashboardViewModel(mutableClock)
        assertEquals(DayPeriod.Morning, viewModel.uiState.value.period)

        mutableClock.instant = Instant.parse("2026-11-24T18:00:00Z")
        viewModel.refresh()

        assertEquals(DayPeriod.Evening, viewModel.uiState.value.period)
    }

    private class MutableClock(var instant: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = instant
    }
}
