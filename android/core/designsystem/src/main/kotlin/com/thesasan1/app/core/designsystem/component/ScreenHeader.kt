package com.thesasan1.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.core.designsystem.theme.Spacing

/**
 * A top-level screen's header: a large title at the start and its quiet actions (shelves, filters)
 * at the end. Tab screens have no back button; the bottom bar takes the user between them.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.padding(top = Spacing.xxl, bottom = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTheme.typography.headline,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            subtitle?.let {
                Text(text = it, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
            }
        }
        Spacer(Modifier.padding(start = Spacing.sm))
        actions()
    }
}
