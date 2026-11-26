package com.thesasan1.app.feature.dashboard

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

data class DashboardUiState(val period: DayPeriod, val today: LocalDate)

/**
 * Home: a greeting, the date, and today's tasks and habits at a glance (ADR-033).
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(private val clock: Clock, private val source: TodaySummarySource) :
    ViewModel() {

    private val _uiState = MutableStateFlow(currentState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /** Today's counts; follows the date when [refresh] moves it past midnight. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val summary: Flow<TodaySummary> = _uiState.map { it.today }.distinctUntilChanged().flatMapLatest(source::observe)

    /** Recomputes time-dependent fields, e.g. when the screen resumes after midnight. */
    fun refresh() {
        _uiState.value = currentState()
    }

    private fun currentState(): DashboardUiState {
        val now = LocalDateTime.now(clock)
        return DashboardUiState(period = DayPeriod.from(now.toLocalTime()), today = now.toLocalDate())
    }
}
