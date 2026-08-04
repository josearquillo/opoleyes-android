package com.opoleyes.ui.navigation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.PreferencesManager
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
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Fast ViewModel-level tests covering a full correct cycle of each exam mode
 * (mini-examen and simulacro) plus edge-case scenarios.
 *
 * These tests do NOT use Compose UI (which is slow under Robolectric). They
 * verify the state flows exposed by GameViewModel after finishExam(), which
 * is what the ExamResultScreen renders. The UI layer is a thin function of
 * this state, so verifying the state covers the full cycle.
 *
 * Scenarios covered:
 *  - Mini-examen: all correct, all wrong, half correct, all unanswered
 *  - Simulacro:   all correct (pass), all wrong (fail), all unanswered (fail),
 *                 mixed at threshold (pass), mixed below threshold (fail),
 *                 mixed with wrongs at threshold (pass),
 *                 mixed with wrongs below threshold (fail)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExamResultFullCycleTest {

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

    // ========================
    // Helpers
    // ========================

    private fun wrongAnswerFor(q: com.opoleyes.data.model.QuestionEntry): String =
        q.opciones.keys.firstOrNull { it != q.correct }
            ?: listOf("A", "B", "C", "D").first { it != q.correct }

    private fun startSimulacroSync() {
        val latch = CountDownLatch(1)
        vm.startSimulacroAsync { latch.countDown() }
        // startSimulacroAsync dispatches the onDone callback to Dispatchers.Main.
        // The test thread blocks the Main looper, so we must pump it explicitly
        // for the callback to fire once the background loadSimulacro() finishes.
        // Without this the latch waits the full 30s timeout (the load itself
        // runs on Dispatchers.Default and completes quickly, but the Main-thread
        // continuation never runs while we block on await()).
        while (!latch.await(50, TimeUnit.MILLISECONDS)) {
            ShadowLooper.idleMainLooper()
        }
    }

    private fun answerAllCorrect(count: Int) {
        for (i in 0 until count) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
    }

    private fun answerAllWrong(count: Int) {
        for (i in 0 until count) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(wrongAnswerFor(q.question))
        }
    }

    private fun answerFirstCorrectThenWrong(correctCount: Int, wrongCount: Int) {
        var answered = 0
        val total = vm.examEngine.getQuestionCount()
        for (i in 0 until total) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            when {
                answered < correctCount -> {
                    vm.examAnswer(q.question.correct)
                    answered++
                }
                answered < correctCount + wrongCount -> {
                    vm.examAnswer(wrongAnswerFor(q.question))
                    answered++
                }
                // rest unanswered
            }
        }
    }

    // ========================
    // Mini-examen: all correct (full correct cycle)
    // ========================

    @Test
    fun miniExam_allCorrect_fullCycle() {
        vm.examEngine.loadExam(10)
        answerAllCorrect(10)
        val gamesBefore = progressRepo.getGamesPlayed()
        vm.finishExam()

        val r = vm.examResult.value
        assertNotNull("examResult should be set", r)
        assertEquals(10, r!!.correct)
        assertEquals(0, r.wrong)
        assertEquals(0, r.unanswered)
        assertEquals(10f, r.score, 0.01f)
        assertEquals(100, vm.xpGained.value) // 10 correct * 10 XP
        assertFalse("isSimulacroMode should be false for mini-exam", vm.isSimulacroMode.value)
        assertNull("simulacroResult should be null for mini-exam", vm.simulacroResult.value)
        assertEquals(gamesBefore + 1, progressRepo.getGamesPlayed())
        assertTrue("perLaw should be populated", r.perLaw.isNotEmpty())
        assertEquals(10, r.perLaw.values.sumOf { it.total })
    }

    // ========================
    // Mini-examen: all wrong
    // ========================

    @Test
    fun miniExam_allWrong_fullCycle() {
        vm.examEngine.loadExam(10)
        answerAllWrong(10)
        vm.finishExam()

        val r = vm.examResult.value!!
        assertEquals(0, r.correct)
        assertEquals(10, r.wrong)
        assertEquals(0, r.unanswered)
        assertEquals(0f, r.score, 0.01f)
        assertEquals(0, vm.xpGained.value) // 0 correct * 10 XP
        assertFalse(vm.isSimulacroMode.value)
    }

    // ========================
    // Mini-examen: half correct (score 5.0 → Aprobado)
    // ========================

    @Test
    fun miniExam_halfCorrect_fullCycle() {
        vm.examEngine.loadExam(10)
        answerFirstCorrectThenWrong(correctCount = 5, wrongCount = 5)
        vm.finishExam()

        val r = vm.examResult.value!!
        assertEquals(5, r.correct)
        assertEquals(5, r.wrong)
        assertEquals(0, r.unanswered)
        assertEquals(5f, r.score, 0.01f)
        assertEquals(50, vm.xpGained.value) // 5 correct * 10 XP
        assertFalse(vm.isSimulacroMode.value)
    }

    // ========================
    // Mini-examen: all unanswered
    // ========================

    @Test
    fun miniExam_allUnanswered_fullCycle() {
        vm.examEngine.loadExam(10)
        // Don't answer any questions
        vm.finishExam()

        val r = vm.examResult.value!!
        assertEquals(0, r.correct)
        assertEquals(0, r.wrong)
        assertEquals(10, r.unanswered)
        assertEquals(0f, r.score, 0.01f)
        assertEquals(0, vm.xpGained.value)
        assertFalse(vm.isSimulacroMode.value)
    }

    // ========================
    // Simulacro: all correct (pass) — full correct cycle
    // ========================

    @Test
    fun simulacro_allCorrect_fullCycle() {
        startSimulacroSync()
        val count = vm.examEngine.getQuestionCount()
        assertEquals(100, count)
        answerAllCorrect(count)
        val gamesBefore = progressRepo.getGamesPlayed()
        vm.finishExam()

        val sr = vm.simulacroResult.value
        assertNotNull("simulacroResult should be set", sr)
        assertEquals(100, sr!!.correct)
        assertEquals(0, sr.wrong)
        assertEquals(0, sr.unanswered)
        val expectedPoints = 100 * 0.60f
        assertEquals(expectedPoints, sr.points, 0.01f)
        assertTrue("Should pass with all correct", sr.passed)
        assertEquals(60f, sr.maxPoints, 0.01f)
        assertEquals(30f, sr.passingScore, 0.01f)
        // XP = points * 10 = 60 * 10 = 600
        assertEquals(600, vm.xpGained.value)
        assertTrue("isSimulacroMode should be true for simulacro", vm.isSimulacroMode.value)
        assertNull("examResult should be null for simulacro", vm.examResult.value)
        assertEquals(gamesBefore + 1, progressRepo.getGamesPlayed())
        assertTrue("perLaw should be populated", sr.perLaw.isNotEmpty())
        assertEquals(100, sr.perLaw.values.sumOf { it.total })
    }

    // ========================
    // Simulacro: all wrong (fail)
    // ========================

    @Test
    fun simulacro_allWrong_fullCycle() {
        startSimulacroSync()
        val count = vm.examEngine.getQuestionCount()
        answerAllWrong(count)
        vm.finishExam()

        val sr = vm.simulacroResult.value!!
        assertEquals(0, sr.correct)
        assertEquals(100, sr.wrong)
        assertEquals(0, sr.unanswered)
        // points = 0 * 0.60 - 100 * 0.15 = -15.0
        val expectedPoints = 0 * 0.60f - 100 * 0.15f
        assertEquals(expectedPoints, sr.points, 0.01f)
        assertFalse("Should fail with all wrong", sr.passed)
        // XP = max(points * 10, 0) = max(-150, 0) = 0
        assertEquals(0, vm.xpGained.value)
        assertTrue(vm.isSimulacroMode.value)
    }

    // ========================
    // Simulacro: all unanswered (fail)
    // ========================

    @Test
    fun simulacro_allUnanswered_fullCycle() {
        startSimulacroSync()
        // Don't answer any questions
        vm.finishExam()

        val sr = vm.simulacroResult.value!!
        assertEquals(0, sr.correct)
        assertEquals(0, sr.wrong)
        assertEquals(100, sr.unanswered)
        assertEquals(0f, sr.points, 0.01f)
        assertFalse("Should fail with 0 points", sr.passed)
        assertEquals(0, vm.xpGained.value)
        assertTrue(vm.isSimulacroMode.value)
    }

    // ========================
    // Simulacro: mixed — pass at exact threshold
    // 50 correct, 0 wrong, 50 unanswered: 50*0.60 = 30.0 >= 30.0 → pass
    // ========================

    @Test
    fun simulacro_mixedAtThreshold_passes() {
        startSimulacroSync()
        answerFirstCorrectThenWrong(correctCount = 50, wrongCount = 0)
        vm.finishExam()

        val sr = vm.simulacroResult.value!!
        assertEquals(50, sr.correct)
        assertEquals(0, sr.wrong)
        assertEquals(50, sr.unanswered)
        val expectedPoints = 50 * 0.60f
        assertEquals(expectedPoints, sr.points, 0.01f)
        assertTrue("Should pass at exact threshold ($expectedPoints >= ${sr.passingScore})",
            sr.passed)
        // XP = 30 * 10 = 300
        assertEquals(300, vm.xpGained.value)
        assertTrue(vm.isSimulacroMode.value)
    }

    // ========================
    // Simulacro: mixed — just below threshold
    // 49 correct, 0 wrong, 51 unanswered: 49*0.60 = 29.4 < 30.0 → fail
    // ========================

    @Test
    fun simulacro_mixedBelowThreshold_fails() {
        startSimulacroSync()
        answerFirstCorrectThenWrong(correctCount = 49, wrongCount = 0)
        vm.finishExam()

        val sr = vm.simulacroResult.value!!
        assertEquals(49, sr.correct)
        assertEquals(0, sr.wrong)
        assertEquals(51, sr.unanswered)
        val expectedPoints = 49 * 0.60f
        assertEquals(expectedPoints, sr.points, 0.01f)
        assertFalse("Should fail below threshold ($expectedPoints < ${sr.passingScore})",
            sr.passed)
        assertTrue(vm.isSimulacroMode.value)
    }

    // ========================
    // Simulacro: mixed with wrongs — above threshold
    // 70 correct, 30 wrong: 70*0.60 - 30*0.15 = 42 - 4.5 = 37.5 > 30.0 → pass
    // ========================

    @Test
    fun simulacro_mixedWithWrongsAboveThreshold_passes() {
        startSimulacroSync()
        answerFirstCorrectThenWrong(correctCount = 70, wrongCount = 30)
        vm.finishExam()

        val sr = vm.simulacroResult.value!!
        assertEquals(70, sr.correct)
        assertEquals(30, sr.wrong)
        assertEquals(0, sr.unanswered)
        val expectedPoints = 70 * 0.60f - 30 * 0.15f
        assertEquals(expectedPoints, sr.points, 0.01f)
        assertTrue("Should pass above threshold with wrongs ($expectedPoints >= ${sr.passingScore})",
            sr.passed)
        // XP = 37.5 * 10 = 375
        assertEquals(375, vm.xpGained.value)
        assertTrue(vm.isSimulacroMode.value)
    }

    // ========================
    // Simulacro: mixed with wrongs — below threshold
    // 55 correct, 45 wrong: 55*0.60 - 45*0.15 = 33 - 6.75 = 26.25 < 30.0 → fail
    // ========================

    @Test
    fun simulacro_mixedWithWrongsBelowThreshold_fails() {
        startSimulacroSync()
        answerFirstCorrectThenWrong(correctCount = 55, wrongCount = 45)
        vm.finishExam()

        val sr = vm.simulacroResult.value!!
        assertEquals(55, sr.correct)
        assertEquals(45, sr.wrong)
        assertEquals(0, sr.unanswered)
        val expectedPoints = 55 * 0.60f - 45 * 0.15f
        assertEquals(expectedPoints, sr.points, 0.01f)
        assertFalse("Should fail below threshold with wrongs ($expectedPoints < ${sr.passingScore})",
            sr.passed)
        // XP = max(26.25 * 10, 0) = 262
        assertEquals(262, vm.xpGained.value)
        assertTrue(vm.isSimulacroMode.value)
    }

    // ========================
    // clearExamResult resets all exam state
    // ========================

    @Test
    fun clearExamResult_resetsAllExamState() {
        vm.examEngine.loadExam(10)
        answerAllCorrect(10)
        vm.finishExam()
        assertNotNull(vm.examResult.value)

        vm.clearExamResult()
        assertNull(vm.examResult.value)
        assertNull(vm.simulacroResult.value)
        assertFalse(vm.isSimulacroMode.value)
        assertEquals(0, vm.examQuestionNum.value)
        assertEquals(0, vm.examAnswered.value)
        assertEquals(0, vm.examTotalQuestions.value)
        assertNull(vm.examCurrentQuestion.value)
    }

    @Test
    fun clearExamResult_resetsAllSimulacroState() {
        startSimulacroSync()
        answerAllCorrect(vm.examEngine.getQuestionCount())
        vm.finishExam()
        assertNotNull(vm.simulacroResult.value)
        assertTrue(vm.isSimulacroMode.value)

        vm.clearExamResult()
        assertNull(vm.examResult.value)
        assertNull(vm.simulacroResult.value)
        assertFalse(vm.isSimulacroMode.value)
    }
}
