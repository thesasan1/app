package com.thesasan1.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.thesasan1.app.core.database.entity.OutboxEntity
import com.thesasan1.app.core.database.entity.SyncCursorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {
    /** Returns the new entry's sequence number. */
    @Insert
    suspend fun enqueue(entry: OutboxEntity): Long

    /** Oldest entries first, so changes replay in the order they were made. */
    @Query("SELECT * FROM sync_outbox ORDER BY seq LIMIT :limit")
    suspend fun nextBatch(limit: Int): List<OutboxEntity>

    @Query("UPDATE sync_outbox SET attempt_count = attempt_count + 1, last_error = :errorCode WHERE seq = :seq")
    suspend fun recordFailure(seq: Long, errorCode: String)

    /** Removes entries the server acknowledged. */
    @Query("DELETE FROM sync_outbox WHERE seq IN (:seqs)")
    suspend fun acknowledge(seqs: List<Long>): Int

    @Query("SELECT COUNT(*) FROM sync_outbox")
    fun observePendingCount(): Flow<Int>
}

@Dao
interface SyncCursorDao {
    @Query("SELECT server_cursor FROM sync_cursor WHERE scope = :scope")
    suspend fun get(scope: String): String?

    @Upsert
    suspend fun put(cursor: SyncCursorEntity)
}
