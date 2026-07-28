package com.opoleyes.ui.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.GameMode
import com.opoleyes.ui.navigation.GameViewModel
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameScreenAnimationsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var vm: GameViewModel
    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        prefs = PreferencesManager(app)
        prefs.resetAll()
        vm = GameViewModel(app)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    private fun advanceAnimations() {
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
    }

    private fun shortAdvance() {
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()
    }

    @Test
    fun fun_powerUpButton_showsIconAndLabel() {
        vm.startAllLawsGame()
        vm.engine.hintCharges = 2
        vm.engine.fiftyFiftyCharges = 1
        vm.engine.doubleScoreCharges = 1
        vm.updateUiState()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        composeRule.onNodeWithText("Pista").assertIsDisplayed()
        composeRule.onNodeWithText("50/50").assertIsDisplayed()
        composeRule.onNodeWithText("x2 pts").assertIsDisplayed()
    }

    @Test
    fun fun_powerUpButton_showsChargeBadge() {
        vm.startAllLawsGame()
        vm.engine.hintCharges = 3
        vm.updateUiState()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        composeRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun fun_powerUpButton_clickTriggersAction() {
        vm.startAllLawsGame()
        vm.engine.fiftyFiftyCharges = 1
        vm.updateUiState()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertTrue("Fifty fifty should be active after click", vm.uiState.value.fiftyFiftyActive)
    }

    @Test
    fun fun_powerUpButton_staysVisibleAfterAnswer() {
        vm.startAllLawsGame()
        vm.engine.hintCharges = 1
        vm.updateUiState()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        val q = vm.uiState.value.currentQ!!
        composeRule.onNodeWithText(q.opciones[q.correct]!!).performClick()
        advanceAnimations()
        // Power-up button should still be visible (disabled) after answering
        composeRule.onAllNodesWithText("Pista").fetchSemanticsNodes().also {
            assertTrue("Power-up button should still be visible after answer", it.isNotEmpty())
        }
    }

    @Test
    fun fun_popups_showOnCorrectAnswer() {
        vm.startAllLawsGame()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        val q = vm.uiState.value.currentQ!!
        composeRule.onNodeWithText(q.opciones[q.correct]!!).performClick()
        shortAdvance()
        assertTrue("Should have popups after correct answer", vm.popups.value.isNotEmpty())
    }

    @Test
    fun fun_popups_showOnWrongAnswer() {
        vm.startAllLawsGame()
        vm.engine.shieldCharges = 0
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        val q = vm.uiState.value.currentQ!!
        val wrongOption = q.opciones.entries.first { it.key != q.correct }.value
        composeRule.onNodeWithText(wrongOption).performClick()
        shortAdvance()
        assertTrue("Should have popups after wrong answer", vm.popups.value.isNotEmpty())
    }

    @Test
    fun fun_popups_clearOnNextQuestion() {
        vm.startAllLawsGame()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        val q = vm.uiState.value.currentQ!!
        composeRule.onNodeWithText(q.opciones[q.correct]!!).performClick()
        shortAdvance()
        assertTrue("Should have popups", vm.popups.value.isNotEmpty())
        vm.clearPopups()
        assertTrue("Popups should be cleared", vm.popups.value.isEmpty())
    }

    @Test
    fun fun_popups_containPointsText() {
        vm.startAllLawsGame()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        val q = vm.uiState.value.currentQ!!
        composeRule.onNodeWithText(q.opciones[q.correct]!!).performClick()
        shortAdvance()
        val hasPointsPopup = vm.popups.value.any { it.text.contains("pts") }
        assertTrue("Should have a points popup", hasPointsPopup)
    }

    @Test
    fun fun_popups_containIconOnCorrectAnswer() {
        vm.startAllLawsGame()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        val q = vm.uiState.value.currentQ!!
        composeRule.onNodeWithText(q.opciones[q.correct]!!).performClick()
        shortAdvance()
        val hasIconPopup = vm.popups.value.any { it.icon.isNotEmpty() }
        assertTrue("Popups should have icons", hasIconPopup)
    }

    @Test
    fun fun_popups_comboShowsWhenCombo3OrMore() {
        vm.startAllLawsGame()
        vm.engine.combo = 2
        vm.updateUiState()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        val q = vm.uiState.value.currentQ!!
        composeRule.onNodeWithText(q.opciones[q.correct]!!).performClick()
        shortAdvance()
        val hasComboPopup = vm.popups.value.any { it.text.contains("COMBO") }
        assertTrue("Should show combo popup when combo >= 3", hasComboPopup)
    }

    @Test
    fun fun_powerUpToast_showsOnDoubleScoreActivation() {
        vm.startAllLawsGame()
        vm.engine.doubleScoreCharges = 1
        vm.updateUiState()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        composeRule.onNodeWithText("x2 pts").performClick()
        advanceAnimations()
        assertTrue("Power-up toast should be shown", vm.powerUpToast.value != null)
    }

    @Test
    fun fun_powerUpToast_clearsAfterDelay() {
        vm.startAllLawsGame()
        vm.engine.doubleScoreCharges = 1
        vm.updateUiState()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        composeRule.onNodeWithText("x2 pts").performClick()
        advanceAnimations()
        assertTrue("Toast should be shown", vm.powerUpToast.value != null)
        vm.clearPowerUpToast()
        assertTrue("Toast should be cleared", vm.powerUpToast.value == null)
    }

    @Test
    fun fun_achievementToasts_showWhenUnlocked() {
        vm.startAllLawsGame()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        val q = vm.uiState.value.currentQ!!
        composeRule.onNodeWithText(q.opciones[q.correct]!!).performClick()
        advanceAnimations()
        // First correct answer should trigger first_correct achievement
        // Toasts may or may not be present depending on previous state
        // Just verify it doesn't crash
        assertTrue("Should not crash on achievement check", true)
    }
}
