package com.thesasan1.app.feature.habits.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesasan1.app.core.common.AppError
import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.core.database.entity.HabitEntity
import com.thesasan1.app.core.database.entity.HabitMeasurement
import com.thesasan1.app.core.database.entity.HabitScheduleType
import com.thesasan1.app.core.database.entity.WeekdayMask
import com.thesasan1.app.core.reminders.domain.HabitReminderRepository
import com.thesasan1.app.feature.habits.domain.HabitDay
import com.thesasan1.app.feature.habits.domain.HabitDraft
import com.thesasan1.app.feature.habits.domain.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    /** Daily reminder time, or null for none (ADR-032). */
    val reminder: LocalTime? = null,
) {
    companion object
}

data class HabitsUiState(
    val today: LocalDate,
    val days: List<HabitDay> = emptyList(),
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    /** The habit open in the Edit dialog. */
    val editing: HabitDay? = null,
    /** The open habit's current reminder time, loaded with the Edit dialog. */
    val editingReminder: LocalTime? = null,
    /** The habit whose amount is being entered (numeric and duration habits). */
    val logging: HabitDay? = null,
    val message: HabitMessage? = null,
) {
    val isEmpty: Boolean get() = !isLoading && days.isEmpty()
}

private data class Dialogs(
    val creating: Boolean = false,
    val editing: HabitDay? = null,
    val editingReminder: LocalTime? = null,
    val logging: HabitDay? = null,
    val message: HabitMessage? = null,
)

/** Today's habits: check in, undo, rest, archive, and create (HAB-01, HAB-02, HAB-06, HAB-07). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val reminders: HabitReminderRepository,
    private val clock: Clock,
) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now(clock))
    private val dialogs = MutableStateFlow(Dialogs())

    val uiState: StateFlow<HabitsUiState> =
        combine(today, today.flatMapLatest(repository::observeDay), dialogs) { date, days, d ->
            HabitsUiState(
                today = date,
                days = days.sortedByDescending { it.scheduled && !it.rested },
                isLoading = false,
                isCreating = d.creating,
                editing = d.editing,
                editingReminder = d.editingReminder,
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
        viewModelScope.launch {
            when (val outcome = repository.create(form.toDraft(today.value))) {
                is Outcome.Success -> {
                    form.reminder?.let { reminders.set(outcome.value.id, it) }
                    dialogs.value = dialogs.value.copy(creating = false, message = null)
                }
                is Outcome.Failure -> dialogs.value = dialogs.value.copy(message = outcome.error.toMessage())
            }
        }
    }

    fun openEdit(day: HabitDay) {
        viewModelScope.launch {
            val reminder = reminders.observe(day.habit.id).first()?.time
            dialogs.value = dialogs.value.copy(editing = day, editingReminder = reminder, message = null)
        }
    }

    fun dismissEdit() {
        dialogs.value = dialogs.value.copy(editing = null, message = null)
    }

    /** Saves the Edit dialog; the habit keeps its measurement and start date. */
    fun saveEdit(form: NewHabitForm) {
        val habit = dialogs.value.editing?.habit ?: return
        val draft = form.copy(measurement = habit.measurementType).toDraft(LocalDate.parse(habit.startDate))
        viewModelScope.launch {
            when (val outcome = repository.update(habit.id, draft)) {
                is Outcome.Success -> {
                    if (form.reminder != dialogs.value.editingReminder) reminders.set(habit.id, form.reminder)
                    dialogs.value = dialogs.value.copy(editing = null, editingReminder = null, message = null)
                }
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

private fun NewHabitForm.toDraft(startDate: LocalDate): HabitDraft {
    val target = target.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    val everyDay = days.size == DayOfWeek.entries.size
    return HabitDraft(
        name = name,
        startDate = startDate,
        measurement = measurement,
        targetValue = target.takeIf { measurement != HabitMeasurement.BINARY },
        unit = unit.takeIf { measurement == HabitMeasurement.NUMERIC },
        scheduleType = if (everyDay) HabitScheduleType.DAILY else HabitScheduleType.WEEKDAYS,
        scheduleDays = if (everyDay) null else days.fold(0) { mask, day -> mask or WeekdayMask.of(day) },
    )
}

/** The Edit dialog's starting values for [habit]. */
internal fun NewHabitForm.Companion.of(habit: HabitEntity) = NewHabitForm(
    name = habit.name,
    measurement = habit.measurementType,
    target = habit.targetValue?.let(::formatAmount).orEmpty(),
    unit = habit.unit.orEmpty(),
    days = if (habit.scheduleType == HabitScheduleType.WEEKDAYS) {
        WeekdayMask.days(habit.scheduleDays)
    } else {
        DayOfWeek.entries.toSet()
    },
)

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
