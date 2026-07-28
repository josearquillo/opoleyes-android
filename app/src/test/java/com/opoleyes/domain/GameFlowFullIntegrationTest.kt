package com.opoleyes.domain

import com.opoleyes.TestContextProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameFlowFullIntegrationTest {

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

    @Test
    fun fun_fullGameFlow_survivalAnswerAllCorrectUntilGameOver() {
        engine.startAllLawsGame()
        engine.initGameStats()
        engine.nextQuestion()
        var answeredCount = 0
        while (!engine.isGameOver()) {
            assertNotNull("Question should be loaded", engine.currentQ)
            engine.answer(engine.currentQ!!.correct)
            assertTrue("Should be answered", engine.answered)
            answeredCount++
            if (!engine.nextQuestion()) break
        }
        assertTrue("Should have answered at least 1 question", answeredCount > 0)
        assertTrue("Score should be positive after correct answers", engine.score > 0)
        assertTrue("Combo should be positive", engine.maxCombo > 0)
    }

    @Test
    fun fun_fullGameFlow_survivalLoseAllLives() {
        engine.startAllLawsGame()
        engine.shieldCharges = 0
        engine.initGameStats()
        engine.nextQuestion()
        while (!engine.isGameOver() && engine.lives > 0) {
            val q = engine.currentQ ?: break
            val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
            engine.answer(wrong)
            if (engine.isGameOver()) break
            engine.nextQuestion()
        }
        assertTrue("Should be game over after losing all lives", engine.isGameOver())
        assertEquals("Score should be 0 after all wrong", 0, engine.score)
    }

    @Test
    fun fun_fullGameFlow_challengeAnswerUntilTimeOrQuestionsEnd() {
        engine.startChallengeGame()
        engine.initGameStats()
        engine.nextQuestion()
        var answeredCount = 0
        while (!engine.isGameOver()) {
            assertNotNull("Question should be loaded", engine.currentQ)
            engine.answer(engine.currentQ!!.correct)
            answeredCount++
            if (!engine.nextQuestion()) break
        }
        assertTrue("Should have answered at least 1 question", answeredCount > 0)
        assertTrue("Should be game over", engine.isGameOver())
    }

    @Test
    fun fun_fullGameFlow_quickModeAnswer20Questions() {
        engine.startQuickGame()
        engine.initGameStats()
        engine.nextQuestion()
        var answeredCount = 0
        while (!engine.isGameOver()) {
            assertNotNull("Question should be loaded", engine.currentQ)
            engine.answer(engine.currentQ!!.correct)
            answeredCount++
            if (!engine.nextQuestion()) break
        }
        assertEquals("Quick mode should have 20 questions", 20, answeredCount)
        assertTrue("Should be game over", engine.isGameOver())
    }

    @Test
    fun fun_fullGameFlow_mixCorrectAndWrong() {
        engine.startAllLawsGame()
        engine.shieldCharges = 0
        engine.initGameStats()
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        val scoreAfterCorrect = engine.score
        assertTrue("Score should increase on correct", scoreAfterCorrect > 0)
        engine.nextQuestion()
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        engine.answer(wrong)
        assertEquals("Combo should reset on wrong", 0, engine.combo)
    }

    @Test
    fun fun_fullGameFlow_shieldPreventsLifeLoss() {
        engine.startAllLawsGame()
        engine.shieldCharges = 1
        engine.initGameStats()
        engine.nextQuestion()
        val livesBefore = engine.lives
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        val result = engine.answer(wrong)
        assertEquals("Should use shield", GameEngine.AnswerResult.SHIELD_USED, result)
        assertEquals("Lives should not decrease when shield is used", livesBefore, engine.lives)
        assertEquals("Shield charges should decrease", 0, engine.shieldCharges)
    }

    @Test
    fun fun_fullGameFlow_doubleScoreDoublesPoints() {
        engine.startAllLawsGame()
        engine.initGameStats()
        engine.nextQuestion()
        engine.doubleScoreCharges = 1
        engine.activateDoubleScore()
        assertTrue("Double score should be active", engine.doubleScoreActive)
        val scoreBefore = engine.score
        engine.answer(engine.currentQ!!.correct)
        assertTrue("Score with double should be at least 20 more", engine.score >= scoreBefore + 20)
        assertFalse("Double score should be consumed after correct answer", engine.doubleScoreActive)
    }

    @Test
    fun fun_fullGameFlow_fiftyFiftyRemovesTwoOptions() {
        engine.startAllLawsGame()
        engine.initGameStats()
        engine.fiftyFiftyCharges = 1
        engine.nextQuestion()
        engine.activateFiftyFifty()
        assertTrue("Fifty fifty should be active", engine.fiftyFiftyActive)
        assertEquals("Should remove 2 options", 2, engine.fiftyFiftyRemoved.size)
        assertFalse("Correct answer should not be removed", engine.fiftyFiftyRemoved.contains(engine.currentQ!!.correct))
    }

    @Test
    fun fun_fullGameFlow_hintRemovesOneWrongOption() {
        engine.startAllLawsGame()
        engine.initGameStats()
        engine.hintCharges = 1
        engine.nextQuestion()
        engine.useHint()
        assertTrue("Hint should be active", engine.hintActive)
        assertEquals("Should remove 1 option", 1, engine.hintRemoved.size)
        assertFalse("Correct answer should not be removed by hint", engine.hintRemoved.contains(engine.currentQ!!.correct))
    }

    @Test
    fun fun_fullGameFlow_comboOverchargeActivatesAt10() {
        engine.startAllLawsGame()
        engine.initGameStats()
        engine.nextQuestion()
        for (i in 1..10) {
            assertNotNull(engine.currentQ)
            engine.answer(engine.currentQ!!.correct)
            if (i < 10) engine.nextQuestion()
        }
        assertTrue("Combo should be 10", engine.combo == 10)
        assertTrue("Combo bar should be full or overcharge active", engine.comboBarFill >= 1f || engine.comboOverchargeActive)
    }

    @Test
    fun fun_fullGameFlow_saveRemainingPowerUpsPreservesUnused() {
        engine.startAllLawsGame()
        engine.initGameStats()
        engine.shieldCharges = 2
        engine.fiftyFiftyCharges = 1
        engine.saveRemainingPowerUps()
        val saved = prefs.getFreePowerUps()
        assertTrue("Should have saved shield charges", saved.count { it == "shield" } == 2)
        assertTrue("Should have saved fiftyFifty charges", saved.count { it == "fiftyFifty" } == 1)
    }
}
