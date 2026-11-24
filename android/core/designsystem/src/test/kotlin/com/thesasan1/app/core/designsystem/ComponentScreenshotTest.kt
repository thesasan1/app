package com.thesasan1.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.thesasan1.app.core.designsystem.component.AppButton
import com.thesasan1.app.core.designsystem.component.AppButtonVariant
import com.thesasan1.app.core.designsystem.component.AppCard
import com.thesasan1.app.core.designsystem.component.EmptyState
import com.thesasan1.app.core.designsystem.theme.AppTheme
import com.thesasan1.app.core.designsystem.theme.Spacing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the foundation components in light, dark and Persian RTL. Images are written to
 * `build/outputs/roborazzi` when recording (`recordRoborazziDebug`); CI uploads them.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi")
class ComponentScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun components_light() = capture("components_light", darkTheme = false)

    @Test
    fun components_dark() = capture("components_dark", darkTheme = true)

    @Test
    @Config(qualifiers = "fa-w360dp-h640dp-xhdpi")
    fun components_persian_rtl() = capture(
        "components_persian_rtl",
        darkTheme = false,
        layoutDirection = LayoutDirection.Rtl,
        strings = PersianStrings,
    )

    @Test
    @Config(fontScale = 2.0f)
    fun components_font_scale_200() = capture("components_font_scale_200", darkTheme = false)

    private fun capture(
        name: String,
        darkTheme: Boolean,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        strings: SampleStrings = EnglishStrings,
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                AppTheme(darkTheme = darkTheme) { Gallery(strings) }
            }
        }
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Composable
    private fun Gallery(strings: SampleStrings) {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.canvas)
                .width(360.dp)
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(strings.heading, style = AppTheme.typography.headline, color = AppTheme.colors.textPrimary)
            AppButton(text = strings.primary, onClick = {})
            AppButton(text = strings.secondary, onClick = {}, variant = AppButtonVariant.Secondary)
            AppButton(text = strings.tertiary, onClick = {}, variant = AppButtonVariant.Tertiary)
            AppButton(text = strings.destructive, onClick = {}, variant = AppButtonVariant.Destructive)
            AppButton(text = strings.primary, onClick = {}, loading = true)
            AppButton(text = strings.primary, onClick = {}, enabled = false)
            AppCard {
                Text(strings.cardTitle, style = AppTheme.typography.titleMedium)
                Text(strings.cardBody, style = AppTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
            }
            EmptyState(
                title = strings.emptyTitle,
                body = strings.emptyBody,
                actionLabel = strings.primary,
                onAction = {},
            )
        }
    }

    private data class SampleStrings(
        val heading: String,
        val primary: String,
        val secondary: String,
        val tertiary: String,
        val destructive: String,
        val cardTitle: String,
        val cardBody: String,
        val emptyTitle: String,
        val emptyBody: String,
    )

    private companion object {
        val EnglishStrings = SampleStrings(
            heading = "Good morning",
            primary = "Capture",
            secondary = "Schedule",
            tertiary = "Not now",
            destructive = "Delete",
            cardTitle = "Plan the week",
            cardBody = "Due tomorrow · Work",
            emptyTitle = "Nothing here yet",
            emptyBody = "Capture your first thought.",
        )
        val PersianStrings = SampleStrings(
            heading = "صبح بخیر",
            primary = "ثبت",
            secondary = "زمان‌بندی",
            tertiary = "بعداً",
            destructive = "حذف",
            cardTitle = "برنامه‌ریزی هفته",
            cardBody = "سررسید فردا · کار",
            emptyTitle = "هنوز چیزی اینجا نیست",
            emptyBody = "اولین فکرتان را ثبت کنید.",
        )
    }
}
