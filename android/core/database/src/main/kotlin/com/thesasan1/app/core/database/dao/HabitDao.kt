package com.thesasan1.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.thesasan1.app.core.database.entity.HabitEntity
import com.thesasan1.app.core.database.entity.HabitEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Habits and their append-only events (DATABASE.md §3.6–3.7). Habits are deleted softly so the
 * deletion syncs. Events have no update or delete: that is the integrity rule of HAB-07, enforced
 * by this interface offering none.
 */
@Dao
interface HabitDao {
    @Upsert
    suspend fun upsert(habit: HabitEntity)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE id = :id AND deleted_at IS NULL")
    fun observe(id: String): Flow<HabitEntity?>

    /** Habits in use: not archived, not deleted. */
    @Query("SELECT * FROM habits WHERE archived_at IS NULL AND deleted_at IS NULL ORDER BY sort_order, created_at")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query(
        "SELECT * FROM habits WHERE archived_at IS NOT NULL AND deleted_at IS NULL ORDER BY archived_at DESC",
    )
    fun observeArchived(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_events WHERE id = :id")
    suspend fun getEvent(id: String): HabitEventEntity?

    /** Events of one habit on one habit day, in recorded order. */
    @Query("SELECT * FROM habit_events WHERE habit_id = :habitId AND local_date = :date ORDER BY recorded_at, id")
    suspend fun getEventsOn(habitId: String, date: String): List<HabitEventEntity>

    /** Records [event]. An id already present is ignored (merge by union); returns -1 then. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: HabitEventEntity): Long

    /** Events of one habit for habit days [from]..[to] (ISO dates, inclusive), in recorded order. */
    @Query(
        "SELECT * FROM habit_events WHERE habit_id = :habitId AND local_date BETWEEN :from AND :to " +
            "ORDER BY local_date, recorded_at, id",
    )
    fun observeEvents(habitId: String, from: String, to: String): Flow<List<HabitEventEntity>>

    /** Every event of one habit, oldest habit day first: what streaks are derived from. */
    @Query("SELECT * FROM habit_events WHERE habit_id = :habitId ORDER BY local_date, recorded_at, id")
    suspend fun getEvents(habitId: String): List<HabitEventEntity>

    /** All events on one habit day, for the dashboard's "today". */
    @Query("SELECT * FROM habit_events WHERE local_date = :date ORDER BY recorded_at, id")
    fun observeEventsOn(date: String): Flow<List<HabitEventEntity>>
}
