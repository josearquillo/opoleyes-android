package com.opoleyes.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.model.GameMode
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.TestStrings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ModeIntroScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setupModeIntro(mode: GameMode) {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        DataProvider.loadData(ctx)
        val vm = GameViewModel(ctx)
        vm.pendingMode = mode
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            ModeIntroScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun modeIntro_displaysPlayButton() {
        setupModeIntro(GameMode.QUICK)
        composeRule.onNodeWithText(TestStrings.introPlay).assertIsDisplayed()
    }

    @Test
    fun modeIntro_displaysBackButton() {
        setupModeIntro(GameMode.QUICK)
        composeRule.onNodeWithContentDescription(TestStrings.back).assertIsDisplayed()
    }

    @Test
    fun modeIntro_displaysDontShowAgainCheckbox() {
        setupModeIntro(GameMode.QUICK)
        composeRule.onNodeWithText(TestStrings.introDontShowAgain).assertIsDisplayed()
    }

    @Test
    fun modeIntro_timetrial_displaysPlayButton() {
        setupModeIntro(GameMode.TIMETRIAL)
        composeRule.onNodeWithText(TestStrings.introPlay).assertIsDisplayed()
    }

    @Test
    fun modeIntro_survival_displaysPlayButton() {
        setupModeIntro(GameMode.SURVIVAL)
        composeRule.onNodeWithText(TestStrings.introPlay).assertIsDisplayed()
    }

    @Test
    fun modeIntro_clickDontShowAgain_noCrash() {
        setupModeIntro(GameMode.QUICK)
        composeRule.onNodeWithText(TestStrings.introDontShowAgain).performClick()
        composeRule.waitForIdle()
        // Should still be on the intro screen
        composeRule.onNodeWithText(TestStrings.introPlay).assertIsDisplayed()
    }
}
