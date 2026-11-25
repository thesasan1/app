package com.thesasan1.app.feature.habits.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesasan1.app.core.common.AppError
import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.core.database.entity.HabitMeasurement
import com.thesasan1.app.core.database.entity.HabitScheduleType
import com.thesasan1.app.core.database.entity.WeekdayMask
import com.thesasan1.app.feature.habits.domain.HabitDay
import com.thesasan1.app.feature.habits.domain.HabitDraft
import com.thesasan1.app.feature.habits.domain.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Why the last action didn't go through, shown once. */
enum class HabitMessage { NameRequired, TargetRequired, DaysRequired, AmountRequired, AlreadyDone, Failed }

/** What the New habit dialog collects. [target] is typed text; blank means none. */
data class NewHabitForm(
    val name: String = "",
    val measurement: HabitMeasurement = HabitMeasurement.BINARY,
    val target: String = "",
    val unit: String = "",
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
)

data class HabitsUiState(
    val today: LocalDate,
    val days: List<HabitDay> = emptyList(),
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    /** The habit whose amount is being entered (numeric and duration habits). */
    val logging: HabitDay? = null,
    val message: HabitMessage? = null,
) {
    val isEmpty: Boolean get() = !isLoading && days.isEmpty()
}

private data class Dialogs(
    val creating: Boolean = false,
    val logging: HabitDay? = null,
    val message: HabitMessage? = null,
)

/** Today's habits: check in, undo, rest, archive, and create (HAB-01, HAB-02, HAB-06, HAB-07). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HabitsViewModel @Inject constructor(private val repository: HabitRepository, private val clock: Clock) :
    ViewModel() {
    private val today = MutableStateFlow(LocalDate.now(clock))
    private val dialogs = MutableStateFlow(Dialogs())

    val uiState: StateFlow<HabitsUiState> =
        combine(today, today.flatMapLatest(repository::observeDay), dialogs) { date, days, d ->
            HabitsUiState(
                today = date,
                days = days.sortedByDescending { it.scheduled && !it.rested },
                isLoading = false,
                isCreating = d.creating,
                logging = d.logging,
                message = d.message,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HabitsUiState(LocalDate.now(clock)))

    /** Call when the screen resumes so the list follows midnight. */
    fun refreshDay() {
        today.value = LocalDate.now(clock)
    }

    /** Yes/no and multi-check-in habits log at once; amount habits ask for the amount first. */
    fun checkIn(day: HabitDay) {
        if (day.habit.measurementType.needsAmount()) {
            dialogs.value = dialogs.value.copy(logging = day)
        } else {
            act { repository.checkIn(day.habit.id, today.value) }
        }
    }

    fun logAmount(amount: Double?) {
        val day = dialogs.value.logging ?: return
        dialogs.value = dialogs.value.copy(logging = null)
        act { repository.checkIn(day.habit.id, today.value, amount) }
    }

    fun dismissLog() {
        dialogs.value = dialogs.value.copy(logging = null)
    }

    /** Undoes the newest check-in that still counts. */
    fun undoLast(day: HabitDay) {
        val last = day.checkIns.maxByOrNull { it.recordedAt } ?: return
        act { repository.undo(last.id) }
    }

    fun rest(day: HabitDay) = act { repository.markRest(day.habit.id, today.value) }

    fun archive(day: HabitDay) = act { repository.archive(day.habit.id) }

    fun openNew() {
        dialogs.value = dialogs.value.copy(creating = true)
    }

    fun dismissNew() {
        dialogs.value = dialogs.value.copy(creating = false)
    }

    fun create(form: NewHabitForm) {
        val target = form.target.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        val everyDay = form.days.size == DayOfWeek.entries.size
        val draft = HabitDraft(
            name = form.name,
            startDate = today.value,
            measurement = form.measurement,
            targetValue = target.takeIf { form.measurement != HabitMeasurement.BINARY },
            unit = form.unit.takeIf { form.measurement == HabitMeasurement.NUMERIC },
            scheduleType = if (everyDay) HabitScheduleType.DAILY else HabitScheduleType.WEEKDAYS,
            scheduleDays = if (everyDay) null else form.days.fold(0) { mask, day -> mask or WeekdayMask.of(day) },
        )
        viewModelScope.launch {
            when (val outcome = repository.create(draft)) {
                is Outcome.Success -> dialogs.value = dialogs.value.copy(creating = false, message = null)
                is Outcome.Failure -> dialogs.value = dialogs.value.copy(message = outcome.error.toMessage())
            }
        }
    }

    fun dismissMessage() {
        dialogs.value = dialogs.value.copy(message = null)
    }

    private fun act(action: suspend () -> Outcome<*>) {
        viewModelScope.launch {
            val outcome = action()
            if (outcome is Outcome.Failure) dialogs.value = dialogs.value.copy(message = outcome.error.toMessage())
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

internal fun HabitMeasurement.needsAmount(): Boolean =
    this == HabitMeasurement.NUMERIC || this == HabitMeasurement.DURATION

private fun AppError.toMessage(): HabitMessage = when ((this as? AppError.Validation)?.field) {
    "name" -> HabitMessage.NameRequired
    "target_value" -> HabitMessage.TargetRequired
    "schedule_days" -> HabitMessage.DaysRequired
    "value" -> HabitMessage.AmountRequired
    "date" -> HabitMessage.AlreadyDone
    else -> HabitMessage.Failed
}
