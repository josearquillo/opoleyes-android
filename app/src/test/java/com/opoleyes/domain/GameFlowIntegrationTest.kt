package com.opoleyes.domain

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository
import com.opoleyes.ui.navigation.GameViewModel
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
class GameFlowIntegrationTest {

    private lateinit var vm: GameViewModel
    private lateinit var prefs: PreferencesManager
    private lateinit var progressRepo: ProgressRepository
    private lateinit var statsRepo: StatsRepository

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        prefs = PreferencesManager(app)
        prefs.resetAll()
        vm = GameViewModel(app)
        progressRepo = vm.getProgressRepo()
        statsRepo = vm.getStatsRepo()
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    // === Full game flow: start → answer → game over → chest → achievements ===

    @Test
    fun fun_fullGameFlow_survival_correctAnswers_grantsXPAndChest() {
        vm.startAllLawsGame()
        val xpBefore = progressRepo.getXP()

        // Answer 10 questions correctly
        for (i in 0 until 10) {
            val q = vm.uiState.value.currentQ!!
            vm.answer(q.correct)
            vm.nextQuestion()
            if (vm.isGameOver()) break
        }

        if (!vm.isGameOver()) {
            // Force game over by losing all lives
            while (!vm.isGameOver()) {
                val q = vm.uiState.value.currentQ!!
                val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
                vm.answer(wrong)
                vm.nextQuestion()
            }
        }

        vm.onGameOver()

        // XP should have increased
        assertTrue("XP should increase after game", progressRepo.getXP() > xpBefore)
        // Games played should be 1
        assertEquals(1, progressRepo.getGamesPlayed())
        // Should have a chest reward (score >= 100 with 10+ answered)
        assertNotNull("Should generate a chest", vm.chestReward.value)
    }

    @Test
    fun fun_fullGameFlow_openChest_grantsXpAndPowerUps() {
        vm.startAllLawsGame()
        // Answer enough correctly to get a good chest
        for (i in 0 until 10) {
            val q = vm.uiState.value.currentQ!!
            vm.answer(q.correct)
            vm.nextQuestion()
            if (vm.isGameOver()) break
        }
        if (!vm.isGameOver()) {
            while (!vm.isGameOver()) {
                val q = vm.uiState.value.currentQ!!
                val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
                vm.answer(wrong)
                vm.nextQuestion()
            }
        }
        vm.onGameOver()

        val chest = vm.chestReward.value
        assertNotNull(chest)
        val xpBefore = progressRepo.getXP()
        vm.openChest()
        assertTrue("Opening chest should grant XP", progressRepo.getXP() > xpBefore)
    }

    @Test
    fun fun_fullGameFlow_newRecord_saved() {
        vm.startAllLawsGame()
        for (i in 0 until 10) {
            val q = vm.uiState.value.currentQ!!
            vm.answer(q.correct)
            vm.nextQuestion()
            if (vm.isGameOver()) break
        }
        if (!vm.isGameOver()) {
            while (!vm.isGameOver()) {
                val q = vm.uiState.value.currentQ!!
                val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
                vm.answer(wrong)
                vm.nextQuestion()
            }
        }
        vm.onGameOver()

        // First game should always be a new record
        assertTrue("First game should be a new record", vm.newRecord.value)
        assertTrue(progressRepo.getRecord("survival") > 0)
    }

    @Test
    fun fun_fullGameFlow_achievementsUnlocked() {
        vm.startAllLawsGame()
        val q = vm.uiState.value.currentQ!!
        vm.answer(q.correct) // first_correct achievement

        // Check that first_correct achievement was unlocked
        val achievements = progressRepo.getAchievements()
        assertTrue("first_correct achievement should be unlocked", achievements.containsKey("first_correct"))
    }

    @Test
    fun fun_fullGameFlow_wrongAnswers_updateStats() {
        vm.startAllLawsGame()
        val q = vm.uiState.value.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        vm.answer(wrong)

        val stats = statsRepo.getStats()
        val key = "${q.testId}:${q.origId}"
        assertNotNull("Stats should have entry for answered question", stats[key])
        assertTrue("Wrong count should be >= 1", stats[key]!!.wrong >= 1)
    }

    @Test
    fun fun_fullGameFlow_correctAnswers_updateStats() {
        vm.startAllLawsGame()
        val q = vm.uiState.value.currentQ!!
        vm.answer(q.correct)

        val stats = statsRepo.getStats()
        val key = "${q.testId}:${q.origId}"
        assertNotNull("Stats should have entry for answered question", stats[key])
        assertTrue("Correct count should be >= 1", stats[key]!!.correct >= 1)
    }

    @Test
    fun fun_fullGameFlow_comboIncreasesScore() {
        vm.startAllLawsGame()
        val q1 = vm.uiState.value.currentQ!!
        vm.answer(q1.correct)
        val scoreAfter1 = vm.uiState.value.score
        vm.nextQuestion()
        val q2 = vm.uiState.value.currentQ!!
        vm.answer(q2.correct)
        val scoreAfter2 = vm.uiState.value.score

        assertTrue("Score should increase with combo", scoreAfter2 > scoreAfter1)
        assertTrue("Combo should be 2", vm.uiState.value.combo >= 2)
    }

    @Test
    fun fun_fullGameFlow_exitGame_savesPowerUps() {
        vm.startAllLawsGame()
        vm.engine.hintCharges = 2
        vm.engine.shieldCharges = 1
        vm.exitGame()

        val saved = prefs.getFreePowerUps()
        assertTrue("Should save remaining hint charges", saved.count { it == "hint" } >= 2)
        assertTrue("Should save remaining shield charges", saved.count { it == "shield" } >= 1)
    }

    @Test
    fun fun_fullGameFlow_gameOverIncrementsGamesPlayed() {
        val before = progressRepo.getGamesPlayed()
        vm.startAllLawsGame()
        // Lose all lives
        while (!vm.isGameOver()) {
            val q = vm.uiState.value.currentQ!!
            val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
            vm.answer(wrong)
            vm.nextQuestion()
        }
        vm.onGameOver()
        assertEquals(before + 1, progressRepo.getGamesPlayed())
    }

    @Test
    fun fun_fullGameFlow_rankUp_grantsPowerUpRewards() {
        // Give enough XP to be near a rank up
        prefs.addXP(400) // Close to rank 1 (500 XP)
        vm.startAllLawsGame()
        for (i in 0 until 10) {
            val q = vm.uiState.value.currentQ!!
            vm.answer(q.correct)
            vm.nextQuestion()
            if (vm.isGameOver()) break
        }
        if (!vm.isGameOver()) {
            while (!vm.isGameOver()) {
                val q = vm.uiState.value.currentQ!!
                val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
                vm.answer(wrong)
                vm.nextQuestion()
            }
        }
        vm.onGameOver()

        // If rank increased, should have rank up overlay
        if (vm.rankUpOverlay.value != null) {
            val powerUps = prefs.getFreePowerUps()
            assertTrue("Rank up should grant power-up rewards", powerUps.isNotEmpty())
        }
    }
}
