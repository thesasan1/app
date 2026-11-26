package com.thesasan1.app.feature.habits

import com.thesasan1.app.core.reminders.domain.HabitReminder
import com.thesasan1.app.core.reminders.domain.HabitReminderRepository
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory habit reminders keyed by habit id. */
class FakeHabitReminders : HabitReminderRepository {
    val times = MutableStateFlow<Map<String, LocalTime>>(emptyMap())

    override fun observe(habitId: String): Flow<HabitReminder?> =
        times.map { all -> all[habitId]?.let { HabitReminder(id = "r-$habitId", time = it) } }

    override suspend fun set(habitId: String, time: LocalTime?) {
        times.value = if (time == null) times.value - habitId else times.value + (habitId to time)
    }
}
