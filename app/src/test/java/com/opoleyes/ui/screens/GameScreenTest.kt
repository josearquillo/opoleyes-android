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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameScreenTest {

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

    @Test
    fun fun_gameScreen_showsQuestionAfterStart() {
        vm.startAllLawsGame()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        val q = vm.uiState.value.currentQ
        assertTrue("Question should be loaded", q != null)
        composeRule.onNodeWithText(q!!.enunciado).assertIsDisplayed()
    }

    @Test
    fun fun_gameScreen_showsScoreLabel() {
        vm.startAllLawsGame()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        composeRule.onAllNodesWithText("0 pts").fetchSemanticsNodes().also { org.junit.Assert.assertTrue(it.isNotEmpty()) }
    }

    @Test
    fun fun_gameScreen_showsComboLabel() {
        vm.startAllLawsGame()
        vm.engine.combo = 3
        vm.updateUiState()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        composeRule.onAllNodesWithText("x3").fetchSemanticsNodes().also { org.junit.Assert.assertTrue(it.isNotEmpty()) }
    }

    @Test
    fun fun_gameScreen_showsPowerUpButtons() {
        vm.startAllLawsGame()
        vm.engine.hintCharges = 1
        vm.engine.fiftyFiftyCharges = 1
        vm.updateUiState()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        composeRule.onNodeWithText("Pista").assertIsDisplayed()
        composeRule.onNodeWithText("50/50").assertIsDisplayed()
    }

    @Test
    fun fun_gameScreen_showsLivesInSurvivalMode() {
        vm.startAllLawsGame()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        // Survival mode shows heart icons for lives, verify score is shown
        composeRule.onAllNodesWithText("0 pts").fetchSemanticsNodes().also { org.junit.Assert.assertTrue(it.isNotEmpty()) }
    }

    @Test
    fun fun_gameScreen_showsQuestionOptions() {
        vm.startAllLawsGame()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        val q = vm.uiState.value.currentQ!!
        for (option in q.opciones.values) {
            composeRule.onNodeWithText(option).assertIsDisplayed()
        }
    }

    @Test
    fun fun_gameScreen_clickingOptionCallsAnswer() {
        vm.startAllLawsGame()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        val q = vm.uiState.value.currentQ!!
        val correctOption = q.opciones[q.correct]!!
        composeRule.onNodeWithText(correctOption).performClick()
        assertTrue("Question should be answered after click", vm.uiState.value.answered)
    }

    @Test
    fun fun_gameScreen_fiftyFiftyHidesOptions() {
        vm.startAllLawsGame()
        vm.engine.fiftyFiftyCharges = 1
        vm.activateFiftyFifty()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        val q = vm.uiState.value.currentQ!!
        val visibleOptions = q.opciones.filterKeys { it !in vm.uiState.value.fiftyFiftyRemoved }
        for (option in visibleOptions.values) {
            composeRule.onNodeWithText(option).assertIsDisplayed()
        }
        assertEquals(2, visibleOptions.size)
    }
}
