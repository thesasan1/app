package com.thesasan1.app.feature.dashboard

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DashboardViewModelTest {

    private val tehran = ZoneId.of("Asia/Tehran")
    private val asked = mutableListOf<LocalDate>()
    private val source = TodaySummarySource { day ->
        asked += day
        flowOf(TodaySummary(tasksDue = 2, tasksOverdue = 0, habitsDue = 3, habitsDone = 1))
    }

    @Test
    fun `summary comes from the source for today`() = runTest {
        val clock = Clock.fixed(Instant.ofEpochSecond(1_795_496_400L), tehran)
        val summary = DashboardViewModel(clock, source).summary.first()
        assertEquals(2, summary.tasksDue)
        assertEquals(listOf(LocalDate.of(2026, 11, 24)), asked)
    }

    @Test
    fun `initial state uses the injected clock and zone`() {
        // 2026-11-24T05:00Z is 08:30 in Tehran (UTC+03:30).
        val clock = Clock.fixed(Instant.parse("2026-11-24T05:00:00Z"), tehran)
        val state = DashboardViewModel(clock, source).uiState.value
        assertEquals(DashboardUiState(DayPeriod.Morning, LocalDate.of(2026, 11, 24)), state)
    }

    @Test
    fun `local date follows the device zone, not UTC`() {
        // 22:00Z on Nov 24 is already 01:30 on Nov 25 in Tehran.
        val clock = Clock.fixed(Instant.parse("2026-11-24T22:00:00Z"), tehran)
        val state = DashboardViewModel(clock, source).uiState.value
        assertEquals(LocalDate.of(2026, 11, 25), state.today)
        assertEquals(DayPeriod.Night, state.period)
    }

    @Test
    fun `refresh recomputes time-dependent state`() {
        val mutableClock = MutableClock(Instant.parse("2026-11-24T09:00:00Z"))
        val viewModel = DashboardViewModel(mutableClock, source)
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
