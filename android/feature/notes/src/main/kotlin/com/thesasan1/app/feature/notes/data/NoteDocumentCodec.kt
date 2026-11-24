package com.thesasan1.app.feature.notes.data

import com.thesasan1.app.feature.notes.domain.Mark
import com.thesasan1.app.feature.notes.domain.NoteBlock
import com.thesasan1.app.feature.notes.domain.NoteDocument
import com.thesasan1.app.feature.notes.domain.Span
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

/**
 * Reads and writes [NoteDocument] JSON (ADR-009):
 *
 * ```json
 * {"v":1,"blocks":[{"type":"heading","level":1,"spans":[{"text":"Plan"}]},
 *   {"type":"check","checked":false,"spans":[{"text":"Call ","marks":["b"]},
 *     {"text":"Sara","marks":[{"link":"https://example.com"}]}]}]}
 * ```
 *
 * Decoding is lenient: unknown block types are kept as [NoteBlock.Unknown], unknown keys and
 * marks are ignored, and a body that isn't a document at all is read as plain text, so no note
 * is ever unreadable.
 */
object NoteDocumentCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(document: NoteDocument): String = buildJsonObject {
        put("v", NoteDocument.VERSION)
        put("blocks", JsonArray(document.blocks.map(::encodeBlock)))
    }.toString()

    fun decode(body: String): NoteDocument {
        val root = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull()
        val blocks = (root?.get("blocks") as? JsonArray) ?: return NoteDocument.fromPlainText(body)
        return NoteDocument(blocks.mapNotNull { (it as? JsonObject)?.let(::decodeBlock) })
    }

    /** Plain text for search and previews: one line per block, formatting dropped. */
    fun plainText(document: NoteDocument): String = document.blocks.mapNotNull(::blockText).joinToString("\n")

    private fun blockText(block: NoteBlock): String? = when (block) {
        is NoteBlock.Paragraph -> block.spans.text()
        is NoteBlock.Heading -> block.spans.text()
        is NoteBlock.ListItem -> block.spans.text()
        is NoteBlock.ChecklistItem -> block.spans.text()
        is NoteBlock.Quote -> block.spans.text()
        is NoteBlock.Code -> block.text
        is NoteBlock.Image -> block.alt
        // Search newer content too, as long as it carries spans the usual way.
        is NoteBlock.Unknown -> (block.raw["spans"] as? JsonArray)?.let(::decodeSpans)?.text()
    }

    private fun List<Span>.text() = joinToString("") { it.text }

    private fun encodeBlock(block: NoteBlock): JsonObject = when (block) {
        is NoteBlock.Paragraph -> textBlock("p", block.spans)
        is NoteBlock.Heading -> textBlock("heading", block.spans) { put("level", block.level) }
        is NoteBlock.ListItem -> textBlock(if (block.ordered) "ol" else "ul", block.spans) { indent(block.indent) }
        is NoteBlock.ChecklistItem -> textBlock("check", block.spans) {
            put("checked", block.checked)
            indent(block.indent)
        }
        is NoteBlock.Quote -> textBlock("quote", block.spans)
        is NoteBlock.Code -> buildJsonObject {
            put("type", "code")
            put("text", block.text)
            block.language?.let { put("lang", it) }
        }
        is NoteBlock.Image -> buildJsonObject {
            put("type", "image")
            put("attachment", block.attachmentId)
            block.alt?.let { put("alt", it) }
        }
        is NoteBlock.Unknown -> block.raw
    }

    private fun textBlock(type: String, spans: List<Span>, extra: JsonObjectBuilder.() -> Unit = {}) = buildJsonObject {
        put("type", type)
        extra()
        put("spans", encodeSpans(spans))
    }

    private fun JsonObjectBuilder.indent(indent: Int) {
        if (indent > 0) put("indent", indent)
    }

    private fun decodeBlock(obj: JsonObject): NoteBlock? {
        val type = obj.string("type") ?: return null
        return decodeTextBlock(type, obj) ?: when (type) {
            "code" -> NoteBlock.Code(obj.string("text").orEmpty(), obj.string("lang"))
            "image" -> obj.string("attachment")?.let { NoteBlock.Image(it, obj.string("alt")) }
            else -> NoteBlock.Unknown(obj)
        }
    }

    /** Blocks made of spans, or null when [type] isn't one of them. */
    private fun decodeTextBlock(type: String, obj: JsonObject): NoteBlock? {
        if (type !in TEXT_BLOCK_TYPES) return null
        val spans = (obj["spans"] as? JsonArray)?.let(::decodeSpans).orEmpty()
        val indent = obj.int("indent")?.coerceIn(0, MAX_INDENT) ?: 0
        return when (type) {
            "heading" -> NoteBlock.Heading((obj.int("level") ?: 1).coerceIn(1, NoteBlock.MAX_HEADING_LEVEL), spans)
            "ul" -> NoteBlock.ListItem(ordered = false, indent = indent, spans = spans)
            "ol" -> NoteBlock.ListItem(ordered = true, indent = indent, spans = spans)
            "check" -> NoteBlock.ChecklistItem(obj.bool("checked") ?: false, indent, spans)
            "quote" -> NoteBlock.Quote(spans)
            else -> NoteBlock.Paragraph(spans)
        }
    }

    private fun encodeSpans(spans: List<Span>): JsonArray = buildJsonArray {
        // Merge neighbours with the same marks and drop empty runs, so equal content encodes equally.
        normalize(spans).forEach { span ->
            add(
                buildJsonObject {
                    put("text", span.text)
                    if (span.marks.isNotEmpty()) {
                        put("marks", JsonArray(span.marks.sortedBy(::markOrder).map(::encodeMark)))
                    }
                },
            )
        }
    }

    private fun decodeSpans(array: JsonArray): List<Span> = normalize(
        array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val text = obj.string("text") ?: return@mapNotNull null
            val marks = (obj["marks"] as? JsonArray)?.mapNotNull(::decodeMark)?.toSet().orEmpty()
            Span(text, marks)
        },
    )

    internal fun normalize(spans: List<Span>): List<Span> {
        val out = ArrayList<Span>(spans.size)
        for (span in spans) {
            if (span.text.isEmpty()) continue
            val last = out.lastOrNull()
            if (last != null && last.marks == span.marks) {
                out[out.lastIndex] = last.copy(text = last.text + span.text)
            } else {
                out += span
            }
        }
        return out
    }

    private fun encodeMark(mark: Mark): JsonElement = when (mark) {
        Mark.Bold -> JsonPrimitive("b")
        Mark.Italic -> JsonPrimitive("i")
        Mark.Strikethrough -> JsonPrimitive("s")
        Mark.Code -> JsonPrimitive("code")
        is Mark.Link -> buildJsonObject { put("link", mark.url) }
    }

    private fun decodeMark(element: JsonElement): Mark? = when (element) {
        is JsonPrimitive -> when (element.contentOrNull) {
            "b" -> Mark.Bold
            "i" -> Mark.Italic
            "s" -> Mark.Strikethrough
            "code" -> Mark.Code
            else -> null
        }
        is JsonObject -> element.string("link")?.let { Mark.Link(it) }
        else -> null
    }

    private fun markOrder(mark: Mark): String = when (mark) {
        Mark.Bold -> "1"
        Mark.Italic -> "2"
        Mark.Strikethrough -> "3"
        Mark.Code -> "4"
        is Mark.Link -> "5" + mark.url
    }

    private fun JsonObject.string(key: String) = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    private fun JsonObject.int(key: String) = (this[key] as? JsonPrimitive)?.intOrNull
    private fun JsonObject.bool(key: String) = (this[key] as? JsonPrimitive)?.booleanOrNull

    private const val MAX_INDENT = 6
    private val TEXT_BLOCK_TYPES = setOf("p", "heading", "ul", "ol", "check", "quote")
}
