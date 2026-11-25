package com.thesasan1.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek

/** How a habit is measured (HAB-02). */
enum class HabitMeasurement {
    /** Done or not done. */
    BINARY,

    /** An amount in [HabitEntity.unit], e.g. pages. */
    NUMERIC,

    /** A length of time; [HabitEntity.targetValue] is in seconds. */
    DURATION,

    /** Several check-ins a day; [HabitEntity.targetValue] is the count. */
    MULTI_CHECKIN,
}

/** When a habit is due (HAB-01). */
enum class HabitScheduleType {
    DAILY,

    /** The days in [HabitEntity.scheduleDays]. */
    WEEKDAYS,

    /** Any [HabitEntity.scheduleCount] days a week. */
    X_PER_WEEK,

    /** A total of [HabitEntity.targetValue] across the week. */
    WEEKLY_TARGET,

    /** A total of [HabitEntity.targetValue] across the month. */
    MONTHLY_TARGET,

    /** An RRULE in [HabitEntity.scheduleRule]. */
    CUSTOM,
}

/**
 * A habit (DATABASE.md §3.6, HAB-01, HAB-02, HAB-06). Dates are ISO local dates (`2026-11-25`), as
 * for tasks. [scheduleDays] and [restDays] are [WeekdayMask] bitmasks. Progress is never stored
 * here: it is derived from `habit_events` (HAB-07). [goalId] is a plain column until goals exist.
 */
@Entity(tableName = "habits", indices = [Index(value = ["archived_at", "deleted_at"])])
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    @ColumnInfo(
        name = "measurement_type",
        defaultValue = "BINARY",
    ) val measurementType: HabitMeasurement = HabitMeasurement.BINARY,
    @ColumnInfo(name = "target_value") val targetValue: Double? = null,
    val unit: String? = null,
    @ColumnInfo(
        name = "schedule_type",
        defaultValue = "DAILY",
    ) val scheduleType: HabitScheduleType = HabitScheduleType.DAILY,
    @ColumnInfo(name = "schedule_days") val scheduleDays: Int? = null,
    @ColumnInfo(name = "schedule_count") val scheduleCount: Int? = null,
    @ColumnInfo(name = "schedule_rule") val scheduleRule: String? = null,
    @ColumnInfo(name = "rest_days") val restDays: Int? = null,
    @ColumnInfo(name = "streak_freeze_enabled", defaultValue = "0") val streakFreezeEnabled: Boolean = false,
    @ColumnInfo(name = "freezes_available", defaultValue = "0") val freezesAvailable: Int = 0,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "archived_at") val archivedAt: Long? = null,
    @ColumnInfo(name = "goal_id") val goalId: String? = null,
    @ColumnInfo(name = "sort_order", defaultValue = "0") val sortOrder: Int = 0,
    @Embedded val sync: SyncColumns,
)

/** What a habit event records (DATABASE.md §3.7). */
enum class HabitEventType {
    CHECK_IN,

    /** Cancels the event in [HabitEventEntity.correctsEventId]. */
    UNDO,
    FREEZE_USED,
    REST,

    /** Replaces the value of the event in [HabitEventEntity.correctsEventId]. */
    CORRECTION,
}

/**
 * One habit event. Append-only (HAB-07): rows are inserted, never updated or deleted, so a streak
 * can never be repaired by rewriting history; an undo or a correction is a new event pointing at
 * the one it changes. Events merge across devices by union on [id] (ARCHITECTURE.md §2.7).
 * [localDate] is the habit day the event counts for; [recordedAt] is the real time it was
 * recorded (UTC epoch millis, never back-dated).
 */
@Entity(
    tableName = "habit_events",
    indices = [Index(value = ["habit_id", "local_date"]), Index("corrects_event_id")],
)
data class HabitEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "habit_id") val habitId: String,
    @ColumnInfo(name = "local_date") val localDate: String,
    @ColumnInfo(name = "event_type") val eventType: HabitEventType,
    val value: Double? = null,
    val note: String? = null,
    @ColumnInfo(name = "corrects_event_id") val correctsEventId: String? = null,
    @ColumnInfo(name = "recorded_at") val recordedAt: Long,
    @ColumnInfo(name = "sync_state", defaultValue = "PENDING") val syncState: SyncState = SyncState.PENDING,
)

/** Days of the week as a bitmask: Monday is 1, Tuesday 2, … Sunday 64 (DATABASE.md §3.6). */
object WeekdayMask {
    const val EVERY_DAY = 0b111_1111

    fun of(vararg days: DayOfWeek): Int = days.fold(0) { mask, day -> mask or bit(day) }

    fun contains(mask: Int?, day: DayOfWeek): Boolean = mask != null && mask and bit(day) != 0

    fun days(mask: Int?): Set<DayOfWeek> = DayOfWeek.entries.filterTo(sortedSetOf()) { contains(mask, it) }

    private fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)
}
