package com.thesasan1.app.core.designsystem

import androidx.compose.ui.graphics.Color
import com.thesasan1.app.core.designsystem.theme.AccentColor
import com.thesasan1.app.core.designsystem.theme.AppColors
import com.thesasan1.app.core.designsystem.theme.darkAppColors
import com.thesasan1.app.core.designsystem.theme.lightAppColors
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** WCAG 2.x contrast checks for the token set (docs/DESIGN_SYSTEM.md §2.1, §10). */
class ColorContrastTest {

    @ParameterizedTest
    @EnumSource(AccentColor::class)
    fun `accent keeps 4_5 to 1 against on-accent text in both themes`(accent: AccentColor) {
        assertContrast(lightAppColors(accent).accent, lightAppColors(accent).onAccent, 4.5, "light $accent")
        assertContrast(darkAppColors(accent).accent, darkAppColors(accent).onAccent, 4.5, "dark $accent")
    }

    @ParameterizedTest
    @EnumSource(AccentColor::class)
    fun `accent as text or focus ring keeps 3 to 1 against canvas`(accent: AccentColor) {
        assertContrast(lightAppColors(accent).accent, lightAppColors(accent).canvas, 3.0, "light $accent")
        assertContrast(darkAppColors(accent).accent, darkAppColors(accent).canvas, 3.0, "dark $accent")
    }

    @Test
    fun `body text keeps 4_5 to 1 on canvas and surface`() {
        for (colors in listOf(lightAppColors(), darkAppColors())) {
            for (background in listOf(colors.canvas, colors.surface, colors.surfaceRaised)) {
                assertContrast(colors.textPrimary, background, 4.5, label(colors, "primary"))
                assertContrast(colors.textSecondary, background, 4.5, label(colors, "secondary"))
            }
        }
    }

    @Test
    fun `tertiary text (placeholders, metadata) keeps 3 to 1 on surface`() {
        for (colors in listOf(lightAppColors(), darkAppColors())) {
            assertContrast(colors.textTertiary, colors.surface, 3.0, label(colors, "tertiary"))
        }
    }

    @Test
    fun `destructive button text keeps 4_5 to 1`() {
        assertContrast(lightAppColors().danger, lightAppColors().onAccent, 4.5, "light danger")
        assertContrast(darkAppColors().danger, darkAppColors().onAccent, 4.5, "dark danger")
    }

    private fun label(colors: AppColors, role: String) = "${if (colors.isDark) "dark" else "light"} $role"

    private fun assertContrast(foreground: Color, background: Color, minimum: Double, label: String) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(ratio >= minimum, "$label contrast ${"%.2f".format(ratio)} < $minimum")
    }

    companion object {
        fun contrastRatio(a: Color, b: Color): Double {
            val la = relativeLuminance(a)
            val lb = relativeLuminance(b)
            return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
        }

        private fun relativeLuminance(color: Color): Double {
            fun channel(value: Float): Double {
                val c = value.toDouble()
                return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
        }
    }
}
