package com.thesasan1.app.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale (§4, base unit 4dp). */
object Spacing {
    val none = 0.dp
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp
    val giant = 48.dp
    val colossal = 64.dp

    /** Horizontal screen margin on phones; 24dp from 600dp width (applied by layouts). */
    val screenHorizontal = 20.dp
    val screenHorizontalWide = 24.dp
    val contentMaxWidth = 720.dp

    /** Minimum touch target (§4, accessibility). */
    val minTouchTarget: Dp = 48.dp
}

/** Radius tokens (§4). */
object Radii {
    val xs: Shape = RoundedCornerShape(6.dp)
    val sm: Shape = RoundedCornerShape(10.dp)
    val md: Shape = RoundedCornerShape(14.dp)
    val lg: Shape = RoundedCornerShape(20.dp)
    val xl: Shape = RoundedCornerShape(28.dp)
    val full: Shape = CircleShape
}

/** Motion tokens (§7). Durations in milliseconds. */
object Motion {
    const val INSTANT_MS = 90
    const val QUICK_MS = 160
    const val BASE_MS = 240
    const val SLOW_MS = 360

    /** Reduced-motion replacement: fades no longer than this (§7). */
    const val REDUCED_FADE_MS = 120

    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    const val SPRING_GENTLE_STIFFNESS = 400f
    const val SPRING_GENTLE_DAMPING = 0.8f
}
