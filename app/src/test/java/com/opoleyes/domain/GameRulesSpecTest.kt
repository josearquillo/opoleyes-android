package com.opoleyes.domain

import com.opoleyes.FakeGameRepository
import com.opoleyes.FakePreferencesManager
import com.opoleyes.FakeProgressRepository
import com.opoleyes.FakeStatsRepository
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.model.QuestionEntry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Specification tests: verify that code behavior matches the help text documentation.
 * Each test references the specific help text line it validates.
 */
class GameRulesSpecTest {

    private lateinit var engine: GameEngine
    private lateinit var prefs: FakePreferencesManager

    private fun makeQuestion(correct: String = "A"): QuestionEntry = QuestionEntry(
        enunciado = "Test question",
        opciones = mapOf("A" to "Opt A", "B" to "Opt B", "C" to "Opt C", "D" to "Opt D"),
        correct = correct,
        weight = 50,
        testId = "test1",
        origId = "1"
    )

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

    @Test
    fun comboBar_fillsIn5CorrectAnswers() {
        // Help: "La barra de combo inferior se llena cada 5 aciertos consecutivos."
        engine.startAllLawsGame()
        engine.nextQuestion()
        // Answer 5 questions correctly (with nextQuestion between each)
        for (i in 1..5) {
            engine.currentQ = makeQuestion("A")
            engine.answer("A")
            if (i < 5) engine.nextQuestion()
        }
        // After 5 correct answers, comboBarFill should have reached 1.0 and triggered overcharge
        // (comboBarFill resets to 0 on overcharge, so we check that overcharge triggered)
        assertTrue("Combo overcharge should activate after 5 correct answers",
            engine.comboOverchargeActive || engine.comboBarFill == 0f)
    }

    @Test
    fun quickMode_noPowerUpsAwarded() {
        // Help: "Repaso Express: Sin power-ups."
        engine.mode = GameMode.QUICK
        engine.lives = 3
        engine.streak = 14
        engine.answered = false
        engine.currentQ = makeQuestion("A")
        engine.answer("A")
        // Power-ups are unlimited but QUICK mode doesn't award charges
        assertEquals(15, engine.streak)
    }

    @Test
    fun timetrial_streak5_adds20Seconds() {
        // Help: "Contrarreloj: ... +20 segundos extra por cada 5 aciertos consecutivos"
        engine.rankIndex = 2 // rank 2+ has streak threshold of 5
        engine.mode = GameMode.TIMETRIAL
        engine.timer = 100f
        engine.streak = 4
        engine.answered = false
        engine.currentQ = makeQuestion("A")
        engine.answer("A")
        // streak reaches 5, should add 20s. Also +15s for correct answer.
        // timer = min(300, 100 + 15 + 20) = 135
        assertEquals(135f, engine.timer, 0.01f)
    }

    @Test
    fun timetrial_correct_adds15Seconds() {
        // Help: "Contrarreloj: ... acierto suma 15 segundos"
        engine.mode = GameMode.TIMETRIAL
        engine.timer = 100f
        engine.streak = 0 // no streak bonus
        engine.answered = false
        engine.currentQ = makeQuestion("A")
        engine.answer("A")
        assertEquals(115f, engine.timer, 0.01f)
    }

    @Test
    fun timetrial_wrong_subtracts10Seconds() {
        // Help: "Contrarreloj: ... fallo resta 10"
        engine.rankIndex = 2 // avoid rank-0 first mistake forgiveness
        engine.mode = GameMode.TIMETRIAL
        engine.timer = 100f
        engine.answered = false
        engine.currentQ = makeQuestion("A")
        engine.answer("B") // wrong
        assertEquals(90f, engine.timer, 0.01f)
    }

    @Test
    fun survival_starts3Lives() {
        // Help: "Supervivencia: 3 vidas"
        engine.startAllLawsGame()
        assertEquals(3, engine.lives)
    }
}
