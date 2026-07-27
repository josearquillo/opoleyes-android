package com.opoleyes.domain

import com.opoleyes.TestContextProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.model.QuestionEntry
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
class GameEngineTest {

    private lateinit var engine: GameEngine
    private lateinit var prefs: PreferencesManager
    private lateinit var progressRepo: ProgressRepository
    private lateinit var statsRepo: StatsRepository

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
        val context = TestContextProvider.getContext()
        prefs = PreferencesManager(context)
        prefs.resetAll()
        progressRepo = ProgressRepository(context)
        statsRepo = StatsRepository(context)
        engine = GameEngine(context)
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
    fun fun_initGameStats_challengeHasTimer() {
        engine.mode = GameMode.CHALLENGE
        engine.initGameStats()
        assertEquals(0, engine.lives)
        assertEquals(120f, engine.timer, 0.01f)
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
    fun fun_startChallengeGame_returnsTrue() {
        val ok = engine.startChallengeGame()
        assertTrue(ok)
        assertEquals(GameMode.CHALLENGE, engine.mode)
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
    fun fun_nextQuestion_quickModeLimits20() {
        engine.startQuickGame()
        engine.questionNum = 20
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
    fun fun_nextQuestion_challengeLimits15() {
        engine.startChallengeGame()
        engine.questionNum = 15
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
        assertEquals(0.1f, engine.comboBarFill, 0.01f)
    }

    @Test
    fun fun_answer_comboBarOverchargeAt10() {
        engine.startAllLawsGame()
        for (i in 1..10) {
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
    fun fun_isGameOver_quickMode20Questions() {
        engine.mode = GameMode.QUICK
        engine.questionNum = 20
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
    fun fun_isGameOver_challenge15Questions() {
        engine.mode = GameMode.CHALLENGE
        engine.questionNum = 15
        engine.timer = 60f
        assertTrue(engine.isGameOver())
    }

    @Test
    fun fun_isGameOver_challengeNoTime() {
        engine.mode = GameMode.CHALLENGE
        engine.questionNum = 5
        engine.timer = 0f
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
    fun fun_activateFiftyFifty_noChargesDoesNothing() {
        engine.fiftyFiftyCharges = 0
        engine.activateFiftyFifty()
        assertFalse(engine.fiftyFiftyActive)
    }

    @Test
    fun fun_activateFiftyFifty_alreadyActiveDoesNothing() {
        engine.fiftyFiftyCharges = 3
        engine.fiftyFiftyActive = true
        engine.activateFiftyFifty()
        assertEquals(3, engine.fiftyFiftyCharges)
    }

    @Test
    fun fun_activateFiftyFifty_whenAnsweredDoesNothing() {
        engine.fiftyFiftyCharges = 3
        engine.answered = true
        engine.activateFiftyFifty()
        assertFalse(engine.fiftyFiftyActive)
    }

    @Test
    fun fun_activateFiftyFifty_removes2Options() {
        engine.fiftyFiftyCharges = 1
        engine.currentQ = makeQuestion("A")
        engine.activateFiftyFifty()
        assertTrue(engine.fiftyFiftyActive)
        assertEquals(0, engine.fiftyFiftyCharges)
        assertEquals(2, engine.fiftyFiftyRemoved.size)
        assertTrue(engine.ctxFiftyFiftyUsed)
    }

    @Test
    fun fun_activateFreeze_noChargesDoesNothing() {
        engine.freezeCharges = 0
        engine.activateFreeze()
        assertFalse(engine.freezeActive)
    }

    @Test
    fun fun_activateFreeze_success() {
        engine.freezeCharges = 1
        engine.activateFreeze()
        assertTrue(engine.freezeActive)
        assertEquals(0, engine.freezeCharges)
        assertEquals(10f, engine.freezeTimer, 0.01f)
    }

    @Test
    fun fun_activateFreeze_alreadyActiveDoesNothing() {
        engine.freezeCharges = 1
        engine.freezeActive = true
        engine.activateFreeze()
        assertEquals(1, engine.freezeCharges)
    }

    @Test
    fun fun_activateDoubleScore_noChargesDoesNothing() {
        engine.doubleScoreCharges = 0
        engine.activateDoubleScore()
        assertFalse(engine.doubleScoreActive)
    }

    @Test
    fun fun_activateDoubleScore_success() {
        engine.doubleScoreCharges = 1
        engine.activateDoubleScore()
        assertTrue(engine.doubleScoreActive)
        assertEquals(0, engine.doubleScoreCharges)
    }

    @Test
    fun fun_useHint_noChargesDoesNothing() {
        engine.hintCharges = 0
        engine.useHint()
        assertFalse(engine.hintActive)
    }

    @Test
    fun fun_useHint_success() {
        engine.hintCharges = 1
        engine.currentQ = makeQuestion("A")
        engine.useHint()
        assertTrue(engine.hintActive)
        assertEquals(0, engine.hintCharges)
        assertEquals(1, engine.hintRemoved.size)
    }

    @Test
    fun fun_useHint_alreadyActiveDoesNothing() {
        engine.hintCharges = 1
        engine.hintActive = true
        engine.useHint()
        assertEquals(1, engine.hintCharges)
    }

    @Test
    fun fun_useHint_whenAnsweredDoesNothing() {
        engine.hintCharges = 1
        engine.answered = true
        engine.useHint()
        assertFalse(engine.hintActive)
    }

    @Test
    fun fun_answer_shieldUsedOnWrong() {
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.shieldCharges = 1
        val q = engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        val result = engine.answer(wrong)
        assertEquals(GameEngine.AnswerResult.SHIELD_USED, result)
        assertEquals(0, engine.shieldCharges)
    }

    @Test
    fun fun_answer_doubleScoreDoublesPoints() {
        engine.startAllLawsGame()
        engine.nextQuestion()
        engine.doubleScoreActive = true
        engine.answer(engine.currentQ!!.correct)
        val expectedScore = 10 * 1 * 2
        assertEquals(expectedScore, engine.score)
        assertFalse(engine.doubleScoreActive)
    }
}
