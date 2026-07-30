package com.opoleyes.domain

import com.opoleyes.FakeGameRepository
import com.opoleyes.FakePreferencesManager
import com.opoleyes.FakeProgressRepository
import com.opoleyes.FakeStatsRepository
import com.opoleyes.data.model.GameMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameEngineTimerFlowTest {

    private lateinit var engine: GameEngine
    private lateinit var prefs: FakePreferencesManager

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
    fun fun_timer_initialChallengeTimerIs120() {
        engine.startChallengeGame()
        assertEquals("Challenge timer should start at 120", 120f, engine.timer, 0.01f)
    }

    @Test
    fun fun_timer_initialTimetrialTimerIs180() {
        engine.startAllLawsGame(GameMode.TIMETRIAL)
        assertEquals("Timetrial timer should start at 180", 180f, engine.timer, 0.01f)
    }

    @Test
    fun fun_timer_initialSurvivalTimerIs0() {
        engine.startAllLawsGame()
        assertEquals("Survival timer should start at 0", 0f, engine.timer, 0.01f)
    }

    @Test
    fun fun_timer_correctAnswerAddsTimeInChallenge() {
        engine.startChallengeGame()
        engine.nextQuestion()
        val timerBefore = engine.timer
        engine.answer(engine.currentQ!!.correct)
        assertTrue("Timer should increase on correct answer in challenge", engine.timer > timerBefore)
    }

    @Test
    fun fun_timer_wrongAnswerSubtractsTimeInChallenge() {
        engine.startChallengeGame()
        engine.shieldCharges = 0
        engine.nextQuestion()
        val timerBefore = engine.timer
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        engine.answer(wrong)
        assertTrue("Timer should decrease on wrong answer in challenge", engine.timer < timerBefore)
    }

    @Test
    fun fun_timer_correctAnswerAddsTimeInTimetrial() {
        engine.startAllLawsGame(GameMode.TIMETRIAL)
        engine.nextQuestion()
        val timerBefore = engine.timer
        engine.answer(engine.currentQ!!.correct)
        assertTrue("Timer should increase on correct in timetrial", engine.timer > timerBefore)
    }

    @Test
    fun fun_timer_wrongAnswerSubtractsTimeInTimetrial() {
        engine.startAllLawsGame(GameMode.TIMETRIAL)
        engine.shieldCharges = 0
        engine.nextQuestion()
        val timerBefore = engine.timer
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        engine.answer(wrong)
        assertTrue("Timer should decrease on wrong in timetrial", engine.timer < timerBefore)
    }

    @Test
    fun fun_timer_doesNotChangeInSurvivalMode() {
        engine.startAllLawsGame()
        engine.nextQuestion()
        val timerBefore = engine.timer
        engine.answer(engine.currentQ!!.correct)
        assertEquals("Timer should not change in survival mode", timerBefore, engine.timer, 0.01f)
    }

    @Test
    fun fun_timer_cappedAt300InTimetrial() {
        engine.startAllLawsGame(GameMode.TIMETRIAL)
        engine.timer = 295f
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertTrue("Timer should be capped at 300", engine.timer <= 300f)
    }

    @Test
    fun fun_timer_neverGoesNegative() {
        engine.startChallengeGame()
        engine.shieldCharges = 0
        engine.timer = 5f
        engine.nextQuestion()
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        engine.answer(wrong)
        assertTrue("Timer should never go below 0", engine.timer >= 0f)
    }

    @Test
    fun fun_timer_gameOverWhenTimerReachesZero() {
        engine.startChallengeGame()
        engine.timer = 0f
        engine.questionNum = 5
        assertTrue("Should be game over when timer is 0 in challenge", engine.isGameOver())
    }

    @Test
    fun fun_timer_gameOverWhenTimerReachesZeroInTimetrial() {
        engine.startAllLawsGame(GameMode.TIMETRIAL)
        engine.timer = 0f
        assertTrue("Should be game over when timer is 0 in timetrial", engine.isGameOver())
    }

    @Test
    fun fun_timer_streakAddsBonusTimeInChallenge() {
        engine.startChallengeGame()
        engine.streak = 4
        engine.nextQuestion()
        val timerBefore = engine.timer
        engine.answer(engine.currentQ!!.correct)
        assertTrue("Streak of 5 should add bonus time", engine.timer > timerBefore + 10f)
    }

    @Test
    fun fun_timer_streakAddsBonusTimeInTimetrial() {
        engine.startAllLawsGame(GameMode.TIMETRIAL)
        engine.streak = 4
        engine.nextQuestion()
        val timerBefore = engine.timer
        engine.answer(engine.currentQ!!.correct)
        assertTrue("Streak of 5 should add bonus time in timetrial", engine.timer > timerBefore + 10f)
    }

    @Test
    fun fun_timer_answeredFlagSetOnAnswer() {
        engine.startChallengeGame()
        engine.nextQuestion()
        assertFalse("Should not be answered before answering", engine.answered)
        engine.answer(engine.currentQ!!.correct)
        assertTrue("Should be answered after answering", engine.answered)
    }

    @Test
    fun fun_timer_answeredFlagClearedOnNextQuestion() {
        engine.startChallengeGame()
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertTrue("Should be answered", engine.answered)
        engine.nextQuestion()
        assertFalse("Should not be answered after nextQuestion", engine.answered)
    }

    @Test
    fun fun_timer_remainsPositiveAfterAnswerAndNextQuestion() {
        engine.startChallengeGame()
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertTrue("Timer should be positive after correct answer", engine.timer > 0f)
        engine.nextQuestion()
        assertTrue("Timer should still be positive after next question", engine.timer > 0f)
    }
}
