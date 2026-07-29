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
class PowerUpUiTest {

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

    private fun startSurvival() {
        vm.startAllLawsGame()
        vm.engine.hintCharges = 5
        vm.engine.fiftyFiftyCharges = 5
        vm.engine.doubleScoreCharges = 5
        vm.updateUiState()
    }

    private fun startTimetrial() {
        vm.startAllLawsGame()
        vm.engine.mode = GameMode.TIMETRIAL
        vm.engine.timer = 180f
        vm.engine.hintCharges = 5
        vm.engine.fiftyFiftyCharges = 5
        vm.engine.doubleScoreCharges = 5
        vm.updateUiState()
    }

    private fun startChallenge() {
        vm.startChallengeGame()
        vm.engine.hintCharges = 5
        vm.engine.fiftyFiftyCharges = 5
        vm.engine.doubleScoreCharges = 5
        vm.updateUiState()
    }

    private fun render() {
        composeRule.setContent {
            GameScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        advanceAnimations()
    }

    private fun countVisibleOptions(): Int {
        val q = vm.uiState.value.currentQ!!
        var count = 0
        for ((_, text) in q.opciones) {
            val nodes = composeRule.onAllNodesWithText(text).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) count++
        }
        return count
    }

    private fun isCorrectOptionVisible(): Boolean {
        val q = vm.uiState.value.currentQ!!
        val correctText = q.opciones[q.correct]!!
        return composeRule.onAllNodesWithText(correctText).fetchSemanticsNodes().isNotEmpty()
    }

    // === 50/50 in Survival ===

    @Test
    fun fun_fiftyFifty_survival_showsExactly2Options() {
        startSurvival()
        render()
        assertEquals("Should start with 4 options", 4, countVisibleOptions())
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertEquals("Should show exactly 2 options after 50/50 in Survival", 2, countVisibleOptions())
    }

    @Test
    fun fun_fiftyFifty_survival_correctOptionVisible() {
        startSurvival()
        render()
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertTrue("Correct option must be visible after 50/50 in Survival", isCorrectOptionVisible())
    }

    // === 50/50 in Timetrial ===

    @Test
    fun fun_fiftyFifty_timetrial_showsExactly2Options() {
        startTimetrial()
        render()
        assertEquals("Should start with 4 options", 4, countVisibleOptions())
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertEquals("Should show exactly 2 options after 50/50 in Timetrial", 2, countVisibleOptions())
    }

    @Test
    fun fun_fiftyFifty_timetrial_correctOptionVisible() {
        startTimetrial()
        render()
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertTrue("Correct option must be visible after 50/50 in Timetrial", isCorrectOptionVisible())
    }

    // === 50/50 in Challenge ===

    @Test
    fun fun_fiftyFifty_challenge_showsExactly2Options() {
        startChallenge()
        render()
        assertEquals("Should start with 4 options", 4, countVisibleOptions())
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertEquals("Should show exactly 2 options after 50/50 in Challenge", 2, countVisibleOptions())
    }

    @Test
    fun fun_fiftyFifty_challenge_correctOptionVisible() {
        startChallenge()
        render()
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertTrue("Correct option must be visible after 50/50 in Challenge", isCorrectOptionVisible())
    }

    // === Power-up visibility per mode ===

    @Test
    fun fun_powerUps_visibleInSurvival() {
        startSurvival()
        render()
        composeRule.onNodeWithText("Pista").assertIsDisplayed()
        composeRule.onNodeWithText("50/50").assertIsDisplayed()
        composeRule.onNodeWithText("x2 pts").assertIsDisplayed()
    }

    @Test
    fun fun_powerUps_visibleInTimetrial() {
        startTimetrial()
        render()
        composeRule.onNodeWithText("Pista").assertIsDisplayed()
        composeRule.onNodeWithText("50/50").assertIsDisplayed()
        composeRule.onNodeWithText("x2 pts").assertIsDisplayed()
    }

    @Test
    fun fun_powerUps_visibleInChallenge() {
        startChallenge()
        render()
        composeRule.onNodeWithText("Pista").assertIsDisplayed()
        composeRule.onNodeWithText("50/50").assertIsDisplayed()
        composeRule.onNodeWithText("x2 pts").assertIsDisplayed()
    }

    @Test
    fun fun_powerUps_notVisibleInQuick() {
        vm.startQuickGame()
        vm.engine.hintCharges = 5
        vm.engine.fiftyFiftyCharges = 5
        vm.engine.doubleScoreCharges = 5
        vm.updateUiState()
        render()
        val hintNodes = composeRule.onAllNodesWithText("Pista").fetchSemanticsNodes()
        assertTrue("Pista should NOT be visible in Quick mode", hintNodes.isEmpty())
    }

    // === Hint in each mode (hint attenuates, doesn't hide) ===

