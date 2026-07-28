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
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    private fun startGame() {
        engine.startAllLawsGame()
        engine.initGameStats()
        engine.nextQuestion()
        engine.hintCharges = 99
        engine.fiftyFiftyCharges = 99
        engine.doubleScoreCharges = 99
        engine.shieldCharges = 99
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
        repeat(100) {
            startGame()
            engine.activateFiftyFifty()
            assertTrue("FiftyFifty should be active (attempt $it)", engine.fiftyFiftyActive)
            assertEquals("Should have exactly 2 visible options (attempt $it)", 2, visibleOptions().size)
        }
    }

    @Test
    fun fun_fiftyFifty_aloneNeverRemovesCorrect() {
        repeat(100) {
            startGame()
            engine.activateFiftyFifty()
            assertFalse("Correct answer should not be in fiftyFiftyRemoved (attempt $it)",
                engine.fiftyFiftyRemoved.contains(engine.currentQ!!.correct))
        }
    }

    @Test
    fun fun_fiftyFifty_alwaysHasCorrectAnswerVisible() {
        repeat(100) {
            startGame()
            engine.activateFiftyFifty()
            assertTrue("Correct answer should be visible (attempt $it)",
                visibleOptions().contains(engine.currentQ!!.correct))
        }
    }

    @Test
    fun fun_fiftyFifty_alwaysHasAtLeast2Clickable() {
        repeat(100) {
            startGame()
            engine.activateFiftyFifty()
            assertTrue("Should have at least 2 clickable options (attempt $it)",
                clickableOptions().size >= 2)
        }
    }

    // === Hint alone ===

    @Test
    fun fun_hint_aloneLeavesAtLeast2Clickable() {
        repeat(100) {
            startGame()
            engine.useHint()
            assertTrue("Hint should be active (attempt $it)", engine.hintActive)
            assertTrue("Should have at least 2 clickable options (attempt $it)",
                clickableOptions().size >= 2)
        }
    }

    @Test
    fun fun_hint_aloneNeverRemovesCorrect() {
        repeat(100) {
            startGame()
            engine.useHint()
            assertFalse("Correct answer should not be in hintRemoved (attempt $it)",
                engine.hintRemoved.contains(engine.currentQ!!.correct))
        }
    }

    @Test
    fun fun_hint_aloneRemovesExactly1Option() {
        repeat(100) {
            startGame()
            engine.useHint()
            assertEquals("Hint should remove exactly 1 option (attempt $it)", 1, engine.hintRemoved.size)
        }
    }

    // === Hint then FiftyFifty ===

    @Test
    fun fun_hintThenFiftyFifty_leavesAtLeast2Clickable() {
        repeat(100) {
            startGame()
            engine.useHint()
            assertTrue("Hint should be active (attempt $it)", engine.hintActive)
            engine.activateFiftyFifty()
            assertTrue("FiftyFifty should be active (attempt $it)", engine.fiftyFiftyActive)
            assertTrue("Should have at least 2 clickable options after hint+50/50 (attempt $it)",
                clickableOptions().size >= 2)
        }
    }

    @Test
    fun fun_hintThenFiftyFifty_leavesAtLeast2Visible() {
        repeat(100) {
            startGame()
            engine.useHint()
            engine.activateFiftyFifty()
            assertTrue("Should have at least 2 visible options after hint+50/50 (attempt $it)",
                visibleOptions().size >= 2)
        }
    }

    @Test
    fun fun_hintThenFiftyFifty_correctAlwaysVisibleAndClickable() {
        repeat(100) {
            startGame()
            engine.useHint()
            engine.activateFiftyFifty()
            val correct = engine.currentQ!!.correct
            assertTrue("Correct should be visible (attempt $it)", visibleOptions().contains(correct))
            assertTrue("Correct should be clickable (attempt $it)", clickableOptions().contains(correct))
        }
    }

    @Test
    fun fun_hintThenFiftyFifty_neverLeaves0Or1Clickable() {
        repeat(200) {
            startGame()
            engine.useHint()
            engine.activateFiftyFifty()
            val clickable = clickableOptions().size
            assertTrue("Should never have 0 clickable (attempt $it, got $clickable)", clickable > 0)
            assertTrue("Should never have 1 clickable (attempt $it, got $clickable)", clickable >= 2)
        }
    }

    // === FiftyFifty then Hint ===

    @Test
    fun fun_fiftyFiftyThenHint_leavesAtLeast2Clickable() {
        repeat(100) {
            startGame()
            engine.activateFiftyFifty()
            assertTrue("FiftyFifty should be active (attempt $it)", engine.fiftyFiftyActive)
            engine.useHint()
            // Hint might fail if not enough options, that's OK
            if (engine.hintActive) {
                assertTrue("Should have at least 2 clickable after 50/50+hint (attempt $it)",
                    clickableOptions().size >= 2)
            }
        }
    }

    @Test
    fun fun_fiftyFiftyThenHint_correctAlwaysClickable() {
        repeat(100) {
            startGame()
            engine.activateFiftyFifty()
            engine.useHint()
            val correct = engine.currentQ!!.correct
            assertTrue("Correct should always be clickable (attempt $it)",
                clickableOptions().contains(correct))
        }
    }

    @Test
    fun fun_fiftyFiftyThenHint_neverLeaves0Or1Clickable() {
        repeat(200) {
            startGame()
            engine.activateFiftyFifty()
            engine.useHint()
            val clickable = clickableOptions().size
            assertTrue("Should never have 0 clickable (attempt $it, got $clickable)", clickable > 0)
            assertTrue("Should never have 1 clickable (attempt $it, got $clickable)", clickable >= 2)
        }
    }

    // === Consecutive questions: FiftyFifty on Q1 then Q2 ===

    @Test
    fun fun_fiftyFifty_consecutiveQuestionsLeaves2EachTime() {
        startGame()
        repeat(5) { round ->
            engine.activateFiftyFifty()
            assertTrue("FiftyFifty should be active on Q${round + 1} (attempt $round)", engine.fiftyFiftyActive)
            assertEquals("Should have exactly 2 visible on Q${round + 1} (attempt $round)",
                2, visibleOptions().size)
            assertTrue("Should have at least 2 clickable on Q${round + 1} (attempt $round)",
                clickableOptions().size >= 2)
            engine.answer(engine.currentQ!!.correct)
            assertTrue("Should advance to next question (attempt $round)", engine.nextQuestion())
        }
    }

    @Test
    fun fun_fiftyFifty_consecutiveQuestionsCorrectAlwaysVisible() {
        startGame()
        repeat(5) { round ->
            engine.activateFiftyFifty()
            assertTrue("Correct should be visible on Q${round + 1} (attempt $round)",
                visibleOptions().contains(engine.currentQ!!.correct))
            engine.answer(engine.currentQ!!.correct)
            engine.nextQuestion()
        }
    }

    // === Consecutive questions: Hint on Q1 then Q2 ===

    @Test
    fun fun_hint_consecutiveQuestionsLeavesAtLeast2Clickable() {
        startGame()
        repeat(5) { round ->
            engine.useHint()
            assertTrue("Hint should be active on Q${round + 1} (attempt $round)", engine.hintActive)
            assertTrue("Should have at least 2 clickable on Q${round + 1} (attempt $round)",
                clickableOptions().size >= 2)
            engine.answer(engine.currentQ!!.correct)
            engine.nextQuestion()
        }
    }

    @Test
    fun fun_hint_consecutiveQuestionsRemovesExactly1EachTime() {
        startGame()
        repeat(5) { round ->
            engine.useHint()
            assertEquals("Hint should remove exactly 1 on Q${round + 1} (attempt $round)",
                1, engine.hintRemoved.size)
            engine.answer(engine.currentQ!!.correct)
            engine.nextQuestion()
        }
    }

    // === Cross-question: Hint on Q1, FiftyFifty on Q2 ===

    @Test
    fun fun_hintQ1_fiftyFiftyQ2_bothWorkCorrectly() {
        startGame()
        // Q1: use hint
        engine.useHint()
        assertTrue("Hint should be active on Q1", engine.hintActive)
        assertEquals("Hint should remove 1 on Q1", 1, engine.hintRemoved.size)
        assertTrue("Should have at least 2 clickable on Q1", clickableOptions().size >= 2)
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()

        // Q2: use fiftyFifty
        engine.activateFiftyFifty()
        assertTrue("FiftyFifty should be active on Q2", engine.fiftyFiftyActive)
        assertEquals("Should have exactly 2 visible on Q2", 2, visibleOptions().size)
        assertTrue("Should have at least 2 clickable on Q2", clickableOptions().size >= 2)
        assertFalse("Hint should not be active on Q2", engine.hintActive)
        assertEquals("HintRemoved should be cleared on Q2", 0, engine.hintRemoved.size)
    }

    @Test
    fun fun_hintQ1_fiftyFiftyQ2_correctVisibleOnBoth() {
        startGame()
        // Q1
        engine.useHint()
        assertTrue("Correct should be clickable on Q1", clickableOptions().contains(engine.currentQ!!.correct))
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()
        // Q2
        engine.activateFiftyFifty()
        assertTrue("Correct should be visible on Q2", visibleOptions().contains(engine.currentQ!!.correct))
        assertTrue("Correct should be clickable on Q2", clickableOptions().contains(engine.currentQ!!.correct))
    }

    // === Cross-question: FiftyFifty on Q1, Hint on Q2 ===

    @Test
    fun fun_fiftyFiftyQ1_hintQ2_bothWorkCorrectly() {
        startGame()
        // Q1: use fiftyFifty
        engine.activateFiftyFifty()
        assertTrue("FiftyFifty should be active on Q1", engine.fiftyFiftyActive)
        assertEquals("Should have 2 visible on Q1", 2, visibleOptions().size)
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()

        // Q2: use hint
        engine.useHint()
        assertTrue("Hint should be active on Q2", engine.hintActive)
        assertEquals("Hint should remove 1 on Q2", 1, engine.hintRemoved.size)
        assertTrue("Should have at least 2 clickable on Q2", clickableOptions().size >= 2)
        assertFalse("FiftyFifty should not be active on Q2", engine.fiftyFiftyActive)
        assertEquals("FiftyFiftyRemoved should be cleared on Q2", 0, engine.fiftyFiftyRemoved.size)
    }

    // === Cross-question: Hint+50/50 on Q1, 50/50 on Q2 ===

    @Test
    fun fun_hintAndFiftyFiftyQ1_fiftyFiftyQ2_allWorkCorrectly() {
        startGame()
        // Q1: hint then fiftyFifty
        engine.useHint()
        engine.activateFiftyFifty()
        assertTrue("Should have at least 2 clickable on Q1", clickableOptions().size >= 2)
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()

        // Q2: fiftyFifty only
        engine.activateFiftyFifty()
        assertTrue("FiftyFifty should be active on Q2", engine.fiftyFiftyActive)
        assertEquals("Should have exactly 2 visible on Q2", 2, visibleOptions().size)
        assertTrue("Should have at least 2 clickable on Q2", clickableOptions().size >= 2)
    }

    // === Cross-question: 50/50+hint on Q1, hint on Q2 ===

    @Test
    fun fun_fiftyFiftyAndHintQ1_hintQ2_allWorkCorrectly() {
        startGame()
        // Q1: fiftyFifty then hint
        engine.activateFiftyFifty()
        engine.useHint()
        assertTrue("Should have at least 2 clickable on Q1", clickableOptions().size >= 2)
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()

        // Q2: hint only
        engine.useHint()
        assertTrue("Hint should be active on Q2", engine.hintActive)
        assertEquals("Hint should remove 1 on Q2", 1, engine.hintRemoved.size)
        assertTrue("Should have at least 2 clickable on Q2", clickableOptions().size >= 2)
    }

    // === State clearing on nextQuestion ===

    @Test
    fun fun_nextQuestion_clearsAllPowerUpState() {
        startGame()
        engine.useHint()
        engine.activateFiftyFifty()
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()

        assertFalse("hintActive should be cleared", engine.hintActive)
        assertEquals("hintRemoved should be cleared", 0, engine.hintRemoved.size)
        assertFalse("fiftyFiftyActive should be cleared", engine.fiftyFiftyActive)
        assertEquals("fiftyFiftyRemoved should be cleared", 0, engine.fiftyFiftyRemoved.size)
    }

    // === Edge case: 3-option questions ===

    @Test
    fun fun_fiftyFifty_with3OptionsLeaves2Visible() {
        repeat(100) {
            startGame()
            // Simulate a 3-option question by manually setting currentQ
            engine.currentQ = QuestionEntry(
                enunciado = "3-option question",
                opciones = mapOf("A" to "Opt A", "B" to "Opt B", "C" to "Opt C"),
                correct = "A",
                weight = 50,
                testId = "test1",
                origId = "1"
            )
            engine.activateFiftyFifty()
            if (engine.fiftyFiftyActive) {
                assertEquals("Should have exactly 2 visible with 3 options (attempt $it)",
                    2, visibleOptions().size)
                assertTrue("Correct should be visible (attempt $it)",
                    visibleOptions().contains("A"))
            }
        }
    }

    @Test
    fun fun_hintThenFiftyFifty_with3OptionsLeavesAtLeast2Clickable() {
        repeat(100) {
            startGame()
            engine.currentQ = QuestionEntry(
                enunciado = "3-option question",
                opciones = mapOf("A" to "Opt A", "B" to "Opt B", "C" to "Opt C"),
                correct = "A",
                weight = 50,
                testId = "test1",
                origId = "1"
            )
            engine.useHint()
            engine.activateFiftyFifty()
            assertTrue("Should have at least 2 clickable with 3 options after hint+50/50 (attempt $it)",
                clickableOptions().size >= 2)
        }
    }

    @Test
    fun fun_fiftyFiftyThenHint_with3OptionsLeavesAtLeast2Clickable() {
        repeat(100) {
            startGame()
            engine.currentQ = QuestionEntry(
                enunciado = "3-option question",
                opciones = mapOf("A" to "Opt A", "B" to "Opt B", "C" to "Opt C"),
                correct = "A",
                weight = 50,
                testId = "test1",
                origId = "1"
            )
            engine.activateFiftyFifty()
            engine.useHint()
            assertTrue("Should have at least 2 clickable with 3 options after 50/50+hint (attempt $it)",
                clickableOptions().size >= 2)
        }
    }

    // === Stress test: all combinations across many questions ===

    @Test
    fun fun_stress_allCombinationsAcross10Questions() {
        startGame()
        val combinations = listOf(
            { e: GameEngine -> e.useHint(); e.activateFiftyFifty() },
            { e: GameEngine -> e.activateFiftyFifty(); e.useHint() },
            { e: GameEngine -> e.useHint() },
            { e: GameEngine -> e.activateFiftyFifty() },
            { e: GameEngine -> } // no power-ups
        )
        repeat(10) { round ->
            combinations[round % combinations.size](engine)
            val clickable = clickableOptions().size
            assertTrue("Q${round + 1}: Should have at least 2 clickable (got $clickable)", clickable >= 2)
            assertTrue("Q${round + 1}: Correct should be clickable",
                clickableOptions().contains(engine.currentQ!!.correct))
            engine.answer(engine.currentQ!!.correct)
            assertTrue("Q${round + 1}: Should advance", engine.nextQuestion())
        }
    }

    @Test
    fun fun_stress_fiftyFifty100TimesNeverLeavesLessThan2() {
        repeat(100) {
            startGame()
            engine.activateFiftyFifty()
            val visible = visibleOptions().size
            val clickable = clickableOptions().size
            assertTrue("Visible should be >= 2 (attempt $it, got $visible)", visible >= 2)
            assertTrue("Clickable should be >= 2 (attempt $it, got $clickable)", clickable >= 2)
        }
    }

    @Test
    fun fun_stress_hintThenFiftyFifty100TimesNeverLeavesLessThan2() {
        repeat(100) {
            startGame()
            engine.useHint()
            engine.activateFiftyFifty()
            val visible = visibleOptions().size
            val clickable = clickableOptions().size
            assertTrue("Visible should be >= 2 (attempt $it, got $visible)", visible >= 2)
            assertTrue("Clickable should be >= 2 (attempt $it, got $clickable)", clickable >= 2)
        }
    }

    @Test
    fun fun_stress_fiftyFiftyThenHint100TimesNeverLeavesLessThan2() {
        repeat(100) {
            startGame()
            engine.activateFiftyFifty()
            engine.useHint()
            val visible = visibleOptions().size
            val clickable = clickableOptions().size
            assertTrue("Visible should be >= 2 (attempt $it, got $visible)", visible >= 2)
            assertTrue("Clickable should be >= 2 (attempt $it, got $clickable)", clickable >= 2)
        }
    }
}
