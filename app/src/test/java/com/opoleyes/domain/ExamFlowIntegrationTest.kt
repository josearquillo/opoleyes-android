package com.opoleyes.domain

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.repository.ProgressRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.opoleyes.ui.navigation.GameViewModel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExamFlowIntegrationTest {

    private lateinit var vm: GameViewModel
    private lateinit var prefs: PreferencesManager
    private lateinit var progressRepo: ProgressRepository

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        prefs = PreferencesManager(app)
        prefs.resetAll()
        vm = GameViewModel(app)
        progressRepo = vm.getProgressRepo()
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    // === Full exam flow: load → answer all → finish → XP ===

    @Test
    fun fun_fullExamFlow_allCorrect_grantsFullXP() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)
        val xpBefore = progressRepo.getXP()

        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }

        vm.finishExam()
        val result = vm.examResult.value
        assertNotNull(result)
        assertEquals(10, result!!.correct)
        assertEquals(0, result.wrong)
        assertEquals(0, result.unanswered)
        assertEquals(10f, result.score, 0.01f)
        assertTrue("XP should increase", progressRepo.getXP() > xpBefore)
        assertEquals(100, vm.xpGained.value) // 10 correct * 10 XP each
    }

    @Test
    fun fun_fullExamFlow_allWrong_grants0XP() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)
        val xpBefore = progressRepo.getXP()

        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            val wrong = listOf("A", "B", "C", "D").filter { it != q.question.correct }.first()
            vm.examAnswer(wrong)
        }

        vm.finishExam()
        val result = vm.examResult.value
        assertNotNull(result)
        assertEquals(0, result!!.correct)
        assertEquals(10, result.wrong)
        assertEquals(0f, result.score, 0.01f)
        assertEquals(xpBefore, progressRepo.getXP())
        assertEquals(0, vm.xpGained.value)
    }

    @Test
    fun fun_fullExamFlow_allUnanswered_grants0XP() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)
        val xpBefore = progressRepo.getXP()

        vm.finishExam()
        val result = vm.examResult.value
        assertNotNull(result)
        assertEquals(0, result!!.correct)
        assertEquals(0, result.wrong)
        assertEquals(10, result.unanswered)
        assertEquals(0f, result.score, 0.01f)
        assertEquals(xpBefore, progressRepo.getXP())
    }

    @Test
    fun fun_fullExamFlow_incrementsGamesPlayed() {
        val before = progressRepo.getGamesPlayed()
        vm.examEngine.loadExam(5)
        vm.examNavigate(0)
        vm.finishExam()
        assertEquals(before + 1, progressRepo.getGamesPlayed())
    }

    @Test
    fun fun_fullExamFlow_mixedAnswers_correctScore() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)

        // 5 correct, 3 wrong, 2 unanswered
        for (i in 0 until 5) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        for (i in 5 until 8) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            val wrong = listOf("A", "B", "C", "D").filter { it != q.question.correct }.first()
            vm.examAnswer(wrong)
        }

        vm.finishExam()
        val result = vm.examResult.value!!
        assertEquals(5, result.correct)
        assertEquals(3, result.wrong)
        assertEquals(2, result.unanswered)
        assertEquals(5f, result.score, 0.01f)
    }

    @Test
    fun fun_fullExamFlow_clearResult_resetsState() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)
        vm.examAnswer("A")
        vm.finishExam()
        vm.clearExamResult()

        assertEquals(null, vm.examResult.value)
        assertEquals(0, vm.examQuestionNum.value)
        assertEquals(0, vm.examAnswered.value)
        assertEquals(null, vm.examCurrentQuestion.value)
        assertEquals(0, vm.examTotalQuestions.value)
    }

    @Test
    fun fun_fullExamFlow_navigateBackAndForward_preservesAnswers() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)
        vm.examAnswer("A")
        vm.examNext()
        vm.examAnswer("B")
        vm.examPrev()
        assertEquals("A", vm.examCurrentQuestion.value?.userAnswer)
        vm.examNext()
        assertEquals("B", vm.examCurrentQuestion.value?.userAnswer)
    }

    @Test
    fun fun_fullExamFlow_partialAnswers_answeredCountCorrect() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)
        vm.examAnswer("A")
        vm.examNext()
        vm.examAnswer("B")
        vm.examNext()
        vm.examAnswer("C")
        assertEquals(3, vm.examAnswered.value)
    }

    @Test
    fun fun_fullExamFlow_perLawResults_populated() {
        vm.examEngine.loadExam(20)
        vm.examNavigate(0)
        for (i in 0 until 20) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        val result = vm.examResult.value!!
        assertTrue("Per-law results should not be empty", result.perLaw.isNotEmpty())
        val perLawTotal = result.perLaw.values.sumOf { it.total }
        assertEquals(20, perLawTotal)
    }
}
