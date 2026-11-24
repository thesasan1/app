package com.thesasan1.app.core.database.entity

import androidx.room.ColumnInfo

/** Local replication state of a syncable row (DATABASE.md §1). */
enum class SyncState {
    /** Matches the last version the server acknowledged. */
    SYNCED,

    /** Changed locally; an outbox entry is waiting to be pushed. */
    PENDING,

    /** The server rejected or diverged from the local change; see `conflict_log`. */
    CONFLICT,
}

/**
 * Columns every syncable table carries, embedded with `@Embedded`.
 *
 * Instants are UTC epoch milliseconds. `version` is assigned by the server and is null until the
 * first successful push. Ordering between devices will use a hybrid logical clock column that
 * the sync engine adds (ADR-017).
 */
data class SyncColumns(
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "version") val version: Long? = null,
    @ColumnInfo(name = "sync_state", defaultValue = "PENDING") val syncState: SyncState = SyncState.PENDING,
    @ColumnInfo(name = "last_synced_version") val lastSyncedVersion: Long? = null,
) {
    /** The same row marked as changed locally at [nowMillis]. */
    fun touched(nowMillis: Long): SyncColumns = copy(updatedAt = nowMillis, syncState = SyncState.PENDING)

    /** The same row as a tombstone deleted at [nowMillis]. */
    fun deleted(nowMillis: Long): SyncColumns = touched(nowMillis).copy(deletedAt = nowMillis)

    companion object {
        fun new(nowMillis: Long): SyncColumns = SyncColumns(createdAt = nowMillis, updatedAt = nowMillis)
    }
}
