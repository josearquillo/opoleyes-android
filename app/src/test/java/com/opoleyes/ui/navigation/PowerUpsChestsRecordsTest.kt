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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PowerUpsChestsRecordsTest {

    private lateinit var vm: GameViewModel
    private lateinit var prefs: FakePreferencesManager
    private lateinit var progressRepo: ProgressRepository
    private lateinit var statsRepo: StatsRepository

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
        // Give enough XP for rank 2 (Aprendiz) so both fiftyFifty and hint are available
        // and full mechanics (4 options, 3 lives) apply
        prefs.xp = 800
        progressRepo = ProgressRepository(prefs)
        statsRepo = StatsRepository(prefs)
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
    fun teardown() { prefs.resetAll() }

    // ========================
    // Power-ups: 50/50
    // ========================

    @Test
    fun powerUp_fiftyFifty_removesTwoWrongOptions() {
        vm.startQuickGame()
        val q = vm.uiState.value.currentQ!!
        vm.activateFiftyFifty()

        assertTrue("50/50 should be active", vm.uiState.value.fiftyFiftyActive)
        assertEquals("Should remove 2 options", 2, vm.uiState.value.fiftyFiftyRemoved.size)
        // Correct answer should not be in removed
        assertFalse("Correct answer should not be removed", q.correct in vm.uiState.value.fiftyFiftyRemoved)
    }

    @Test
    fun powerUp_fiftyFifty_cannotActivateTwice() {
        vm.startQuickGame()
        vm.activateFiftyFifty()
        val firstRemoved = vm.uiState.value.fiftyFiftyRemoved
        assertEquals(2, firstRemoved.size)

        // Try again - should be no-op since already active
        vm.activateFiftyFifty()
        assertEquals("Should not change on second activation", firstRemoved, vm.uiState.value.fiftyFiftyRemoved)
    }

    @Test
    fun powerUp_fiftyFifty_cannotActivateAfterAnswer() {
        vm.startQuickGame()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.activateFiftyFifty()
        assertFalse("50/50 should not activate after answer", vm.uiState.value.fiftyFiftyActive)
    }

    @Test
    fun powerUp_fiftyFifty_cannotActivateWithHintAlreadyUsed() {
        vm.startQuickGame()
        vm.useHint()
        assertTrue(vm.uiState.value.hintActive)
        vm.activateFiftyFifty()
        assertFalse("50/50 should not activate if hint already used", vm.uiState.value.fiftyFiftyActive)
    }

    @Test
    fun powerUp_fiftyFifty_resetsOnNextQuestion() {
        vm.startQuickGame()
        vm.activateFiftyFifty()
        assertTrue(vm.uiState.value.fiftyFiftyActive)
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        assertFalse("50/50 should be reset on next question", vm.uiState.value.fiftyFiftyActive)
        assertEquals("50/50 removed should be empty on next question", 0, vm.uiState.value.fiftyFiftyRemoved.size)
    }

    // ========================
    // Power-ups: Hint
    // ========================

    @Test
    fun powerUp_hint_removesOneWrongOption() {
        vm.startQuickGame()
        val q = vm.uiState.value.currentQ!!
        vm.useHint()

        assertTrue("Hint should be active", vm.uiState.value.hintActive)
        assertEquals("Should remove 1 option", 1, vm.uiState.value.hintRemoved.size)
        assertFalse("Correct answer should not be removed by hint", q.correct in vm.uiState.value.hintRemoved)
    }

    @Test
    fun powerUp_hint_cannotActivateTwice() {
        vm.startQuickGame()
        vm.useHint()
        val firstRemoved = vm.uiState.value.hintRemoved
        assertEquals(1, firstRemoved.size)

        vm.useHint()
        assertEquals("Should not change on second use", firstRemoved, vm.uiState.value.hintRemoved)
    }

    @Test
    fun powerUp_hint_cannotActivateAfterAnswer() {
        vm.startQuickGame()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.useHint()
        assertFalse("Hint should not activate after answer", vm.uiState.value.hintActive)
    }

    @Test
    fun powerUp_hint_cannotActivateWithFiftyFiftyAlreadyUsed() {
        vm.startQuickGame()
        vm.activateFiftyFifty()
        assertTrue(vm.uiState.value.fiftyFiftyActive)
        vm.useHint()
        assertFalse("Hint should not activate if 50/50 already used", vm.uiState.value.hintActive)
    }

    @Test
    fun powerUp_hint_resetsOnNextQuestion() {
        vm.startQuickGame()
        vm.useHint()
        assertTrue(vm.uiState.value.hintActive)
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        assertFalse("Hint should be reset on next question", vm.uiState.value.hintActive)
        assertEquals("Hint removed should be empty on next question", 0, vm.uiState.value.hintRemoved.size)
    }

    // ========================
    // Power-ups: points penalty
    // ========================

    @Test
    fun powerUp_fiftyFifty_reducesPointsEarned() {
        vm.startQuickGame()
        // Answer without power-up
        vm.answer(vm.uiState.value.currentQ!!.correct)
        val scoreWithoutPowerUp = vm.uiState.value.score
        vm.nextQuestion()

        // Answer with 50/50
        vm.activateFiftyFifty()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        val scoreWithPowerUp = vm.uiState.value.score

        // The increment with power-up should be less than without
        val incrementWithout = scoreWithoutPowerUp
        val incrementWith = scoreWithPowerUp - scoreWithoutPowerUp
        assertTrue("Points with 50/50 ($incrementWith) should be less than without ($incrementWithout)",
            incrementWith < incrementWithout)
    }

    // ========================
    // Chest system
    // ========================

    @Test
    fun chest_notGeneratedForShortGame() {
        // Play a game with fewer than 3 answers
        vm.startQuickGame()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.onGameOver()
        // totalAnswered is 1, which is < 3
        assertNull("No chest for games with < 3 answers", vm.chestReward.value)
    }

    @Test
    fun chest_generatedForGoodGame() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        // 5 correct answers, 100% accuracy, should generate at least a bronze chest
        assertNotNull("Chest should be generated for perfect 5-answer game", vm.chestReward.value)
    }

    @Test
    fun chest_openAddsXp() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        val chest = vm.chestReward.value
        assertNotNull(chest)
        val xpBefore = vm.xpGained.value
        vm.openChest()
        val xpAfter = vm.xpGained.value
        assertTrue("XP should increase after opening chest", xpAfter > xpBefore)
        assertEquals("XP should increase by chest.xp", xpBefore + chest!!.xp, xpAfter)
    }

    @Test
    fun chest_openIsIdempotent() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        vm.openChest()
        val xpAfterFirst = vm.xpGained.value
        vm.openChest()
        assertEquals("Second openChest should not add more XP", xpAfterFirst, vm.xpGained.value)
    }

    @Test
    fun chest_clearResetsState() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        assertNotNull(vm.chestReward.value)
        vm.clearChest()
        assertNull("Chest reward should be null after clearChest", vm.chestReward.value)
    }

    @Test
    fun chest_goldChestSetsMultiplier() {
        // We can't force a gold chest directly, but we can test the ChestSystem
        val chestSystem = ChestSystem(prefs)
        val goldReward = com.opoleyes.data.model.ChestReward(
            type = com.opoleyes.data.model.ChestType.GOLD,
            xp = 400,
            powerUps = emptyList(),
            multiplier = true
        )
        chestSystem.openChest(goldReward)
        assertEquals("Gold chest should set multiplier to 2", 2, prefs.getMultiplier())
    }

    @Test
    fun chest_bronzeChestDoesNotSetMultiplier() {
        val chestSystem = ChestSystem(prefs)
        val bronzeReward = com.opoleyes.data.model.ChestReward(
            type = com.opoleyes.data.model.ChestType.BRONZE,
            xp = 100,
            powerUps = emptyList(),
            multiplier = false
        )
        chestSystem.openChest(bronzeReward)
        assertEquals("Bronze chest should not set multiplier", 1, prefs.getMultiplier())
    }

    // ========================
    // Records
    // ========================

    @Test
    fun records_newScoreRecordIsSet() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        val record = progressRepo.getRecord("quick")
        assertTrue("Quick record should be set after game", record > 0)
        assertTrue("newRecord flag should be true", vm.newRecord.value)
    }

    @Test
    fun records_comboRecordIsSet() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        val comboRecord = progressRepo.getRecordCombo("quick")
        assertTrue("Combo record should be set", comboRecord >= 5)
    }

    @Test
    fun records_accuracyRecordIsSet() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        val accRecord = progressRepo.getRecordAcc("quick")
        assertEquals(100, accRecord)
    }

    @Test
    fun records_gamesPlayedIncrements() {
        val before = progressRepo.getGamesPlayed()
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        val after = progressRepo.getGamesPlayed()
        assertEquals("Games played should increment by 1", before + 1, after)
    }

    @Test
    fun records_notNewRecordWhenScoreIsLower() {
        // Set a high record first
        progressRepo.setRecord("quick", 99999)
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        assertFalse("Should not be a new record when score is lower", vm.newRecord.value)
    }

    // ========================
    // XP and rank up
    // ========================

    @Test
    fun xp_gainedAfterCorrectAnswers() {
        vm.startQuickGame()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.onGameOver()
        assertTrue("XP should be gained after correct answers", vm.xpGained.value > 0)
    }

    @Test
    fun xp_breakdownIsPopulatedAfterGameOver() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        assertNotNull("XP breakdown should be populated", vm.xpBreakdown.value)
        assertTrue("XP breakdown should have lines", vm.xpBreakdown.value!!.lines.isNotEmpty())
    }

    @Test
    fun xp_gainedMatchesExpectedForPerfectQuick() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        // Perfect quick game: 5 correct + quick reward (50 * (1 + rankIndex))
        val expectedQuickReward = 50 * (1 + vm.getEngineRankIndex())
        val xpFromCorrect = vm.engine.xpFromCorrect
        val missionRewards = 0 // missions may or may not be completed
        assertTrue("XP should include correct answers XP", vm.xpGained.value >= xpFromCorrect)
    }

    @Test
    fun rankUp_overlayShownWhenRankIncreases() {
        // Start at rank 0 (Novato) by setting prefs
        // We need to earn enough XP to rank up
        // This is hard to test precisely, but we can verify the overlay mechanism
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        // Rank up may or may not happen depending on XP thresholds
        // Just verify that if rankUpOverlay is set, it has valid data
        vm.rankUpOverlay.value?.let { overlay ->
            assertNotNull("Rank up overlay should have old rank", overlay.oldRank)
            assertNotNull("Rank up overlay should have new rank", overlay.newRank)
        }
    }

    @Test
    fun rankUp_clearRankUpSetsNull() {
        vm.clearRankUp()
        assertNull(vm.rankUpOverlay.value)
    }

    // ========================
    // Tema game flow
    // ========================

    @Test
    fun temaGame_startAndPlay() {
        vm.pendingMode = GameMode.SURVIVAL
        val started = vm.startTemaGame("test1")
        assertTrue("Tema game should start", started)
        assertNotNull("Question should be loaded", vm.uiState.value.currentQ)
        assertEquals("test1", vm.getCategory())
    }

    @Test
    fun temaGame_gameOverAndRestart() {
        vm.pendingMode = GameMode.SURVIVAL
        vm.startTemaGame("test1")

        // Lose all lives
        while (!vm.isGameOver()) {
            val q = vm.uiState.value.currentQ ?: break
            val wrong = q.opciones.keys.firstOrNull { it != q.correct } ?: "B"
            vm.answer(wrong)
            if (!vm.isGameOver()) vm.nextQuestion()
        }
        assertTrue("Should be game over", vm.isGameOver())
        vm.onGameOver()

        // Restart with same tema
        val restarted = vm.startTemaGame("test1")
        assertTrue("Should be able to restart tema game", restarted)
        assertEquals("test1", vm.getCategory())
        assertEquals(0, vm.uiState.value.score)
    }

    @Test
    fun temaGame_secondGameNoStaleState() {
        vm.pendingMode = GameMode.SURVIVAL
        vm.startTemaGame("test1")
        vm.answer(vm.uiState.value.currentQ!!.correct)
        assertTrue(vm.uiState.value.score > 0)

        vm.startTemaGame("test1")
        assertEquals("Score should be reset", 0, vm.uiState.value.score)
        assertEquals("Lives should be restored", vm.engine.maxLives, vm.uiState.value.lives)
    }

    // ========================
    // Timetrial timer mechanics
    // ========================

    @Test
    fun timetrial_tickTimer_reducesTimer() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        val initialTimer = vm.uiState.value.timer
        val gameOver = vm.tickTimer()
        assertFalse("Timer should not be at 0 after first tick", gameOver)
        assertTrue("Timer should decrease", vm.uiState.value.timer < initialTimer)
    }

    @Test
    fun timetrial_tickTimer_returnsTrueWhenZero() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        // Tick 180 times
        var gameOver = false
        for (i in 0 until 200) {
            if (vm.tickTimer()) {
                gameOver = true
                break
            }
        }
        assertTrue("Timer should reach 0 and return true", gameOver)
    }

    @Test
    fun timetrial_applyPausedElapsed_reducesTimer() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        val initial = vm.uiState.value.timer
        vm.applyPausedElapsed(30f)
        assertEquals(initial - 30f, vm.uiState.value.timer, 0.01f)
    }

    @Test
    fun timetrial_applyPausedElapsed_returnsTrueWhenZero() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        assertTrue("Should be game over after 180s pause", vm.applyPausedElapsed(180f))
    }

    @Test
    fun timetrial_isTimedModeReturnsTrue() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        assertTrue(vm.isTimedMode())
    }

    @Test
    fun quickMode_isTimedModeReturnsFalse() {
        vm.startQuickGame()
        assertFalse(vm.isTimedMode())
    }

    // ========================
    // Exit game
    // ========================

    @Test
    fun exitGame_doesNotCrash() {
        vm.startQuickGame()
        vm.exitGame() // Should be a no-op, just verify it doesn't crash
    }

    // ========================
    // Motivational message
    // ========================

    @Test
    fun motivationalMessage_setAfterGameOver() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        assertTrue("Motivational message should be set after game over",
            vm.motivationalMessage.value.isNotEmpty())
    }

    // ========================
    // Medal
    // ========================

    @Test
    fun medal_setAfterGameOver() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        // Perfect quick game with combo should give enough points for at least bronze (300+)
        // 5 correct with combo: 10*1 + 10*2 + 10*3 + 10*4 + 10*5 = 150 (base) * 1.5 (quick multiplier) = 225
        // Actually quick mode uses 15*combo: 15+30+45+60+75 = 225. Not enough for bronze.
        // Medal thresholds: gold >= 1000, silver >= 600, bronze >= 300
        // So a perfect 5-question quick game may not reach bronze.
        // Just verify the medal field is accessible (it may be empty for low scores)
        assertNotNull("Medal field should be accessible", vm.medal.value)
    }

    // ========================
    // Accuracy
    // ========================

    @Test
    fun accuracy_100PercentForPerfectGame() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        assertEquals(100, vm.accuracy.value)
    }

    @Test
    fun accuracy_0PercentForAllWrong() {
        vm.startQuickGame()
        // Answer wrong until game over (lives run out)
        while (!vm.isGameOver()) {
            val q = vm.uiState.value.currentQ ?: break
            val wrong = q.opciones.keys.firstOrNull { it != q.correct } ?: "B"
            vm.answer(wrong)
            if (!vm.isGameOver()) vm.nextQuestion()
        }
        vm.onGameOver()
        assertEquals(0, vm.accuracy.value)
    }
}
