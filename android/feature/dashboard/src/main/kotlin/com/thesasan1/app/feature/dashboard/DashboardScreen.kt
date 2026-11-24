package com.thesasan1.app.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thesasan1.app.core.designsystem.AppTheme
import com.thesasan1.app.core.designsystem.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun DashboardRoute(
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    DashboardScreen(uiState = uiState)
}

@Composable
internal fun DashboardScreen(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xxl),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = stringResource(uiState.period.greetingRes()),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = uiState.today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xxxl))
            Text(
                text = stringResource(R.string.feature_dashboard_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.feature_dashboard_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun DayPeriod.greetingRes(): Int = when (this) {
    DayPeriod.Morning -> R.string.feature_dashboard_greeting_morning
    DayPeriod.Afternoon -> R.string.feature_dashboard_greeting_afternoon
    DayPeriod.Evening -> R.string.feature_dashboard_greeting_evening
    DayPeriod.Night -> R.string.feature_dashboard_greeting_night
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    AppTheme {
        DashboardScreen(DashboardUiState(period = DayPeriod.Morning, today = LocalDate.of(2026, 11, 24)))
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
private fun DashboardScreenPersianPreview() {
    AppTheme {
        DashboardScreen(DashboardUiState(period = DayPeriod.Evening, today = LocalDate.of(2026, 11, 24)))
    }
}
