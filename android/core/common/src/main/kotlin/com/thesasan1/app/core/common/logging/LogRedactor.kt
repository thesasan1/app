package com.thesasan1.app.core.common.logging

/**
 * Masks values that look like personal data or secrets in a log line. Patterns run in order, so
 * tokens and emails are masked before the generic number rule can cut them up.
 */
object LogRedactor {

    private val rules: List<Pair<Regex, String>> = listOf(
        // Authorization headers and bearer tokens.
        Regex("(?i)\\b(bearer|basic)\\s+[A-Za-z0-9._~+/=-]+") to "$1 [token]",
        // JSON Web Tokens (three base64url segments).
        Regex("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+") to "[token]",
        // key=value or "key": "value" for well-known secret names.
        Regex(
            "(?i)(\"?(?:password|passwd|secret|token|api[_-]?key|refresh[_-]?token)\"?\\s*[:=]\\s*)\"?[^\"\\s,&}]+\"?",
        ) to
            "$1[redacted]",
        Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}") to "[email]",
        // Long opaque strings (keys, hashes).
        Regex("\\b[A-Za-z0-9_-]{32,}\\b") to "[secret]",
        // Phone and card-like numbers: 7+ digits (Latin, Persian or Arabic-Indic), allowing separators.
        Regex("\\+?[0-9۰-۹٠-٩](?:[ ().-]{0,2}[0-9۰-۹٠-٩]){6,}") to "[number]",
    )

    /** UUIDs are ids, which is exactly what logs should contain, so they are shielded from the rules. */
    private val uuid = Regex("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b")
    private val placeholder = Regex("\u0000(\\d+)\u0000")

    fun redact(input: String): String {
        val ids = mutableListOf<String>()
        val shielded = uuid.replace(input) { match ->
            ids += match.value
            "\u0000${ids.lastIndex}\u0000"
        }
        val redacted = rules.fold(shielded) { text, (pattern, replacement) -> pattern.replace(text, replacement) }
        return placeholder.replace(redacted) { ids[it.groupValues[1].toInt()] }
    }
}
