package com.thesasan1.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** What a reminder is attached to (DATABASE.md §3.4). */
enum class ReminderTargetType {
    TASK,
    HABIT,
    NOTE,
    STANDALONE,
}

/** What fires a reminder. Only [TIME] is scheduled so far; the others need platform support (REM-03, REM-04). */
enum class ReminderTriggerType {
    TIME,
    LOCATION,
    EVENT,
}

/**
 * A reminder (DATABASE.md §3.4, REM-01). A TIME reminder fires at one of:
 * - [triggerAtLocal], an ISO local date-time (`2026-11-25T09:30`) read in the device zone when
 *   [timeZoneMode] is FLOATING;
 * - [triggerAtUtc], a fixed instant (epoch millis) when [timeZoneMode] is FIXED;
 * - [offsetMinutes] relative to its task's due date and time (negative is before), when set.
 *
 * Location and event columns are stored now so later triggers need no migration. [targetId] is a
 * plain column, not a foreign key, as for tasks (ADR-023).
 */
@Entity(
    tableName = "reminders",
    indices = [Index(value = ["target_type", "target_id"]), Index(value = ["is_enabled", "trigger_type"])],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "target_type") val targetType: ReminderTargetType,
    @ColumnInfo(name = "target_id") val targetId: String? = null,
    val title: String? = null,
    @ColumnInfo(
        name = "trigger_type",
        defaultValue = "TIME",
    ) val triggerType: ReminderTriggerType = ReminderTriggerType.TIME,
    @ColumnInfo(name = "trigger_at_local") val triggerAtLocal: String? = null,
    @ColumnInfo(name = "trigger_at_utc") val triggerAtUtc: Long? = null,
    @ColumnInfo(
        name = "time_zone_mode",
        defaultValue = "FLOATING",
    ) val timeZoneMode: TimeZoneMode = TimeZoneMode.FLOATING,
    @ColumnInfo(name = "offset_minutes") val offsetMinutes: Int? = null,
    @ColumnInfo(name = "recurrence_rule") val recurrenceRule: String? = null,
    @ColumnInfo(name = "repeat_until_done", defaultValue = "0") val repeatUntilDone: Boolean = false,
    @ColumnInfo(name = "repeat_interval_min") val repeatIntervalMinutes: Int? = null,
    @ColumnInfo(name = "location_lat") val locationLat: Double? = null,
    @ColumnInfo(name = "location_lng") val locationLng: Double? = null,
    @ColumnInfo(name = "location_radius_m") val locationRadiusMeters: Int? = null,
    @ColumnInfo(name = "location_transition") val locationTransition: String? = null,
    @ColumnInfo(name = "event_kind") val eventKind: String? = null,
    @ColumnInfo(name = "is_enabled", defaultValue = "1") val isEnabled: Boolean = true,
    @Embedded val sync: SyncColumns,
)

/** Where one scheduled firing of a reminder stands (DATABASE.md §3.5). */
enum class OccurrenceState {
    SCHEDULED,
    DELIVERED,
    SNOOZED,
    DISMISSED,
    COMPLETED,
    MISSED,
}

/**
 * One firing of a reminder at [scheduledForUtc]. Local only (never synced): it is rebuilt from
 * `reminders` on boot and time changes. The [id] is derived from the reminder and the instant
 * ([idFor]), so rebuilding never creates a second row for the same firing and a delivered
 * occurrence is never delivered again (REM-06).
 */
@Entity(
    tableName = "reminder_occurrences",
    indices = [Index("reminder_id"), Index(value = ["state", "scheduled_for_utc"])],
)
data class ReminderOccurrenceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "reminder_id") val reminderId: String,
    @ColumnInfo(name = "scheduled_for_utc") val scheduledForUtc: Long,
    @ColumnInfo(defaultValue = "SCHEDULED") val state: OccurrenceState = OccurrenceState.SCHEDULED,
    @ColumnInfo(name = "delivered_at") val deliveredAt: Long? = null,
    @ColumnInfo(name = "snooze_until") val snoozeUntil: Long? = null,
) {
    /** When the alarm for this occurrence should ring: the snooze time if snoozed. */
    val nextFireAt: Long get() = snoozeUntil ?: scheduledForUtc

    companion object {
        /** Deterministic id for the firing of [reminderId] at [scheduledForUtc]. */
        fun idFor(reminderId: String, scheduledForUtc: Long): String =
            UUID.nameUUIDFromBytes("$reminderId@$scheduledForUtc".toByteArray()).toString()

        fun scheduled(reminderId: String, scheduledForUtc: Long) = ReminderOccurrenceEntity(
            id = idFor(reminderId, scheduledForUtc),
            reminderId = reminderId,
            scheduledForUtc = scheduledForUtc,
        )
    }
}
