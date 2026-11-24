package com.thesasan1.app.feature.dashboard

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DashboardUiState(val period: DayPeriod, val today: LocalDate)

/**
 * Dashboard shell for the engineering foundation. Today's tasks, habits and reminders are added
 * in Phase 2 once their data layers exist; until then the screen shows an honest empty state.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(private val clock: Clock) : ViewModel() {

    private val _uiState = MutableStateFlow(currentState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /** Recomputes time-dependent fields, e.g. when the screen resumes after midnight. */
    fun refresh() {
        _uiState.value = currentState()
    }

    private fun currentState(): DashboardUiState {
        val now = LocalDateTime.now(clock)
        return DashboardUiState(period = DayPeriod.from(now.toLocalTime()), today = now.toLocalDate())
    }
}
