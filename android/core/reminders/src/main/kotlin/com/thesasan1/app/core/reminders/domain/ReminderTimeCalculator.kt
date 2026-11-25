package com.thesasan1.app.core.reminders.domain

import com.thesasan1.app.core.database.entity.ReminderEntity
import com.thesasan1.app.core.database.entity.TimeZoneMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/** What a reminder needs to know about the task it belongs to. */
data class ReminderTarget(
    val title: String,
    /** ISO local date, or null when the task has no due date. */
    val dueDate: String?,
    /** ISO local time, or null for an all-day task. */
    val dueTime: String?,
    /** Set for FIXED tasks; FLOATING tasks follow the device zone. */
    val zoneId: String? = null,
    /** False once the task is done, cancelled, archived or trashed: its reminders stop. */
    val isOpen: Boolean = true,
)

/**
 * Works out when a TIME reminder should next ring (ADR-025). Pure, so time-zone and DST cases
 * are unit-tested on the JVM.
 *
 * - An offset reminder rings [ReminderEntity.offsetMinutes] from its task's due date and time; an
 *   all-day task counts as due at [ALL_DAY_TIME]. Closed or undated tasks give no time.
 * - A FIXED reminder rings at [ReminderEntity.triggerAtUtc].
 * - A FLOATING reminder rings at [ReminderEntity.triggerAtLocal] in [zone], so it follows the
 *   user across time zones. A wall time skipped by a DST jump moves forward by the gap.
 */
object ReminderTimeCalculator {
    private const val ALL_DAY_HOUR = 9
    private const val SECONDS_PER_MINUTE = 60L
    val ALL_DAY_TIME: LocalTime = LocalTime.of(ALL_DAY_HOUR, 0)

    fun fireAt(reminder: ReminderEntity, target: ReminderTarget?, zone: ZoneId): Instant? {
        val offset = reminder.offsetMinutes
        return when {
            offset != null -> target?.takeIf { it.isOpen }?.let { dueInstant(it, zone) }?.plusSeconds(
                offset * SECONDS_PER_MINUTE,
            )
            reminder.timeZoneMode == TimeZoneMode.FIXED && reminder.triggerAtUtc != null ->
                Instant.ofEpochMilli(reminder.triggerAtUtc!!)
            reminder.triggerAtLocal != null -> parseLocal(reminder.triggerAtLocal!!)?.atZone(zone)?.toInstant()
            else -> reminder.triggerAtUtc?.let(Instant::ofEpochMilli)
        }
    }

    private fun dueInstant(target: ReminderTarget, zone: ZoneId): Instant? {
        val date = target.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val time = target.dueTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: ALL_DAY_TIME
        val targetZone = target.zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: zone
        return date.atTime(time).atZone(targetZone).toInstant()
    }

    private fun parseLocal(text: String): LocalDateTime? = try {
        LocalDateTime.parse(text)
    } catch (_: DateTimeParseException) {
        null
    }
}
