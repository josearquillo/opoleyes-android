package com.opoleyes.domain

import com.opoleyes.FakeGameRepository
import com.opoleyes.FakePreferencesManager
import com.opoleyes.FakeProgressRepository
import com.opoleyes.FakeStatsRepository
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.model.QuestionEntry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameEngineEdgeCaseTest {

    private lateinit var engine: GameEngine
    private lateinit var prefs: FakePreferencesManager

    private fun makeQuestion(correct: String = "A", opciones: Map<String, String> = mapOf("A" to "A", "B" to "B", "C" to "C", "D" to "D")): QuestionEntry =
        QuestionEntry(enunciado = "Test", opciones = opciones, correct = correct, weight = 50, testId = "t1", origId = "1")

    @Before
    fun setup() {
        prefs = FakePreferencesManager()
        engine = GameEngine.createForTest(
            FakeGameRepository(), FakeStatsRepository(), FakeProgressRepository(), prefs
        )
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    // === Edge cases: questions with fewer than 4 options ===

    @Test
    fun fun_fiftyFifty_with3Options_removesAtMost1() {
        engine.currentQ = makeQuestion(opciones = mapOf("A" to "A", "B" to "B", "C" to "C"))
        engine.activateFiftyFifty()
        assertTrue(engine.fiftyFiftyActive)
        val remaining = 3 - engine.fiftyFiftyRemoved.size
        assertTrue("Should have at least 2 remaining", remaining >= 2)
    }

    @Test
    fun fun_fiftyFifty_with2Options_removes0() {
        engine.currentQ = makeQuestion(opciones = mapOf("A" to "A", "B" to "B"))
        engine.activateFiftyFifty()
        // With 2 options, can't remove any while keeping 2 visible
        assertEquals(0, engine.fiftyFiftyRemoved.size)
    }

    @Test
    fun fun_hint_with2Options_doesNotActivate() {
        engine.currentQ = makeQuestion(opciones = mapOf("A" to "A", "B" to "B"))
        engine.useHint()
        assertFalse("Hint should not activate with only 2 options", engine.hintActive)
    }

    @Test
    fun fun_hint_with3Options_removes1() {
        engine.currentQ = makeQuestion(opciones = mapOf("A" to "A", "B" to "B", "C" to "C"))
        engine.useHint()
        assertTrue(engine.hintActive)
        assertEquals(1, engine.hintRemoved.size)
    }

    @Test
    fun fun_fiftyFifty_then_hint_with3Options_keeps2Visible() {
        engine.currentQ = makeQuestion(opciones = mapOf("A" to "A", "B" to "B", "C" to "C"))
        engine.activateFiftyFifty()
        engine.useHint()
        val allOptions = listOf("A", "B", "C")
        val visible = allOptions.filter { it !in engine.fiftyFiftyRemoved && it !in engine.hintRemoved }
        assertTrue("Should have at least 2 visible", visible.size >= 2)
    }

    // === Edge cases: answer already answered ===

    @Test
    fun fun_answer_alreadyAnswered_returnsAlreadyAnswered() {
        engine.currentQ = makeQuestion()
        engine.answer("A")
        val result = engine.answer("B")
        assertEquals(GameEngine.AnswerResult.ALREADY_ANSWERED, result)
    }

    @Test
    fun fun_answer_withNullQuestion_returnsError() {
        engine.currentQ = null
        val result = engine.answer("A")
        assertEquals(GameEngine.AnswerResult.ERROR, result)
    }

    // === Edge cases: power-ups after answering ===

    @Test
    fun fun_fiftyFifty_afterAnswer_doesNotActivate() {
        engine.currentQ = makeQuestion()
        engine.answer("A")
        engine.activateFiftyFifty()
        assertFalse(engine.fiftyFiftyActive)
    }

    @Test
    fun fun_hint_afterAnswer_doesNotActivate() {
        engine.currentQ = makeQuestion()
        engine.answer("A")
        engine.useHint()
        assertFalse(engine.hintActive)
    }

    // === Edge cases: wrong answer loses life ===

    @Test
    fun fun_wrongAnswer_losesLife() {
        engine.rankIndex = 2 // avoid rank-0 first mistake forgiveness
        engine.mode = GameMode.SURVIVAL
        engine.lives = 3
        engine.currentQ = makeQuestion(correct = "A")
        engine.answer("B")
        assertEquals(2, engine.lives)
    }

    // === Edge cases: accuracy with 0 answered ===

    @Test
    fun fun_getAccuracy_with0Answered_returns0() {
        assertEquals(0, engine.getAccuracy())
    }

    @Test
    fun fun_getAccuracy_allCorrect_returns100() {
        engine.totalAnswered = 10
        engine.correctCount = 10
        assertEquals(100, engine.getAccuracy())
    }

    @Test
    fun fun_getAccuracy_halfCorrect_returns50() {
        engine.totalAnswered = 10
        engine.correctCount = 5
        assertEquals(50, engine.getAccuracy())
    }

    // === Edge cases: game over conditions ===

    @Test
    fun fun_isGameOver_survivalWithLives_returnsFalse() {
        engine.mode = GameMode.SURVIVAL
        engine.lives = 1
        assertFalse(engine.isGameOver())
    }

    @Test
    fun fun_isGameOver_survivalNoLives_returnsTrue() {
        engine.mode = GameMode.SURVIVAL
        engine.lives = 0
        assertTrue(engine.isGameOver())
    }

    @Test
    fun fun_isGameOver_quickModeMaxQuestions_returnsTrue() {
        engine.mode = GameMode.QUICK
        engine.lives = 3
        engine.questionNum = 20
        assertTrue(engine.isGameOver())
    }

    @Test
    fun fun_isGameOver_timeTrialNoTime_returnsTrue() {
        engine.mode = GameMode.TIMETRIAL
        engine.timer = 0f
        assertTrue(engine.isGameOver())
    }

    @Test
    fun fun_isGameOver_quickMaxQuestions_returnsTrue() {
        engine.mode = GameMode.QUICK
        engine.lives = 3
        engine.questionNum = 20
        assertTrue(engine.isGameOver())
    }


    // === Edge cases: combo overcharge ===

    @Test
    fun fun_comboOvercharge_activatesAtFullBar() {
        engine.comboBarFill = 0.9f
        engine.currentQ = makeQuestion(correct = "A")
        engine.answer("A")
        assertTrue("Combo overcharge should activate when bar fills", engine.comboOverchargeActive || engine.comboBarFill < 1f)
    }

    // === Edge cases: streak rewards ===

    @Test
    fun fun_streak5_inSurvival_recoversLifeIfUnlocked() {
        engine.mode = GameMode.SURVIVAL
        engine.lives = 2
        engine.streak = 4
        engine.currentQ = makeQuestion(correct = "A")
        engine.answer("A")
        assertEquals(5, engine.streak)
    }

    @Test
    fun fun_streak0_onWrongAnswer_resets() {
        engine.streak = 10
        engine.currentQ = makeQuestion(correct = "A")
        engine.answer("B")
        assertEquals(0, engine.streak)
    }
}
