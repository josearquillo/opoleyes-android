package com.opoleyes.domain

import com.opoleyes.TestContextProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.model.QuestionEntry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PowerUpCombinationTest {

    private lateinit var engine: GameEngine
    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        val context = TestContextProvider.getContext()
        prefs = PreferencesManager(context)
        prefs.resetAll()
        engine = GameEngine(context)
        engine.startAllLawsGame()
        engine.initGameStats()
        engine.nextQuestion()
        engine.hintCharges = 99
        engine.fiftyFiftyCharges = 99
        engine.doubleScoreCharges = 99
        engine.shieldCharges = 99
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    private fun resetPowerUpState() {
        engine.fiftyFiftyActive = false
        engine.fiftyFiftyRemoved = emptyList()
        engine.hintActive = false
        engine.hintRemoved = emptyList()
        engine.doubleScoreActive = false
        engine.powerUpUsedThisQuestion = false
        engine.answered = false
        engine.selectedOption = null
    }

    private fun visibleOptions(): List<String> {
        val q = engine.currentQ!!
        val all = listOf("A", "B", "C", "D").filter { q.opciones[it] != null }
        return all.filter { it !in engine.fiftyFiftyRemoved }
    }

    private fun clickableOptions(): List<String> {
        return visibleOptions().filter { it !in engine.hintRemoved }
    }

    // === FiftyFifty alone ===

    @Test
    fun fun_fiftyFifty_aloneLeavesExactly2Visible() {
        repeat(20) {
            resetPowerUpState()
            engine.activateFiftyFifty()
            assertTrue("FiftyFifty should be active (attempt $it)", engine.fiftyFiftyActive)
            assertEquals("Should have exactly 2 visible options (attempt $it)", 2, visibleOptions().size)
        }
    }

    @Test
    fun fun_fiftyFifty_aloneNeverRemovesCorrect() {
        repeat(20) {
            resetPowerUpState()
            engine.activateFiftyFifty()
            assertFalse("Correct answer should not be in fiftyFiftyRemoved (attempt $it)",
                engine.fiftyFiftyRemoved.contains(engine.currentQ!!.correct))
        }
    }

    @Test
    fun fun_fiftyFifty_alwaysHasCorrectAnswerVisible() {
        repeat(20) {
            resetPowerUpState()
            engine.activateFiftyFifty()
            assertTrue("Correct answer should be visible (attempt $it)",
                visibleOptions().contains(engine.currentQ!!.correct))
        }
    }

    @Test
    fun fun_fiftyFifty_alwaysHasAtLeast2Clickable() {
        repeat(20) {
            resetPowerUpState()
            engine.activateFiftyFifty()
            assertTrue("Should have at least 2 clickable options (attempt $it)",
                clickableOptions().size >= 2)
        }
    }

    // === Hint alone ===

    @Test
    fun fun_hint_aloneLeavesAtLeast2Clickable() {
        repeat(20) {
            resetPowerUpState()
            engine.useHint()
            assertTrue("Hint should be active (attempt $it)", engine.hintActive)
            assertTrue("Should have at least 2 clickable options (attempt $it)",
                clickableOptions().size >= 2)
        }
    }

    @Test
    fun fun_hint_aloneNeverRemovesCorrect() {
        repeat(20) {
            resetPowerUpState()
            engine.useHint()
            assertFalse("Correct answer should not be in hintRemoved (attempt $it)",
                engine.hintRemoved.contains(engine.currentQ!!.correct))
        }
    }

    @Test
    fun fun_hint_aloneRemovesExactly1Option() {
        repeat(20) {
            resetPowerUpState()
            engine.useHint()
            assertEquals("Hint should remove exactly 1 option (attempt $it)", 1, engine.hintRemoved.size)
        }
    }

    // === Mutual exclusivity: only one power-up per question ===

    @Test
    fun fun_mutualExclusivity_hintThenFiftyFifty_blocked() {
        resetPowerUpState()
        engine.useHint()
        assertTrue("Hint should be active", engine.hintActive)
        engine.activateFiftyFifty()
        assertFalse("FiftyFifty should NOT be active after hint", engine.fiftyFiftyActive)
        assertTrue("Should have at least 2 clickable", clickableOptions().size >= 2)
    }

    @Test
    fun fun_mutualExclusivity_fiftyFiftyThenHint_blocked() {
        resetPowerUpState()
        engine.activateFiftyFifty()
        assertTrue("FiftyFifty should be active", engine.fiftyFiftyActive)
        engine.useHint()
        assertFalse("Hint should NOT be active after 50/50", engine.hintActive)
        assertTrue("Should have at least 2 clickable", clickableOptions().size >= 2)
    }

    @Test
    fun fun_mutualExclusivity_hintThenDoubleScore_blocked() {
        resetPowerUpState()
        engine.useHint()
        assertTrue("Hint should be active", engine.hintActive)
        engine.activateDoubleScore()
        assertFalse("DoubleScore should NOT be active after hint", engine.doubleScoreActive)
    }

    @Test
    fun fun_mutualExclusivity_doubleScoreThenHint_blocked() {
        resetPowerUpState()
        engine.activateDoubleScore()
        assertTrue("DoubleScore should be active", engine.doubleScoreActive)
        engine.useHint()
        assertFalse("Hint should NOT be active after doubleScore", engine.hintActive)
    }

    @Test
    fun fun_mutualExclusivity_fiftyFiftyThenDoubleScore_blocked() {
        resetPowerUpState()
        engine.activateFiftyFifty()
        assertTrue("FiftyFifty should be active", engine.fiftyFiftyActive)
        engine.activateDoubleScore()
        assertFalse("DoubleScore should NOT be active after 50/50", engine.doubleScoreActive)
    }

    @Test
    fun fun_mutualExclusivity_doubleScoreThenFiftyFifty_blocked() {
        resetPowerUpState()
        engine.activateDoubleScore()
        assertTrue("DoubleScore should be active", engine.doubleScoreActive)
        engine.activateFiftyFifty()
        assertFalse("FiftyFifty should NOT be active after doubleScore", engine.fiftyFiftyActive)
    }

    @Test
    fun fun_mutualExclusivity_powerUpUsedThisQuestionResetsOnNextQuestion() {
        resetPowerUpState()
        engine.useHint()
        assertTrue("powerUpUsedThisQuestion should be true", engine.powerUpUsedThisQuestion)
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()
        assertFalse("powerUpUsedThisQuestion should be false on new question", engine.powerUpUsedThisQuestion)
    }

    // === Consecutive questions ===

    @Test
    fun fun_fiftyFifty_consecutiveQuestionsLeaves2EachTime() {
        repeat(5) { round ->
            engine.activateFiftyFifty()
            assertTrue("FiftyFifty should be active on Q${round + 1}", engine.fiftyFiftyActive)
            assertEquals("Should have exactly 2 visible on Q${round + 1}", 2, visibleOptions().size)
            assertTrue("Should have at least 2 clickable on Q${round + 1}", clickableOptions().size >= 2)
            engine.answer(engine.currentQ!!.correct)
            assertTrue("Should advance to next question", engine.nextQuestion())
        }
    }

    @Test
    fun fun_fiftyFifty_consecutiveQuestionsCorrectAlwaysVisible() {
        repeat(5) { round ->
            engine.activateFiftyFifty()
            assertTrue("Correct should be visible on Q${round + 1}",
                visibleOptions().contains(engine.currentQ!!.correct))
            engine.answer(engine.currentQ!!.correct)
            engine.nextQuestion()
        }
    }

    @Test
    fun fun_hint_consecutiveQuestionsLeavesAtLeast2Clickable() {
        repeat(5) { round ->
            engine.useHint()
            assertTrue("Hint should be active on Q${round + 1}", engine.hintActive)
            assertTrue("Should have at least 2 clickable on Q${round + 1}", clickableOptions().size >= 2)
            engine.answer(engine.currentQ!!.correct)
            engine.nextQuestion()
        }
    }

    @Test
    fun fun_hint_consecutiveQuestionsRemovesExactly1EachTime() {
        repeat(5) { round ->
            engine.useHint()
            assertEquals("Hint should remove exactly 1 on Q${round + 1}", 1, engine.hintRemoved.size)
            engine.answer(engine.currentQ!!.correct)
            engine.nextQuestion()
        }
    }

    // === Cross-question: different power-ups on different questions ===

    @Test
    fun fun_hintQ1_fiftyFiftyQ2_bothWorkCorrectly() {
        engine.useHint()
        assertTrue("Hint should be active on Q1", engine.hintActive)
        assertEquals("Hint should remove 1 on Q1", 1, engine.hintRemoved.size)
        assertTrue("Should have at least 2 clickable on Q1", clickableOptions().size >= 2)
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()

        engine.activateFiftyFifty()
        assertTrue("FiftyFifty should be active on Q2", engine.fiftyFiftyActive)
        assertEquals("Should have exactly 2 visible on Q2", 2, visibleOptions().size)
        assertTrue("Should have at least 2 clickable on Q2", clickableOptions().size >= 2)
        assertFalse("Hint should not be active on Q2", engine.hintActive)
        assertEquals("HintRemoved should be cleared on Q2", 0, engine.hintRemoved.size)
    }

    @Test
    fun fun_fiftyFiftyQ1_hintQ2_bothWorkCorrectly() {
        engine.activateFiftyFifty()
        assertTrue("FiftyFifty should be active on Q1", engine.fiftyFiftyActive)
        assertEquals("Should have 2 visible on Q1", 2, visibleOptions().size)
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()

        engine.useHint()
        assertTrue("Hint should be active on Q2", engine.hintActive)
        assertEquals("Hint should remove 1 on Q2", 1, engine.hintRemoved.size)
        assertTrue("Should have at least 2 clickable on Q2", clickableOptions().size >= 2)
        assertFalse("FiftyFifty should not be active on Q2", engine.fiftyFiftyActive)
        assertEquals("FiftyFiftyRemoved should be cleared on Q2", 0, engine.fiftyFiftyRemoved.size)
    }

    @Test
    fun fun_hintQ1_fiftyFiftyQ2_correctVisibleOnBoth() {
        engine.useHint()
        assertTrue("Correct should be clickable on Q1", clickableOptions().contains(engine.currentQ!!.correct))
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()
        engine.activateFiftyFifty()
        assertTrue("Correct should be visible on Q2", visibleOptions().contains(engine.currentQ!!.correct))
        assertTrue("Correct should be clickable on Q2", clickableOptions().contains(engine.currentQ!!.correct))
    }

    // === State clearing on nextQuestion ===

    @Test
    fun fun_nextQuestion_clearsAllPowerUpState() {
        engine.useHint()
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()

        assertFalse("hintActive should be cleared", engine.hintActive)
        assertEquals("hintRemoved should be cleared", 0, engine.hintRemoved.size)
        assertFalse("fiftyFiftyActive should be cleared", engine.fiftyFiftyActive)
        assertEquals("fiftyFiftyRemoved should be cleared", 0, engine.fiftyFiftyRemoved.size)
        assertFalse("powerUpUsedThisQuestion should be cleared", engine.powerUpUsedThisQuestion)
    }

    // === Edge case: 3-option questions ===

    @Test
    fun fun_fiftyFifty_with3OptionsLeaves2Visible() {
        val q3 = QuestionEntry(
            enunciado = "3-option question",
            opciones = mapOf("A" to "Opt A", "B" to "Opt B", "C" to "Opt C"),
            correct = "A",
            weight = 50,
            testId = "test1",
            origId = "1"
        )
        repeat(20) {
            resetPowerUpState()
            engine.currentQ = q3
            engine.activateFiftyFifty()
            if (engine.fiftyFiftyActive) {
                assertEquals("Should have exactly 2 visible with 3 options (attempt $it)", 2, visibleOptions().size)
                assertTrue("Correct should be visible (attempt $it)", visibleOptions().contains("A"))
            }
        }
    }

    @Test
    fun fun_hint_with3OptionsLeavesAtLeast2Clickable() {
        val q3 = QuestionEntry(
            enunciado = "3-option question",
            opciones = mapOf("A" to "Opt A", "B" to "Opt B", "C" to "Opt C"),
            correct = "A",
            weight = 50,
            testId = "test1",
            origId = "1"
        )
        repeat(20) {
            resetPowerUpState()
            engine.currentQ = q3
            engine.useHint()
            assertTrue("Should have at least 2 clickable with 3 options after hint (attempt $it)",
                clickableOptions().size >= 2)
        }
    }

    @Test
    fun fun_fiftyFifty_with3OptionsLeaves2Clickable() {
        val q3 = QuestionEntry(
            enunciado = "3-option question",
            opciones = mapOf("A" to "Opt A", "B" to "Opt B", "C" to "Opt C"),
            correct = "A",
            weight = 50,
            testId = "test1",
            origId = "1"
        )
        repeat(20) {
            resetPowerUpState()
            engine.currentQ = q3
            engine.activateFiftyFifty()
            if (engine.fiftyFiftyActive) {
                assertTrue("Should have at least 2 clickable with 3 options after 50/50 (attempt $it)",
                    clickableOptions().size >= 2)
            }
        }
    }

    // === Stress test across multiple questions ===

    @Test
    fun fun_stress_allPowerUpsAcross10Questions() {
        val actions = listOf(
            { e: GameEngine -> e.useHint() },
            { e: GameEngine -> e.activateFiftyFifty() },
            { e: GameEngine -> e.activateDoubleScore() },
            { e: GameEngine -> } // no power-ups
        )
        repeat(10) { round ->
            actions[round % actions.size](engine)
            val clickable = clickableOptions().size
            assertTrue("Q${round + 1}: Should have at least 2 clickable (got $clickable)", clickable >= 2)
            assertTrue("Q${round + 1}: Correct should be clickable",
                clickableOptions().contains(engine.currentQ!!.correct))
            engine.answer(engine.currentQ!!.correct)
            assertTrue("Q${round + 1}: Should advance", engine.nextQuestion())
        }
    }

    @Test
    fun fun_stress_fiftyFifty20TimesNeverLeavesLessThan2() {
        repeat(20) {
            resetPowerUpState()
            engine.activateFiftyFifty()
            val visible = visibleOptions().size
            val clickable = clickableOptions().size
            assertTrue("Visible should be >= 2 (attempt $it, got $visible)", visible >= 2)
            assertTrue("Clickable should be >= 2 (attempt $it, got $clickable)", clickable >= 2)
        }
    }

    @Test
    fun fun_stress_hint20TimesNeverLeavesLessThan2() {
        repeat(20) {
            resetPowerUpState()
            engine.useHint()
            val clickable = clickableOptions().size
            assertTrue("Clickable should be >= 2 (attempt $it, got $clickable)", clickable >= 2)
        }
    }
}
