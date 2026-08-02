package com.opoleyes.ui.navigation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.repository.ProgressRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        assertEquals("gamesPlayed should NOT increment on double simulacro submission",
            gamesAfterFirst, gamesAfterSecond)
    }
}
