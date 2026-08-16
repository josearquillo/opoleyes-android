package com.opoleyes.ui.navigation

import com.opoleyes.FakePreferencesManager
import com.opoleyes.RealisticFixtures
import com.opoleyes.data.repository.MissionRepository
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository
import com.opoleyes.domain.AchievementChecker
import com.opoleyes.domain.ChestSystem
import com.opoleyes.domain.ExamEngine
import com.opoleyes.domain.GameEngine
import com.opoleyes.data.model.GameMode
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

/**
 * Adversarial tests for the exam flow using realistic data.
 * These tests reproduce the exact scenarios that caused bugs in manual testing:
 * - "Otros" appearing in perLaw results
 * - Blank screen after second exam
 * - Reintentar button not working
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExamFlowAdversarialTest {

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
        prefs.xp = 18000 // Rank 2 (Aprendiz) for full mechanics
        progressRepo = ProgressRepository(prefs)
        val statsRepo = StatsRepository(prefs)
        val missionRepo = MissionRepository(prefs)
        // Use realistic data that mirrors data.json structure
        val testData = RealisticFixtures.buildTestDataList()
        val examEngine = ExamEngine.createForTestData(statsRepo, testData)
        // GameEngine with realistic pool
        val gameRepo = object : com.opoleyes.data.IGameRepository {
            override fun startTemaGame(testId: String) = RealisticFixtures.buildRealisticPool()
            override fun startAllLawsGame() = RealisticFixtures.buildRealisticPool()
            override fun startQuickGame() = RealisticFixtures.buildRealisticPool().take(5)
        }
        val engine = GameEngine.createForTest(gameRepo, statsRepo, progressRepo, prefs)
        vm = GameViewModel.createForTest(
            progressRepo, statsRepo, missionRepo,
            AchievementChecker(prefs), ChestSystem(prefs),
            prefs, engine, examEngine
        )
    }

    @After
    fun teardown() { prefs.resetAll() }

    // ========================
    // Scenario: Mini-exam with 10 questions, answer only 1 wrong
    // (exact scenario the user tested manually and found "Otros" bug)
    // ========================

    @Test
    fun miniExam_answerOneWrong_perLawDoesNotShowOtros() {
        vm.examEngine.loadExam(10)
        // Answer only question 0, incorrectly
        vm.examNavigate(0)
        val q = vm.examEngine.getCurrentQuestion()!!
        val wrong = q.question.opciones.keys.firstOrNull { it != q.question.correct } ?: "B"
        vm.examAnswer(wrong)
        vm.finishExam()

        val r = vm.examResult.value!!
        assertEquals(10, r.total)
        assertEquals(0, r.correct)
        assertEquals(1, r.wrong)
        assertEquals(9, r.unanswered)

        // This is the exact bug the user found: perLaw showing "Otros"
        assertFalse("perLaw should not contain only 'Otros': ${r.perLaw.keys}",
            r.perLaw.keys == setOf("Otros"))
    }

    @Test
    fun miniExam_answerOneWrong_perLawContainsRealLawNames() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)
        val q = vm.examEngine.getCurrentQuestion()!!
        val wrong = q.question.opciones.keys.firstOrNull { it != q.question.correct } ?: "B"
        vm.examAnswer(wrong)
        vm.finishExam()

        val r = vm.examResult.value!!
        val knownLaws = setOf("LOPJ", "LEC", "LECrim", "Constitución/UE/Org", "Contencioso", "Social", "Registro Civil", "Concursal")
        assertTrue("perLaw should contain at least one known law: ${r.perLaw.keys}",
            r.perLaw.keys.any { it in knownLaws })
    }

    // ========================
    // Scenario: Second exam after clearExamResult (blank screen bug)
    // ========================

    @Test
    fun secondExam_afterClearExamResult_resultIsNotNull() {
        // First exam
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        assertNotNull(vm.examResult.value)

        // Clear (simulates user pressing "Menu" or navigating away)
        vm.clearExamResult()
        assertNull(vm.examResult.value)

        // Second exam - this is where the blank screen appeared
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()

        // This was null when the bug existed, causing ExamResultScreen to navigate to HOME
        assertNotNull("Second exam result must not be null (blank screen bug)", vm.examResult.value)
        assertEquals(10, vm.examResult.value!!.total)
        assertEquals(10, vm.examResult.value!!.correct)
    }

    @Test
    fun secondExam_afterClearExamResult_perLawIsCorrect() {
        // First exam
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()

        vm.clearExamResult()

        // Second exam
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()

        val r = vm.examResult.value!!
        assertFalse("Second exam perLaw should not be only 'Otros'", r.perLaw.keys == setOf("Otros"))
        val perLawCorrect = r.perLaw.values.sumOf { it.correct }
        assertEquals(10, perLawCorrect)
    }

    @Test
    fun secondExam_isSimulacroModeIsFalse() {
        vm.examEngine.loadExam(10)
        vm.finishExam()
        assertFalse(vm.isSimulacroMode.value)

        vm.clearExamResult()

        vm.examEngine.loadExam(10)
        assertFalse("isSimulacroMode should be false for regular exam", vm.isSimulacroMode.value)
        vm.finishExam()
        assertFalse(vm.isSimulacroMode.value)
    }

    // ========================
    // Scenario: finishExam is idempotent (Reintentar button bug)
    // ========================

    @Test
    fun finishExam_calledTwice_doesNotClearResult() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        val r1 = vm.examResult.value!!
        assertEquals(10, r1.correct)

        // If user presses back and finishes again
        vm.finishExam()
        val r2 = vm.examResult.value!!
        assertEquals("Result should not change on double finish", r1.correct, r2.correct)
    }

    // ========================
    // Scenario: Exam with 0 answers (all unanswered)
    // ========================

    @Test
    fun exam_allUnanswered_perLawStillHasRealLaws() {
        vm.examEngine.loadExam(10)
        vm.finishExam()

        val r = vm.examResult.value!!
        assertEquals(10, r.unanswered)
        assertEquals(0, r.correct)
        assertEquals(0, r.wrong)
        assertFalse("Unanswered exam should not show only 'Otros'", r.perLaw.keys == setOf("Otros"))
    }

    // ========================
    // Scenario: Exam with different question counts
    // ========================

    @Test
    fun exam_5Questions_allCorrect() {
        vm.examEngine.loadExam(5)
        for (i in 0 until 5) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        assertEquals(5, vm.examResult.value!!.total)
        assertEquals(5, vm.examResult.value!!.correct)
        assertEquals(10.0f, vm.examResult.value!!.score, 0.01f)
    }

    @Test
    fun exam_20Questions_mixedAnswers() {
        vm.examEngine.loadExam(20)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        for (i in 10 until 15) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            val wrong = q.question.opciones.keys.firstOrNull { it != q.question.correct } ?: "B"
            vm.examAnswer(wrong)
        }
        // Leave 5 unanswered
        vm.finishExam()

        val r = vm.examResult.value!!
        assertEquals(20, r.total)
        assertEquals(10, r.correct)
        assertEquals(5, r.wrong)
        assertEquals(5, r.unanswered)
    }

    // ========================
    // Scenario: Clear answer and re-answer
    // ========================

    @Test
    fun exam_clearAndReanswer_gradesCorrectly() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)
        val q = vm.examEngine.getCurrentQuestion()!!

        // Answer correctly
        vm.examAnswer(q.question.correct)
        // Clear
        vm.examClearAnswer()
        // Re-answer incorrectly
        val wrong = q.question.opciones.keys.firstOrNull { it != q.question.correct } ?: "B"
        vm.examAnswer(wrong)

        vm.finishExam()
        val r = vm.examResult.value!!
        assertEquals(0, r.correct)
        assertEquals(1, r.wrong)
    }

    // ========================
    // Scenario: Navigate back and forth during exam
    // ========================

    @Test
    fun exam_navigateBackAndForth_answersArePreserved() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)
        vm.examAnswer(vm.examEngine.getCurrentQuestion()!!.question.correct)
        vm.examNext()
        vm.examAnswer(vm.examEngine.getCurrentQuestion()!!.question.correct)
        vm.examNext()
        vm.examAnswer(vm.examEngine.getCurrentQuestion()!!.question.correct)

        // Go back to question 0
        vm.examPrev()
        vm.examPrev()
        assertEquals(0, vm.examEngine.getCurrentIndex())
        // Answer should still be there
        assertEquals(3, vm.examEngine.getAnsweredCount())

        // Go forward again
        vm.examNext()
        vm.examNext()
        assertEquals(2, vm.examEngine.getCurrentIndex())
        assertEquals(3, vm.examEngine.getAnsweredCount())
    }

    // ========================
    // Scenario: Simulacro with realistic data
    // ========================

    @Test
    fun simulacro_allCorrect_perLawHasRealLaws() {
        vm.loadSimulacroSync()
        val count = vm.examEngine.getQuestionCount()
        for (i in 0 until count) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()

        val r = vm.simulacroResult.value!!
        assertTrue(r.passed)
        assertFalse("Simulacro perLaw should not be only 'Otros'", r.perLaw.keys == setOf("Otros"))
    }

    @Test
    fun simulacro_secondSimulacro_afterClear_resultIsNotNull() {
        // First simulacro
        vm.loadSimulacroSync()
        val count = vm.examEngine.getQuestionCount()
        for (i in 0 until count) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        assertNotNull(vm.simulacroResult.value)

        // Clear
        vm.clearExamResult()
        assertNull(vm.simulacroResult.value)
        assertFalse(vm.isSimulacroMode.value)

        // Second simulacro
        vm.loadSimulacroSync()
        for (i in 0 until count) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()

        assertNotNull("Second simulacro result must not be null", vm.simulacroResult.value)
        assertTrue(vm.simulacroResult.value!!.passed)
    }

    @Test
    fun simulacro_timerExpires_resultIsNotNull() {
        vm.loadSimulacroSync()
        // Simulate timer expiring without answering
        // Tick all the way down
        for (i in 0 until ExamEngine.SIMULACRO_TIME_SECONDS) {
            vm.tickSimulacroTimer()
        }
        assertEquals(0, vm.getSimulacroTimer())

        // When timer expires, ExamScreen calls finishExam
        vm.finishExam()
        assertNotNull("Simulacro result should be set when timer expires", vm.simulacroResult.value)
        assertEquals(100, vm.simulacroResult.value!!.unanswered)
        assertFalse(vm.simulacroResult.value!!.passed)
    }

    @Test
    fun simulacro_partialAnswers_timerExpires() {
        vm.loadSimulacroSync()
        // Answer 30 questions correctly, then timer expires
        for (i in 0 until 30) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        // Simulate timer expiry
        for (i in 0 until ExamEngine.SIMULACRO_TIME_SECONDS) {
            vm.tickSimulacroTimer()
        }
        vm.finishExam()

        val r = vm.simulacroResult.value!!
        assertEquals(30, r.correct)
        assertEquals(70, r.unanswered)
        // 30 * 0.60 = 18 points. Passing = 30. So fails.
        assertFalse("30 correct should not pass (18 < 30)", r.passed)
    }

    // ========================
    // Scenario: Switch between exam and simulacro
    // ========================

    @Test
    fun switchFromExamToSimulacro_stateIsCorrect() {
        // Do an exam first
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        assertFalse(vm.isSimulacroMode.value)

        vm.clearExamResult()

        // Now do a simulacro
        vm.loadSimulacroSync()
        assertTrue("Should be in simulacro mode", vm.isSimulacroMode.value)
        assertEquals(100, vm.examEngine.getQuestionCount())
    }

    @Test
    fun switchFromSimulacroToExam_stateIsCorrect() {
        // Do a simulacro first
        vm.loadSimulacroSync()
        assertTrue(vm.isSimulacroMode.value)

        vm.clearExamResult()
        assertFalse(vm.isSimulacroMode.value)

        // Now do an exam
        vm.examEngine.loadExam(10)
        assertFalse("Should not be in simulacro mode for regular exam", vm.isSimulacroMode.value)
        assertEquals(10, vm.examEngine.getQuestionCount())
    }

    // ========================
    // Scenario: Exam result score calculation
    // ========================

    @Test
    fun exam_scoreCalculation_5of10() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 5) {
            vm.examNavigate(i)
            vm.examAnswer(vm.examEngine.getCurrentQuestion()!!.question.correct)
        }
        vm.finishExam()
        assertEquals(5.0f, vm.examResult.value!!.score, 0.01f)
    }

    @Test
    fun exam_scoreCalculation_0of10() {
        vm.examEngine.loadExam(10)
        vm.finishExam()
        assertEquals(0.0f, vm.examResult.value!!.score, 0.01f)
    }

    @Test
    fun exam_scoreCalculation_10of10() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            vm.examAnswer(vm.examEngine.getCurrentQuestion()!!.question.correct)
        }
        vm.finishExam()
        assertEquals(10.0f, vm.examResult.value!!.score, 0.01f)
    }

    // ========================
    // Scenario: perLaw totals always sum to total questions
    // ========================

    @Test
    fun exam_perLawTotalsSumToTotal_10Questions() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 3) {
            vm.examNavigate(i)
            vm.examAnswer(vm.examEngine.getCurrentQuestion()!!.question.correct)
        }
        for (i in 3 until 7) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            val wrong = q.question.opciones.keys.firstOrNull { it != q.question.correct } ?: "B"
            vm.examAnswer(wrong)
        }
        vm.finishExam()

        val r = vm.examResult.value!!
        val perLawTotal = r.perLaw.values.sumOf { it.total }
        assertEquals("perLaw totals must sum to total questions", r.total, perLawTotal)
    }

    @Test
    fun simulacro_perLawTotalsSumTo100() {
        vm.loadSimulacroSync()
        for (i in 0 until 50) {
            vm.examNavigate(i)
            vm.examAnswer(vm.examEngine.getCurrentQuestion()!!.question.correct)
        }
        vm.finishExam()

        val r = vm.simulacroResult.value!!
        val perLawTotal = r.perLaw.values.sumOf { it.total }
        assertEquals(100, perLawTotal)
    }
}
