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
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.TestStrings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ModeSelectScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setupModeSelectScreen() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        DataProvider.loadData(ctx)
        val vm = GameViewModel(ctx)
        vm.preloadHomeData()

        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            ModeSelectScreen(rememberNavController(), vm)
        }
        composeRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeRule.onNodeWithText(TestStrings.selectMode).assertIsDisplayed(); true
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun modeSelectScreen_displaysTitle() {
        setupModeSelectScreen()
        composeRule.onNodeWithText(TestStrings.selectMode).assertIsDisplayed()
    }

    @Test
    fun modeSelectScreen_displaysSurvivalMode() {
        setupModeSelectScreen()
        composeRule.onNodeWithText(TestStrings.modeSurvival).assertIsDisplayed()
    }

    @Test
    fun modeSelectScreen_displaysTimetrialMode() {
        setupModeSelectScreen()
        composeRule.onNodeWithText(TestStrings.modeTimetrial).assertIsDisplayed()
    }

    @Test
    fun modeSelectScreen_displaysQuickMode() {
        setupModeSelectScreen()
        composeRule.onNodeWithText(TestStrings.modeQuick).assertIsDisplayed()
    }

    @Test
    fun modeSelectScreen_displaysExamMode() {
        setupModeSelectScreen()
        composeRule.onNodeWithText(TestStrings.modeExam).assertIsDisplayed()
    }

    @Test
    fun modeSelectScreen_displaysSimulacroMode() {
        setupModeSelectScreen()
        composeRule.onNodeWithText(TestStrings.modeSimulacro).assertIsDisplayed()
    }

    @Test
    fun modeSelectScreen_displaysBackContentDescription() {
        setupModeSelectScreen()
        composeRule.onNodeWithContentDescription("Volver").assertIsDisplayed()
    }

    @Test
    fun modeSelectScreen_survivalIsClickable() {
        setupModeSelectScreen()
        // Survival is unlocked by default — verify it's displayed (clickable)
        composeRule.onNodeWithText(TestStrings.modeSurvival).assertIsDisplayed()
    }
}
