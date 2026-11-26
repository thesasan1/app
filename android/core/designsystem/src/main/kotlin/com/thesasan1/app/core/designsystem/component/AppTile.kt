package com.thesasan1.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.core.designsystem.theme.Radii
import com.thesasan1.app.core.designsystem.theme.Spacing

private const val BADGE_ALPHA = 0.14f

/** A tappable card row: a tinted icon badge, a title with an optional line under it, and a chevron. */
@Composable
fun AppTile(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = Radii.lg,
        color = colors.surface,
        contentColor = colors.textPrimary,
        border = if (colors.isDark) null else BorderStroke(1.dp, colors.borderSubtle),
    ) {
        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = Radii.md, color = colors.accent.copy(alpha = BADGE_ALPHA)) {
                Box(Modifier.padding(Spacing.sm)) {
                    Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.weight(1f)) {
                Text(title, style = AppTheme.typography.titleMedium, color = colors.textPrimary)
                subtitle?.let { Text(it, style = AppTheme.typography.bodyMedium, color = colors.textSecondary) }
            }
            Icon(AppIcons.ChevronForward, contentDescription = null, tint = colors.textTertiary)
        }
    }
}
