package com.thesasan1.app.feature.notes.domain

import kotlinx.serialization.json.JsonObject

/**
 * A note body as a portable block list (ADR-009). Stored as JSON by
 * [com.thesasan1.app.feature.notes.data.NoteDocumentCodec].
 *
 * Lists and checklists are one block per item with an [indent] level, which keeps editing,
 * diffing and sync merges line-based.
 */
data class NoteDocument(val blocks: List<NoteBlock>) {
    companion object {
        /** The format version written today. Readers keep blocks they don't know. */
        const val VERSION = 1

        val EMPTY = NoteDocument(emptyList())

        /** One paragraph per line of [text]; blank input gives [EMPTY]. */
        fun fromPlainText(text: String): NoteDocument =
            if (text.isBlank()) EMPTY else NoteDocument(text.lines().map { NoteBlock.Paragraph(listOf(Span(it))) })
    }
}

sealed interface NoteBlock {
    data class Paragraph(val spans: List<Span>) : NoteBlock

    /** [level] is 1 to 3. */
    data class Heading(val level: Int, val spans: List<Span>) : NoteBlock {
        init {
            require(level in 1..MAX_HEADING_LEVEL) { "heading level $level" }
        }
    }

    data class ListItem(val ordered: Boolean, val indent: Int = 0, val spans: List<Span>) : NoteBlock

    data class ChecklistItem(val checked: Boolean, val indent: Int = 0, val spans: List<Span>) : NoteBlock

    data class Quote(val spans: List<Span>) : NoteBlock

    data class Code(val text: String, val language: String? = null) : NoteBlock

    /** An image stored as an attachment (NOTE-05). [alt] is searchable text. */
    data class Image(val attachmentId: String, val alt: String? = null) : NoteBlock

    /**
     * A block from a newer app version. It is kept as-is and written back unchanged, so editing a
     * note on an older device never drops content.
     */
    data class Unknown(val raw: JsonObject) : NoteBlock

    companion object {
        const val MAX_HEADING_LEVEL = 3
    }
}

/** A run of text with the same formatting. */
data class Span(val text: String, val marks: Set<Mark> = emptySet())

sealed interface Mark {
    data object Bold : Mark
    data object Italic : Mark
    data object Strikethrough : Mark
    data object Code : Mark
    data class Link(val url: String) : Mark
}
