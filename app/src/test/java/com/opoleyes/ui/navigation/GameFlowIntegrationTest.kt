package com.opoleyes.ui.navigation

import com.opoleyes.FakeGameRepository
import com.opoleyes.FakePreferencesManager
import com.opoleyes.TestFakes
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GameFlowIntegrationTest {

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
        progressRepo = ProgressRepository(prefs)
        val statsRepo = StatsRepository(prefs)
        val missionRepo = MissionRepository(prefs)
        val engine = GameEngine.createForTest(FakeGameRepository(), statsRepo, progressRepo, prefs)
        val examEngine = ExamEngine.createForTest(statsRepo, TestFakes.makePool(100))
        vm = GameViewModel.createForTest(
            progressRepo, statsRepo, missionRepo,
            AchievementChecker(prefs), ChestSystem(prefs),
            prefs, engine, examEngine
        )
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    private fun answerAllCorrectAndFinish(mode: GameMode, count: Int) {
        for (i in 0 until count) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (!vm.isGameOver()) vm.nextQuestion()
        }
        if (!vm.isGameOver()) {
            // For timetrial, we need to exhaust timer
            while (!vm.isGameOver()) {
                vm.answer(vm.uiState.value.currentQ!!.correct)
                if (!vm.isGameOver()) vm.nextQuestion()
            }
        }
        vm.onGameOver()
    }

    private fun answerAllWrongAndFinish(mode: GameMode, maxAttempts: Int) {
        for (i in 0 until maxAttempts) {
            val q = vm.uiState.value.currentQ ?: break
            val wrong = q.opciones.keys.firstOrNull { it != q.correct } ?: "B"
            vm.answer(wrong)
            if (!vm.isGameOver()) vm.nextQuestion()
        }
        vm.onGameOver()
    }

    // ========================
    // Quick mode: full flow start → play → game over → restart
    // ========================

    @Test
    fun quickMode_fullFlow_startPlayGameOverAndRestart() {
        // Start
        val started = vm.startQuickGame()
        assertTrue("Quick game should start", started)
        assertEquals(GameMode.QUICK, vm.getEngineMode())
        assertNotNull("Current question should be loaded", vm.uiState.value.currentQ)

        // Play all 5 questions correctly
        for (i in 0 until Constants.QUICK_MODE_QUESTIONS) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < Constants.QUICK_MODE_QUESTIONS - 1) vm.nextQuestion()
        }

        assertTrue("Should be game over after 5 questions", vm.isGameOver())
        vm.onGameOver()

        // Verify game over state
        assertEquals(Constants.QUICK_MODE_QUESTIONS, vm.uiState.value.totalAnswered)
        assertEquals(Constants.QUICK_MODE_QUESTIONS, vm.uiState.value.correctCount)
        assertTrue("Quick reward should be earned for perfect game", vm.quickRewardEarned.value)

        // Restart
        val restarted = vm.startQuickGame()
        assertTrue("Should be able to restart quick game", restarted)
        assertFalse("gameOverProcessed should be reset on restart", vm.isGameOver())
        assertNotNull("New question should be loaded", vm.uiState.value.currentQ)
    }

    @Test
    fun quickMode_fullFlow_wrongAnswers_gameOverAndRestart() {
        vm.startQuickGame()

        // Answer all wrong
        for (i in 0 until Constants.QUICK_MODE_QUESTIONS) {
            val q = vm.uiState.value.currentQ!!
            val wrong = q.opciones.keys.firstOrNull { it != q.correct } ?: "B"
            vm.answer(wrong)
            if (vm.isGameOver()) break
            vm.nextQuestion()
        }

        // Quick mode ends on lives <= 0 or 5 questions answered
        // With wrong answers, lives may run out before 5 questions
        vm.onGameOver()

        assertEquals(0, vm.uiState.value.correctCount)
        assertFalse("No quick reward for imperfect game", vm.quickRewardEarned.value)

        // Restart should work
        val restarted = vm.startQuickGame()
        assertTrue(restarted)
    }

    @Test
    fun quickMode_secondGame_doesNotHaveStaleState() {
        // First game
        vm.startQuickGame()
        for (i in 0 until Constants.QUICK_MODE_QUESTIONS) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < Constants.QUICK_MODE_QUESTIONS - 1) vm.nextQuestion()
        }
        vm.onGameOver()
        val firstScore = vm.uiState.value.score
        assertTrue(firstScore > 0)

        // Second game
        vm.startQuickGame()
        assertEquals("Score should be reset", 0, vm.uiState.value.score)
        assertEquals("Combo should be reset", 0, vm.uiState.value.combo)
        assertEquals("Question num should be reset", 1, vm.uiState.value.questionNum)
        assertFalse("Quick reward should be reset", vm.quickRewardEarned.value)
    }

    // ========================
    // Survival mode: full flow
    // ========================

    @Test
    fun survivalMode_fullFlow_startPlayLoseLivesGameOver() {
        vm.pendingMode = GameMode.SURVIVAL
        val started = vm.startAllLawsGame()
        assertTrue("Survival game should start", started)
        assertEquals(GameMode.SURVIVAL, vm.getEngineMode())
        assertNotNull("Current question should be loaded", vm.uiState.value.currentQ)

        // Answer wrong until game over
        val maxLives = vm.uiState.value.lives
        var attempts = 0
        while (!vm.isGameOver() && attempts < 50) {
            val q = vm.uiState.value.currentQ ?: break
            val wrong = q.opciones.keys.firstOrNull { it != q.correct } ?: "B"
            vm.answer(wrong)
            attempts++
            if (!vm.isGameOver()) vm.nextQuestion()
        }

        assertTrue("Should be game over after losing all lives", vm.isGameOver())
        vm.onGameOver()

        assertEquals(0, vm.uiState.value.lives)
        assertEquals(0, vm.uiState.value.correctCount)
    }

    @Test
    fun survivalMode_secondGame_doesNotHaveStaleState() {
        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()

        // Lose immediately
        while (!vm.isGameOver()) {
            val q = vm.uiState.value.currentQ ?: break
            val wrong = q.opciones.keys.firstOrNull { it != q.correct } ?: "B"
            vm.answer(wrong)
            if (!vm.isGameOver()) vm.nextQuestion()
        }
        vm.onGameOver()

        // Second game
        vm.startAllLawsGame()
        assertEquals("Score should be reset", 0, vm.uiState.value.score)
        assertEquals("Lives should be restored", vm.engine.maxLives, vm.uiState.value.lives)
        assertEquals("Question num should be reset", 1, vm.uiState.value.questionNum)
    }

    // ========================
    // Timetrial mode: full flow
    // ========================

    @Test
    fun timetrialMode_fullFlow_startPlayGameOver() {
        vm.pendingMode = GameMode.TIMETRIAL
        val started = vm.startAllLawsGame()
        assertTrue("Timetrial game should start", started)
        assertEquals(GameMode.TIMETRIAL, vm.getEngineMode())
        assertEquals("Timer should be 180s", 180f, vm.uiState.value.timer, 0.01f)
        assertEquals("No lives in timetrial", 0, vm.uiState.value.lives)

        // Answer a few questions correctly
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (!vm.isGameOver()) vm.nextQuestion()
        }

        assertEquals(5, vm.uiState.value.correctCount)
        assertTrue("Score should be positive", vm.uiState.value.score > 0)
    }

    @Test
    fun timetrialMode_secondGame_doesNotHaveStaleState() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()

        // Answer a few
        for (i in 0 until 3) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (!vm.isGameOver()) vm.nextQuestion()
        }

        // Second game
        vm.startAllLawsGame()
        assertEquals("Score should be reset", 0, vm.uiState.value.score)
        assertEquals("Timer should be reset", 180f, vm.uiState.value.timer, 0.01f)
        assertEquals("Question num should be reset", 1, vm.uiState.value.questionNum)
    }

    // ========================
    // Cross-mode: switch between modes
    // ========================

    @Test
    fun crossMode_quickToSurvival_stateIsReset() {
        vm.startQuickGame()
        for (i in 0 until Constants.QUICK_MODE_QUESTIONS) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < Constants.QUICK_MODE_QUESTIONS - 1) vm.nextQuestion()
        }
        vm.onGameOver()

        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()

        assertEquals(GameMode.SURVIVAL, vm.getEngineMode())
        assertEquals(0, vm.uiState.value.score)
        assertNotNull("Question should be loaded", vm.uiState.value.currentQ)
    }

    @Test
    fun crossMode_survivalToTimetrial_stateIsReset() {
        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()
        // Answer one correctly
        vm.answer(vm.uiState.value.currentQ!!.correct)
        assertTrue(vm.uiState.value.score > 0)

        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()

        assertEquals(GameMode.TIMETRIAL, vm.getEngineMode())
        assertEquals(0, vm.uiState.value.score)
        assertEquals(180f, vm.uiState.value.timer, 0.01f)
    }

    // ========================
    // Game over: onGameOver is idempotent
    // ========================

    @Test
    fun gameOver_onGameOverProcessedOnlyOnce() {
        vm.startQuickGame()
        for (i in 0 until Constants.QUICK_MODE_QUESTIONS) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < Constants.QUICK_MODE_QUESTIONS - 1) vm.nextQuestion()
        }
        vm.onGameOver()

        val xpAfterFirst = vm.xpGained.value
        val gamesAfterFirst = progressRepo.getGamesPlayed()

        // Call onGameOver again - should be no-op
        vm.onGameOver()

        assertEquals("XP should not change on double onGameOver", xpAfterFirst, vm.xpGained.value)
        assertEquals("Games played should not change", gamesAfterFirst, progressRepo.getGamesPlayed())
    }

    // ========================
    // Exam + game mode: switch from exam to game mode
    // ========================

    @Test
    fun crossMode_examToQuick_stateIsClean() {
        // Start exam
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)
        vm.examAnswer(vm.examEngine.getCurrentQuestion()!!.question.correct)
        vm.finishExam()
        assertNotNull(vm.examResult.value)

        // Clear and start quick
        vm.clearExamResult()
        val started = vm.startQuickGame()
        assertTrue(started)
        assertEquals(GameMode.QUICK, vm.getEngineMode())
        assertNotNull(vm.uiState.value.currentQ)
    }
}
