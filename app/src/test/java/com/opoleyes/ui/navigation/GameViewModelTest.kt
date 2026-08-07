package com.opoleyes.ui.navigation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.Constants
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.repository.ProgressRepository
import org.junit.After
import org.junit.AfterClass
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
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameViewModelTest {

    companion object {
        private lateinit var sharedApp: Application
        private lateinit var sharedVm: GameViewModel
        private lateinit var sharedPrefs: PreferencesManager
        private lateinit var sharedProgressRepo: ProgressRepository
        private var initialized = false

        @JvmStatic
        @AfterClass
        fun cleanupCompanion() {
            if (initialized) {
                sharedPrefs.resetAll()
            }
        }
    }

    private val vm: GameViewModel get() = sharedVm
    private val prefs: PreferencesManager get() = sharedPrefs
    private val progressRepo: ProgressRepository get() = sharedProgressRepo

    @Before
    fun setup() {
        if (!initialized) {
            sharedApp = ApplicationProvider.getApplicationContext()
            sharedPrefs = PreferencesManager(sharedApp)
            sharedProgressRepo = ProgressRepository(sharedApp)
            sharedVm = GameViewModel(sharedApp)
            initialized = true
        }
        // Full state reset without recreating objects
        sharedPrefs.resetAll()
        sharedVm.resetProgress()
        sharedVm.clearExamResult()
        sharedVm.clearChest()
        sharedVm.clearRankUp()
        sharedVm.clearQuickReward()
        sharedVm.clearToasts()
        sharedVm.clearPopups()
        sharedVm.clearPowerUpToast()
    }

    @After
    fun teardown() {
        sharedPrefs.resetAll()
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
        // Start simulacro via async API and wait for completion.
        // Pump the Main looper so the onDone callback (dispatched to
        // Dispatchers.Main) fires once the background load finishes.
        val latch = CountDownLatch(1)
        vm.startSimulacroAsync { latch.countDown() }
        while (!latch.await(50, TimeUnit.MILLISECONDS)) {
            ShadowLooper.idleMainLooper()
        }

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
        vm.engine.sessionDifficultyCap = 5
        vm.engine.maxDifficulty = 5
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
    fun gameLifeCycle_rankUp_showsOverlay() {
        // Rank 0 -> 1 should show rank-up overlay
        prefs.addXP(190)
        assertTrue(playSurvivalGame(1))
        val overlay = vm.rankUpOverlay.value
        assertNotNull(overlay)
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
        // Fresh user (0 XP, rank 0). 1 correct answer = 10 XP (combo 1).
        // At rank 0: 7 lives + first mistake forgiven = 8 wrong answers to end game.
        // Each wrong answer gives 1 XP consolation = 8 XP.
        // Total XP = 10 + 8 = 18.
        assertTrue(playSurvivalGame(1))
        assertEquals("XP gained should be 18 (10 correct + 8 consolation)",
            18, vm.xpGained.value)
        assertEquals("Total XP should be 18",
            18, progressRepo.getXP())
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
    fun gameLifeCycle_survivalRank0_hasFiftyFiftyAvailable() {
        // Rank 0 players should have 50/50 available (unlimited, no charges)
        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()
        val uiState = vm.uiState.value
        // Power-ups are unlimited now; verify fiftyFifty is in available power-ups
        assertTrue("Rank 0 should have fiftyFifty available",
            "fiftyFifty" in vm.engine.availablePowerUps)
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

    // === QUICK mode lifecycle ===

    private fun playQuickGame(correctCount: Int): Boolean {
        val ok = vm.startQuickGame()
        if (!ok) return false
        vm.engine.sessionDifficultyCap = 5
        vm.engine.maxDifficulty = 5
        var correctAnswered = 0
        val target = Constants.QUICK_MODE_QUESTIONS
        while (vm.engine.totalAnswered < target && vm.engine.lives > 0) {
            val q = vm.engine.currentQ ?: break
            if (correctAnswered < correctCount) {
                vm.answer(q.correct)
                correctAnswered++
            } else {
                vm.answer("Z")
            }
            if (vm.engine.totalAnswered < target && vm.engine.lives > 0) {
                vm.nextQuestion()
            }
        }
        vm.onGameOver()
        return true
    }

    @Test
    fun quickMode_endsWith5Questions() {
        // Rank 5 (7000 XP) unlocks QUICK mode and gives maxDifficulty=4
        prefs.addXP(7000)
        assertTrue(playQuickGame(5))
        assertEquals("Quick mode should end with exactly 5 questions answered",
            5, vm.engine.totalAnswered)
    }

    @Test
    fun quickMode_perfectGame_earnsQuickReward() {
        prefs.addXP(7000)
        assertTrue(playQuickGame(5))
        assertEquals("All 5 answers should be correct", 5, vm.engine.correctCount)
        assertTrue("Perfect quick game should earn quick reward",
            vm.quickRewardEarned.value)
    }

    @Test
    fun quickMode_imperfectGame_missesQuickReward() {
        prefs.addXP(7000)
        assertTrue(playQuickGame(4))
        assertTrue("Imperfect quick game should show missed reward",
            vm.quickRewardMissed.value)
        assertFalse("Should not earn quick reward", vm.quickRewardEarned.value)
    }

    @Test
    fun quickMode_xpGainedMatchesExpected() {
        prefs.addXP(7000)
        // 5 correct: combo 1..5 = 15*(1+2+3+4+5) = 225 XP + 300 quick reward (50*(1+5)) = 525 XP
        assertTrue(playQuickGame(5))
        assertEquals("XP for 5 correct in quick mode (15*combo + 300 quick reward at rank 5)",
            525, vm.xpGained.value)
    }

    @Test
    fun quickMode_rankUp_overlayShown() {
        // Rank 5 (7000 XP); close to rank 6 (12000 XP)
        prefs.addXP(11990)
        // 1 correct = 15 XP (combo 1) -> total 12005 -> rank 6
        assertTrue(playQuickGame(1))
        assertNotNull("Quick mode rank-up should show overlay",
            vm.rankUpOverlay.value)
    }

    @Test
    fun quickMode_clearsOverlaysOnNextStart() {
        prefs.addXP(11990)
        assertTrue(playQuickGame(1))
        assertNotNull(vm.rankUpOverlay.value)
        vm.startQuickGame()
        assertNull("Quick mode should clear rankUpOverlay on new start",
            vm.rankUpOverlay.value)
    }

    @Test
    fun quickMode_gameOverProcessedOnlyOnce() {
        vm.startQuickGame()
        vm.engine.lives = 0
        vm.onGameOver()
        val xpFirst = progressRepo.getXP()
        vm.onGameOver()
        assertEquals("Double onGameOver should not add XP", xpFirst, progressRepo.getXP())
    }

    @Test
    fun quickMode_rank0_hasFiftyFiftyAvailable() {
        vm.startQuickGame()
        // Power-ups are unlimited; verify fiftyFifty is available
        assertTrue("Quick rank 0 should have fiftyFifty available",
            "fiftyFifty" in vm.engine.availablePowerUps)
    }

    // === TIMETRIAL mode lifecycle ===

    private fun playTimetrialGame(correctCount: Int): Boolean {
        vm.pendingMode = GameMode.TIMETRIAL
        val ok = vm.startAllLawsGame()
        if (!ok) return false
        vm.engine.sessionDifficultyCap = 5
        vm.engine.maxDifficulty = 5
        if (vm.engine.currentQ == null) { vm.engine.nextQuestion(); vm.updateUiState() }
        var answered = 0
        while (answered < correctCount && !vm.isGameOver()) {
            val q = vm.engine.currentQ!!
            vm.answer(q.correct)
            answered++
            if (!vm.isGameOver()) vm.nextQuestion()
        }
        // End by exhausting timer
        vm.engine.timer = 0f
        vm.onGameOver()
        return true
    }

    @Test
    fun timetrialMode_starts180sTimer() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        assertEquals("Timetrial should start with 180s timer",
            180f, vm.engine.timer)
    }

    @Test
    fun timetrialMode_noLives() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        assertEquals("Timetrial should have 0 lives (timer-based)",
            0, vm.engine.lives)
    }

    @Test
    fun timetrialMode_xpGainedMatchesExpected() {
        // 3 correct: combo 1,2,3 = 10+20+30 = 60 XP
        assertTrue(playTimetrialGame(3))
        assertEquals("XP for 3 correct in timetrial (10*combo)",
            60, vm.xpGained.value)
    }

    @Test
    fun timetrialMode_rankUp_overlayShown() {
        prefs.addXP(190)
        // 1 correct = 10 XP -> total 200 -> rank 1
        assertTrue(playTimetrialGame(1))
        assertNotNull("Timetrial rank-up should show overlay",
            vm.rankUpOverlay.value)
    }

    @Test
    fun timetrialMode_noRankUp_overlayIsNull() {
        assertTrue(playTimetrialGame(1))
        assertNull("Timetrial without rank-up should have null overlay",
            vm.rankUpOverlay.value)
    }

    @Test
    fun timetrialMode_clearsOverlaysOnNextStart() {
        prefs.addXP(190)
        assertTrue(playTimetrialGame(1))
        assertNotNull(vm.rankUpOverlay.value)
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        assertNull("Timetrial should clear rankUpOverlay on new start",
            vm.rankUpOverlay.value)
    }

    @Test
    fun timetrialMode_gameOverProcessedOnlyOnce() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        vm.engine.timer = 0f
        vm.onGameOver()
        val xpFirst = progressRepo.getXP()
        vm.onGameOver()
        assertEquals("Double onGameOver should not add XP", xpFirst, progressRepo.getXP())
    }

    @Test
    fun timetrialMode_wrongAnswerReducesTimer() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        val timerBefore = vm.engine.timer
        vm.answer("Z")
        assertTrue("Wrong answer in timetrial should reduce timer",
            vm.engine.timer < timerBefore)
    }

    @Test
    fun timetrialMode_correctAnswerAddsTimer() {
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        val timerBefore = vm.engine.timer
        val q = vm.engine.currentQ!!
        vm.answer(q.correct)
        assertTrue("Correct answer in timetrial should add 15s timer",
            vm.engine.timer > timerBefore)
    }

    // === EXAM mode lifecycle ===

    @Test
    fun examMode_loadsCorrectQuestionCount() {
        vm.examEngine.loadExam(10)
        assertEquals("Exam should load 10 questions",
            10, vm.examEngine.getQuestionCount())
    }

    @Test
    fun examMode_xpGainedMatchesCorrect() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        // 10 correct * 10 XP = 100 XP
        assertEquals("Exam XP should be correct*10",
            100, vm.xpGained.value)
    }

    @Test
    fun examMode_rankUp_overlayShown() {
        prefs.addXP(190)
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        // 10 correct * 10 = 100 XP -> total 290 -> rank 1 (200 XP threshold)
        assertNotNull("Exam rank-up should show overlay",
            vm.rankUpOverlay.value)
    }

    @Test
    fun examMode_noRankUp_overlayIsNull() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        // 100 XP from 0 -> still rank 0 (needs 200)
        assertNull("Exam without rank-up should have null overlay",
            vm.rankUpOverlay.value)
    }

    @Test
    fun examMode_finishClearsExamResult() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        assertNotNull("Exam result should be set", vm.examResult.value)
        vm.clearExamResult()
        assertNull("Exam result should be cleared", vm.examResult.value)
    }

    @Test
    fun examMode_partialAnswers_gradedCorrectly() {
        vm.examEngine.loadExam(10)
        // Answer 6 correctly, 2 wrong, leave 2 unanswered
        for (i in 0 until 6) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        for (i in 6 until 8) {
            vm.examNavigate(i)
            vm.examAnswer("Z")
        }
        // Leave questions 8,9 unanswered
        vm.finishExam()
        val result = vm.examResult.value!!
        assertEquals("6 correct", 6, result.correct)
        assertEquals("2 wrong", 2, result.wrong)
        assertEquals("2 unanswered", 2, result.unanswered)
    }

    @Test
    fun examMode_highScoreUnlocksMoreQuestions() {
        val maxBefore = progressRepo.getMaxExamQuestions()
        vm.examEngine.loadExam(maxBefore)
        for (i in 0 until maxBefore) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        val result = vm.examResult.value!!
        assertTrue("Score should be >= 5.0 to unlock", result.score >= 5.0f)
        val maxAfter = progressRepo.getMaxExamQuestions()
        assertTrue("Max exam questions should increase after high score",
            maxAfter > maxBefore)
    }

    // === SIMULACRO mode lifecycle ===

    private fun startSimulacro(): Boolean {
        val latch = CountDownLatch(1)
        vm.startSimulacroAsync { latch.countDown() }
        // startSimulacroAsync dispatches onDone to Dispatchers.Main; pump the
        // Main looper so the callback fires as soon as the background
        // loadSimulacro() finishes (otherwise the latch hits its 30s timeout
        // even though the load itself completes in well under a second).
        while (!latch.await(50, TimeUnit.MILLISECONDS)) {
            ShadowLooper.idleMainLooper()
        }
        return vm.examEngine.getQuestionCount() > 0
    }

    @Test
    fun simulacroMode_loads100Questions() {
        assertTrue(startSimulacro())
        assertEquals("Simulacro should load 100 questions",
            100, vm.examEngine.getQuestionCount())
    }

    @Test
    fun simulacroMode_xpGainedMatchesPoints() {
        assertTrue(startSimulacro())
        // Answer all 100 correctly
        for (i in 0 until 100) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam() // routes to finishSimulacro
        val result = vm.simulacroResult.value!!
        val expectedXp = (result.points * 10).toInt()
        assertEquals("Simulacro XP should be points*10",
            expectedXp, vm.xpGained.value)
    }

    @Test
    fun simulacroMode_rankUp_overlayShown() {
        // Need 2 correct: 2 * 0.60 = 1.2 points * 10 = 12 XP
        // Start with 189 XP -> total 201 -> rank 1 (200 XP threshold)
        prefs.resetAll()
        prefs.addXP(189)
        assertTrue(startSimulacro())
        vm.examNavigate(0)
        val q0 = vm.examEngine.getCurrentQuestion()!!
        vm.examAnswer(q0.question.correct)
        vm.examNavigate(1)
        val q1 = vm.examEngine.getCurrentQuestion()!!
        vm.examAnswer(q1.question.correct)
        vm.finishExam()
        assertNotNull("Simulacro rank-up should show overlay",
            vm.rankUpOverlay.value)
    }

    @Test
    fun simulacroMode_passed_setsResult() {
        assertTrue(startSimulacro())
        // Answer all correctly to guarantee pass
        for (i in 0 until 100) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        val result = vm.simulacroResult.value!!
        assertTrue("Perfect simulacro should pass", result.passed)
    }

    @Test
    fun simulacroMode_failed_setsResult() {
        assertTrue(startSimulacro())
        // Answer all wrong to guarantee fail
        for (i in 0 until 100) {
            vm.examNavigate(i)
            vm.examAnswer("Z")
        }
        vm.finishExam()
        val result = vm.simulacroResult.value!!
        assertFalse("All-wrong simulacro should fail", result.passed)
    }

    @Test
    fun simulacroMode_clearsOverlaysOnFinish() {
        // Simulacro doesn't use startGame methods, but finishExam should
        // still produce clean state. Verify no stale chest from previous game.
        vm.startQuickGame()
        vm.engine.lives = 0
        vm.onGameOver()
        // Now start simulacro
        assertTrue(startSimulacro())
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        // The chest from the quick game should not interfere
        // (simulacro doesn't generate chests, so chestReward should be from quick game or null)
        // The key point: rankUpOverlay should reflect only simulacro rank-up, not stale
    }

    // === Cross-mode cleanup ===

    @Test
    fun crossMode_survivalToQuick_clearsOverlays() {
        // Play survival, get rank-up, then start quick
        prefs.addXP(190)
        assertTrue(playSurvivalGame(1))
        assertNotNull(vm.rankUpOverlay.value)
        vm.startQuickGame()
        assertNull("Switching from survival to quick should clear rankUpOverlay",
            vm.rankUpOverlay.value)
        assertNull("Switching from survival to quick should clear chestReward",
            vm.chestReward.value)
    }

    @Test
    fun crossMode_quickToSurvival_clearsOverlays() {
        // Play quick, then start survival
        prefs.addXP(11990)
        assertTrue(playQuickGame(1))
        assertNotNull("Quick game should have rank-up overlay", vm.rankUpOverlay.value)
        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()
        assertNull("Switching from quick to survival should clear rankUpOverlay",
            vm.rankUpOverlay.value)
    }

    @Test
    fun crossMode_survivalToTimetrial_clearsOverlays() {
        prefs.addXP(190)
        assertTrue(playSurvivalGame(1))
        assertNotNull(vm.rankUpOverlay.value)
        vm.pendingMode = GameMode.TIMETRIAL
        vm.startAllLawsGame()
        assertNull("Switching from survival to timetrial should clear rankUpOverlay",
            vm.rankUpOverlay.value)
    }

    @Test
    fun crossMode_timetrialToSurvival_clearsOverlays() {
        prefs.addXP(190)
        assertTrue(playTimetrialGame(1))
        assertNotNull(vm.rankUpOverlay.value)
        vm.pendingMode = GameMode.SURVIVAL
        vm.startAllLawsGame()
        assertNull("Switching from timetrial to survival should clear rankUpOverlay",
            vm.rankUpOverlay.value)
    }
}
