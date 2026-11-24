package com.thesasan1.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration

internal val LocalAppColors = staticCompositionLocalOf { lightAppColors() }
internal val LocalAppTypography = staticCompositionLocalOf { appTypography(persian = false) }

/** Languages whose UI uses the Persian/Arabic type family. */
private val PersianScriptLanguages = setOf("fa", "ar", "ur")

fun isPersianScript(languageTag: String): Boolean =
    languageTag.substringBefore('-').lowercase() in PersianScriptLanguages

/**
 * Application theme: semantic colors (with user accent), locale-aware typography, and the
 * equivalent Material 3 scheme so stock components match.
 *
 * Layout direction comes from the locale (RTL for Persian) via the platform; components use
 * start/end only.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AccentColor = AccentColor.Indigo,
    content: @Composable () -> Unit,
) {
    val language = LocalConfiguration.current.locales[0]?.toLanguageTag().orEmpty()
    val persian = isPersianScript(language)
    val colors = remember(darkTheme, accent) { if (darkTheme) darkAppColors(accent) else lightAppColors(accent) }
    val typography = remember(persian) { appTypography(persian) }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = typography.toMaterial(),
            content = content,
        )
    }
}

/** Accessors for the current theme's tokens. */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current

    val typography: AppTypography
        @Composable @ReadOnlyComposable
        get() = LocalAppTypography.current
}

internal fun AppColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        secondary = accent,
        onSecondary = onAccent,
        tertiary = success,
        background = canvas,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = sunken,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = canvas,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceRaised,
        outline = textTertiary,
        outlineVariant = borderSubtle,
        error = danger,
        onError = onAccent,
    )
}
