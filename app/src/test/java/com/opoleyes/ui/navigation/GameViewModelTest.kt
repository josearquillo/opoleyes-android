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
}
