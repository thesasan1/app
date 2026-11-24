package com.thesasan1.app.feature.notes.domain

import com.thesasan1.app.core.common.Outcome
import kotlinx.coroutines.flow.Flow

/** A note as the rest of the app sees it. Instants are UTC epoch milliseconds. */
data class Note(
    val id: String,
    val title: String?,
    val document: NoteDocument,
    val categoryId: String?,
    val projectId: String?,
    val isPinned: Boolean,
    val archivedAt: Long?,
    val deletedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val isArchived: Boolean get() = archivedAt != null
    val isInTrash: Boolean get() = deletedAt != null
}

/**
 * Offline-first note storage (ARCHITECTURE.md §2.5). Every write commits the note and its sync
 * outbox entry together. Writes return [Outcome.Failure] with `AppError.NotFound` for unknown ids.
 */
interface NoteRepository {
    fun observeActive(): Flow<List<Note>>
    fun observeByProject(projectId: String): Flow<List<Note>>
    fun observeArchived(): Flow<List<Note>>
    fun observeTrash(): Flow<List<Note>>

    /** Active and archived notes matching every word of [query] by prefix. Blank queries match nothing. */
    fun search(query: String): Flow<List<Note>>

    suspend fun get(id: String): Outcome<Note>

    suspend fun create(
        title: String? = null,
        document: NoteDocument = NoteDocument.EMPTY,
        projectId: String? = null,
        categoryId: String? = null,
    ): Outcome<Note>

    /**
     * Saves edited content. Safe to call on every debounced keystroke and on lifecycle stop: an
     * unchanged note is not rewritten and queues nothing for sync.
     */
    suspend fun saveContent(id: String, title: String?, document: NoteDocument): Outcome<Note>

    /** Files the note under a project and category; null clears either. */
    suspend fun organize(id: String, projectId: String?, categoryId: String?): Outcome<Note>

    suspend fun setPinned(id: String, pinned: Boolean): Outcome<Note>
    suspend fun setArchived(id: String, archived: Boolean): Outcome<Note>
    suspend fun moveToTrash(id: String): Outcome<Note>
    suspend fun restoreFromTrash(id: String): Outcome<Note>

    /**
     * Deletes a trashed note for good and tells the server to delete it too. The UI confirms first
     * (NOTE-04). Fails with `AppError.Validation` for notes that aren't in the trash.
     */
    suspend fun deletePermanently(id: String): Outcome<Unit>

    /** Removes notes that have been in the trash longer than the retention window. Returns the count. */
    suspend fun purgeExpiredTrash(): Outcome<Int>
}
