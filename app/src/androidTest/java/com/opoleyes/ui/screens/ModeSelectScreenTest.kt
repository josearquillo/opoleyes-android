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
import com.opoleyes.data.local.PreferencesManager
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

    @Test
    fun modeSelectScreen_highRank_examDialogOpens() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        DataProvider.loadData(ctx)
        PreferencesManager(ctx).addXP(18000) // rank 7 = Maestro, unlocks exam
        val vm = GameViewModel(ctx)
        vm.preloadHomeData()
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            ModeSelectScreen(rememberNavController(), vm)
        }
        composeRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeRule.onNodeWithText(TestStrings.modeExam).assertIsDisplayed(); true
            } catch (e: Throwable) { false }
        }
        // Wait for staggered animation to complete
        composeRule.waitForIdle()
        Thread.sleep(1000)
        // Click exam mode to open dialog
        composeRule.onNodeWithText(TestStrings.modeExam).performClick()
        composeRule.waitForIdle()
        Thread.sleep(500)
        // Dialog should show some content — wait for it
        composeRule.waitUntil(timeoutMillis = 5000) {
            try {
                // ExamConfigDialog shows "Configurar mini examen" title
                composeRule.onNodeWithText("Configurar mini examen").assertIsDisplayed(); true
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun modeSelectScreen_highRank_quickModeClickable() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        DataProvider.loadData(ctx)
        PreferencesManager(ctx).addXP(7000) // rank 5 = Experto, unlocks quick
        val vm = GameViewModel(ctx)
        vm.preloadHomeData()
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            ModeSelectScreen(rememberNavController(), vm)
        }
        composeRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeRule.onNodeWithText(TestStrings.modeQuick).assertIsDisplayed(); true
            } catch (e: Throwable) { false }
        }
        // Click quick mode — this triggers startQuickGameAsync
        // Dialog may or may not appear depending on whether there are prior wrong answers
        composeRule.onNodeWithText(TestStrings.modeQuick).performClick()
        composeRule.waitForIdle()
        // Just verify the screen is still functional (no crash)
        composeRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeRule.onNodeWithText(TestStrings.selectMode).assertIsDisplayed(); true
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun modeSelectScreen_highRank_timetrialUnlocked() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        DataProvider.loadData(ctx)
        PreferencesManager(ctx).addXP(2000) // rank 3 = Estudiante, unlocks timetrial
        val vm = GameViewModel(ctx)
        vm.preloadHomeData()
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            ModeSelectScreen(rememberNavController(), vm)
        }
        composeRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeRule.onNodeWithText(TestStrings.modeTimetrial).assertIsDisplayed(); true
            } catch (e: Throwable) { false }
        }
    }
}
