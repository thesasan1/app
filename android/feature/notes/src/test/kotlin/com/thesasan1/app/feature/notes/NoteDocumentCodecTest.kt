package com.thesasan1.app.feature.notes

import com.thesasan1.app.feature.notes.data.NoteDocumentCodec
import com.thesasan1.app.feature.notes.domain.Mark
import com.thesasan1.app.feature.notes.domain.NoteBlock
import com.thesasan1.app.feature.notes.domain.NoteDocument
import com.thesasan1.app.feature.notes.domain.Span
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class NoteDocumentCodecTest {

    private val everyBlock = NoteDocument(
        listOf(
            NoteBlock.Heading(2, listOf(Span("Plan"))),
            NoteBlock.Paragraph(
                listOf(
                    Span("Call "),
                    Span("Sara", setOf(Mark.Bold, Mark.Link("https://example.com"))),
                    Span(" today", setOf(Mark.Italic)),
                ),
            ),
            NoteBlock.ListItem(ordered = false, spans = listOf(Span("bread"))),
            NoteBlock.ListItem(ordered = true, indent = 1, spans = listOf(Span("rye"))),
            NoteBlock.ChecklistItem(checked = true, spans = listOf(Span("milk", setOf(Mark.Strikethrough)))),
            NoteBlock.Quote(listOf(Span("یادداشت"))),
            NoteBlock.Code("val x = 1", language = "kotlin"),
            NoteBlock.Image(attachmentId = "att-1", alt = "receipt"),
        ),
    )

    @Test
    fun `every block and mark round-trips`() {
        assertEquals(everyBlock, NoteDocumentCodec.decode(NoteDocumentCodec.encode(everyBlock)))
    }

    @Test
    fun `encoding is stable and versioned`() {
        val json = NoteDocumentCodec.encode(NoteDocument(listOf(NoteBlock.Paragraph(listOf(Span("hi"))))))
        assertEquals("""{"v":1,"blocks":[{"type":"p","spans":[{"text":"hi"}]}]}""", json)
    }

    @Test
    fun `adjacent spans with the same marks merge and empty spans drop`() {
        val doc = NoteDocument(
            listOf(NoteBlock.Paragraph(listOf(Span("a", setOf(Mark.Bold)), Span(""), Span("b", setOf(Mark.Bold))))),
        )
        val decoded = NoteDocumentCodec.decode(NoteDocumentCodec.encode(doc))
        assertEquals(NoteBlock.Paragraph(listOf(Span("ab", setOf(Mark.Bold)))), decoded.blocks.single())
    }

    @Test
    fun `plain text has one line per block`() {
        assertEquals(
            "Plan\nCall Sara today\nbread\nrye\nmilk\nیادداشت\nval x = 1\nreceipt",
            NoteDocumentCodec.plainText(everyBlock),
        )
    }

    @Test
    fun `blocks from a newer version are kept verbatim and searchable`() {
        val newer = """{"v":2,"blocks":[{"type":"callout","tone":"info","spans":[{"text":"Heads up"}]},""" +
            """{"type":"p","spans":[{"text":"x","marks":["b","glow"]}],"extra":true}]}"""

        val doc = NoteDocumentCodec.decode(newer)

        assertInstanceOf(NoteBlock.Unknown::class.java, doc.blocks[0])
        assertEquals(NoteBlock.Paragraph(listOf(Span("x", setOf(Mark.Bold)))), doc.blocks[1])
        assertEquals("Heads up\nx", NoteDocumentCodec.plainText(doc))
        val rewritten = Json.parseToJsonElement(NoteDocumentCodec.encode(doc)).jsonObject
        assertEquals(
            Json.parseToJsonElement(newer).jsonObject.getValue("blocks").jsonArray[0],
            rewritten.getValue("blocks").jsonArray[0],
        )
    }

    @Test
    fun `a body that is not a document is read as plain text`() {
        assertEquals(NoteDocument.fromPlainText("hello\nworld"), NoteDocumentCodec.decode("hello\nworld"))
        assertEquals(NoteDocument.EMPTY, NoteDocumentCodec.decode(""))
    }

    @Test
    fun `out-of-range values are clamped and broken blocks skipped`() {
        val doc = NoteDocumentCodec.decode(
            """{"v":1,"blocks":[{"type":"heading","level":9,"spans":[]},{"type":"image"},{"spans":[]},5]}""",
        )
        assertEquals(listOf(NoteBlock.Heading(3, emptyList())), doc.blocks)
    }
}
