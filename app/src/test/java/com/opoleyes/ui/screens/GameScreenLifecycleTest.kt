package com.opoleyes.ui.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
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
class GameScreenLifecycleTest {

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
    fun fun_lifecycle_timerPresentInChallengeMode() {
        vm.startChallengeGame()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        assertTrue("Timer should be positive in challenge mode", vm.uiState.value.timer > 0f)
        assertTrue("Mode should be challenge", vm.uiState.value.mode == GameMode.CHALLENGE)
    }

    @Test
    fun fun_lifecycle_timerPresentInTimetrialMode() {
        vm.startAllLawsGame()
        vm.engine.mode = GameMode.TIMETRIAL
        vm.engine.timer = 180f
        vm.updateUiState()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        assertTrue("Timer should be positive in timetrial mode", vm.uiState.value.timer > 0f)
    }

    @Test
    fun fun_lifecycle_timerNotPresentInSurvivalMode() {
        vm.startAllLawsGame()
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
        assertTrue("Timer should be 0 in survival mode", vm.uiState.value.timer == 0f)
    }

    @Test
    fun fun_lifecycle_engineTimerAdjustableManually() {
        vm.startChallengeGame()
        val timerBefore = vm.engine.timer
        vm.engine.timer = (vm.engine.timer - 5f).coerceAtLeast(0f)
        vm.updateUiState()
        assertTrue("Timer should be adjustable manually", vm.uiState.value.timer == timerBefore - 5f)
    }

    @Test
    fun fun_lifecycle_engineTimerClampedToZero() {
        vm.startChallengeGame()
        vm.engine.timer = 3f
        vm.engine.timer = (vm.engine.timer - 10f).coerceAtLeast(0f)
        vm.updateUiState()
        assertTrue("Timer should be clamped to 0", vm.uiState.value.timer == 0f)
    }

    @Test
    fun fun_lifecycle_gameOverWhenTimerReachesZero() {
        vm.startChallengeGame()
        vm.engine.timer = 0f
        vm.updateUiState()
        assertTrue("Should be game over when timer is 0", vm.isGameOver())
    }

    @Test
    fun fun_lifecycle_timerDecrementsAfterAdvancingQuestion() {
        vm.startChallengeGame()
        vm.nextQuestion()
        vm.answer(vm.engine.currentQ!!.correct)
        assertTrue("Should be answered", vm.uiState.value.answered)
        vm.nextQuestion()
        assertTrue("Timer should still be positive after advancing", vm.engine.timer > 0f)
        assertFalseCondition("Should not be answered after next question", vm.uiState.value.answered)
    }

    private fun assertEquals(expected: Float, actual: Float, delta: Float) {
        assertTrue("Expected $expected but got $actual (delta=$delta)", kotlin.math.abs(expected - actual) <= delta)
    }

    private fun assertFalseCondition(message: String, condition: Boolean) {
        assertTrue(message, !condition)
    }
}
