package com.thesasan1.app.core.designsystem

import com.thesasan1.app.core.designsystem.theme.InterFontFamily
import com.thesasan1.app.core.designsystem.theme.VazirmatnFontFamily
import com.thesasan1.app.core.designsystem.theme.appTypography
import com.thesasan1.app.core.designsystem.theme.isPersianScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypographyTest {

    @Test
    fun `latin scale matches the design system table`() {
        val type = appTypography(persian = false)
        assertEquals(36f, type.display.fontSize.value)
        assertEquals(44f, type.display.lineHeight.value)
        assertEquals(15f, type.bodyMedium.fontSize.value)
        assertEquals(22f, type.bodyMedium.lineHeight.value)
        assertEquals(InterFontFamily, type.bodyMedium.fontFamily)
    }

    @Test
    fun `persian scale uses vazirmatn and adds 2sp line height`() {
        val latin = appTypography(persian = false)
        val persian = appTypography(persian = true)
        assertEquals(VazirmatnFontFamily, persian.bodyMedium.fontFamily)
        assertEquals(latin.bodyMedium.fontSize, persian.bodyMedium.fontSize)
        assertEquals(latin.bodyMedium.lineHeight.value + 2f, persian.bodyMedium.lineHeight.value)
        assertEquals(latin.display.lineHeight.value + 2f, persian.display.lineHeight.value)
    }

    @Test
    fun `persian script detection uses the language subtag`() {
        assertTrue(isPersianScript("fa"))
        assertTrue(isPersianScript("fa-IR"))
        assertTrue(isPersianScript("ar-EG"))
        assertFalse(isPersianScript("en-US"))
        assertFalse(isPersianScript(""))
    }
}
