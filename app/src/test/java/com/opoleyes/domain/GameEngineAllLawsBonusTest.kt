package com.opoleyes.domain

import com.opoleyes.FakeGameRepository
import com.opoleyes.FakePreferencesManager
import com.opoleyes.FakeProgressRepository
import com.opoleyes.FakeStatsRepository
import com.opoleyes.data.model.GameMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for the +25% points bonus when playing Survival on all laws.
 */
class GameEngineAllLawsBonusTest {

    private lateinit var engine: GameEngine
    private lateinit var prefs: FakePreferencesManager
    private lateinit var progressRepo: FakeProgressRepository
    private lateinit var statsRepo: FakeStatsRepository
    private lateinit var gameRepo: FakeGameRepository

    @Before
    fun setup() {
        prefs = FakePreferencesManager()
        progressRepo = FakeProgressRepository()
        statsRepo = FakeStatsRepository()
        gameRepo = FakeGameRepository()
        engine = GameEngine.createForTest(gameRepo, statsRepo, progressRepo, prefs)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    /** Answer the current question correctly. */
    private fun answerCorrect() {
        val correct = engine.currentQ!!.correct
        engine.answer(correct)
    }

    @Test
    fun survival_allLaws_givesBonusPoints() {
        engine.startAllLawsGame(GameMode.SURVIVAL)
        engine.nextQuestion()
        answerCorrect()
        val ptsEarned = engine.lastPtsEarned
        // Base pts for combo=1 in Survival = 10 * 1 = 10
        // With x1.25 bonus = 12 (10 * 1.25 = 12.5 -> 12)
        assertTrue("Should earn more than base 10 pts with all-laws bonus, got $ptsEarned", ptsEarned > 10)
    }

    @Test
    fun survival_singleLaw_noBonusPoints() {
        engine.startTemaGame("test1", GameMode.SURVIVAL)
        engine.nextQuestion()
        answerCorrect()
        val ptsEarned = engine.lastPtsEarned
        // Base pts for combo=1 in Survival = 10 * 1 = 10, no bonus
        assertEquals("Single law should give base 10 pts", 10, ptsEarned)
    }

    @Test
    fun timetrial_allLaws_noBonusPoints() {
        engine.startAllLawsGame(GameMode.TIMETRIAL)
        engine.nextQuestion()
        answerCorrect()
        val ptsEarned = engine.lastPtsEarned
        // Base pts for combo=1 in non-QUICK = 10 * 1 = 10, no all-laws bonus in Timetrial
        assertEquals("Timetrial should not get all-laws bonus", 10, ptsEarned)
    }

    @Test
    fun quick_allLaws_noBonusPoints() {
        engine.startQuickGame()
        engine.nextQuestion()
        answerCorrect()
        val ptsEarned = engine.lastPtsEarned
        // Base pts for combo=1 in QUICK = 15 * 1 = 15, no all-laws bonus
        assertEquals("Quick mode should not get all-laws bonus", 15, ptsEarned)
    }

    @Test
    fun survival_allLaws_bonusScalesWithCombo() {
        engine.startAllLawsGame(GameMode.SURVIVAL)
        // Answer 3 correct to build combo
        engine.nextQuestion(); answerCorrect() // combo=1, pts = 10*1*1.25 = 12
        val pts1 = engine.lastPtsEarned
        engine.nextQuestion(); answerCorrect() // combo=2, pts = 10*2*1.25 = 25
        val pts2 = engine.lastPtsEarned
        engine.nextQuestion(); answerCorrect() // combo=3, pts = 10*3*1.25 = 37
        val pts3 = engine.lastPtsEarned

        assertTrue("Combo 2 should earn more than combo 1 ($pts1 vs $pts2)", pts2 > pts1)
        assertTrue("Combo 3 should earn more than combo 2 ($pts2 vs $pts3)", pts3 > pts2)
        // Without bonus it would be 30
        assertTrue("Combo 3 with bonus should be > 30 base, got $pts3", pts3 > 30)
    }
}
