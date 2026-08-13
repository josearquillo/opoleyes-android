package com.opoleyes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun themeColors_areAccessible() {
        // Reference all color vals to trigger their initialization
        val colors = listOf(
            BgDark, BgCard, Primary, PrimaryLight, Accent, AccentLight,
            Success, SuccessDark, SuccessLight, Danger, DangerDark,
            Warning, WarningDark, TextLight, TextMuted, TextDim,
            TextOption, SurfaceVariant, Orange, OrangeDark,
            HintRemoved, HintRemovedDark, PurpleDark, Silver,
            TrackColor, TrackColorDim, GlassSurface, Scrim, ScrimStrong, ScrimHeavy,
            ConfettiBlue, ConfettiAmber, ConfettiPurple, ConfettiCyan,
            ConfettiPink, ConfettiLime
        )
        assert(colors.isNotEmpty())
    }

    @Test
    fun typography_hasAllStyles() {
        assert(OPOLEYESTypography.displayLarge.fontSize.value > 0f)
        assert(OPOLEYESTypography.headlineLarge.fontSize.value > 0f)
        assert(OPOLEYESTypography.headlineMedium.fontSize.value > 0f)
        assert(OPOLEYESTypography.headlineSmall.fontSize.value > 0f)
        assert(OPOLEYESTypography.titleLarge.fontSize.value > 0f)
        assert(OPOLEYESTypography.titleMedium.fontSize.value > 0f)
        assert(OPOLEYESTypography.titleSmall.fontSize.value > 0f)
        assert(OPOLEYESTypography.bodyLarge.fontSize.value > 0f)
        assert(OPOLEYESTypography.bodyMedium.fontSize.value > 0f)
        assert(OPOLEYESTypography.bodySmall.fontSize.value > 0f)
        assert(OPOLEYESTypography.labelLarge.fontSize.value > 0f)
        assert(OPOLEYESTypography.labelMedium.fontSize.value > 0f)
        assert(OPOLEYESTypography.labelSmall.fontSize.value > 0f)
    }

    @Test
    fun shapes_hasAllSizes() {
        assert(OPOLEYESShapes.small != null)
        assert(OPOLEYESShapes.medium != null)
        assert(OPOLEYESShapes.large != null)
        assert(OPOLEYESShapes.extraLarge != null)
    }

    @Test
    fun opoleyesTheme_rendersContent() {
        composeRule.setContent {
            OpoleyesTheme {
                Text("Theme Test", color = MaterialTheme.colorScheme.onBackground)
            }
        }
        composeRule.onNodeWithText("Theme Test").assertIsDisplayed()
    }
}
