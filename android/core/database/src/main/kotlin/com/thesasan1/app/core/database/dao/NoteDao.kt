package com.thesasan1.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.thesasan1.app.core.database.SearchText
import com.thesasan1.app.core.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Notes and their full-text index. Always write through [upsert] so `notes_fts` stays in step.
 * Deleting is soft (`sync.deleted(now)`); trashed notes stay searchable only through [observeTrash].
 */
@Dao
interface NoteDao {
    /** Writes the note and re-indexes it, in one transaction. */
    @Transaction
    suspend fun upsert(note: NoteEntity) {
        upsertRow(note)
        deleteIndex(note.id)
        insertIndex(
            noteId = note.id,
            title = SearchText.normalize(note.title.orEmpty()),
            bodyPlain = SearchText.normalize(note.bodyPlain),
        )
    }

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: String): NoteEntity?

    /** Notes that are neither archived nor trashed: pinned first, then most recently edited. */
    @Query(
        "SELECT * FROM notes WHERE deleted_at IS NULL AND archived_at IS NULL " +
            "ORDER BY is_pinned DESC, updated_at DESC",
    )
    fun observeActive(): Flow<List<NoteEntity>>

    @Query(
        "SELECT * FROM notes WHERE deleted_at IS NULL AND archived_at IS NULL AND project_id = :projectId " +
            "ORDER BY is_pinned DESC, updated_at DESC",
    )
    fun observeByProject(projectId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deleted_at IS NULL AND archived_at IS NOT NULL ORDER BY archived_at DESC")
    fun observeArchived(): Flow<List<NoteEntity>>

    /** The trash, most recently deleted first. */
    @Query("SELECT * FROM notes WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun observeTrash(): Flow<List<NoteEntity>>

    /**
     * Active and archived notes matching [matchQuery], which must come from [SearchText.matchQuery].
     * Ordered like [observeActive].
     */
    @Query(
        "SELECT notes.* FROM notes JOIN notes_fts ON notes_fts.note_id = notes.id " +
            "WHERE notes_fts MATCH :matchQuery AND notes.deleted_at IS NULL " +
            "ORDER BY notes.is_pinned DESC, notes.updated_at DESC",
    )
    fun search(matchQuery: String): Flow<List<NoteEntity>>

    /** Hard-deletes one note and its index row (NOTE-04 "delete forever"). */
    @Transaction
    suspend fun deletePermanently(id: String) {
        deleteIndex(id)
        deleteRow(id)
    }

    /** Hard-deletes notes trashed before [cutoffMillis] and their index rows. Returns the count. */
    @Transaction
    suspend fun purgeDeletedBefore(cutoffMillis: Long): Int {
        deleteIndexForDeletedBefore(cutoffMillis)
        return deleteRowsDeletedBefore(cutoffMillis)
    }

    @Upsert
    suspend fun upsertRow(note: NoteEntity)

    @Query("DELETE FROM notes_fts WHERE note_id = :noteId")
    suspend fun deleteIndex(noteId: String)

    @Query("INSERT INTO notes_fts (note_id, title, body_plain) VALUES (:noteId, :title, :bodyPlain)")
    suspend fun insertIndex(noteId: String, title: String, bodyPlain: String)

    @Query(
        "DELETE FROM notes_fts WHERE note_id IN " +
            "(SELECT id FROM notes WHERE deleted_at IS NOT NULL AND deleted_at < :cutoffMillis)",
    )
    suspend fun deleteIndexForDeletedBefore(cutoffMillis: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteRow(id: String)

    @Query("DELETE FROM notes WHERE deleted_at IS NOT NULL AND deleted_at < :cutoffMillis")
    suspend fun deleteRowsDeletedBefore(cutoffMillis: Long): Int
}
