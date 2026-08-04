package com.opoleyes.ui.navigation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.repository.ProgressRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameViewModelTest {

    private lateinit var vm: GameViewModel
    private lateinit var prefs: PreferencesManager
    private lateinit var progressRepo: ProgressRepository

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        prefs = PreferencesManager(app)
        prefs.resetAll()
        progressRepo = ProgressRepository(app)
        vm = GameViewModel(app)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun finishExam_guardAgainstDoubleSubmission() {
        // Start and complete an exam
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        val gamesBefore = progressRepo.getGamesPlayed()
        vm.finishExam()
        val gamesAfterFirst = progressRepo.getGamesPlayed()
        assertEquals("gamesPlayed should increment by 1 after first finishExam",
            gamesBefore + 1, gamesAfterFirst)
        assertNotNull("examResult should be set", vm.examResult.value)

        // Second call should be a no-op
        vm.finishExam()
        val gamesAfterSecond = progressRepo.getGamesPlayed()
        assertEquals("gamesPlayed should NOT increment on double submission",
            gamesAfterFirst, gamesAfterSecond)
    }

    @Test
    fun finishSimulacro_guardAgainstDoubleSubmission() {
        // Start simulacro via async API and wait for completion
        val latch = CountDownLatch(1)
        vm.startSimulacroAsync { latch.countDown() }
        latch.await(30, TimeUnit.SECONDS)

        val count = vm.examEngine.getQuestionCount()
        for (i in 0 until count) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        val gamesBefore = progressRepo.getGamesPlayed()
        vm.finishExam() // finishExam routes to finishSimulacro when in simulacro mode
        val gamesAfterFirst = progressRepo.getGamesPlayed()
        assertEquals("gamesPlayed should increment by 1 after first finishSimulacro",
            gamesBefore + 1, gamesAfterFirst)
        assertNotNull("simulacroResult should be set", vm.simulacroResult.value)

        // Second call should be a no-op
        vm.finishExam()
        val gamesAfterSecond = progressRepo.getGamesPlayed()
        assertEquals("gamesPlayed should NOT increment on double simulaco submission",
            gamesAfterFirst, gamesAfterSecond)
    }

    // === Mode intro / onboarding ===

    @Test
    fun getIntroKey_survivalPerRankClampedAt2() {
        assertEquals("intro_survival_rank_0", vm.getIntroKey(GameMode.SURVIVAL, 0))
        assertEquals("intro_survival_rank_1", vm.getIntroKey(GameMode.SURVIVAL, 1))
        assertEquals("intro_survival_rank_2", vm.getIntroKey(GameMode.SURVIVAL, 2))
        assertEquals("Ranks 3+ clamp to rank_2", "intro_survival_rank_2", vm.getIntroKey(GameMode.SURVIVAL, 3))
        assertEquals("Ranks 8 clamp to rank_2", "intro_survival_rank_2", vm.getIntroKey(GameMode.SURVIVAL, 8))
    }

    @Test
    fun getIntroKey_otherModesAreRankIndependent() {
        assertEquals("intro_timetrial", vm.getIntroKey(GameMode.TIMETRIAL, 0))
        assertEquals("intro_timetrial", vm.getIntroKey(GameMode.TIMETRIAL, 8))
        assertEquals("intro_quick", vm.getIntroKey(GameMode.QUICK, 0))
        assertEquals("intro_quick", vm.getIntroKey(GameMode.QUICK, 8))
        assertEquals("intro_exam", vm.getIntroKey(GameMode.EXAM, 0))
        assertEquals("intro_exam", vm.getIntroKey(GameMode.EXAM, 8))
    }

    @Test
    fun getIntroKey_simulacroHasKey() {
        assertEquals("intro_simulacro", vm.getIntroKey(GameMode.SIMULACRO, 0))
    }

    @Test
    fun shouldShowModeIntro_trueByDefault() {
        // Fresh user at rank 0 (prefs.resetAll in setup clears XP).
        assertTrue("Survival intro should show by default", vm.shouldShowModeIntro(GameMode.SURVIVAL))
        assertTrue("Timetrial intro should show by default", vm.shouldShowModeIntro(GameMode.TIMETRIAL))
        assertTrue("Quick intro should show by default", vm.shouldShowModeIntro(GameMode.QUICK))
        assertTrue("Exam intro should show by default", vm.shouldShowModeIntro(GameMode.EXAM))
    }

    @Test
    fun shouldShowModeIntro_falseForSimulacro() {
        // Simulacro has its own dedicated intro screen, so the generic intro is disabled.
        assertFalse("Simulacro should not use generic intro", vm.shouldShowModeIntro(GameMode.SIMULACRO))
    }

    @Test
    fun dismissModeIntro_withDontShowAgain_persistsFlag() {
        // User at rank 0 dismisses survival intro
        vm.dismissModeIntro(GameMode.SURVIVAL, dontShowAgain = true)
        assertFalse("Survival intro should be dismissed", vm.shouldShowModeIntro(GameMode.SURVIVAL))
        // Other modes still show
        assertTrue("Timetrial should still show", vm.shouldShowModeIntro(GameMode.TIMETRIAL))
    }

    @Test
    fun dismissModeIntro_withoutDontShowAgain_keepsShowing() {
        vm.dismissModeIntro(GameMode.SURVIVAL, dontShowAgain = false)
        assertTrue("Survival intro should still show when not dismissed", vm.shouldShowModeIntro(GameMode.SURVIVAL))
    }

    @Test
    fun dismissModeIntro_survivalRanksAreIndependent() {
        // Dismiss rank 0 intro; rank 1 should still show.
        // We need to be at rank 0 to dismiss the rank 0 key.
        // prefs.resetAll() in setup leaves XP=0 -> rank 0.
        vm.dismissModeIntro(GameMode.SURVIVAL, dontShowAgain = true)
        assertFalse(vm.shouldShowModeIntro(GameMode.SURVIVAL))

        // Promote to rank 1 (XP 200) and verify rank 1 intro still shows.
        prefs.addXP(200)
        assertTrue("Rank 1 survival intro should still show", vm.shouldShowModeIntro(GameMode.SURVIVAL))
    }

    @Test
    fun dismissModeIntro_survivalRank2CoversHigherRanks() {
        // Dismiss the rank-2 "modo completo" intro. All ranks >= 2 should skip it.
        prefs.addXP(800) // rank 2 (Aprendiz)
        vm.dismissModeIntro(GameMode.SURVIVAL, dontShowAgain = true)
        assertFalse("Rank 2 intro dismissed", vm.shouldShowModeIntro(GameMode.SURVIVAL))

        prefs.addXP(2000) // rank 3 (Estudiante)
        assertFalse("Rank 3 reuses rank_2 key -> still dismissed", vm.shouldShowModeIntro(GameMode.SURVIVAL))

        prefs.addXP(50000) // rank 8 (Leyenda)
        assertFalse("Rank 8 reuses rank_2 key -> still dismissed", vm.shouldShowModeIntro(GameMode.SURVIVAL))
    }

    @Test
    fun dismissModeIntro_otherModesAreOneShot() {
        vm.dismissModeIntro(GameMode.TIMETRIAL, dontShowAgain = true)
        assertFalse(vm.shouldShowModeIntro(GameMode.TIMETRIAL))

        // Promote to a higher rank; timetrial intro should still be dismissed.
        prefs.addXP(50000) // rank 8
        assertFalse("Timetrial intro stays dismissed across ranks", vm.shouldShowModeIntro(GameMode.TIMETRIAL))
    }

    // === Functional lifecycle tests ===

    private fun playSurvivalGame(correctCount: Int): Boolean {
        vm.pendingMode = GameMode.SURVIVAL
        val ok = vm.startAllLawsGame()
        if (!ok) return false
        var answered = 0
        while (answered < correctCount && !vm.isGameOver()) {
            val q = vm.engine.currentQ!!
            vm.answer(q.correct)
            answered++
            if (!vm.isGameOver()) vm.nextQuestion()
        }
        // End the game by failing remaining lives
        while (!vm.isGameOver()) {
            vm.nextQuestion()
            vm.answer("Z") // invalid = wrong
        }
        vm.onGameOver()
        return true
    }

    @Test
    fun gameLifeCycle_startGame_clearsRankUpAndChestState() {
        // Simulate stale state from a previous game
        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()
        // Manually trigger onGameOver to populate state
        vm.engine.lives = 0
        vm.onGameOver()
        // Verify state was populated
        assertNotNull("chestReward or xpBreakdown should be set after game over",
            vm.chestReward.value ?: vm.xpBreakdown.value)

        // Start a new game - should clear stale state
        vm.startAllLawsGame()
        assertNull("rankUpOverlay should be cleared on new game", vm.rankUpOverlay.value)
        assertNull("chestReward should be cleared on new game", vm.chestReward.value)
    }

    @Test
    fun gameLifeCycle_noRankUp_overlayIsNull() {
        // User at rank 0 (0 XP). Answer 1 question correctly = 10 XP.
        // 10 XP is not enough to reach rank 1 (200 XP).
        assertTrue(playSurvivalGame(1))
        assertNull("rankUpOverlay should be null when no rank-up occurred",
            vm.rankUpOverlay.value)
    }

    @Test
    fun gameLifeCycle_rankUp_overlayIsShown() {
        // Give user enough XP to be close to rank 1 (needs 200 XP).
        // 19 correct answers = 190 XP (combo 1..19, 10+20+...+190 = 1900 XP).
        // Actually 10*1 + 10*2 + ... + 10*19 = 10 * (19*20/2) = 1900 XP.
        // That's way more than 200. Let's give 190 XP upfront and answer 1 question.
        prefs.addXP(190)
        // 1 correct answer with combo=1 gives 10 XP -> total 200 -> rank 1
        assertTrue(playSurvivalGame(1))
        assertNotNull("rankUpOverlay should be shown when rank-up occurred",
            vm.rankUpOverlay.value)
        assertEquals("Should have ranked up from Novato to Principiante",
            "Novato", vm.rankUpOverlay.value?.oldRank?.name)
        assertEquals("Should have ranked up to Principiante",
            "Principiante", vm.rankUpOverlay.value?.newRank?.name)
    }

    @Test
    fun gameLifeCycle_rankUp_grantsPowerUpRewards() {
        // Rank 0 -> 1 should grant fiftyFifty and hint (per RANK_POWERUP_REWARDS)
        prefs.addXP(190)
        assertTrue(playSurvivalGame(1))
        val overlay = vm.rankUpOverlay.value
        assertNotNull(overlay)
        assertTrue("Rank-up to rank 1 should grant fiftyFifty",
            overlay!!.powerUpRewards.contains("fiftyFifty"))
        assertTrue("Rank-up to rank 1 should grant hint",
            overlay.powerUpRewards.contains("hint"))
    }

    @Test
    fun gameLifeCycle_secondGame_noStaleRankUpOverlay() {
        // First game: cause a rank-up
        prefs.addXP(190)
        assertTrue(playSurvivalGame(1))
        assertNotNull("First game should have rank-up overlay", vm.rankUpOverlay.value)
        vm.clearRankUp()

        // Second game: no rank-up (already at rank 1, only 10 more XP)
        assertTrue(playSurvivalGame(1))
        assertNull("Second game should NOT have stale rank-up overlay",
            vm.rankUpOverlay.value)
    }

    @Test
    fun gameLifeCycle_xpGainedMatchesExpected() {
        // Fresh user (0 XP). 1 correct answer = 10 XP (combo 1).
        assertTrue(playSurvivalGame(1))
        assertEquals("XP gained should be 10 for 1 correct answer",
            10, vm.xpGained.value)
        assertEquals("Total XP should be 10",
            10, progressRepo.getXP())
    }

    @Test
    fun gameLifeCycle_gameOverProcessedOnlyOnce() {
        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()
        vm.engine.lives = 0
        vm.onGameOver()
        val xpAfterFirst = progressRepo.getXP()
        val gamesAfterFirst = progressRepo.getGamesPlayed()
        // Second call should be a no-op
        vm.onGameOver()
        assertEquals("XP should not change on double onGameOver",
            xpAfterFirst, progressRepo.getXP())
        assertEquals("gamesPlayed should not change on double onGameOver",
            gamesAfterFirst, progressRepo.getGamesPlayed())
    }

    @Test
    fun gameLifeCycle_chestRankUp_detectedOnOpen() {
        // Give user 190 XP. A correct answer gives 10 XP (total 200, rank 1).
        // Then the chest gives bonus XP that could push to rank 2 (800 XP).
        // We need a chest to be generated. Chest requires totalAnswered >= 3
        // and accuracy >= 60. Let's answer 3 correctly, then fail to end game.
        prefs.addXP(190)
        // Answer 3 correctly then fail all remaining lives
        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()
        // Answer 3 correctly (combo 1,2,3 = 10+20+30 = 60 XP -> total 250)
        vm.answer("A"); vm.nextQuestion()
        vm.answer("A"); vm.nextQuestion()
        vm.answer("A")
        // Now fail to end the game (lose all lives)
        while (!vm.isGameOver()) {
            vm.nextQuestion()
            vm.answer("Z") // invalid answer = wrong
        }
        vm.onGameOver()
        // If a chest was generated, opening it should detect rank-up
        val chest = vm.chestReward.value
        if (chest != null) {
            val rankBeforeOpen = progressRepo.getRankIndex()
            vm.openChest()
            val rankAfterOpen = progressRepo.getRankIndex()
            if (rankAfterOpen > rankBeforeOpen) {
                assertNotNull("openChest should trigger rank-up overlay when XP crosses threshold",
                    vm.rankUpOverlay.value)
            }
        }
    }

    @Test
    fun gameLifeCycle_resetProgress_clearsOverlays() {
        // Populate state
        prefs.addXP(190)
        assertTrue(playSurvivalGame(1))
        // resetProgress should clear everything
        vm.resetProgress()
        assertNull("rankUpOverlay should be null after resetProgress",
            vm.rankUpOverlay.value)
        assertNull("chestReward should be null after resetProgress",
            vm.chestReward.value)
        assertEquals("XP should be 0 after resetProgress",
            0, progressRepo.getXP())
    }

    @Test
    fun gameLifeCycle_quickMode_clearsOverlaysOnStart() {
        // Populate state from survival
        prefs.addXP(190)
        assertTrue(playSurvivalGame(1))
        assertNotNull(vm.rankUpOverlay.value)
        // Start quick game - should clear
        vm.startQuickGame()
        assertNull("rankUpOverlay should be cleared on startQuickGame",
            vm.rankUpOverlay.value)
        assertNull("chestReward should be cleared on startQuickGame",
            vm.chestReward.value)
    }

    @Test
    fun gameLifeCycle_temaGame_clearsOverlaysOnStart() {
        // Populate state from survival
        prefs.addXP(190)
        assertTrue(playSurvivalGame(1))
        assertNotNull(vm.rankUpOverlay.value)
        // Start tema game - should clear
        vm.startTemaGame("test1")
        assertNull("rankUpOverlay should be cleared on startTemaGame",
            vm.rankUpOverlay.value)
        assertNull("chestReward should be cleared on startTemaGame",
            vm.chestReward.value)
    }

    @Test
    fun gameLifeCycle_survivalRank0_hasFreeHintAndFiftyFifty() {
        // Rank 0 players should get free hint and 50/50 charges
        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()
        val uiState = vm.uiState.value
        assertTrue("Rank 0 should have at least 1 hint charge",
            uiState.hintCharges >= 1)
        assertTrue("Rank 0 should have at least 1 fiftyFifty charge",
            uiState.fiftyFiftyCharges >= 1)
    }

    @Test
    fun gameLifeCycle_survivalRank0_doesNotHaveShieldOrDoubleScore() {
        // Rank 0 players should NOT have shield or doubleScore available
        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()
        val uiState = vm.uiState.value
        assertEquals("Rank 0 should have 0 shield charges",
            0, uiState.shieldCharges)
        assertEquals("Rank 0 should have 0 doubleScore charges",
            0, uiState.doubleScoreCharges)
    }

    @Test
    fun gameLifeCycle_allOptionsShownInOrder() {
        // Verify that all 4 options are shown and in A-B-C-D order
        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()
        val q = vm.engine.currentQ!!
        val allLetters = listOf("A", "B", "C", "D")
        val presentLetters = allLetters.filter { q.opciones[it] != null }
        assertEquals("All 4 options should be present", 4, presentLetters.size)
        assertEquals("Options should be in A-B-C-D order",
            listOf("A", "B", "C", "D"), presentLetters)
    }
}
