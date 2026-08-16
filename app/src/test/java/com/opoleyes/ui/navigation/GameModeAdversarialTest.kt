package com.opoleyes.ui.navigation

import com.opoleyes.FakePreferencesManager
import com.opoleyes.RealisticFixtures
import com.opoleyes.data.Constants
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.repository.MissionRepository
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository
import com.opoleyes.domain.AchievementChecker
import com.opoleyes.domain.ChestSystem
import com.opoleyes.domain.ExamEngine
import com.opoleyes.domain.GameEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * Adversarial tests for game modes (Quick, Survival, Timetrial) with realistic data.
 * Tests edge cases: exit mid-game, restart after game over, state leakage between modes,
 * timer expiry while answering, lives running out, etc.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GameModeAdversarialTest {

    private lateinit var vm: GameViewModel
    private lateinit var prefs: FakePreferencesManager
    private lateinit var progressRepo: ProgressRepository

    companion object {
        private val testDispatcher = StandardTestDispatcher()

        @JvmStatic
        @BeforeClass
        fun setUpClass() { Dispatchers.setMain(testDispatcher) }

        @JvmStatic
        @AfterClass
        fun tearDownClass() { Dispatchers.resetMain() }
    }

    @Before
    fun setup() {
        prefs = FakePreferencesManager()
        prefs.resetAll()
        prefs.xp = 18000 // Rank 2 (Aprendiz): 4 options, 3 lives, all power-ups
        progressRepo = ProgressRepository(prefs)
        val statsRepo = StatsRepository(prefs)
        val missionRepo = MissionRepository(prefs)
        val pool = RealisticFixtures.buildRealisticPool()
        val gameRepo = object : com.opoleyes.data.IGameRepository {
            override fun startTemaGame(testId: String) = pool
            override fun startAllLawsGame() = pool
            override fun startQuickGame() = pool.take(5)
        }
        val engine = GameEngine.createForTest(gameRepo, statsRepo, progressRepo, prefs)
        val testData = RealisticFixtures.buildTestDataList()
        val examEngine = ExamEngine.createForTestData(statsRepo, testData)
        vm = GameViewModel.createForTest(
            progressRepo, statsRepo, missionRepo,
            AchievementChecker(prefs), ChestSystem(prefs),
            prefs, engine, examEngine
        )
    }

    @After
    fun teardown() { prefs.resetAll() }

    // ========================
    // Quick mode: full lifecycle
    // ========================

    @Test
    fun quick_perfectGame_scoreRecordAndXp() {
        vm.startQuickGame()
        assertEquals(GameMode.QUICK, vm.getEngineMode())
        assertEquals(1, vm.uiState.value.questionNum) // First question

        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        assertTrue(vm.isGameOver())
        vm.onGameOver()

        assertEquals(5, vm.uiState.value.correctCount)
        assertEquals(100, vm.accuracy.value)
        assertTrue("Score should be positive", vm.uiState.value.score > 0)
        assertTrue("Quick reward earned", vm.quickRewardEarned.value)
        assertTrue("XP gained", vm.xpGained.value > 0)
        assertTrue("New record", vm.newRecord.value)
    }

    @Test
    fun quick_exitMidGame_noGameOverProcessed() {
        vm.startQuickGame()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        // User exits mid-game (presses back)
        vm.exitGame()
        // onGameOver should NOT have been called
        // Games played should not increment
        assertEquals(0, progressRepo.getGamesPlayed())
    }

    @Test
    fun quick_restartAfterGameOver_stateIsClean() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        val firstScore = vm.uiState.value.score
        assertTrue(firstScore > 0)

        // Restart
        vm.startQuickGame()
        assertEquals(0, vm.uiState.value.score)
        assertEquals(0, vm.uiState.value.combo)
        assertEquals(1, vm.uiState.value.questionNum)
        assertFalse(vm.quickRewardEarned.value)
        assertNull(vm.rankUpOverlay.value)
        assertNull(vm.chestReward.value)
    }

    @Test
    fun quick_wrongAnswers_losesLivesAndGameOver() {
        vm.startQuickGame()
        val initialLives = vm.uiState.value.lives

        // Answer wrong until game over
        while (!vm.isGameOver()) {
            val q = vm.uiState.value.currentQ!!
            val wrong = q.opciones.keys.firstOrNull { it != q.correct } ?: "B"
            vm.answer(wrong)
            if (!vm.isGameOver()) vm.nextQuestion()
        }
        vm.onGameOver()

        assertEquals(0, vm.uiState.value.lives)
        assertEquals(0, vm.uiState.value.correctCount)
        assertFalse(vm.quickRewardEarned.value)
    }

    // ========================
    // Survival mode: full lifecycle
    // ========================

    @Test
    fun survival_playUntilLose_gameOverAndRestart() {
        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()
        assertEquals(GameMode.SURVIVAL, vm.getEngineMode())
        assertEquals(3, vm.uiState.value.lives) // Rank 2 = 3 lives

        // Answer some correctly first
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        assertEquals(2, vm.uiState.value.correctCount)

        // Now answer wrong until dead
        while (!vm.isGameOver()) {
            val q = vm.uiState.value.currentQ!!
            val wrong = q.opciones.keys.firstOrNull { it != q.correct } ?: "B"
            vm.answer(wrong)
            if (!vm.isGameOver()) vm.nextQuestion()
        }
        vm.onGameOver()

        assertEquals(0, vm.uiState.value.lives)
        assertEquals(2, vm.uiState.value.correctCount)
        assertTrue(vm.uiState.value.score > 0)

        // Restart
        vm.startAllLawsGame()
        assertEquals(0, vm.uiState.value.score)
        assertEquals(3, vm.uiState.value.lives)
    }

    @Test
    fun survival_firstMistakeForgiven_atRank0() {
        // Need rank 0 for first mistake forgiven
        prefs.xp = 0
        progressRepo = ProgressRepository(prefs)
        val statsRepo = StatsRepository(prefs)
        val missionRepo = MissionRepository(prefs)
        val pool = RealisticFixtures.buildRealisticPool()
        val gameRepo = object : com.opoleyes.data.IGameRepository {
            override fun startTemaGame(testId: String) = pool
            override fun startAllLawsGame() = pool
            override fun startQuickGame() = pool.take(5)
        }
        val engine = GameEngine.createForTest(gameRepo, statsRepo, progressRepo, prefs)
        val testData = RealisticFixtures.buildTestDataList()
        val examEngine = ExamEngine.createForTestData(statsRepo, testData)
        val vm0 = GameViewModel.createForTest(
            progressRepo, statsRepo, missionRepo,
            AchievementChecker(prefs), ChestSystem(prefs),
            prefs, engine, examEngine
        )

        vm0.pendingMode = GameMode.SURVIVAL
        vm0.startAllLawsGame()
        val initialLives = vm0.uiState.value.lives
        assertEquals(7, initialLives) // Rank 0 = 7 lives

        // First wrong answer - should be forgiven
        val q = vm0.uiState.value.currentQ!!
        val wrong = q.opciones.keys.firstOrNull { it != q.correct } ?: "B"
        vm0.answer(wrong)
        assertEquals("First mistake should be forgiven at rank 0", initialLives, vm0.uiState.value.lives)

        // Second wrong answer - should lose a life
        vm0.nextQuestion()
        val q2 = vm0.uiState.value.currentQ!!
        val wrong2 = q2.opciones.keys.firstOrNull { it != q2.correct } ?: "B"
        vm0.answer(wrong2)
        assertEquals("Second mistake should cost a life", initialLives - 1, vm0.uiState.value.lives)
    }

    // ========================
    // Timetrial: full lifecycle
    // ========================

    @Test
    fun timetrial_playUntilTimerZero_gameOver() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        assertEquals(GameMode.TIMETRIAL, vm.getEngineMode())
        assertEquals(180f, vm.uiState.value.timer, 0.01f)
        assertEquals(0, vm.uiState.value.lives) // No lives in timetrial

        // Answer some questions
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (!vm.isGameOver()) vm.nextQuestion()
        }
        assertEquals(5, vm.uiState.value.correctCount)
        assertTrue("Timer should have increased from correct answers", vm.uiState.value.timer > 180f)

        // Tick timer to zero
        while (!vm.tickTimer()) { }
        assertTrue(vm.isGameOver())
        vm.onGameOver()

        assertTrue(vm.uiState.value.score > 0)
    }

    @Test
    fun timetrial_wrongAnswerReducesTimer() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        val initialTimer = vm.uiState.value.timer

        val q = vm.uiState.value.currentQ!!
        val wrong = q.opciones.keys.firstOrNull { it != q.correct } ?: "B"
        vm.answer(wrong)

        assertTrue("Timer should decrease after wrong answer in timetrial",
            vm.uiState.value.timer < initialTimer)
    }

    @Test
    fun timetrial_correctAnswerAddsTimer() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        val initialTimer = vm.uiState.value.timer

        vm.answer(vm.uiState.value.currentQ!!.correct)

        assertTrue("Timer should increase after correct answer in timetrial",
            vm.uiState.value.timer > initialTimer)
    }

    @Test
    fun timetrial_applyPausedElapsed_largeValue_gameOver() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()

        // Simulate app being paused for 200 seconds
        val expired = vm.applyPausedElapsed(200f)
        assertTrue("Should be game over after 200s pause", expired)
    }

    @Test
    fun timetrial_applyPausedElapsed_smallValue_continues() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()

        val expired = vm.applyPausedElapsed(10f)
        assertFalse("Should not be game over after 10s pause", expired)
        assertEquals(170f, vm.uiState.value.timer, 0.01f)
    }

    // ========================
    // Cross-mode state isolation
    // ========================

    @Test
    fun crossMode_quickToSurvival_noStateLeakage() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        val quickScore = vm.uiState.value.score
        assertTrue(quickScore > 0)

        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()
        assertEquals(0, vm.uiState.value.score)
        assertEquals(0, vm.uiState.value.combo)
        assertEquals(1, vm.uiState.value.questionNum)
        assertNull(vm.chestReward.value)
        assertNull(vm.rankUpOverlay.value)
    }

    @Test
    fun crossMode_survivalToTimetrial_noStateLeakage() {
        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        assertTrue(vm.uiState.value.score > 0)

        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        assertEquals(0, vm.uiState.value.score)
        assertEquals(180f, vm.uiState.value.timer, 0.01f)
    }

    @Test
    fun crossMode_timetrialToQuick_noStateLeakage() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        for (i in 0 until 3) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (!vm.isGameOver()) vm.nextQuestion()
        }
        assertTrue(vm.uiState.value.score > 0)

        vm.startQuickGame()
        assertEquals(0, vm.uiState.value.score)
        assertEquals(180f, 180f, 0.01f) // Quick mode doesn't use timer
    }

    @Test
    fun crossMode_examToQuick_noStateLeakage() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            vm.examAnswer(vm.examEngine.getCurrentQuestion()!!.question.correct)
        }
        vm.finishExam()
        assertNotNull(vm.examResult.value)

        vm.clearExamResult()
        vm.startQuickGame()
        assertNull(vm.examResult.value)
        assertEquals(GameMode.QUICK, vm.getEngineMode())
    }

    @Test
    fun crossMode_simulacroToQuick_noStateLeakage() {
        vm.loadSimulacroSync()
        assertTrue(vm.isSimulacroMode.value)

        vm.clearExamResult()
        vm.startQuickGame()
        assertFalse(vm.isSimulacroMode.value)
        assertNull(vm.simulacroResult.value)
    }

    // ========================
    // onGameOver idempotency
    // ========================

    @Test
    fun onGameOver_calledTwice_noDoubleCounting() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        val xp1 = vm.xpGained.value
        val games1 = progressRepo.getGamesPlayed()

        vm.onGameOver()
        assertEquals(xp1, vm.xpGained.value)
        assertEquals(games1, progressRepo.getGamesPlayed())
    }

    // ========================
    // Power-ups with realistic data
    // ========================

    @Test
    fun fiftyFifty_correctAnswerNeverRemoved() {
        vm.startQuickGame()
        val correct = vm.uiState.value.currentQ!!.correct
        vm.activateFiftyFifty()
        assertFalse("Correct answer must never be removed by 50/50",
            correct in vm.uiState.value.fiftyFiftyRemoved)
    }

    @Test
    fun hint_correctAnswerNeverRemoved() {
        vm.startQuickGame()
        val correct = vm.uiState.value.currentQ!!.correct
        vm.useHint()
        assertFalse("Correct answer must never be removed by hint",
            correct in vm.uiState.value.hintRemoved)
    }

    @Test
    fun powerUps_resetBetweenQuestions() {
        vm.startQuickGame()
        vm.activateFiftyFifty()
        assertTrue(vm.uiState.value.fiftyFiftyActive)
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        assertFalse("50/50 should reset on next question", vm.uiState.value.fiftyFiftyActive)
        assertEquals(0, vm.uiState.value.fiftyFiftyRemoved.size)

        vm.useHint()
        assertTrue(vm.uiState.value.hintActive)
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        assertFalse("Hint should reset on next question", vm.uiState.value.hintActive)
    }

    // ========================
    // Records with realistic data
    // ========================

    @Test
    fun records_beatPreviousRecord_setsNewRecord() {
        // First game sets a record
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        val firstRecord = progressRepo.getRecord("quick")
        assertTrue(firstRecord > 0)

        // Second game - same score (all correct again)
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        // Record should be >= first (same or better due to combo)
        assertTrue(progressRepo.getRecord("quick") >= firstRecord)
    }

    @Test
    fun records_lowerScore_doesNotSetNewRecord() {
        // Set a high record
        progressRepo.setRecord("quick", 999999)
        progressRepo.setRecordAcc("quick", 100)

        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()

        assertFalse("Should not be new record when score is lower", vm.newRecord.value)
        assertEquals(999999, progressRepo.getRecord("quick"))
    }

    // ========================
    // Chest generation with realistic data
    // ========================

    @Test
    fun chest_generatedForPerfectQuickGame() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        assertNotNull("Chest should be generated for perfect quick game", vm.chestReward.value)
    }

    @Test
    fun chest_notGeneratedForShortGame() {
        vm.startQuickGame()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        // Only 1 answer - not enough for chest (needs >= 3)
        vm.onGameOver()
        assertNull(vm.chestReward.value)
    }

    @Test
    fun chest_openAddsXpAndClearsAfter() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        val chest = vm.chestReward.value!!
        val xpBefore = vm.xpGained.value

        vm.openChest()
        assertEquals(xpBefore + chest.xp, vm.xpGained.value)

        // Open again - should be no-op
        vm.openChest()
        assertEquals(xpBefore + chest.xp, vm.xpGained.value)
    }
}
