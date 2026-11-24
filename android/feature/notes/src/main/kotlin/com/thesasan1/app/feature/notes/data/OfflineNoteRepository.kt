package com.thesasan1.app.feature.notes.data

import com.thesasan1.app.core.common.AppError
import com.thesasan1.app.core.common.IdGenerator
import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.core.common.runCatchingOutcome
import com.thesasan1.app.core.database.SearchText
import com.thesasan1.app.core.database.TransactionRunner
import com.thesasan1.app.core.database.dao.NoteDao
import com.thesasan1.app.core.database.dao.OutboxDao
import com.thesasan1.app.core.database.entity.NoteEntity
import com.thesasan1.app.core.database.entity.OutboxEntity
import com.thesasan1.app.core.database.entity.OutboxOp
import com.thesasan1.app.core.database.entity.SyncColumns
import com.thesasan1.app.feature.notes.domain.Note
import com.thesasan1.app.feature.notes.domain.NoteDocument
import com.thesasan1.app.feature.notes.domain.NoteRepository
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Room-backed [NoteRepository]. Each write runs in one transaction: the note row, its search index
 * row and an outbox entry for the sync engine (DATABASE.md §5).
 */
class OfflineNoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val outboxDao: OutboxDao,
    private val transaction: TransactionRunner,
    private val ids: IdGenerator,
    private val clock: Clock,
) : NoteRepository {

    override fun observeActive(): Flow<List<Note>> = noteDao.observeActive().toNotes()

    override fun observeByProject(projectId: String): Flow<List<Note>> = noteDao.observeByProject(projectId).toNotes()

    override fun observeArchived(): Flow<List<Note>> = noteDao.observeArchived().toNotes()

    override fun observeTrash(): Flow<List<Note>> = noteDao.observeTrash().toNotes()

    override fun search(query: String): Flow<List<Note>> {
        val match = SearchText.matchQuery(query) ?: return flowOf(emptyList())
        return noteDao.search(match).toNotes()
    }

    override suspend fun get(id: String): Outcome<Note> = storage {
        noteDao.getById(id)?.toNote() ?: return Outcome.Failure(AppError.NotFound)
    }

    override suspend fun create(
        title: String?,
        document: NoteDocument,
        projectId: String?,
        categoryId: String?,
    ): Outcome<Note> = storage {
        val now = clock.millis()
        val entity = NoteEntity(
            id = ids.newId(),
            title = title.cleanTitle(),
            body = NoteDocumentCodec.encode(document),
            bodyPlain = NoteDocumentCodec.plainText(document),
            projectId = projectId,
            categoryId = categoryId,
            sync = SyncColumns.new(now),
        )
        transaction { write(entity) }
        entity.toNote()
    }

    override suspend fun saveContent(id: String, title: String?, document: NoteDocument): Outcome<Note> = update(id) {
        copy(
            title = title.cleanTitle(),
            body = NoteDocumentCodec.encode(document),
            bodyPlain = NoteDocumentCodec.plainText(document),
        )
    }

    override suspend fun organize(id: String, projectId: String?, categoryId: String?): Outcome<Note> =
        update(id) { copy(projectId = projectId, categoryId = categoryId) }

    override suspend fun setPinned(id: String, pinned: Boolean): Outcome<Note> = update(id) { copy(isPinned = pinned) }

    override suspend fun setArchived(id: String, archived: Boolean): Outcome<Note> = update(id) {
        when {
            archived && archivedAt == null -> copy(archivedAt = clock.millis())
            !archived -> copy(archivedAt = null)
            else -> this
        }
    }

    override suspend fun moveToTrash(id: String): Outcome<Note> = update(id, touch = false) {
        if (sync.deletedAt != null) this else copy(sync = sync.deleted(clock.millis()))
    }

    override suspend fun restoreFromTrash(id: String): Outcome<Note> = update(id) {
        copy(sync = sync.copy(deletedAt = null))
    }

    override suspend fun deletePermanently(id: String): Outcome<Unit> = storage {
        transaction tx@{
            val note = noteDao.getById(id) ?: return@tx Outcome.Failure(AppError.NotFound)
            if (note.sync.deletedAt == null) {
                return@tx Outcome.Failure(
                    AppError.Validation(field = "deleted_at", reason = "only notes in the trash can be deleted"),
                )
            }
            noteDao.deletePermanently(id)
            outboxDao.enqueue(outboxEntry(id, OutboxOp.DELETE, payload = null))
            Outcome.Success(Unit)
        }
    }.flatten()

    override suspend fun purgeExpiredTrash(): Outcome<Int> = storage {
        noteDao.purgeDeletedBefore(clock.millis() - TRASH_RETENTION.toMillis())
    }

    /**
     * Applies [change] to the stored note. Nothing is written, and nothing queued, when the change
     * leaves the note as it was. [touch] bumps `updated_at` and marks the row pending.
     */
    private suspend fun update(id: String, touch: Boolean = true, change: NoteEntity.() -> NoteEntity): Outcome<Note> =
        storage {
            transaction tx@{
                val current = noteDao.getById(id) ?: return@tx Outcome.Failure(AppError.NotFound)
                val changed = current.change()
                if (changed == current) return@tx Outcome.Success(current.toNote())
                val updated = if (touch) changed.copy(sync = changed.sync.touched(clock.millis())) else changed
                write(updated)
                Outcome.Success(updated.toNote())
            }
        }.flatten()

    private suspend fun write(entity: NoteEntity) {
        noteDao.upsert(entity)
        outboxDao.enqueue(outboxEntry(entity.id, OutboxOp.UPSERT, payload = syncPayload(entity)))
    }

    private fun outboxEntry(noteId: String, op: OutboxOp, payload: String?) = OutboxEntity(
        entityType = ENTITY_TYPE,
        entityId = noteId,
        op = op,
        payload = payload,
        idempotencyKey = ids.newId(),
        createdAt = clock.millis(),
    )

    private inline fun <T> storage(block: () -> T): Outcome<T> = runCatchingOutcome({ AppError.Storage(it) }, block)

    private fun Flow<List<NoteEntity>>.toNotes(): Flow<List<Note>> = map { rows -> rows.map { it.toNote() } }

    companion object {
        /** `sync_outbox.entity_type` for notes; matches the server's resource name. */
        const val ENTITY_TYPE = "note"

        /** Trash retention (DATABASE.md §6). */
        val TRASH_RETENTION: Duration = Duration.ofDays(30)

        /** The body the sync engine pushes for a note. Content is never logged (SECURITY.md). */
        internal fun syncPayload(entity: NoteEntity): String = buildJsonObject {
            put("id", entity.id)
            put("title", entity.title?.let(::JsonPrimitive) ?: JsonNull)
            put("body", entity.body)
            put("category_id", entity.categoryId?.let(::JsonPrimitive) ?: JsonNull)
            put("project_id", entity.projectId?.let(::JsonPrimitive) ?: JsonNull)
            put("is_pinned", entity.isPinned)
            put("archived_at", entity.archivedAt?.let(::JsonPrimitive) ?: JsonNull)
            put("deleted_at", entity.sync.deletedAt?.let(::JsonPrimitive) ?: JsonNull)
            put("created_at", entity.sync.createdAt)
            put("updated_at", entity.sync.updatedAt)
            put("base_version", entity.sync.version?.let(::JsonPrimitive) ?: JsonNull)
        }.toString()
    }
}

/** Titles are trimmed; a blank title is stored as null. */
private fun String?.cleanTitle(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private fun <T> Outcome<Outcome<T>>.flatten(): Outcome<T> = when (this) {
    is Outcome.Success -> value
    is Outcome.Failure -> this
}

internal fun NoteEntity.toNote() = Note(
    id = id,
    title = title,
    document = NoteDocumentCodec.decode(body),
    categoryId = categoryId,
    projectId = projectId,
    isPinned = isPinned,
    archivedAt = archivedAt,
    deletedAt = sync.deletedAt,
    createdAt = sync.createdAt,
    updatedAt = sync.updatedAt,
)
