package com.thesasan1.app.core.database

/**
 * Normalises text for the full-text index and for queries, so Persian and English search behave
 * the same way whatever keyboard produced the text.
 */
object SearchText {
    private const val ZWNJ = '\u200C'
    private const val TATWEEL = '\u0640'
    private val diacritics = '\u064B'..'\u065F'
    private const val SUPERSCRIPT_ALEF = '\u0670'

    /**
     * Lower-cases, maps Arabic yeh/kaf to their Persian forms, maps Persian and Arabic-Indic digits
     * to ASCII, drops tatweel and harakat, and turns the zero-width non-joiner into a space (the
     * tokenizer would split there anyway).
     */
    fun normalize(text: String): String {
        val out = StringBuilder(text.length)
        for (c in text) {
            when {
                c == ZWNJ -> out.append(' ')
                c == TATWEEL || c == SUPERSCRIPT_ALEF || c in diacritics -> Unit
                c == '\u064A' || c == '\u0649' -> out.append('\u06CC') // ي ى → ی
                c == '\u0643' -> out.append('\u06A9') // ك → ک
                c in '\u06F0'..'\u06F9' -> out.append('0' + (c - '\u06F0'))
                c in '\u0660'..'\u0669' -> out.append('0' + (c - '\u0660'))
                else -> out.append(c.lowercaseChar())
            }
        }
        return out.toString()
    }

    /**
     * Turns free text into a safe FTS4 MATCH expression: every word becomes a quoted prefix term (`"word*"`),
     * and all terms must match. Returns null when nothing searchable is left, so callers can skip
     * the query instead of sending an empty MATCH, which is an error.
     */
    fun matchQuery(input: String): String? {
        val terms = normalize(input)
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotEmpty() }
        if (terms.isEmpty()) return null
        // FTS4 prefix syntax: the star goes inside the quotes ("milk*"); "milk"* is FTS5 only.
        return terms.joinToString(" ") { "\"$it*\"" }
    }
}
