package com.thesasan1.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.thesasan1.app.core.database.entity.ReminderEntity
import com.thesasan1.app.core.database.entity.ReminderOccurrenceEntity
import com.thesasan1.app.core.database.entity.ReminderTargetType
import kotlinx.coroutines.flow.Flow

/**
 * Reminders and their local occurrences (ADR-024). Reminders are deleted softly
 * (`sync.deleted(now)`) so the deletion syncs; occurrences are local bookkeeping and are deleted
 * outright.
 */
@Dao
@Suppress("TooManyFunctions") // reminder rows plus the occurrence bookkeeping the scheduler needs
interface ReminderDao {
    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: String): ReminderEntity?

    /** Live reminders for one task, habit or note, earliest offset first. */
    @Query(
        "SELECT * FROM reminders WHERE target_type = :targetType AND target_id = :targetId " +
            "AND deleted_at IS NULL ORDER BY offset_minutes, trigger_at_local, trigger_at_utc, created_at",
    )
    fun observeForTarget(targetType: ReminderTargetType, targetId: String): Flow<List<ReminderEntity>>

    @Query(
        "SELECT * FROM reminders WHERE target_type = :targetType AND target_id = :targetId AND deleted_at IS NULL",
    )
    suspend fun getForTarget(targetType: ReminderTargetType, targetId: String): List<ReminderEntity>

    /** Every enabled, live time reminder: what the scheduler rebuilds from on boot or a time change. */
    @Query("SELECT * FROM reminders WHERE is_enabled = 1 AND trigger_type = 'TIME' AND deleted_at IS NULL")
    suspend fun getEnabledTimeReminders(): List<ReminderEntity>

    /** Adds [occurrence] unless that firing already exists. Returns -1 when it did. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOccurrence(occurrence: ReminderOccurrenceEntity): Long

    @Update
    suspend fun updateOccurrence(occurrence: ReminderOccurrenceEntity)

    @Query("SELECT * FROM reminder_occurrences WHERE id = :id")
    suspend fun getOccurrence(id: String): ReminderOccurrenceEntity?

    /** Occurrences still waiting to ring (scheduled or snoozed), soonest first. */
    @Query(
        "SELECT * FROM reminder_occurrences WHERE state IN ('SCHEDULED', 'SNOOZED') " +
            "ORDER BY COALESCE(snooze_until, scheduled_for_utc)",
    )
    suspend fun getPendingOccurrences(): List<ReminderOccurrenceEntity>

    /** Occurrences whose notification may still be on screen: delivered, or ringing again later. */
    @Query("SELECT * FROM reminder_occurrences WHERE state IN ('DELIVERED', 'SNOOZED') AND delivered_at IS NOT NULL")
    suspend fun getRingingOccurrences(): List<ReminderOccurrenceEntity>

    @Query("SELECT * FROM reminder_occurrences WHERE reminder_id = :reminderId ORDER BY scheduled_for_utc")
    suspend fun getOccurrencesFor(reminderId: String): List<ReminderOccurrenceEntity>

    /** Drops pending occurrences of [reminderId], e.g. when its time changes. Delivered ones stay. */
    @Query("DELETE FROM reminder_occurrences WHERE reminder_id = :reminderId AND state IN ('SCHEDULED', 'SNOOZED')")
    suspend fun deletePendingOccurrencesFor(reminderId: String)

    /** Forgets finished occurrences older than [beforeUtc]; pending ones are kept whatever their age. */
    @Query(
        "DELETE FROM reminder_occurrences WHERE state NOT IN ('SCHEDULED', 'SNOOZED') " +
            "AND scheduled_for_utc < :beforeUtc",
    )
    suspend fun pruneFinishedOccurrences(beforeUtc: Long): Int

    /** Removes a reminder row and every occurrence of it. Used after the deletion has synced. */
    @Transaction
    suspend fun deletePermanently(id: String) {
        deleteAllOccurrencesFor(id)
        deleteRow(id)
    }

    @Query("DELETE FROM reminder_occurrences WHERE reminder_id = :reminderId")
    suspend fun deleteAllOccurrencesFor(reminderId: String)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteRow(id: String)
}
