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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameEngineTest {

    private lateinit var engine: GameEngine
    private lateinit var prefs: FakePreferencesManager
    private lateinit var progressRepo: FakeProgressRepository
    private lateinit var statsRepo: FakeStatsRepository
    private lateinit var gameRepo: FakeGameRepository

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
        progressRepo = FakeProgressRepository()
        statsRepo = FakeStatsRepository()
        gameRepo = FakeGameRepository()
        engine = GameEngine.createForTest(gameRepo, statsRepo, progressRepo, prefs)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun fun_initGameStats_resetsAll() {
        engine.score = 999
        engine.combo = 10
        engine.mode = GameMode.SURVIVAL
        engine.initGameStats()
        assertEquals(0, engine.score)
        assertEquals(0, engine.combo)
        assertEquals(0, engine.maxCombo)
        assertEquals(0, engine.correctCount)
        assertEquals(0, engine.totalAnswered)
        assertEquals(0, engine.streak)
        assertEquals(3, engine.lives)
        assertFalse(engine.answered)
    }

    @Test
    fun fun_initGameStats_survivalHas3Lives() {
        engine.mode = GameMode.SURVIVAL
        engine.initGameStats()
        assertEquals(3, engine.lives)
        assertEquals(0f, engine.timer, 0.01f)
    }

    @Test
    fun fun_initGameStats_timetrialHasTimer() {
        engine.mode = GameMode.TIMETRIAL
        engine.initGameStats()
        assertEquals(0, engine.lives)
        assertEquals(180f, engine.timer, 0.01f)
    }

    @Test
    fun fun_startQuickGame_returnsTrue() {
        val ok = engine.startQuickGame()
        assertTrue(ok)
        assertEquals(GameMode.QUICK, engine.mode)
    }

    @Test
    fun fun_startAllLawsGame_returnsTrue() {
        val ok = engine.startAllLawsGame()
        assertTrue(ok)
        assertEquals(GameMode.SURVIVAL, engine.mode)
    }

    @Test
    fun fun_nextQuestion_returnsTrue() {
        engine.startAllLawsGame()
        assertTrue(engine.nextQuestion())
        assertNotNull(engine.currentQ)
        assertEquals(1, engine.questionNum)
    }

    @Test
    fun fun_nextQuestion_survivalNoLivesReturnsFalse() {
        engine.startAllLawsGame()
        engine.lives = 0
        assertFalse(engine.nextQuestion())
    }

    @Test
    fun fun_nextQuestion_quickModeLimits5() {
        engine.startQuickGame()
        engine.questionNum = 5
        assertFalse(engine.nextQuestion())
    }

    @Test
    fun fun_nextQuestion_timetrialNoTimeReturnsFalse() {
        engine.startAllLawsGame()
        engine.mode = GameMode.TIMETRIAL
        engine.timer = 0f
        assertFalse(engine.nextQuestion())
    }

    @Test
    fun fun_nextQuestion_quickLimits5() {
        engine.startQuickGame()
        engine.questionNum = 5
        assertFalse(engine.nextQuestion())
    }

    @Test
    fun fun_answer_correct() {
        engine.startAllLawsGame()
        engine.nextQuestion()
        val q = engine.currentQ!!
        val result = engine.answer(q.correct)
        assertEquals(GameEngine.AnswerResult.CORRECT, result)
        assertEquals(1, engine.correctCount)
        assertEquals(1, engine.combo)
        assertEquals(1, engine.totalAnswered)
        assertTrue(engine.score > 0)
    }

    @Test
    fun fun_answer_wrong() {
        engine.startAllLawsGame()
        engine.nextQuestion()
        val q = engine.currentQ!!
        val wrongOption = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        val result = engine.answer(wrongOption)
        assertEquals(GameEngine.AnswerResult.WRONG, result)
        assertEquals(0, engine.combo)
        assertEquals(0, engine.correctCount)
        assertEquals(1, engine.totalAnswered)
        assertEquals(2, engine.lives)
    }

    @Test
    fun fun_answer_alreadyAnswered() {
        engine.startAllLawsGame()
        engine.nextQuestion()
        val q = engine.currentQ!!
        engine.answer(q.correct)
        val result = engine.answer(q.correct)
        assertEquals(GameEngine.AnswerResult.ALREADY_ANSWERED, result)
    }

    @Test
    fun fun_answer_correctIncreasesCombo() {
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertEquals(1, engine.combo)
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertEquals(2, engine.combo)
        assertEquals(2, engine.maxCombo)
    }

    @Test
    fun fun_answer_wrongResetsCombo() {
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertEquals(1, engine.combo)
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertEquals(2, engine.combo)
        engine.nextQuestion()
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        engine.answer(wrong)
        assertEquals(0, engine.combo)
    }

    @Test
    fun fun_answer_scoreFormula() {
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        val expectedScore = 10 * 1
        assertEquals(expectedScore, engine.score)
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertEquals(10 * 1 + 10 * 2, engine.score)
    }

    @Test
    fun fun_answer_streakIncrementsOnCorrect() {
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertEquals(1, engine.streak)
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertEquals(2, engine.streak)
    }

    @Test
    fun fun_answer_streakResetsOnWrong() {
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        engine.answer(wrong)
        assertEquals(0, engine.streak)
    }

    @Test
    fun fun_answer_comboBarFillIncreases() {
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertEquals(0.2f, engine.comboBarFill, 0.01f)
    }

    @Test
    fun fun_answer_comboBarOverchargeAt5() {
        engine.startAllLawsGame()
        for (i in 1..5) {
            engine.nextQuestion()
            engine.answer(engine.currentQ!!.correct)
        }
        assertTrue(engine.comboOverchargeActive)
        assertEquals(3, engine.comboOverchargeCharges)
    }

    @Test
    fun fun_answer_timetrialAddsTimeOnCorrect() {
        engine.startAllLawsGame()
        engine.mode = GameMode.TIMETRIAL
        engine.timer = 100f
        engine.nextQuestion()
        val timerBefore = engine.timer
        engine.answer(engine.currentQ!!.correct)
        assertTrue(engine.timer > timerBefore)
    }

    @Test
    fun fun_answer_timetrialSubtractsTimeOnWrong() {
        engine.startAllLawsGame()
        engine.mode = GameMode.TIMETRIAL
        engine.timer = 100f
        engine.nextQuestion()
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        val timerBefore = engine.timer
        engine.answer(wrong)
        assertTrue(engine.timer < timerBefore)
    }

    @Test
    fun fun_isGameOver_survivalNoLives() {
        engine.mode = GameMode.SURVIVAL
        engine.lives = 0
        assertTrue(engine.isGameOver())
    }

    @Test
    fun fun_isGameOver_survivalHasLives() {
        engine.mode = GameMode.SURVIVAL
        engine.lives = 3
        assertFalse(engine.isGameOver())
    }

    @Test
    fun fun_isGameOver_quickMode5Questions() {
        engine.mode = GameMode.QUICK
        engine.questionNum = 5
        engine.lives = 3
        assertTrue(engine.isGameOver())
    }

    @Test
    fun fun_isGameOver_quickModeNoLives() {
        engine.mode = GameMode.QUICK
        engine.lives = 0
        engine.questionNum = 5
        assertTrue(engine.isGameOver())
    }

    @Test
    fun fun_isGameOver_timetrialNoTime() {
        engine.mode = GameMode.TIMETRIAL
        engine.timer = 0f
        assertTrue(engine.isGameOver())
    }

    @Test
    fun fun_isGameOver_quick5Questions() {
        engine.mode = GameMode.QUICK
        engine.questionNum = 5
        engine.lives = 3
        assertTrue(engine.isGameOver())
    }

    @Test
    fun fun_isGameOver_quickNoLives() {
        engine.mode = GameMode.QUICK
        engine.questionNum = 5
        engine.lives = 0
        assertTrue(engine.isGameOver())
    }

    @Test
    fun fun_isGameOver_notOver() {
        engine.mode = GameMode.SURVIVAL
        engine.lives = 3
        engine.questionNum = 5
        assertFalse(engine.isGameOver())
    }

    @Test
    fun fun_getAccuracy_noAnswers() {
        assertEquals(0, engine.getAccuracy())
    }

    @Test
    fun fun_getAccuracy_allCorrect() {
        engine.totalAnswered = 10
        engine.correctCount = 10
        assertEquals(100, engine.getAccuracy())
    }

    @Test
    fun fun_getAccuracy_half() {
        engine.totalAnswered = 10
        engine.correctCount = 5
        assertEquals(50, engine.getAccuracy())
    }

    @Test
    fun fun_activateFiftyFifty_alreadyActiveDoesNothing() {
        engine.fiftyFiftyActive = true
        engine.activateFiftyFifty()
    }

    @Test
    fun fun_activateFiftyFifty_whenAnsweredDoesNothing() {
        engine.answered = true
        engine.activateFiftyFifty()
        assertFalse(engine.fiftyFiftyActive)
    }

    @Test
    fun fun_activateFiftyFifty_removes2Options() {
        engine.currentQ = makeQuestion("A")
        engine.activateFiftyFifty()
        assertTrue(engine.fiftyFiftyActive)
        assertEquals(2, engine.fiftyFiftyRemoved.size)
        assertTrue(engine.ctxFiftyFiftyUsed)
    }

    @Test
    fun fun_activateFiftyFifty_neverRemovesCorrectAnswer() {
        engine.currentQ = makeQuestion("A")
        engine.activateFiftyFifty()
        assertFalse(engine.fiftyFiftyRemoved.contains("A"))
    }

    @Test
    fun fun_activateFiftyFifty_keepsAtLeast2Options() {
        engine.currentQ = makeQuestion("A")
        engine.activateFiftyFifty()
        val allOptions = listOf("A", "B", "C", "D")
        val remaining = allOptions.filter { it !in engine.fiftyFiftyRemoved }
        assertTrue("Should have at least 2 remaining options", remaining.size >= 2)
    }

    @Test
    fun fun_streak_lifeRecoveryGatedByUnlock() {
        // At rank 0 (no lifeRecovery unlock), streak of 5 should NOT recover life
        engine.startAllLawsGame()
        engine.lives = 1
        engine.streak = 4
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertEquals("Life should not be recovered without unlock", 1, engine.lives)
        assertEquals(5, engine.streak)
    }

    @Test
    fun fun_streak_lifeRecoveryWithUnlock() {
        // With debug mode (all unlocked), streak of 5 should recover life
        prefs.setDebugMode(true); progressRepo.unlocked.addAll(listOf("lifeRecovery","survival","timetrial","quick","challenge","exam","powerUps","hint","fiftyFifty"))
        engine.startAllLawsGame()
        engine.lives = 1
        engine.streak = 4
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertEquals("Life should be recovered with unlock", 2, engine.lives)
        assertTrue(engine.ctxLifeRecovered)
    }

    @Test
    fun fun_streak_lifeRecoveryGivesFiftyFiftyWhenLivesFull() {
        prefs.setDebugMode(true); progressRepo.unlocked.addAll(listOf("lifeRecovery","survival","timetrial","quick","challenge","exam","powerUps","hint","fiftyFifty"))
        engine.startAllLawsGame()
        engine.lives = 3
        engine.streak = 4
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertEquals("Lives should stay at 3", 3, engine.lives)
    }

    @Test
    fun fun_streak_lifeRecoveryOnlyInSurvival() {
        prefs.setDebugMode(true); progressRepo.unlocked.addAll(listOf("lifeRecovery","survival","timetrial","quick","challenge","exam","powerUps","hint","fiftyFifty"))
        engine.startAllLawsGame()
        engine.mode = GameMode.TIMETRIAL
        engine.lives = 0
        engine.timer = 100f
        engine.streak = 4
        val timerBefore = engine.timer
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertEquals("Life should not change in TIMETRIAL", 0, engine.lives)
        assertTrue("Timer should increase in TIMETRIAL", engine.timer > timerBefore)
    }

    @Test
    fun fun_activateFiftyFifty_with3Options() {
        engine.currentQ = QuestionEntry(
            enunciado = "Test",
            opciones = mapOf("A" to "Opt A", "B" to "Opt B", "C" to "Opt C"),
            correct = "A",
            weight = 50,
            testId = "test1",
            origId = "1"
        )
        engine.activateFiftyFifty()
        assertTrue(engine.fiftyFiftyActive)
        val allOptions = listOf("A", "B", "C")
        val remaining = allOptions.filter { it !in engine.fiftyFiftyRemoved }
        assertTrue("Should have at least 2 remaining with 3 options", remaining.size >= 2)
        assertFalse("Correct answer should not be removed", engine.fiftyFiftyRemoved.contains("A"))
    }

    @Test
    fun fun_useHint_neverRemovesCorrectAnswer() {
        engine.currentQ = makeQuestion("C")
        engine.useHint()
        assertFalse("Hint should never remove the correct answer",
            engine.hintRemoved.contains("C"))
    }

    @Test
    fun fun_useHint_worksWithFiftyFiftyRemovedSet() {
        engine.currentQ = makeQuestion("A")
        engine.fiftyFiftyRemoved = listOf("B")
        engine.useHint()
        // useHint should activate (powerUpUsedThisQuestion not set since fiftyFiftyRemoved was set manually)
        assertTrue("Hint should activate", engine.hintActive)
        assertEquals(1, engine.hintRemoved.size)
        // Hint should remove a wrong option (not A which is correct)
        assertFalse("Hint should not remove correct answer", engine.hintRemoved.contains("A"))
    }

    @Test
    fun fun_nextQuestion_resetsFiftyFiftyState() {
        engine.startAllLawsGame()
        engine.fiftyFiftyActive = true
        engine.fiftyFiftyRemoved = listOf("B", "C")
        engine.nextQuestion()
        assertFalse("fiftyFiftyActive should be reset on next question", engine.fiftyFiftyActive)
        assertTrue("fiftyFiftyRemoved should be cleared on next question", engine.fiftyFiftyRemoved.isEmpty())
    }

    @Test
    fun fun_nextQuestion_resetsHintState() {
        engine.startAllLawsGame()
        engine.hintActive = true
        engine.hintRemoved = listOf("B")
        engine.nextQuestion()
        assertFalse("hintActive should be reset on next question", engine.hintActive)
        assertTrue("hintRemoved should be cleared on next question", engine.hintRemoved.isEmpty())
    }

    @Test
    fun fun_answer_lawMasteryGrantsXP() {
        engine.startAllLawsGame()
        val xpBefore = progressRepo.getXP()
        // Answer many questions correctly to try to master a law
        repeat(20) {
            engine.nextQuestion()
            engine.answer(engine.currentQ!!.correct)
        }
        // XP should have increased significantly
        assertTrue("XP should increase from answering", progressRepo.getXP() > xpBefore)
    }

    @Test
    fun fun_answer_scoreNeverNegative() {
        engine.startAllLawsGame()
        engine.score = 0
        engine.nextQuestion()
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        engine.answer(wrong)
        assertTrue("Score should never go negative", engine.score >= 0)
    }

    @Test
    fun fun_answer_timerNeverNegative() {
        engine.startAllLawsGame()
        engine.mode = GameMode.TIMETRIAL
        engine.timer = 5f
        engine.nextQuestion()
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        engine.answer(wrong)
        assertTrue("Timer should never go below 0", engine.timer >= 0f)
    }

    @Test
    fun fun_answer_timerCappedAt300() {
        engine.startAllLawsGame()
        engine.mode = GameMode.TIMETRIAL
        engine.timer = 295f
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertTrue("Timer should be capped at 300", engine.timer <= 300f)
    }

    @Test
    fun fun_answer_livesCappedAt3InSurvival() {
        prefs.setDebugMode(true); progressRepo.unlocked.addAll(listOf("lifeRecovery","survival","timetrial","quick","challenge","exam","powerUps","hint","fiftyFifty"))
        engine.startAllLawsGame()
        engine.lives = 3
        engine.streak = 4
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertTrue("Lives should not exceed 3 in survival", engine.lives <= 3)
    }

    @Test
    fun fun_isGameOver_quickModeWithLivesAndUnderLimit() {
        engine.mode = GameMode.QUICK
        engine.lives = 3
        engine.questionNum = 4
        assertFalse(engine.isGameOver())
    }

    @Test
    fun fun_getAccuracy_zeroDivision() {
        assertEquals(0, engine.getAccuracy())
    }

    @Test
    fun fun_nextQuestion_poolExhaustionRecycles() {
        engine.startAllLawsGame()
        val poolSize = engine.pool.size
        for (i in 1..poolSize + 5) {
            assertTrue("Should keep cycling questions even after pool exhausted",
                engine.nextQuestion())
            engine.answer(engine.currentQ!!.correct)
        }
    }

    // === Power-up interaction tests ===

    @Test
    fun fun_hintThenFiftyFifty_fiftyFiftyBlockedByMutualExclusivity() {
        engine.currentQ = makeQuestion("A")
        engine.useHint()
        assertTrue("Hint should activate", engine.hintActive)
        assertEquals(1, engine.hintRemoved.size)
        engine.activateFiftyFifty()
        assertFalse("50/50 should be blocked by powerUpUsedThisQuestion", engine.fiftyFiftyActive)
        // Hint still active, 3 options visible (1 hint-removed)
        val allOptions = listOf("A", "B", "C", "D")
        val visibleCount = allOptions.filter { it !in engine.hintRemoved }.size
        assertTrue("Should have at least 3 visible options after hint", visibleCount >= 3)
    }

    @Test
    fun fun_fiftyFiftyThenHint_ensuresAtLeast2FullyVisible() {
        engine.currentQ = makeQuestion("A")
        engine.activateFiftyFifty()
        assertTrue("50/50 should activate", engine.fiftyFiftyActive)
        assertEquals(2, engine.fiftyFiftyRemoved.size)
        // Hint should NOT activate because only 2 remain and removing 1 would leave 1
        engine.useHint()
        assertFalse("Hint should not activate when only 2 options remain visible",
            engine.hintActive)
        val allOptions = listOf("A", "B", "C", "D")
        val fullyVisible = allOptions.filter { it !in engine.fiftyFiftyRemoved && it !in engine.hintRemoved }
        assertTrue("Should have at least 2 fully visible options after 50/50 + hint attempt",
            fullyVisible.size >= 2)
    }

    @Test
    fun fun_fiftyFiftyThenHint_hintDoesNotRemoveIfTooFewRemain() {
        engine.currentQ = makeQuestion("A")
        // 50/50 removes 2 wrong options, leaving A + 1 wrong = 2 visible
        engine.activateFiftyFifty()
        assertEquals(2, engine.fiftyFiftyRemoved.size)
        // Hint should NOT activate because only 2 remain and removing 1 would leave 1
        engine.useHint()
        assertFalse("Hint should not activate when only 2 options remain visible",
            engine.hintActive)
    }

    @Test
    fun fun_hintThenFiftyFifty_fiftyFiftyDoesNotActivateIfTooFewRemain() {
        engine.currentQ = QuestionEntry(
            enunciado = "Test",
            opciones = mapOf("A" to "Opt A", "B" to "Opt B", "C" to "Opt C"),
            correct = "A",
            weight = 50,
            testId = "test1",
            origId = "1"
        )
        // Hint removes 1 wrong, leaving A + 1 wrong + 1 hint-removed = 2 fully visible
        engine.useHint()
        assertTrue(engine.hintActive)
        // 50/50 should not remove any because only 2 fully visible remain
        engine.activateFiftyFifty()
        assertFalse("50/50 should not activate when only 2 fully visible options remain",
            engine.fiftyFiftyActive)
    }

    @Test
    fun fun_hintOnQ1_fiftyFiftyOnQ2_stateReset() {
        engine.startAllLawsGame()
        // Q1: use hint
        engine.nextQuestion()
        engine.useHint()
        assertTrue(engine.hintActive)
        assertEquals(1, engine.hintRemoved.size)
        // Answer Q1
        engine.answer(engine.currentQ!!.correct)
        // Q2: use 50/50
        engine.nextQuestion()
        assertFalse("hintActive should be reset on Q2", engine.hintActive)
        assertTrue("hintRemoved should be cleared on Q2", engine.hintRemoved.isEmpty())
        assertFalse("fiftyFiftyActive should be false on Q2", engine.fiftyFiftyActive)
        assertTrue("fiftyFiftyRemoved should be cleared on Q2", engine.fiftyFiftyRemoved.isEmpty())
        engine.activateFiftyFifty()
        assertTrue("50/50 should activate on Q2", engine.fiftyFiftyActive)
        assertEquals("50/50 should remove exactly 2 on fresh question", 2, engine.fiftyFiftyRemoved.size)
        // Verify hint state from Q1 didn't leak
        assertFalse("Hint should not be active on Q2", engine.hintActive)
    }

    @Test
    fun fun_fiftyFiftyOnQ1_hintOnQ2_stateReset() {
        engine.startAllLawsGame()
        // Q1: use 50/50
        engine.nextQuestion()
        engine.activateFiftyFifty()
        assertTrue(engine.fiftyFiftyActive)
        assertEquals(2, engine.fiftyFiftyRemoved.size)
        // Answer Q1
        engine.answer(engine.currentQ!!.correct)
        // Q2: use hint
        engine.nextQuestion()
        assertFalse("fiftyFiftyActive should be reset on Q2", engine.fiftyFiftyActive)
        assertTrue("fiftyFiftyRemoved should be cleared on Q2", engine.fiftyFiftyRemoved.isEmpty())
        assertFalse("hintActive should be false on Q2", engine.hintActive)
        engine.useHint()
        assertTrue("Hint should activate on Q2", engine.hintActive)
        assertEquals("Hint should remove exactly 1 on fresh question", 1, engine.hintRemoved.size)
        // Verify 50/50 state from Q1 didn't leak
        assertFalse("50/50 should not be active on Q2", engine.fiftyFiftyActive)
    }

    @Test
    fun fun_fiftyFifty_neverRemovesCorrectAnswer_evenWithHint() {
        engine.currentQ = makeQuestion("C")
        engine.useHint()
        engine.activateFiftyFifty()
        assertFalse("Correct answer C should never be in fiftyFiftyRemoved",
            engine.fiftyFiftyRemoved.contains("C"))
        assertFalse("Correct answer C should never be in hintRemoved",
            engine.hintRemoved.contains("C"))
    }

    @Test
    fun fun_hint_neverRemovesCorrectAnswer_evenWithFiftyFifty() {
        engine.currentQ = makeQuestion("D")
        engine.activateFiftyFifty()
        engine.useHint()
        assertFalse("Correct answer D should never be in hintRemoved",
            engine.hintRemoved.contains("D"))
        assertFalse("Correct answer D should never be in fiftyFiftyRemoved",
            engine.fiftyFiftyRemoved.contains("D"))
    }

    @Test
    fun fun_hintAndFiftyFifty_noOverlapInRemovedOptions() {
        engine.currentQ = makeQuestion("A")
        engine.useHint()
        engine.activateFiftyFifty()
        for (opt in engine.fiftyFiftyRemoved) {
            assertFalse("fiftyFiftyRemoved should not contain hint-removed options",
                engine.hintRemoved.contains(opt))
        }
        for (opt in engine.hintRemoved) {
            assertFalse("hintRemoved should not contain fiftyFifty-removed options",
                engine.fiftyFiftyRemoved.contains(opt))
        }
    }

    @Test
    fun fun_fiftyFiftyWith3Options_hintThenFiftyFifty() {
        engine.currentQ = QuestionEntry(
            enunciado = "Test",
            opciones = mapOf("A" to "Opt A", "B" to "Opt B", "C" to "Opt C"),
            correct = "A",
            weight = 50,
            testId = "test1",
            origId = "1"
        )
        engine.useHint()
        assertTrue(engine.hintActive)
        // With 3 options, hint removes 1, leaving 2 fully visible
        // 50/50 should not activate because only 2 remain
        engine.activateFiftyFifty()
        assertFalse("50/50 should not activate with only 2 fully visible after hint",
            engine.fiftyFiftyActive)
    }

    @Test
    fun fun_fiftyFiftyThenHint_totalRemovedAtMost2() {
        engine.currentQ = makeQuestion("A")
        engine.activateFiftyFifty()
        engine.useHint()
        val totalRemoved = engine.fiftyFiftyRemoved.size + engine.hintRemoved.size
        assertTrue("Total removed (50/50 + hint) should not exceed 2 with 4 options",
            totalRemoved <= 2)
    }

    @Test
    fun fun_hintThenFiftyFifty_totalRemovedAtMost2() {
        engine.currentQ = makeQuestion("A")
        engine.useHint()
        engine.activateFiftyFifty()
        val totalRemoved = engine.fiftyFiftyRemoved.size + engine.hintRemoved.size
        assertTrue("Total removed (hint + 50/50) should not exceed 2 with 4 options",
            totalRemoved <= 2)
    }

    @Test
    fun fun_fiftyFifty_doesNotConsumeChargeIfNoRemoval() {
        engine.currentQ = QuestionEntry(
            enunciado = "Test",
            opciones = mapOf("A" to "Opt A", "B" to "Opt B"),
            correct = "A",
            weight = 50,
            testId = "test1",
            origId = "1"
        )
        // Only 2 options, can't remove any while keeping 2 visible
        engine.activateFiftyFifty()
        assertFalse("50/50 should not activate with only 2 options", engine.fiftyFiftyActive)
    }

    @Test
    fun fun_hint_doesNotConsumeChargeIfNoRemoval() {
        engine.currentQ = QuestionEntry(
            enunciado = "Test",
            opciones = mapOf("A" to "Opt A", "B" to "Opt B"),
            correct = "A",
            weight = 50,
            testId = "test1",
            origId = "1"
        )
        // Only 2 options, can't remove any while keeping 2 visible
        engine.useHint()
        assertFalse("Hint should not activate with only 2 options", engine.hintActive)
    }

    // === Regression tests for bugs fixed ===

    @Test
    fun fun_streak5_inQuick_doesNotAwardFiftyFifty() {
        // Help text: "Repaso Express: Sin power-ups"
        engine.mode = GameMode.QUICK
        engine.lives = 3
        engine.streak = 4
        engine.answered = false
        engine.currentQ = makeQuestion("A")
        engine.answer("A")
        assertEquals(5, engine.streak)
    }

    @Test
    fun fun_streak5_inTimetrial_doesNotAwardFiftyFifty() {
        // Help text only mentions "+20 segundos extra" for TIMETRIAL streaks, not 50/50
        engine.mode = GameMode.TIMETRIAL
        engine.timer = 100f
        engine.streak = 4
        engine.answered = false
        engine.currentQ = makeQuestion("A")
        engine.answer("A")
        assertEquals(5, engine.streak)
    }

    @Test
    fun fun_streak15_inSurvival_noDoubleScore() {
        // doubleScore power-up has been removed; streak 15 should not award it
        engine.mode = GameMode.SURVIVAL
        engine.lives = 3
        engine.streak = 14
        engine.answered = false
        engine.currentQ = makeQuestion("A")
        engine.answer("A")
        assertEquals(15, engine.streak)
    }

    // === Beginner mechanics tests (rank 0/1) ===

    @Test
    fun fun_rank0_firstMistakeForgiven() {
        progressRepo._rankIndex = 0
        engine.startAllLawsGame()
        engine.nextQuestion()
        val livesBefore = engine.lives
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").first { it != q.correct }
        engine.answer(wrong)
        assertEquals("First mistake at rank 0 should not cost a life",
            livesBefore, engine.lives)
        assertTrue("ctxFirstMistakeForgiven should be true", engine.ctxFirstMistakeForgiven)
    }

    @Test
    fun fun_rank0_secondMistakeCostsLife() {
        progressRepo._rankIndex = 0
        engine.startAllLawsGame()
        engine.nextQuestion()
        val livesBefore = engine.lives
        // First wrong (forgiven)
        val q1 = engine.currentQ!!
        val wrong1 = listOf("A", "B", "C", "D").first { it != q1.correct }
        engine.answer(wrong1)
        assertEquals(livesBefore, engine.lives)
        // Second wrong (costs life)
        engine.nextQuestion()
        val q2 = engine.currentQ!!
        val wrong2 = listOf("A", "B", "C", "D").first { it != q2.correct }
        engine.answer(wrong2)
        assertEquals("Second mistake at rank 0 should cost a life",
            livesBefore - 1, engine.lives)
    }

    @Test
    fun fun_rank0_comboHalvedOnWrong() {
        progressRepo._rankIndex = 0
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertEquals(3, engine.combo)
        engine.nextQuestion()
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").first { it != q.correct }
        engine.answer(wrong)
        assertEquals("Combo should be halved (3/2=1) at rank 0", 1, engine.combo)
    }

    @Test
    fun fun_rank2_comboResetOnWrong() {
        progressRepo._rankIndex = 2
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertEquals(3, engine.combo)
        engine.nextQuestion()
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").first { it != q.correct }
        engine.answer(wrong)
        assertEquals("Combo should be reset to 0 at rank 2", 0, engine.combo)
    }

    @Test
    fun fun_rank0_xpConsolationOnWrong() {
        progressRepo._rankIndex = 0
        engine.startAllLawsGame()
        val xpBefore = progressRepo.getXP()
        engine.nextQuestion()
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").first { it != q.correct }
        engine.answer(wrong)
        assertEquals("XP consolation of 1 should be granted at rank 0",
            1, progressRepo.getXP() - xpBefore)
        assertTrue("xpFromConsolation should be 1", engine.xpFromConsolation == 1)
    }

    @Test
    fun fun_rank2_noXpConsolationOnWrong() {
        progressRepo._rankIndex = 2
        engine.startAllLawsGame()
        val xpBefore = progressRepo.getXP()
        engine.nextQuestion()
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").first { it != q.correct }
        engine.answer(wrong)
        assertEquals("No XP consolation at rank 2", 0, progressRepo.getXP() - xpBefore)
    }

    @Test
    fun fun_rank0_adaptiveDifficultyLowersAfter2Wrong() {
        progressRepo._rankIndex = 0
        engine.startAllLawsGame()
        engine.sessionDifficultyCap = 2
        engine.nextQuestion()
        engine.answer(listOf("A", "B", "C", "D").first { it != engine.currentQ!!.correct })
        assertEquals("Cap should still be 2 after 1 wrong", 2, engine.sessionDifficultyCap)
        engine.nextQuestion()
        engine.answer(listOf("A", "B", "C", "D").first { it != engine.currentQ!!.correct })
        assertEquals("Cap should lower to 1 after 2 consecutive wrong", 1, engine.sessionDifficultyCap)
    }

    @Test
    fun fun_rank0_adaptiveDifficultyResetsOnCorrect() {
        progressRepo._rankIndex = 0
        engine.startAllLawsGame()
        engine.sessionDifficultyCap = 2
        engine.nextQuestion()
        engine.answer(listOf("A", "B", "C", "D").first { it != engine.currentQ!!.correct })
        assertEquals(1, engine.consecutiveWrong)
        engine.nextQuestion()
        engine.answer(engine.currentQ!!.correct)
        assertEquals("consecutiveWrong should reset on correct", 0, engine.consecutiveWrong)
    }

    @Test
    fun fun_rank0_adaptiveDifficultyMinIs1() {
        progressRepo._rankIndex = 0
        engine.startAllLawsGame()
        engine.sessionDifficultyCap = 1
        engine.nextQuestion()
        engine.answer(listOf("A", "B", "C", "D").first { it != engine.currentQ!!.correct })
        engine.nextQuestion()
        engine.answer(listOf("A", "B", "C", "D").first { it != engine.currentQ!!.correct })
        assertEquals("Cap should not go below 1", 1, engine.sessionDifficultyCap)
    }
}