    @Test
    fun fun_hint_survival_engineRemoves1CorrectVisible() {
        startSurvival()
        render()
        composeRule.onNodeWithText("Pista").performClick()
        advanceAnimations()
        assertTrue("Hint should be active in Survival", vm.uiState.value.hintActive)
        assertEquals("Hint should remove exactly 1 option", 1, vm.uiState.value.hintRemoved.size)
        assertFalse("Correct option must NOT be in hintRemoved", vm.uiState.value.hintRemoved.contains(vm.uiState.value.currentQ!!.correct))
        assertTrue("Correct option must still be visible in UI", isCorrectOptionVisible())
    }

    @Test
    fun fun_hint_timetrial_engineRemoves1CorrectVisible() {
        startTimetrial()
        render()
        composeRule.onNodeWithText("Pista").performClick()
        advanceAnimations()
        assertTrue("Hint should be active in Timetrial", vm.uiState.value.hintActive)
        assertEquals("Hint should remove exactly 1 option", 1, vm.uiState.value.hintRemoved.size)
        assertFalse("Correct option must NOT be in hintRemoved", vm.uiState.value.hintRemoved.contains(vm.uiState.value.currentQ!!.correct))
        assertTrue("Correct option must still be visible in UI", isCorrectOptionVisible())
    }

    @Test
    fun fun_hint_challenge_engineRemoves1CorrectVisible() {
        startChallenge()
        render()
        composeRule.onNodeWithText("Pista").performClick()
        advanceAnimations()
        assertTrue("Hint should be active in Challenge", vm.uiState.value.hintActive)
        assertEquals("Hint should remove exactly 1 option", 1, vm.uiState.value.hintRemoved.size)
        assertFalse("Correct option must NOT be in hintRemoved", vm.uiState.value.hintRemoved.contains(vm.uiState.value.currentQ!!.correct))
        assertTrue("Correct option must still be visible in UI", isCorrectOptionVisible())
    }

    // === Double score in each mode ===

    @Test
    fun fun_doubleScore_survival_activates() {
        startSurvival()
        render()
        composeRule.onNodeWithText("x2 pts").performClick()
        advanceAnimations()
        assertTrue("Double score should be active in Survival", vm.uiState.value.doubleScoreActive)
    }

    @Test
    fun fun_doubleScore_timetrial_activates() {
        startTimetrial()
        render()
        composeRule.onNodeWithText("x2 pts").performClick()
        advanceAnimations()
        assertTrue("Double score should be active in Timetrial", vm.uiState.value.doubleScoreActive)
    }

    @Test
    fun fun_doubleScore_challenge_activates() {
        startChallenge()
        render()
        composeRule.onNodeWithText("x2 pts").performClick()
        advanceAnimations()
        assertTrue("Double score should be active in Challenge", vm.uiState.value.doubleScoreActive)
    }

    // === Mutual exclusivity from UI ===

    @Test
    fun fun_mutualExclusivity_hintThenFiftyFifty_fromUI() {
        startSurvival()
        render()
        composeRule.onNodeWithText("Pista").performClick()
        advanceAnimations()
        assertTrue("Hint should be active", vm.uiState.value.hintActive)
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertFalse("50/50 should NOT be active after hint", vm.uiState.value.fiftyFiftyActive)
        assertTrue("Hint should still be active", vm.uiState.value.hintActive)
    }

    @Test
    fun fun_mutualExclusivity_fiftyFiftyThenHint_fromUI() {
        startSurvival()
        render()
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertTrue("50/50 should be active", vm.uiState.value.fiftyFiftyActive)
        composeRule.onNodeWithText("Pista").performClick()
        advanceAnimations()
        assertFalse("Hint should NOT be active after 50/50", vm.uiState.value.hintActive)
        assertTrue("50/50 should still be active", vm.uiState.value.fiftyFiftyActive)
        assertEquals("Should still show 2 options (50/50 only)", 2, countVisibleOptions())
    }

    @Test
    fun fun_mutualExclusivity_hintThenDoubleScore_fromUI() {
        startSurvival()
        render()
        composeRule.onNodeWithText("Pista").performClick()
        advanceAnimations()
        assertTrue("Hint should be active", vm.uiState.value.hintActive)
        composeRule.onNodeWithText("x2 pts").performClick()
        advanceAnimations()
        assertFalse("Double score should NOT be active after hint", vm.uiState.value.doubleScoreActive)
    }

    @Test
    fun fun_mutualExclusivity_fiftyFiftyThenDoubleScore_fromUI() {
        startSurvival()
        render()
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertTrue("50/50 should be active", vm.uiState.value.fiftyFiftyActive)
        composeRule.onNodeWithText("x2 pts").performClick()
        advanceAnimations()
        assertFalse("Double score should NOT be active after 50/50", vm.uiState.value.doubleScoreActive)
    }

    @Test
    fun fun_mutualExclusivity_doubleScoreThenFiftyFifty_fromUI() {
        startSurvival()
        render()
        composeRule.onNodeWithText("x2 pts").performClick()
        advanceAnimations()
        assertTrue("Double score should be active", vm.uiState.value.doubleScoreActive)
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertFalse("50/50 should NOT be active after double score", vm.uiState.value.fiftyFiftyActive)
    }

