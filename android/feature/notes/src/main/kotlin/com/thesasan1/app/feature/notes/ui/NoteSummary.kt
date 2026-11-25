package com.thesasan1.app.feature.notes.ui

import com.thesasan1.app.feature.notes.data.NoteDocumentCodec
import com.thesasan1.app.feature.notes.domain.Note

/** What a list row shows. [title] falls back to the first body line; both may be empty. */
data class NoteSummary(
    val id: String,
    val title: String,
    val preview: String,
    val isPinned: Boolean,
    val updatedAt: Long,
)

private const val PREVIEW_MAX_CHARS = 140

internal fun Note.toSummary(): NoteSummary {
    val lines = NoteDocumentCodec.plainText(document).lines().map(String::trim).filter(String::isNotEmpty)
    val heading = title?.takeIf(String::isNotBlank)
    return NoteSummary(
        id = id,
        title = heading ?: lines.firstOrNull().orEmpty(),
        preview = (if (heading == null) lines.drop(1) else lines).joinToString(" ").take(PREVIEW_MAX_CHARS),
        isPinned = isPinned,
        updatedAt = updatedAt,
    )
}
