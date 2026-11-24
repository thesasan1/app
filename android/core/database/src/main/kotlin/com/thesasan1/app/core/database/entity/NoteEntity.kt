package com.thesasan1.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A note (DATABASE.md §3.1).
 *
 * [body] is the versioned JSON block document (ADR-009), and [bodyPlain] is its plain text, derived
 * by the caller on every write. `category_id` and `project_id` are plain columns, not foreign keys:
 * sync can deliver a note before its project, and deletes are soft anyway.
 */
@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["is_pinned", "updated_at"]),
        Index("updated_at"),
        Index("category_id"),
        Index("project_id"),
    ],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String? = null,
    val body: String,
    @ColumnInfo(name = "body_plain") val bodyPlain: String,
    @ColumnInfo(name = "category_id") val categoryId: String? = null,
    @ColumnInfo(name = "project_id") val projectId: String? = null,
    @ColumnInfo(name = "is_pinned", defaultValue = "0") val isPinned: Boolean = false,
    @ColumnInfo(name = "archived_at") val archivedAt: Long? = null,
    @Embedded val sync: SyncColumns,
)

/**
 * Full-text index over notes, kept in step by [com.thesasan1.app.core.database.dao.NoteDao].
 *
 * It isn't an external-content table: `notes` has a text primary key, and SQLite may renumber the
 * implicit rowid on VACUUM. Text is stored normalised by [com.thesasan1.app.core.database.SearchText].
 */
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61, notIndexed = ["note_id"])
@Entity(tableName = "notes_fts")
data class NoteFtsEntity(
    @ColumnInfo(name = "note_id") val noteId: String,
    val title: String,
    @ColumnInfo(name = "body_plain") val bodyPlain: String,
)
