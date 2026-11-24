package com.thesasan1.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens (docs/DESIGN_SYSTEM.md §2.1). Feature code reads these through
 * [AppTheme.colors] and never uses raw hex values.
 */
@Immutable
data class AppColors(
    val canvas: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val sunken: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val borderSubtle: Color,
    val accent: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val isDark: Boolean,
)

/**
 * User-selectable accent (§2.1). Each pair keeps at least 4.5:1 contrast against its
 * on-accent text color; `ColorContrastTest` enforces this.
 */
enum class AccentColor(val light: Color, val dark: Color) {
    Indigo(Color(0xFF4F5BD5), Color(0xFF8B93F0)),
    Ocean(Color(0xFF1F6FB2), Color(0xFF6CB4EE)),
    Emerald(Color(0xFF1E7F5C), Color(0xFF5CCB9B)),
    Amber(Color(0xFF9A5B00), Color(0xFFF0B54A)),
    Rose(Color(0xFFC0315A), Color(0xFFF28BA8)),
    Graphite(Color(0xFF3D434B), Color(0xFFB8BEC6)),
}

fun lightAppColors(accent: AccentColor = AccentColor.Indigo): AppColors = AppColors(
    canvas = Color(0xFFF7F7F5),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFFFFFFF),
    sunken = Color(0xFFEFEFEC),
    textPrimary = Color(0xFF16181B),
    textSecondary = Color(0xFF5C6168),
    textTertiary = Color(0xFF8A8F96),
    borderSubtle = Color(0xFFE4E4E0),
    accent = accent.light,
    onAccent = Color(0xFFFFFFFF),
    success = Color(0xFF2F9E6B),
    warning = Color(0xFFC98A12),
    danger = Color(0xFFD2453C),
    info = Color(0xFF2F7FD1),
    isDark = false,
)

fun darkAppColors(accent: AccentColor = AccentColor.Indigo): AppColors = AppColors(
    canvas = Color(0xFF0E0F11),
    surface = Color(0xFF16181B),
    surfaceRaised = Color(0xFF1E2024),
    sunken = Color(0xFF0A0B0C),
    textPrimary = Color(0xFFF2F2F0),
    textSecondary = Color(0xFFA3A8AF),
    textTertiary = Color(0xFF6F747B),
    borderSubtle = Color(0xFF26292E),
    accent = accent.dark,
    onAccent = Color(0xFF0E0F11),
    success = Color(0xFF4CC38A),
    warning = Color(0xFFE8B04A),
    danger = Color(0xFFF06A60),
    info = Color(0xFF5FA3EC),
    isDark = true,
)
