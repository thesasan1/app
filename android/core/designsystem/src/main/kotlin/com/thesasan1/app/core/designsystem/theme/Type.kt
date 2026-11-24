package com.thesasan1.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.thesasan1.app.core.designsystem.R

private val UiWeights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

private fun variableFamily(resId: Int): FontFamily = FontFamily(
    UiWeights.map { weight ->
        Font(
            resId = resId,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

/** Inter (OFL) for Latin script. */
val InterFontFamily: FontFamily = variableFamily(R.font.inter_variable)

/** Vazirmatn (OFL) for Persian; it also contains Latin glyphs for mixed text. */
val VazirmatnFontFamily: FontFamily = variableFamily(R.font.vazirmatn_variable)

/** Type scale from docs/DESIGN_SYSTEM.md §3. */
@Immutable
data class AppTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
)

/** Persian text gets +2sp line height so diacritics are not clipped (§3). */
internal const val PERSIAN_EXTRA_LINE_HEIGHT_SP = 2

fun appTypography(persian: Boolean): AppTypography {
    val family = if (persian) VazirmatnFontFamily else InterFontFamily
    val extra = if (persian) PERSIAN_EXTRA_LINE_HEIGHT_SP else 0
    fun style(size: Int, lineHeight: Int, weight: FontWeight): TextStyle = TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        lineHeight = (lineHeight + extra).sp,
        fontWeight = weight,
        letterSpacing = TextUnit.Unspecified,
    )
    return AppTypography(
        display = style(36, 44, FontWeight.SemiBold),
        headline = style(28, 36, FontWeight.SemiBold),
        titleLarge = style(22, 28, FontWeight.SemiBold),
        titleMedium = style(18, 24, FontWeight.SemiBold),
        bodyLarge = style(17, 26, FontWeight.Normal),
        bodyMedium = style(15, 22, FontWeight.Normal),
        bodySmall = style(13, 18, FontWeight.Normal),
        labelMedium = style(14, 20, FontWeight.Medium),
        labelSmall = style(12, 16, FontWeight.Medium),
    )
}

/** Maps the product scale onto Material roles so Material components pick up the same fonts. */
internal fun AppTypography.toMaterial(): Typography = Typography(
    displaySmall = display,
    headlineMedium = headline,
    headlineSmall = headline,
    titleLarge = titleLarge,
    titleMedium = titleMedium,
    titleSmall = labelMedium,
    bodyLarge = bodyLarge,
    bodyMedium = bodyMedium,
    bodySmall = bodySmall,
    labelLarge = labelMedium,
    labelMedium = labelMedium,
    labelSmall = labelSmall,
)
