package com.thesasan1.app.core.database

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SearchTextTest {

    @Test
    fun `normalises Arabic letters, digits, diacritics and ZWNJ`() {
        assertEquals("کتاب های 1403", SearchText.normalize("كتاب\u200Cهاي ١٤٠٣"))
        assertEquals("مدرسه", SearchText.normalize("مَدرســه"))
        assertEquals("hello world", SearchText.normalize("Hello WORLD"))
    }

    @Test
    fun `builds a quoted prefix term per word`() {
        assertEquals("\"milk\"* \"eggs\"*", SearchText.matchQuery("  Milk, eggs! "))
        assertEquals("\"کتاب\"* \"ها\"*", SearchText.matchQuery("کتاب\u200Cها"))
    }

    @Test
    fun `strips FTS syntax so user input can't change the query`() {
        assertEquals("\"a\"* \"or\"* \"b\"*", SearchText.matchQuery("a\" OR b*"))
        assertEquals("\"near\"* \"x\"*", SearchText.matchQuery("NEAR(x)"))
    }

    @Test
    fun `returns null when nothing searchable is left`() {
        assertNull(SearchText.matchQuery(""))
        assertNull(SearchText.matchQuery("  -*\"  "))
    }
}
