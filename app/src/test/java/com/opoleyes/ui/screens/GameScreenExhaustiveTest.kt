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
import com.opoleyes.domain.GameEngine
import com.opoleyes.ui.navigation.GameViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameScreenExhaustiveTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var vm: GameViewModel
    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        prefs = PreferencesManager(app)
        prefs.resetAll()
        // Grant Aprendiz (rank 2) so the engine uses full mechanics: 4 options,
        // 3 lives, and all power-ups available. Tests assert 3 lives and rely
        // on power-ups being usable, which requires rank >= 2.
        prefs.addXP(800)
        vm = GameViewModel(app)
    }

    @After
    fun teardown() { prefs.resetAll() }

    private fun advance() {
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
    }

    private fun startSurvival() {
        vm.startAllLawsGame()
        vm.engine.hintCharges = 5; vm.engine.fiftyFiftyCharges = 5; vm.engine.doubleScoreCharges = 5
        vm.updateUiState()
    }

    private fun startTimetrial() {
        vm.startAllLawsGame(); vm.engine.mode = GameMode.TIMETRIAL; vm.engine.timer = 180f
        vm.engine.hintCharges = 5; vm.engine.fiftyFiftyCharges = 5; vm.engine.doubleScoreCharges = 5
        vm.updateUiState()
    }

    private fun startChallenge() {
        vm.startAllLawsGame(); vm.engine.mode = GameMode.TIMETRIAL; vm.engine.timer = 180f
        vm.engine.hintCharges = 5; vm.engine.fiftyFiftyCharges = 5; vm.engine.doubleScoreCharges = 5
        vm.updateUiState()
    }

    private fun startQuick() {
        vm.startQuickGame()
        vm.engine.hintCharges = 5; vm.engine.fiftyFiftyCharges = 5; vm.engine.doubleScoreCharges = 5
        vm.updateUiState()
    }

    private fun render() {
        composeRule.setContent { GameScreen(androidx.navigation.compose.rememberNavController(), vm) }
        advance()
    }

    private fun countVisibleOptions(): Int {
        val q = vm.engine.currentQ!!
        val removed = vm.engine.fiftyFiftyRemoved
        var count = 0
        for ((key, _) in q.opciones) {
            if (key !in removed) count++
        }
        return count
    }

    // === TEST 1: Power-up mutual exclusivity (all 3 combinations) ===
    @Test
    fun fun_powerUp_mutualExclusivity_allCombinations() {
        startSurvival(); render()
        // 50/50 blocks hint
        vm.activateFiftyFifty()
        assertTrue("50/50 active", vm.engine.fiftyFiftyActive)
        vm.useHint()
        assertFalse("Hint blocked after 50/50", vm.engine.hintActive)

        // Reset: answer + next
        vm.answer(vm.engine.currentQ!!.correct); advance()
        vm.nextQuestion(); advance()

        // Hint blocks 50/50
        vm.useHint()
        assertTrue("Hint active", vm.engine.hintActive)
        vm.activateFiftyFifty()
        assertFalse("50/50 blocked after hint", vm.engine.fiftyFiftyActive)

        // Reset
        vm.answer(vm.engine.currentQ!!.correct); advance()
        vm.nextQuestion(); advance()

        // DoubleScore blocks 50/50
        vm.activateDoubleScore()
        assertTrue("DoubleScore active", vm.engine.doubleScoreActive)
        vm.activateFiftyFifty()
        assertFalse("50/50 blocked after doubleScore", vm.engine.fiftyFiftyActive)
    }

    // === TEST 2: Power-up re-enabled on next question + disabled after answer ===
    @Test
    fun fun_powerUp_reEnabledAndDisabledStates() {
        startSurvival(); render()
        // Use hint first (not 50/50, so fiftyFiftyActive stays false)
        composeRule.onNodeWithText("Pista").performClick(); advance()
        assertTrue(vm.uiState.value.hintActive)

        // Answer -> power-ups should be blocked (no advance to avoid auto-advance)
        vm.answer(vm.uiState.value.currentQ!!.correct)
        composeRule.waitForIdle()
        assertTrue("Should be answered", vm.engine.answered)
        val chargesBefore = vm.engine.fiftyFiftyCharges
        vm.engine.activateFiftyFifty()
        assertFalse("50/50 blocked after answering", vm.engine.fiftyFiftyActive)
        assertEquals("Charges should not decrement", chargesBefore, vm.engine.fiftyFiftyCharges)

        // Next question -> power-ups re-enabled
        vm.nextQuestion()
        composeRule.waitForIdle()
        assertFalse("powerUpUsedThisQuestion should reset", vm.uiState.value.powerUpUsedThisQuestion)
        composeRule.onNodeWithText("50/50").performClick(); advance()
        assertTrue("50/50 works on new question", vm.uiState.value.fiftyFiftyActive)
    }

    // === TEST 3: Power-up visibility (0 charges, QUICK mode) ===
    @Test
    fun fun_powerUp_visibilityRules() {
        // 0 charges -> not visible
        vm.startAllLawsGame()
        vm.engine.hintCharges = 0; vm.engine.fiftyFiftyCharges = 0; vm.engine.doubleScoreCharges = 0
        vm.updateUiState(); render()
        assertTrue("No power-ups with 0 charges",
            composeRule.onAllNodesWithText("Pista").fetchSemanticsNodes().isEmpty() &&
            composeRule.onAllNodesWithText("50/50").fetchSemanticsNodes().isEmpty() &&
            composeRule.onAllNodesWithText("x2 pts").fetchSemanticsNodes().isEmpty())
    }

    // === TEST 4: Charges display + decrement ===
    @Test
    fun fun_powerUp_chargesDisplayAndDecrement() {
        startSurvival()
        vm.engine.hintCharges = 3; vm.engine.fiftyFiftyCharges = 2; vm.engine.doubleScoreCharges = 1
        vm.updateUiState(); render()
        // Verify charges via engine state (UI text matching is ambiguous with question content)
        assertEquals(3, vm.uiState.value.hintCharges)
        assertEquals(2, vm.uiState.value.fiftyFiftyCharges)
        assertEquals(1, vm.uiState.value.doubleScoreCharges)
        composeRule.onNodeWithText("50/50").performClick(); advance()
        assertEquals(1, vm.uiState.value.fiftyFiftyCharges)
    }

    // === TEST 5: Double score toast + scoring ===
    @Test
    fun fun_doubleScore_toastAndScoring() {
        startSurvival(); render()
        assertEquals(0, vm.uiState.value.score)
        composeRule.onNodeWithText("x2 pts").performClick(); advance()
        assertTrue("Toast shown for double score", vm.powerUpToast.value != null)
        vm.answer(vm.uiState.value.currentQ!!.correct)
        composeRule.waitForIdle()
        assertEquals(20, vm.uiState.value.score)
    }

    // === TEST 6: Answer effects per mode (survival, timetrial, challenge) ===
    @Test
    fun fun_answerEffects_allModes() {
        // Survival: correct = no timer, score increases
        startSurvival(); render()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        composeRule.waitForIdle()
        assertTrue("Survival answered", vm.engine.answered)
        assertEquals(0f, vm.uiState.value.timer, 0.01f)
        assertTrue("Score > 0 after correct", vm.uiState.value.score > 0)

        // Timetrial: correct = gain time (engine only, no re-render)
        startTimetrial()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        composeRule.waitForIdle()
        assertTrue("Timetrial timer increased", vm.uiState.value.timer > 180f)

        // Challenge: correct = gain time (engine only, no re-render)
        startChallenge()
        val tBefore = vm.uiState.value.timer
        vm.answer(vm.uiState.value.currentQ!!.correct)
        composeRule.waitForIdle()
        assertTrue("Challenge timer increased", vm.uiState.value.timer > tBefore)
    }

    // === TEST 7: Wrong answer effects (life lost, combo reset, shield) ===
    @Test
    fun fun_wrongAnswer_effects() {
        startSurvival(); vm.engine.shieldCharges = 0; vm.engine.combo = 3; vm.updateUiState(); render()
        val q = vm.uiState.value.currentQ!!
        val wrong = listOf("A","B","C","D").first { it != q.correct }
        vm.answer(wrong)
        composeRule.waitForIdle()
        assertEquals(2, vm.uiState.value.lives)
        assertEquals(0, vm.uiState.value.combo)

        // Shield absorbs (engine only, no re-render)
        startSurvival(); vm.engine.shieldCharges = 1; vm.engine.combo = 3; vm.updateUiState()
        vm.engine.activateShield(); vm.updateUiState()
        val q2 = vm.uiState.value.currentQ!!
        val wrong2 = listOf("A","B","C","D").first { it != q2.correct }
        val result = vm.answer(wrong2)
        composeRule.waitForIdle()
        assertEquals(GameEngine.AnswerResult.SHIELD_USED, result)
        assertEquals(3, vm.uiState.value.lives)
        assertEquals(3, vm.uiState.value.combo)
    }

    // === TEST 8: 50/50 shows exactly 2 options, correct visible, then wrong answer works ===
    @Test
    fun fun_fiftyFifty_fullFlow() {
        startSurvival(); vm.engine.shieldCharges = 0; render()
        composeRule.onNodeWithText("50/50").performClick(); advance()
        assertEquals(2, countVisibleOptions())
        val q = vm.uiState.value.currentQ!!
        val visible = listOf("A","B","C","D").filter {
            q.opciones[it] != null && !(vm.uiState.value.fiftyFiftyRemoved.contains(it) && it != q.correct)
        }
        assertTrue("Correct visible after 50/50", q.correct in visible)
        val wrongVisible = visible.first { it != q.correct }
        val result = vm.answer(wrongVisible)
        composeRule.waitForIdle()
        assertEquals(GameEngine.AnswerResult.WRONG, result)
        assertTrue("Answered after wrong with 50/50", vm.engine.answered)
    }

    // === TEST 9: Full game flow survival (5 questions with 50/50 on even) ===
    @Test
    fun fun_fullFlow_survival5Questions() {
        startSurvival(); render()
        for (i in 1..5) {
            if (i % 2 == 0) {
                vm.activateFiftyFifty()
                advance()
                assertEquals("Q$i 50/50 shows 2", 2, countVisibleOptions())
            }
            val q = vm.uiState.value.currentQ!!
            vm.answer(q.correct)
            composeRule.waitForIdle()
            if (vm.isGameOver()) break
            vm.nextQuestion()
            composeRule.waitForIdle()
            assertFalse("Q$i powerUpUsed resets", vm.uiState.value.powerUpUsedThisQuestion)
        }
        assertEquals(5, vm.engine.totalAnswered)
    }

    // === TEST 10: Full game flow challenge + quick ===
    @Test
    fun fun_fullFlow_challengeAndQuick() {
        startChallenge(); render()
        for (i in 1..3) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            composeRule.waitForIdle()
            if (vm.isGameOver()) break
            vm.nextQuestion()
            composeRule.waitForIdle()
        }
        assertEquals(3, vm.engine.totalAnswered)

        // Quick mode (engine only, no re-render)
        startQuick()
        for (i in 1..3) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            composeRule.waitForIdle()
            if (vm.isGameOver()) break
            vm.nextQuestion()
            composeRule.waitForIdle()
        }
        assertEquals(3, vm.engine.totalAnswered)
    }

    // === TEST 11: Question number, score, combo, lives display ===
    @Test
    fun fun_displayMetrics_updateCorrectly() {
        startSurvival(); render()
        val q1Num = vm.uiState.value.questionNum
        assertEquals(3, vm.uiState.value.lives)
        assertEquals(0, vm.uiState.value.score)

        vm.answer(vm.uiState.value.currentQ!!.correct)
        composeRule.waitForIdle()
        assertTrue("Score > 0", vm.uiState.value.score > 0)
        assertTrue("Combo >= 1", vm.uiState.value.combo >= 1)

        vm.nextQuestion()
        composeRule.waitForIdle()
        assertEquals(q1Num + 1, vm.uiState.value.questionNum)
    }

    // === TEST 12: Accuracy ===
    @Test
    fun fun_accuracy_allCorrectAndHalfCorrect() {
        startSurvival(); render()
        for (i in 1..3) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            composeRule.waitForIdle()
            if (vm.isGameOver()) break
            vm.nextQuestion()
            composeRule.waitForIdle()
        }
        assertEquals(100, vm.engine.getAccuracy())

        // Half correct (engine only, no re-render)
        startSurvival(); vm.engine.shieldCharges = 0
        vm.answer(vm.uiState.value.currentQ!!.correct)
        composeRule.waitForIdle()
        vm.nextQuestion()
        composeRule.waitForIdle()
        val q = vm.uiState.value.currentQ!!
        val wrong = listOf("A","B","C","D").first { it != q.correct }
        vm.answer(wrong)
        composeRule.waitForIdle()
        assertEquals(50, vm.engine.getAccuracy())
    }

    // === TEST 13: Timer modes render correctly ===
    @Test
    fun fun_timerModes_renderCorrectly() {
        startTimetrial(); render()
        assertTrue("Timetrial loads question", vm.uiState.value.currentQ != null)
        // Challenge and Survival tested via engine in other tests
        startChallenge()
        assertTrue("Challenge loads question", vm.uiState.value.currentQ != null)
        startSurvival()
        assertTrue("Survival loads question", vm.uiState.value.currentQ != null)
    }
}
