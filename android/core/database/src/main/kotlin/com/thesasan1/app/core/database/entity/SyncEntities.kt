package com.thesasan1.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class OutboxOp {
    UPSERT,
    DELETE,
}

/**
 * A local change waiting to be pushed (DATABASE.md §5). Rows are written in the same
 * transaction as the change they describe and removed once the server acknowledges them.
 */
@Entity(
    tableName = "sync_outbox",
    indices = [Index(value = ["idempotency_key"], unique = true), Index(value = ["entity_type", "entity_id"])],
)
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    val op: OutboxOp,
    /** JSON body for the push request; null for deletes. Never logged (SECURITY.md). */
    val payload: String?,
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    @ColumnInfo(name = "attempt_count", defaultValue = "0") val attemptCount: Int = 0,
    /** Short machine-readable error code from the last failed attempt, never user content. */
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** Pull position per sync scope (DATABASE.md §5). */
@Entity(tableName = "sync_cursor")
data class SyncCursorEntity(@PrimaryKey val scope: String, @ColumnInfo(name = "server_cursor") val serverCursor: String)
