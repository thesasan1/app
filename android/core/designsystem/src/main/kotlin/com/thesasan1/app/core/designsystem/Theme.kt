package com.thesasan1.app.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

internal val LightColors: ColorScheme = lightColorScheme(
    primary = Palette.AccentIndigoLight,
    onPrimary = Palette.OnAccentLight,
    background = Palette.CanvasLight,
    onBackground = Palette.TextPrimaryLight,
    surface = Palette.SurfaceLight,
    onSurface = Palette.TextPrimaryLight,
    surfaceVariant = Palette.SunkenLight,
    onSurfaceVariant = Palette.TextSecondaryLight,
    outlineVariant = Palette.BorderSubtleLight,
    tertiary = Palette.SuccessLight,
    error = Palette.DangerLight,
)

internal val DarkColors: ColorScheme = darkColorScheme(
    primary = Palette.AccentIndigoDark,
    onPrimary = Palette.OnAccentDark,
    background = Palette.CanvasDark,
    onBackground = Palette.TextPrimaryDark,
    surface = Palette.SurfaceDark,
    onSurface = Palette.TextPrimaryDark,
    surfaceVariant = Palette.SunkenDark,
    onSurfaceVariant = Palette.TextSecondaryDark,
    surfaceContainerHigh = Palette.SurfaceRaisedDark,
    outlineVariant = Palette.BorderSubtleDark,
    tertiary = Palette.SuccessDark,
    error = Palette.DangerDark,
)

/**
 * Application theme. Foundation only: typography (Inter + Vazirmatn), accent customization,
 * motion and component tokens arrive in the design-system foundation PR (Phase 1, PR 4).
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
