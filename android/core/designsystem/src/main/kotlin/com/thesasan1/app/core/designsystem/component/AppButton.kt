package com.thesasan1.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thesasan1.app.core.designsystem.R
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.core.designsystem.theme.Radii
import com.thesasan1.app.core.designsystem.theme.Spacing

enum class AppButtonVariant { Primary, Secondary, Tertiary, Destructive }

enum class AppButtonSize(internal val minHeight: Dp, internal val horizontalPadding: Dp) {
    Small(minHeight = Spacing.minTouchTarget, horizontalPadding = Spacing.md),
    Medium(minHeight = 52.dp, horizontalPadding = Spacing.lg),
    Large(minHeight = 56.dp, horizontalPadding = Spacing.xxl),
}

/**
 * Product button (docs/DESIGN_SYSTEM.md §8). While [loading] it ignores taps, keeps its width
 * (label hidden, not removed) and announces a loading state to accessibility services.
 * Every size keeps the 48dp minimum touch target.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    size: AppButtonSize = AppButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = AppTheme.colors
    val loadingLabel = stringResource(R.string.core_designsystem_loading)
    val buttonModifier = modifier
        .heightIn(min = size.minHeight)
        .semantics {
            if (loading) {
                stateDescription = loadingLabel
                contentDescription = "$text, $loadingLabel"
            }
        }
    val padding = PaddingValues(horizontal = size.horizontalPadding)
    val clickable = enabled && !loading
    val onClickGuarded = { if (clickable) onClick() }
    val label: @Composable (Color) -> Unit = { contentColor -> ButtonLabel(text, loading, contentColor) }

    when (variant) {
        AppButtonVariant.Primary, AppButtonVariant.Destructive -> {
            val container = if (variant == AppButtonVariant.Destructive) colors.danger else colors.accent
            Button(
                onClick = onClickGuarded,
                modifier = buttonModifier,
                enabled = enabled,
                shape = Radii.sm,
                contentPadding = padding,
                colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = colors.onAccent),
            ) { label(colors.onAccent) }
        }
        AppButtonVariant.Secondary -> OutlinedButton(
            onClick = onClickGuarded,
            modifier = buttonModifier,
            enabled = enabled,
            shape = Radii.sm,
            contentPadding = padding,
            border = BorderStroke(1.dp, colors.borderSubtle),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
        ) { label(colors.textPrimary) }
        AppButtonVariant.Tertiary -> TextButton(
            onClick = onClickGuarded,
            modifier = buttonModifier,
            enabled = enabled,
            shape = Radii.sm,
            contentPadding = padding,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.accent),
        ) { label(colors.accent) }
    }
}

@Composable
private fun ButtonLabel(text: String, loading: Boolean, contentColor: Color) {
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = AppTheme.typography.labelMedium,
            modifier = Modifier.alpha(if (loading) 0f else 1f),
        )
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = contentColor,
                strokeWidth = 2.dp,
            )
        }
    }
}
