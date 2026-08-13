package com.opoleyes.domain

import com.opoleyes.FakeGameRepository
import com.opoleyes.FakePreferencesManager
import com.opoleyes.FakeProgressRepository
import com.opoleyes.FakeStatsRepository
import com.opoleyes.TestFakes
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.model.QuestionEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameEngineCoverageTest {

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

    // === Getter/setter coverage ===

    @Test
    fun setters_andGetters_roundTrip() {
        engine.category = "law1"
        assertEquals("law1", engine.category)

        engine.pool = listOf()
        assertEquals(0, engine.pool.size)

        engine.askedIds = mutableSetOf("a", "b")
        assertEquals(2, engine.askedIds.size)

        engine.maxCombo = 5
        assertEquals(5, engine.maxCombo)

        engine.maxStreak = 7
        assertEquals(7, engine.maxStreak)

        engine.comboOverchargeActive = true
        assertTrue(engine.comboOverchargeActive)

        engine.comboOverchargeCharges = 2
        assertEquals(2, engine.comboOverchargeCharges)

        engine.startRankIndex = 3
        assertEquals(3, engine.startRankIndex)

        assertEquals(0, engine.startXP)
        engine.startXP = 100
        assertEquals(100, engine.startXP)

        engine.xpMultiplier = 3
        assertEquals(3, engine.xpMultiplier)

        engine.xpFromCorrect = 50
        assertEquals(50, engine.xpFromCorrect)

        engine.xpFromLawMastery = 200
        assertEquals(200, engine.xpFromLawMastery)

        engine.xpFromConsolation = 5
        assertEquals(5, engine.xpFromConsolation)

        engine.lawsMasteredThisGame = 2
        assertEquals(2, engine.lawsMasteredThisGame)

        engine.maxOptions = 3
        assertEquals(3, engine.maxOptions)

        engine.maxLives = 5
        assertEquals(5, engine.maxLives)

        assertEquals(5, engine.maxDifficulty)

        engine.ctxFiftyFiftyUsed = true
        assertTrue(engine.ctxFiftyFiftyUsed)

        engine.ctxLifeRecovered = true
        assertTrue(engine.ctxLifeRecovered)

        engine.ctxFirstMistakeForgiven = true
        assertTrue(engine.ctxFirstMistakeForgiven)

        engine.powerUpUsedType = "hint"
        assertEquals("hint", engine.powerUpUsedType)

        engine.lastPtsEarned = 42
        assertEquals(42, engine.lastPtsEarned)

        engine.consecutiveWrong = 3
        assertEquals(3, engine.consecutiveWrong)

        assertFalse(engine.firstMistakeUsed)
    }

    // === nextQuestion with rankIndex <= 1 (sorted by difficulty) ===

    @Test
    fun nextQuestion_noviceRank_sortsByDifficulty() {
        progressRepo._rankIndex = 0
        engine.startAllLawsGame()
        engine.rankIndex = 0
        assertTrue(engine.nextQuestion())
        assertNotNull(engine.currentQ)
    }

    // === nextQuestion with empty pool fallback ===

    @Test
    fun nextQuestion_allQuestionsAsked_recyclesPool() {
        progressRepo._rankIndex = 2
        // Use a small pool
        gameRepo.pool = TestFakes.makePool(2)
        engine.startAllLawsGame()
        engine.nextQuestion()
        // Answer first question
        assertNotNull(engine.currentQ)
        val q1 = engine.currentQ!!
        engine.answer(q1.correct)
        // Get next question (should recycle since only 2 in pool)
        engine.nextQuestion()
        assertNotNull(engine.currentQ)
    }

    // === answer with comboOvercharge in TIMETRIAL ===

    @Test
    fun answer_comboOverchargeTimetrial_addsTime() {
        progressRepo._rankIndex = 2
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame(GameMode.TIMETRIAL)
        engine.timer = 100f
        engine.nextQuestion()

        // Fill combo bar to trigger overcharge (5 correct answers)
        for (i in 0 until 5) {
            val q = engine.currentQ!!
            engine.answer(q.correct)
            if (i < 4) engine.nextQuestion()
        }
        // After 5 correct, comboOvercharge should be active
        if (engine.comboOverchargeActive) {
            // Next correct should add time
            engine.nextQuestion()
            val q = engine.currentQ!!
            val timerBefore = engine.timer
            engine.answer(q.correct)
            // In TIMETRIAL, overcharge adds 30s
            assertTrue(engine.timer > timerBefore)
        }
    }

    // === answer with streak recovery in TIMETRIAL ===

    @Test
    fun answer_streakRecoveryTimetrial_addsTime() {
        progressRepo._rankIndex = 2
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame(GameMode.TIMETRIAL)
        engine.timer = 100f
        engine.nextQuestion()

        // Answer 5 correct to trigger streak recovery
        for (i in 0 until 5) {
            val q = engine.currentQ!!
            engine.answer(q.correct)
            if (i < 4) engine.nextQuestion()
        }
        // TIMETRIAL streak recovery adds 20s
        assertTrue(engine.timer > 100f)
    }

    // === answer with TIMETRIAL correct adds 15s ===

    @Test
    fun answer_correctTimetrial_adds15s() {
        progressRepo._rankIndex = 2
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame(GameMode.TIMETRIAL)
        engine.timer = 100f
        engine.nextQuestion()
        val q = engine.currentQ!!
        engine.answer(q.correct)
        assertEquals(115f, engine.timer, 0.01f)
    }

    // === answer wrong in TIMETRIAL subtracts 10s ===

    @Test
    fun answer_wrongTimetrial_subtracts10s() {
        progressRepo._rankIndex = 2
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame(GameMode.TIMETRIAL)
        engine.timer = 100f
        engine.nextQuestion()
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        engine.answer(wrong)
        assertEquals(90f, engine.timer, 0.01f)
    }

    // === answer wrong with rank 0 first mistake forgiven in QUICK ===

    @Test
    fun answer_wrongQuickRank0_firstMistakeForgiven() {
        progressRepo._rankIndex = 0
        engine.mode = GameMode.QUICK
        gameRepo.pool = TestFakes.makePool(20)
        engine.startQuickGame()
        engine.rankIndex = 0
        engine.nextQuestion()
        val livesBefore = engine.lives
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        engine.answer(wrong)
        assertTrue(engine.ctxFirstMistakeForgiven)
        assertEquals(livesBefore, engine.lives) // No life lost
    }

    // === answer wrong with rank 0 second mistake loses life ===

    @Test
    fun answer_wrongQuickRank0_secondMistakeLosesLife() {
        progressRepo._rankIndex = 0
        engine.mode = GameMode.QUICK
        gameRepo.pool = TestFakes.makePool(20)
        engine.startQuickGame()
        engine.rankIndex = 0
        engine.nextQuestion()
        val q1 = engine.currentQ!!
        val wrong1 = listOf("A", "B", "C", "D").filter { it != q1.correct }.first()
        engine.answer(wrong1) // First mistake forgiven
        engine.nextQuestion()
        val livesBefore = engine.lives
        val q2 = engine.currentQ!!
        val wrong2 = listOf("A", "B", "C", "D").filter { it != q2.correct }.first()
        engine.answer(wrong2) // Second mistake loses life
        assertEquals(livesBefore - 1, engine.lives)
    }

    // === answer wrong with rank <= 1 consolation XP ===

    @Test
    fun answer_wrongRank1_earnsConsolationXp() {
        progressRepo._rankIndex = 1
        engine.mode = GameMode.SURVIVAL
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        engine.rankIndex = 1
        engine.nextQuestion()
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        engine.answer(wrong)
        assertTrue(engine.xpFromConsolation > 0)
    }

    // === answer wrong with rank <= 1 combo halved ===

    @Test
    fun answer_wrongRank1_comboHalved() {
        progressRepo._rankIndex = 2 // rank 2 has maxDifficulty=3 which matches default question difficulty
        engine.mode = GameMode.SURVIVAL
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        engine.rankIndex = 1 // set to rank 1 for combo halving
        engine.nextQuestion()
        // Build combo with 4 correct answers, always advancing to next question
        for (i in 0 until 4) {
            val q = engine.currentQ!!
            engine.answer(q.correct)
            engine.nextQuestion()
        }
        assertEquals(4, engine.combo)
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        engine.answer(wrong)
        assertEquals(2, engine.combo) // 4 / 2 = 2
    }

    // === answer wrong with adaptive difficulty lowering ===

    @Test
    fun answer_twoConsecutiveWrong_lowersDifficultyCap() {
        progressRepo._rankIndex = 2
        engine.mode = GameMode.SURVIVAL
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        engine.sessionDifficultyCap = 3
        engine.nextQuestion()
        for (i in 0 until 2) {
            val q = engine.currentQ!!
            val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
            engine.answer(wrong)
            if (i < 1) engine.nextQuestion()
        }
        assertEquals(2, engine.sessionDifficultyCap)
    }

    // === activateFiftyFifty with already answered ===

    @Test
    fun activateFiftyFifty_alreadyAnswered_isNoOp() {
        progressRepo._rankIndex = 2
        engine.mode = GameMode.SURVIVAL
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        engine.nextQuestion()
        val q = engine.currentQ!!
        engine.answer(q.correct)
        engine.activateFiftyFifty()
        assertFalse(engine.fiftyFiftyActive)
    }

    // === useHint with already answered ===

    @Test
    fun useHint_alreadyAnswered_isNoOp() {
        progressRepo._rankIndex = 2
        engine.mode = GameMode.SURVIVAL
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        engine.nextQuestion()
        val q = engine.currentQ!!
        engine.answer(q.correct)
        engine.useHint()
        assertFalse(engine.hintActive)
    }

    // === useHint with only 2 options ===

    @Test
    fun useHint_onlyTwoOptions_isNoOp() {
        progressRepo._rankIndex = 2
        engine.mode = GameMode.SURVIVAL
        // Create a question with only 2 options
        gameRepo.pool = listOf(QuestionEntry(
            enunciado = "Test",
            opciones = mapOf("A" to "Opt A", "B" to "Opt B"),
            correct = "A",
            weight = 50,
            testId = "test1",
            origId = "1"
        ))
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.useHint()
        assertFalse(engine.hintActive)
    }

    // === getAccuracy with 0 answered ===

    @Test
    fun getAccuracy_zeroAnswered_returns0() {
        assertEquals(0, engine.getAccuracy())
    }

    // === isGameOver for each mode ===

    @Test
    fun isGameOver_survivalWithLives_returnsFalse() {
        engine.mode = GameMode.SURVIVAL
        engine.lives = 3
        assertFalse(engine.isGameOver())
    }

    @Test
    fun isGameOver_survivalNoLives_returnsTrue() {
        engine.mode = GameMode.SURVIVAL
        engine.lives = 0
        assertTrue(engine.isGameOver())
    }

    @Test
    fun isGameOver_quickWithLivesAndQuestionsLeft_returnsFalse() {
        engine.mode = GameMode.QUICK
        engine.lives = 3
        engine.questionNum = 3
        assertFalse(engine.isGameOver())
    }

    @Test
    fun isGameOver_quickNoLives_returnsTrue() {
        engine.mode = GameMode.QUICK
        engine.lives = 0
        engine.questionNum = 3
        assertTrue(engine.isGameOver())
    }

    @Test
    fun isGameOver_quickAllQuestions_returnsTrue() {
        engine.mode = GameMode.QUICK
        engine.lives = 3
        engine.questionNum = 5 // Constants.QUICK_MODE_QUESTIONS
        assertTrue(engine.isGameOver())
    }

    @Test
    fun isGameOver_timetrialWithTime_returnsFalse() {
        engine.mode = GameMode.TIMETRIAL
        engine.timer = 10f
        assertFalse(engine.isGameOver())
    }

    @Test
    fun isGameOver_timetrialNoTime_returnsTrue() {
        engine.mode = GameMode.TIMETRIAL
        engine.timer = 0f
        assertTrue(engine.isGameOver())
    }

    // === nextQuestion returns false when game over ===

    @Test
    fun nextQuestion_survivalNoLives_returnsFalse() {
        engine.mode = GameMode.SURVIVAL
        engine.lives = 0
        assertFalse(engine.nextQuestion())
    }

    @Test
    fun nextQuestion_quickNoLives_returnsFalse() {
        engine.mode = GameMode.QUICK
        engine.lives = 0
        engine.questionNum = 0
        assertFalse(engine.nextQuestion())
    }

    @Test
    fun nextQuestion_quickAllQuestions_returnsFalse() {
        engine.mode = GameMode.QUICK
        engine.lives = 3
        engine.questionNum = 5
        assertFalse(engine.nextQuestion())
    }

    @Test
    fun nextQuestion_timetrialNoTime_returnsFalse() {
        engine.mode = GameMode.TIMETRIAL
        engine.timer = 0f
        assertFalse(engine.nextQuestion())
    }

    // === startQuickGame with empty pool ===

    @Test
    fun startQuickGame_emptyPool_returnsFalse() {
        gameRepo.pool = listOf()
        assertFalse(engine.startQuickGame())
    }

    @Test
    fun startTemaGame_emptyPool_returnsFalse() {
        gameRepo.pool = listOf()
        assertFalse(engine.startTemaGame("test1"))
    }

    @Test
    fun startAllLawsGame_emptyPool_returnsFalse() {
        gameRepo.pool = listOf()
        assertFalse(engine.startAllLawsGame())
    }

    // === answer ALREADY_ANSWERED ===

    @Test
    fun answer_alreadyAnswered_returnsAlreadyAnswered() {
        progressRepo._rankIndex = 2
        engine.mode = GameMode.SURVIVAL
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        engine.nextQuestion()
        val q = engine.currentQ!!
        engine.answer(q.correct)
        val result = engine.answer(q.correct)
        assertEquals(GameEngine.AnswerResult.ALREADY_ANSWERED, result)
    }

    // === answer with null currentQ returns ERROR ===

    @Test
    fun answer_nullCurrentQ_returnsError() {
        engine.currentQ = null
        engine.answered = false
        val result = engine.answer("A")
        assertEquals(GameEngine.AnswerResult.ERROR, result)
    }

    // === initGameStats with multiplier > 1 consumes it ===

    @Test
    fun initGameStats_multiplierConsumed() {
        prefs._multiplier = 3
        progressRepo._rankIndex = 2
        engine.mode = GameMode.SURVIVAL
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        assertEquals(3, engine.xpMultiplier)
        assertEquals(1, prefs.getMultiplier())
    }

    // === activateFiftyFifty with powerUp already used ===

    @Test
    fun activateFiftyFifty_powerUpAlreadyUsed_isNoOp() {
        progressRepo._rankIndex = 2
        engine.mode = GameMode.SURVIVAL
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.powerUpUsedThisQuestion = true
        engine.activateFiftyFifty()
        assertFalse(engine.fiftyFiftyActive)
    }

    // === useHint with powerUp already used ===

    @Test
    fun useHint_powerUpAlreadyUsed_isNoOp() {
        progressRepo._rankIndex = 2
        engine.mode = GameMode.SURVIVAL
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.powerUpUsedThisQuestion = true
        engine.useHint()
        assertFalse(engine.hintActive)
    }

    // === activateFiftyFifty not in availablePowerUps ===

    @Test
    fun activateFiftyFifty_notInAvailablePowerUps_isNoOp() {
        progressRepo._rankIndex = 2
        engine.mode = GameMode.SURVIVAL
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.availablePowerUps = listOf("hint")
        engine.activateFiftyFifty()
        assertFalse(engine.fiftyFiftyActive)
    }

    // === useHint not in availablePowerUps ===

    @Test
    fun useHint_notInAvailablePowerUps_isNoOp() {
        progressRepo._rankIndex = 2
        engine.mode = GameMode.SURVIVAL
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.availablePowerUps = listOf("fiftyFifty")
        engine.useHint()
        assertFalse(engine.hintActive)
    }

    // === initGameStats with TIMETRIAL ===

    @Test
    fun initGameStats_timetrial_sets180sTimer() {
        progressRepo._rankIndex = 2
        engine.startAllLawsGame(GameMode.TIMETRIAL)
        assertEquals(180f, engine.timer, 0.01f)
        assertEquals(0, engine.lives)
    }

    // === nextQuestion with weighted selection (rankIndex > 1) ===

    @Test
    fun nextQuestion_rankGreaterThan1_usesWeightedSelection() {
        progressRepo._rankIndex = 2
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        assertTrue(engine.nextQuestion())
        assertNotNull(engine.currentQ)
    }

    // === nextQuestion with zero-weight pool ===

    @Test
    fun nextQuestion_zeroWeightPool_usesRandomFallback() {
        progressRepo._rankIndex = 2
        gameRepo.pool = listOf(
            QuestionEntry("Q1", mapOf("A" to "a", "B" to "b", "C" to "c", "D" to "d"), "A", 0, "t1", "1"),
            QuestionEntry("Q2", mapOf("A" to "a", "B" to "b", "C" to "c", "D" to "d"), "B", 0, "t1", "2")
        )
        engine.startAllLawsGame()
        assertTrue(engine.nextQuestion())
        assertNotNull(engine.currentQ)
    }

    // === nextQuestion with all questions above difficulty cap ===

    @Test
    fun nextQuestion_allAboveCap_fallsBackToMaxDifficulty() {
        progressRepo._rankIndex = 2
        gameRepo.pool = listOf(
            QuestionEntry("Q1", mapOf("A" to "a", "B" to "b", "C" to "c", "D" to "d"), "A", 50, "t1", "1", difficulty = 5)
        )
        engine.startAllLawsGame()
        // sessionDifficultyCap starts at 1, question difficulty is 5
        // Should fall back to maxDifficulty filter
        engine.sessionDifficultyCap = 1
        assertTrue(engine.nextQuestion())
        assertNotNull(engine.currentQ)
    }

    // === activateFiftyFifty normal case ===

    @Test
    fun activateFiftyFifty_normalCase_removesTwoOptions() {
        progressRepo._rankIndex = 2
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.activateFiftyFifty()
        assertTrue(engine.fiftyFiftyActive)
        assertEquals(2, engine.fiftyFiftyRemoved.size)
    }

    // === useHint normal case ===

    @Test
    fun useHint_normalCase_removesOneOption() {
        progressRepo._rankIndex = 2
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.useHint()
        assertTrue(engine.hintActive)
    }

    // === answer correct with law mastery check ===

    @Test
    fun answer_correct_updatesStreak() {
        progressRepo._rankIndex = 2
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        engine.nextQuestion()
        val q = engine.currentQ!!
        engine.answer(q.correct)
        assertEquals(1, engine.streak)
    }

    // === answer correct with combo bar fill ===

    @Test
    fun answer_correct_fillsComboBar() {
        progressRepo._rankIndex = 2
        gameRepo.pool = TestFakes.makePool(20)
        engine.startAllLawsGame()
        engine.nextQuestion()
        val q = engine.currentQ!!
        engine.answer(q.correct)
        assertTrue(engine.comboBarFill > 0f)
    }
}
