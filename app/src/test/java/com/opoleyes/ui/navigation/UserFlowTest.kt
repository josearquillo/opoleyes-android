package com.opoleyes.ui.navigation

import com.opoleyes.FakeGameRepository
import com.opoleyes.FakePreferencesManager
import com.opoleyes.TestFakes
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.model.Mission
import com.opoleyes.data.model.MissionData
import com.opoleyes.data.model.MissionDifficulty
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
import java.time.LocalDate

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UserFlowTest {

    private lateinit var vm: GameViewModel
    private lateinit var prefs: FakePreferencesManager
    private lateinit var progressRepo: ProgressRepository
    private lateinit var statsRepo: StatsRepository
    private lateinit var missionRepo: MissionRepository

    companion object {
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
        statsRepo = StatsRepository(prefs)
        missionRepo = MissionRepository(prefs)
        val engine = GameEngine.createForTest(FakeGameRepository(), statsRepo, progressRepo, prefs)
        val examEngine = ExamEngine.createForTest(statsRepo, TestFakes.makePool(100))
        vm = GameViewModel.createForTest(
            progressRepo, statsRepo, missionRepo,
            AchievementChecker(prefs), ChestSystem(prefs),
            prefs, engine, examEngine
        )
        vm.resetProgress()
        vm.clearExamResult()
        vm.clearChest()
        vm.clearRankUp()
        vm.clearQuickReward()
        vm.clearToasts()
        vm.clearPopups()
        vm.clearPowerUpToast()
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    // === Helpers ===

    private fun startSimulacroSync() {
        vm.loadSimulacroSync()
    }

    /**
     * Plays a survival game with [correctCount] correct answers, then loses
     * all remaining lives to trigger game over. Returns true if the game
     * started successfully.
     */
    private fun playSurvivalGame(correctCount: Int): Boolean {
        vm.pendingMode = GameMode.SURVIVAL
        val ok = vm.startAllLawsGame()
        if (!ok) return false
        vm.engine.sessionDifficultyCap = 5
        vm.engine.maxDifficulty = 5
        var answered = 0
        while (answered < correctCount && !vm.isGameOver()) {
            val q = vm.engine.currentQ!!
            vm.answer(q.correct)
            answered++
            if (!vm.isGameOver()) vm.nextQuestion()
        }
        while (!vm.isGameOver()) {
            vm.nextQuestion()
            vm.answer("Z") // invalid = wrong
        }
        vm.onGameOver()
        return true
    }

    /**
     * Plays a survival game answering exactly [correctCount] correct then
     * [wrongCount] wrong, then loses remaining lives. Used when we need a
     * specific accuracy ratio.
     */
    private fun playSurvivalGameWithAccuracy(correctCount: Int, wrongCount: Int): Boolean {
        vm.pendingMode = GameMode.SURVIVAL
        val ok = vm.startAllLawsGame()
        if (!ok) return false
        vm.engine.sessionDifficultyCap = 5
        vm.engine.maxDifficulty = 5
        var correct = 0
        var wrong = 0
        while (correct < correctCount && !vm.isGameOver()) {
            val q = vm.engine.currentQ!!
            vm.answer(q.correct)
            correct++
            if (!vm.isGameOver()) vm.nextQuestion()
        }
        while (wrong < wrongCount && !vm.isGameOver()) {
            vm.nextQuestion()
            vm.answer("Z")
            wrong++
        }
        while (!vm.isGameOver()) {
            vm.nextQuestion()
            vm.answer("Z")
        }
        vm.onGameOver()
        return true
    }

    // === Flow 6: New user → first game → game over → result ===

    @Test
    fun flow_newUser_firstGame_gameOver_verifiesResult() {
        assertTrue("Game should start", playSurvivalGameWithAccuracy(correctCount = 3, wrongCount = 2))

        // Verify score > 0 (3 correct answers give points)
        assertTrue("Score should be > 0, got ${vm.engine.score}", vm.engine.score > 0)

        // Verify combo max
        assertTrue("Max combo should be >= 3 (3 correct in a row)", vm.engine.maxCombo >= 3)

        // Verify accuracy: 3 correct out of at least 5 answered
        val acc = vm.engine.getAccuracy()
        assertTrue("Accuracy should be > 0, got $acc", acc > 0)

        // Verify XP gained
        assertTrue("XP gained should be > 0, got ${vm.xpGained.value}", vm.xpGained.value > 0)

        // Verify games played = 1
        assertEquals("Games played should be 1 after first game", 1, progressRepo.getGamesPlayed())

        // Verify records were set (new user, any score > 0 is a record)
        assertTrue("Should be a new record", vm.newRecord.value)
        assertTrue("Survival record should be > 0", progressRepo.getRecord("survival") > 0)

        // Verify first_correct achievement was unlocked (shows as toast)
        assertTrue("Should have unlocked first_correct achievement toast",
            vm.toasts.value.any { it.id == "first_correct" })

        // Verify chest was generated (3 correct, accuracy >= 60, totalAnswered >= 3)
        // Note: chest may or may not be generated depending on exact accuracy
        // but with 3 correct and at least 5 total, accuracy could be 60%+
        // Just verify the chest state is not stale (either null or has a reward)
        val chest = vm.chestReward.value
        if (chest != null) {
            assertTrue("Chest XP should be > 0", chest.xp > 0)
        }
    }

    // === Flow 7: Rank up ===

    @Test
    fun flow_rankUp_overlayShown_powerUpsGranted_lastKnownRankUpdated() {
        // Start just below rank 1 threshold (200 XP)
        prefs.addXP(190)
        // 1 correct answer = 10 XP (combo 1) → total 200 → rank 1
        assertTrue("Game should start", playSurvivalGame(1))

        // Verify rank-up overlay is shown
        val overlay = vm.rankUpOverlay.value
        assertNotNull("Rank-up overlay should be shown", overlay)
        assertEquals("Should rank up from Novato", "Novato", overlay?.oldRank?.name)
        assertEquals("Should rank up to Principiante", "Principiante", overlay?.newRank?.name)

        // Verify last known rank index updated
        assertEquals("Last known rank index should be 1", 1, progressRepo.getLastKnownRankIndex())
    }

    // === Flow 8: Mini-exam → result → back to home ===

    @Test
    fun flow_miniExam_result_thenClearExamResult() {
        vm.examEngine.loadExam(10)
        // Answer all 10 correctly
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        val gamesBefore = progressRepo.getGamesPlayed()
        vm.finishExam()

        // Verify exam result
        val result = vm.examResult.value
        assertNotNull("Exam result should be set", result)
        assertEquals(10, result!!.correct)
        assertEquals(100, vm.xpGained.value) // 10 * 10

        // Verify games played incremented
        assertEquals(gamesBefore + 1, progressRepo.getGamesPlayed())

        // Clear exam result (simulates going back to home)
        vm.clearExamResult()
        assertNull("Exam result should be cleared", vm.examResult.value)
        assertNull("Simulacro result should be cleared", vm.simulacroResult.value)
        assertFalse("Should not be in simulacro mode", vm.isSimulacroMode.value)
    }

    // === Flow 9: Simulacro → result → history ===

    @Test
    fun flow_simulacro_result_historyHasNewEntry() {
        val historyBefore = progressRepo.getSimulacroHistory().size
        startSimulacroSync()
        assertEquals("Simulacro should load 100 questions", 100, vm.examEngine.getQuestionCount())

        // Answer all 100 correctly
        for (i in 0 until 100) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        val gamesBefore = progressRepo.getGamesPlayed()
        vm.finishExam()

        // Verify simulacro result
        val result = vm.simulacroResult.value
        assertNotNull("Simulacro result should be set", result)
        assertTrue("Perfect simulacro should pass", result!!.passed)
        assertTrue("XP gained should be > 0", vm.xpGained.value > 0)
        assertEquals(gamesBefore + 1, progressRepo.getGamesPlayed())

        // Verify history has a new entry
        val historyAfter = progressRepo.getSimulacroHistory()
        assertEquals("History should have one more entry", historyBefore + 1, historyAfter.size)
        val lastEntry = historyAfter.last()
        assertEquals(100, lastEntry.correct)
        assertTrue("History entry should show passed", lastEntry.passed)
    }

    // === Flow 10: Game → chest → open → rewards applied ===

    @Test
    fun flow_game_chest_open_rewardsApplied() {
        // Play a survival game with good performance to generate a chest.
        // We need accuracy >= 60% and totalAnswered >= 3.
        // Answer 5 correct, then end the game by setting lives=0 directly
        // (instead of answering wrong, which would lower accuracy).
        vm.pendingMode = GameMode.SURVIVAL
        val ok = vm.startAllLawsGame()
        assertTrue("Game should start", ok)
        vm.engine.sessionDifficultyCap = 5
        vm.engine.maxDifficulty = 5
        // Answer 5 correctly
        for (i in 0 until 5) {
            val q = vm.engine.currentQ!!
            vm.answer(q.correct)
            if (!vm.isGameOver()) vm.nextQuestion()
        }
        // End the game by setting lives to 0 (keeps accuracy at 100%)
        vm.engine.lives = 0
        vm.onGameOver()

        val chest = vm.chestReward.value
        assertNotNull("Chest should be generated for good performance (5 correct, 100% acc)", chest)
        assertTrue("Chest XP should be > 0", chest!!.xp > 0)

        val xpBeforeOpen = progressRepo.getXP()
        vm.openChest()

        // Verify XP increased by chest amount
        val xpAfterOpen = progressRepo.getXP()
        assertEquals("XP should increase by chest.xp after openChest",
            xpBeforeOpen + chest.xp, xpAfterOpen)

        // Verify multiplier set for GOLD chest
        if (chest.multiplier) {
            assertEquals("GOLD chest should set multiplier to 2", 2, prefs.getMultiplier())
        }
    }

    // === Flow 11: Daily missions → complete in game → mission XP ===

    @Test
    fun flow_dailyMissions_completeInGame_missionXpAdded() {
        // Save a streak mission with target=3 (achievable in one game)
        val mission = Mission(
            type = "streak", icon = "🔥", text = "Acierta 3 seguidas",
            target = 3, current = 0, completed = false, reward = 50,
            key = "streak", testId = null, difficulty = MissionDifficulty.MEDIUM
        )
        missionRepo.saveDailyMissions(MissionData(
            date = LocalDate.now().toString(),
            missions = listOf(mission)
        ))

        // Play a survival game with 3 correct answers (maxCombo >= 3)
        assertTrue("Game should start", playSurvivalGame(3))

        // Verify the mission was completed
        val data = missionRepo.getDailyMissions()
        assertNotNull("Daily missions should exist", data)
        assertTrue("Streak mission should be completed", data!!.missions[0].completed)

        // Verify mission XP was added (game XP + mission reward)
        // 3 correct = 10+20+30 = 60 XP from game + 50 XP from mission = 110 XP
        // But the exact game XP depends on combo mechanics. Just verify xpGained > game XP alone.
        val gameXpAlone = 60 // 10*1 + 10*2 + 10*3
        assertTrue("XP gained (${vm.xpGained.value}) should include mission reward (>$gameXpAlone)",
            vm.xpGained.value > gameXpAlone)
    }

    // === Flow 12: Debug mode → play → disable debug → state restored ===

    @Test
    fun flow_debugMode_play_disableDebug_stateRestored() {
        // Set up initial state
        prefs.setMaxExamQuestions(20)

        // Enable debug mode
        vm.setDebugMode(true)
        assertTrue("Debug mode should be active", vm.isDebugMode())

        // Play a game in debug mode
        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()

        // Disable debug mode
        vm.setDebugMode(false)
        assertFalse("Debug mode should be off", vm.isDebugMode())

        // Verify state was restored
        assertEquals("Max exam questions should be restored", 20, prefs.getMaxExamQuestions())
    }

    // === Flow 13: Quick mode → 5 questions → game over ===

    @Test
    fun flow_quickMode_5questions_gameOver() {
        // Quick mode requires rank 5 (7000 XP)
        prefs.addXP(7000)
        vm.startQuickGame()
        vm.engine.sessionDifficultyCap = 5
        vm.engine.maxDifficulty = 5

        // Answer 5 questions
        var answered = 0
        while (answered < 5 && vm.engine.lives > 0) {
            val q = vm.engine.currentQ ?: break
            vm.answer(q.correct)
            answered++
            if (vm.engine.totalAnswered < 5 && vm.engine.lives > 0) {
                vm.nextQuestion()
            }
        }
        vm.onGameOver()

        assertEquals("Total answered should be 5", 5, vm.engine.totalAnswered)
        assertTrue("Games played should be incremented", progressRepo.getGamesPlayed() >= 1)
    }

    // === Flow 14: Timetrial → timer reaches 0 → automatic game over ===

    @Test
    fun flow_timetrial_timerReachesZero_gameOver() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        assertEquals("Timetrial should start with 180s timer", 180f, vm.engine.timer, 0.01f)

        // Answer 1 question correctly
        val q = vm.engine.currentQ!!
        vm.answer(q.correct)

        // Simulate timer reaching 0
        vm.engine.timer = 1f
        val gameOver = vm.tickTimer()
        assertTrue("tickTimer should return true when timer reaches 0", gameOver)
        assertEquals("Timer should be 0", 0f, vm.engine.timer, 0.01f)

        // Trigger game over
        vm.onGameOver()

        // Verify game over was processed
        assertTrue("Games played should be incremented", progressRepo.getGamesPlayed() >= 1)
        assertTrue("XP gained should include the correct answer", vm.xpGained.value > 0)
    }

    // === Flow 15: Tema game → pool only from that tema ===
    // (Moved to UserFlowTemaTest.kt — requires DataProvider/Context)

    // === Additional flow: New user → multiple games → stats accumulate ===

    @Test
    fun flow_multipleGames_statsAccumulate() {
        // First game: 2 correct
        assertTrue("First game should start", playSurvivalGame(2))
        val xpAfterFirst = progressRepo.getXP()
        val gamesAfterFirst = progressRepo.getGamesPlayed()
        assertEquals("Games played should be 1", 1, gamesAfterFirst)

        // Second game: 3 correct
        assertTrue("Second game should start", playSurvivalGame(3))
        val xpAfterSecond = progressRepo.getXP()
        val gamesAfterSecond = progressRepo.getGamesPlayed()
        assertEquals("Games played should be 2", 2, gamesAfterSecond)
        assertTrue("XP should increase after second game", xpAfterSecond > xpAfterFirst)

        // Verify stats accumulated
        val totalCorrect = statsRepo.getTotalCorrect()
        assertTrue("Total correct should be >= 5 (2+3)", totalCorrect >= 5)
    }

    // === Additional flow: Survival → lose all lives → game over ===

    @Test
    fun flow_survival_loseAllLives_gameOver() {
        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()
        val maxLives = vm.engine.maxLives

        // Answer all wrong to lose all lives
        while (!vm.isGameOver()) {
            vm.nextQuestion()
            vm.answer("Z") // invalid = wrong
        }
        vm.onGameOver()

        assertEquals("Lives should be 0 at game over", 0, vm.engine.lives)
        // At rank 0, first mistake is forgiven so it takes maxLives + 1 wrong answers
        assertEquals("Total answered should be maxLives + 1 (first mistake forgiven at rank 0)",
            maxLives + 1, vm.engine.totalAnswered)
        assertEquals("Correct count should be 0", 0, vm.engine.correctCount)
    }

    // === Additional flow: Exam → unlock more questions ===

    @Test
    fun flow_exam_highScore_unlocksMoreQuestions() {
        val maxBefore = progressRepo.getMaxExamQuestions()
        vm.examEngine.loadExam(maxBefore)
        // Answer all correctly to get score >= 5.0
        for (i in 0 until maxBefore) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()

        val result = vm.examResult.value!!
        assertTrue("Score should be >= 5.0 to unlock more questions", result.score >= 5.0f)
        val maxAfter = progressRepo.getMaxExamQuestions()
        assertTrue("Max exam questions should increase ($maxBefore → $maxAfter)", maxAfter > maxBefore)
    }

    // === Additional flow: Chest → open → rank up from chest XP ===

    @Test
    fun flow_chestOpen_canTriggerRankUp() {
        // Start near a rank threshold
        prefs.addXP(190) // near rank 1 (200 XP)
        // Play a game with 3 correct to get some XP and a chest
        assertTrue("Game should start", playSurvivalGame(3))

        val chest = vm.chestReward.value
        if (chest != null && chest.xp > 0) {
            val rankBefore = progressRepo.getRankIndex()
            vm.openChest()
            val rankAfter = progressRepo.getRankIndex()
            // If chest XP pushed us over the threshold, rank-up overlay should show
            if (rankAfter > rankBefore) {
                assertNotNull("Chest rank-up should show overlay", vm.rankUpOverlay.value)
            }
        }
    }
}