    @Test
    fun fun_mutualExclusivity_doubleScoreThenHint_fromUI() {
        startSurvival()
        render()
        composeRule.onNodeWithText("x2 pts").performClick()
        advanceAnimations()
        assertTrue("Double score should be active", vm.uiState.value.doubleScoreActive)
        composeRule.onNodeWithText("Pista").performClick()
        advanceAnimations()
        assertFalse("Hint should NOT be active after double score", vm.uiState.value.hintActive)
    }

    // === Power-ups across successive questions ===

    @Test
    fun fun_fiftyFiftyQ1_then_fiftyFiftyQ2_bothWork() {
        startSurvival()
        render()
        // Q1: 50/50
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertEquals("Q1: Should show 2 options", 2, countVisibleOptions())
        assertTrue("Q1: Correct should be visible", isCorrectOptionVisible())
        // Answer and advance
        val q1 = vm.uiState.value.currentQ!!
        composeRule.onNodeWithText(q1.opciones[q1.correct]!!).performClick()
        advanceAnimations()
        vm.nextQuestion()
        advanceAnimations()
        // Q2: 50/50 again
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertEquals("Q2: Should show 2 options", 2, countVisibleOptions())
        assertTrue("Q2: Correct should be visible", isCorrectOptionVisible())
    }

    @Test
    fun fun_hintQ1_then_fiftyFiftyQ2_bothWork() {
        startSurvival()
        render()
        // Q1: hint
        composeRule.onNodeWithText("Pista").performClick()
        advanceAnimations()
        assertTrue("Q1: Hint should be active", vm.uiState.value.hintActive)
        // Answer and advance
        val q1 = vm.uiState.value.currentQ!!
        composeRule.onNodeWithText(q1.opciones[q1.correct]!!).performClick()
        advanceAnimations()
        vm.nextQuestion()
        advanceAnimations()
        // Q2: 50/50
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertEquals("Q2: Should show 2 options", 2, countVisibleOptions())
        assertTrue("Q2: Correct should be visible", isCorrectOptionVisible())
    }

    @Test
    fun fun_fiftyFiftyQ1_then_hintQ2_bothWork() {
        startSurvival()
        render()
        // Q1: 50/50
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertEquals("Q1: Should show 2 options", 2, countVisibleOptions())
        // Answer and advance
        val q1 = vm.uiState.value.currentQ!!
        composeRule.onNodeWithText(q1.opciones[q1.correct]!!).performClick()
        advanceAnimations()
        vm.nextQuestion()
        advanceAnimations()
        // Q2: hint
        composeRule.onNodeWithText("Pista").performClick()
        advanceAnimations()
        assertTrue("Q2: Hint should be active", vm.uiState.value.hintActive)
        assertEquals("Q2: Hint should remove 1 option", 1, vm.uiState.value.hintRemoved.size)
    }

    @Test
    fun fun_doubleScoreQ1_then_fiftyFiftyQ2_bothWork() {
        startSurvival()
        render()
        // Q1: double score
        composeRule.onNodeWithText("x2 pts").performClick()
        advanceAnimations()
        assertTrue("Q1: Double score should be active", vm.uiState.value.doubleScoreActive)
        // Answer and advance
        val q1 = vm.uiState.value.currentQ!!
        composeRule.onNodeWithText(q1.opciones[q1.correct]!!).performClick()
        advanceAnimations()
        vm.nextQuestion()
        advanceAnimations()
        // Q2: 50/50
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertEquals("Q2: Should show 2 options", 2, countVisibleOptions())
        assertTrue("Q2: Correct should be visible", isCorrectOptionVisible())
    }

    // === 50/50 then answer correctly ===

    @Test
    fun fun_fiftyFifty_thenAnswerCorrect_works() {
        startSurvival()
        render()
        composeRule.onNodeWithText("50/50").performClick()
        advanceAnimations()
        assertEquals("Should show 2 options", 2, countVisibleOptions())
        val q = vm.uiState.value.currentQ!!
        // Call answer directly
        val result = vm.answer(q.correct)
        assertTrue("Answer result should not be ALREADY_ANSWERED: $result", result != com.opoleyes.domain.GameEngine.AnswerResult.ALREADY_ANSWERED)
        assertTrue("Engine answered should be true", vm.engine.answered)
        assertTrue("UI state answered should be true", vm.uiState.value.answered)
    }

    // === 50/50 stress from UI (5 rounds, single setContent) ===

    @Test
    fun fun_fiftyFifty_stress5RoundsFromUI() {
        startSurvival()
        render()
        repeat(5) { round ->
            composeRule.onNodeWithText("50/50").performClick()
            advanceAnimations()
            val visible = countVisibleOptions()
            assertTrue("Round $round: Should show >=2 options (got $visible)", visible >= 2)
            assertTrue("Round $round: Correct should be visible", isCorrectOptionVisible())
            // Answer and advance
            val q = vm.uiState.value.currentQ!!
            val correctText = q.opciones[q.correct]!!
            composeRule.onNodeWithText(correctText).performClick()
            advanceAnimations()
            vm.nextQuestion()
            advanceAnimations()
        }
    }
}
