package com.thesasan1.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.core.designsystem.theme.Radii
import com.thesasan1.app.core.designsystem.theme.Spacing

/**
 * Level-1 surface (§5): elevation by tone, not shadow. Light theme adds a 1dp subtle border;
 * dark theme relies on the surface tone alone.
 */
@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = AppTheme.colors
    Card(
        modifier = modifier,
        shape = Radii.md,
        colors = CardDefaults.cardColors(containerColor = colors.surface, contentColor = colors.textPrimary),
        border = if (colors.isDark) null else BorderStroke(1.dp, colors.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), content = content)
    }
}
