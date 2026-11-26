package com.thesasan1.app.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.thesasan1.app.core.designsystem.R
import com.thesasan1.app.core.designsystem.theme.AppTheme

/** The back arrow for detail screens (editors, shelves); it mirrors in RTL. */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            AppIcons.Back,
            contentDescription = stringResource(R.string.core_designsystem_back),
            tint = AppTheme.colors.textPrimary,
        )
    }
}
