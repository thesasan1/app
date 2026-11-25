package com.thesasan1.app.feature.habits.domain

import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.core.database.entity.HabitEntity
import com.thesasan1.app.core.database.entity.HabitEventEntity
import com.thesasan1.app.core.database.entity.HabitMeasurement
import com.thesasan1.app.core.database.entity.HabitScheduleType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** What the user fills in to create a habit (HAB-01, HAB-02). Days are [WeekdayMask] bitmasks. */
data class HabitDraft(
    val name: String,
    val startDate: LocalDate,
    val description: String? = null,
    val measurement: HabitMeasurement = HabitMeasurement.BINARY,
    val targetValue: Double? = null,
    val unit: String? = null,
    val scheduleType: HabitScheduleType = HabitScheduleType.DAILY,
    val scheduleDays: Int? = null,
    val scheduleCount: Int? = null,
    val restDays: Int? = null,
    /** Streak freezes the habit starts with; 0 turns freezes off (ADR-030). */
    val freezes: Int = 0,
)

/** One habit on one day, derived from its events (HAB-07: nothing here is stored). */
data class HabitDay(
    val habit: HabitEntity,
    val date: LocalDate,
    /** Whether the habit is due that day (its schedule, start date and rest days). */
    val scheduled: Boolean,
    /** The day was marked as a rest day. */
    val rested: Boolean,
    /** Check-ins that still count, with corrections applied. */
    val checkIns: List<HabitEventEntity>,
    /** Count of check-ins (binary, multi) or the summed amount (numeric, duration). */
    val progress: Double,
    val isComplete: Boolean,
    /** Day streak up to this day; null for weekly and monthly targets. */
    val streak: HabitStreak? = null,
)

/** Habits and their check-ins. History is append-only: undo and corrections are new events (ADR-028). */
interface HabitRepository {
    fun observeActive(): Flow<List<HabitEntity>>

    fun observeArchived(): Flow<List<HabitEntity>>

    /** Every active habit on [date] with its progress, for the Habits screen and the dashboard. */
    fun observeDay(date: LocalDate): Flow<List<HabitDay>>

    suspend fun create(draft: HabitDraft): Outcome<HabitEntity>

    /**
     * Changes a habit's name, description, goal, unit and schedule (ADR-031). How it's measured and
     * its start date stay as they are, so past check-ins keep their meaning; its events are untouched.
     */
    suspend fun update(habitId: String, draft: HabitDraft): Outcome<HabitEntity>

    suspend fun archive(habitId: String): Outcome<Unit>

    suspend fun unarchive(habitId: String): Outcome<Unit>

    /** Soft delete; its events stay so history and sync are intact. */
    suspend fun delete(habitId: String): Outcome<Unit>

    /** Records a check-in for [date]. Numeric and duration habits need a positive [value]. */
    suspend fun checkIn(habitId: String, date: LocalDate, value: Double? = null): Outcome<HabitEventEntity>

    /** Cancels a check-in, rest day or correction by recording an UNDO that points at it. */
    suspend fun undo(eventId: String): Outcome<Unit>

    /** Replaces a check-in's amount by recording a CORRECTION that points at it. */
    suspend fun correct(eventId: String, value: Double): Outcome<Unit>

    /** Marks [date] as a rest day for the habit (HAB-06). */
    suspend fun markRest(habitId: String, date: LocalDate): Outcome<Unit>

    /**
     * Spends one of the habit's freezes on a missed past due day so its streak carries on (HAB-06).
     * Recorded as a FREEZE_USED event; undoing it gives the freeze back.
     */
    suspend fun useFreeze(habitId: String, date: LocalDate): Outcome<Unit>
}
