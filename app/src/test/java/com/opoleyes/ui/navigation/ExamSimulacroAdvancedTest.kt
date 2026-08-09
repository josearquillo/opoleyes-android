package com.opoleyes.ui.navigation

import com.opoleyes.FakeGameRepository
import com.opoleyes.FakePreferencesManager
import com.opoleyes.FakeStatsRepository
import com.opoleyes.TestFakes
import com.opoleyes.data.model.Answer
import com.opoleyes.data.model.Question
import com.opoleyes.data.model.TestData
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
class ExamSimulacroAdvancedTest {

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

    private fun makeTestData(testId: String, testName: String, questionCount: Int = 15): TestData {
        val questions = (1..questionCount).map { i ->
            Question(
                id = i,
                test_id = "Tema_${testId}_$i",
                orig_id = i,
                enunciado = "Question $i of $testName",
                opciones = mapOf("A" to "Option A", "B" to "Option B", "C" to "Option C", "D" to "Option D"),
                difficulty = 3
            )
        }
        val answers = questions.map { Answer(id = it.id, correct = "A") }
        return TestData(
            test = com.opoleyes.data.model.Test(id = testId, name = testName, tema = 1),
            questions = questions,
            answers = answers
        )
    }

    private fun makeRealisticTestData(): List<TestData> = listOf(
        makeTestData("test_lopj", "Poder Judicial y CGPJ"),
        makeTestData("test_lec", "Juicio Ordinario"),
        makeTestData("test_lecrim", "Procedimiento Abreviado (Penal)"),
        makeTestData("test_const", "Constitución Española"),
        makeTestData("test_cont", "Contencioso-Administrativa"),
        makeTestData("test_social", "Social: Principios Laborales"),
        makeTestData("test_registro", "Registro Civil"),
        makeTestData("test_concursal", "Concursal"),
        makeTestData("test_ue", "Unión Europea")
    )

    @Before
    fun setup() {
        prefs = FakePreferencesManager()
        prefs.resetAll()
        progressRepo = ProgressRepository(prefs)
        val statsRepo = StatsRepository(prefs)
        val missionRepo = MissionRepository(prefs)
        val engine = GameEngine.createForTest(FakeGameRepository(), statsRepo, progressRepo, prefs)
        // Use createForTestData so mapTestToLaw works with realistic data
        val examEngine = ExamEngine.createForTestData(statsRepo, makeRealisticTestData())
        vm = GameViewModel.createForTest(
            progressRepo, statsRepo, missionRepo,
            AchievementChecker(prefs), ChestSystem(prefs),
            prefs, engine, examEngine
        )
    }

    @After
    fun teardown() { prefs.resetAll() }

    // ========================
    // Exam: partial answers
    // ========================

    @Test
    fun exam_partialAnswers_gradedCorrectly() {
        vm.examEngine.loadExam(10)
        // Answer 3 correct, 2 wrong, leave 5 unanswered
        for (i in 0 until 3) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        for (i in 3 until 5) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            val wrong = q.question.opciones.keys.firstOrNull { it != q.question.correct } ?: "B"
            vm.examAnswer(wrong)
        }
        vm.finishExam()

        val r = vm.examResult.value!!
        assertEquals(10, r.total)
        assertEquals(3, r.correct)
        assertEquals(2, r.wrong)
        assertEquals(5, r.unanswered)
    }

    @Test
    fun exam_partialAnswers_scoreIsCorrect() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 5) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()

        val r = vm.examResult.value!!
        // Score = correct / total * 10 = 5/10 * 10 = 5.0
        assertEquals(5.0f, r.score, 0.01f)
    }

    @Test
    fun exam_allCorrect_scoreIs10() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()

        val r = vm.examResult.value!!
        assertEquals(10.0f, r.score, 0.01f)
    }

    @Test
    fun exam_allWrong_scoreIs0() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            val wrong = q.question.opciones.keys.firstOrNull { it != q.question.correct } ?: "B"
            vm.examAnswer(wrong)
        }
        vm.finishExam()

        val r = vm.examResult.value!!
        assertEquals(0.0f, r.score, 0.01f)
    }

    @Test
    fun exam_allUnanswered_scoreIs0() {
        vm.examEngine.loadExam(10)
        vm.finishExam()

        val r = vm.examResult.value!!
        assertEquals(0.0f, r.score, 0.01f)
        assertEquals(10, r.unanswered)
    }

    // ========================
    // Exam: navigation
    // ========================

    @Test
    fun exam_navigation_nextAndPrev() {
        vm.examEngine.loadExam(10)
        assertEquals(0, vm.examEngine.getCurrentIndex())

        assertTrue(vm.examNext())
        assertEquals(1, vm.examEngine.getCurrentIndex())

        assertTrue(vm.examNext())
        assertEquals(2, vm.examEngine.getCurrentIndex())

        assertTrue(vm.examPrev())
        assertEquals(1, vm.examEngine.getCurrentIndex())
    }

    @Test
    fun exam_navigation_nextAtEndReturnsFalse() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 9) vm.examNext()
        assertEquals(9, vm.examEngine.getCurrentIndex())
        assertFalse("Next at last question should return false", vm.examNext())
    }

    @Test
    fun exam_navigation_prevAtStartReturnsFalse() {
        vm.examEngine.loadExam(10)
        assertEquals(0, vm.examEngine.getCurrentIndex())
        assertFalse("Prev at first question should return false", vm.examPrev())
    }

    @Test
    fun exam_navigation_jumpToIndex() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(5)
        assertEquals(5, vm.examEngine.getCurrentIndex())
    }

    @Test
    fun exam_navigation_jumpOutOfRangeClamps() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(100)
        assertEquals("Should clamp to last index", 9, vm.examEngine.getCurrentIndex())
        vm.examNavigate(-5)
        assertEquals("Should clamp to 0", 0, vm.examEngine.getCurrentIndex())
    }

    // ========================
    // Exam: clear answer
    // ========================

    @Test
    fun exam_clearAnswer_removesUserAnswer() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)
        val q = vm.examEngine.getCurrentQuestion()!!
        vm.examAnswer(q.question.correct)
        assertEquals(1, vm.examEngine.getAnsweredCount())

        vm.examClearAnswer()
        assertEquals(0, vm.examEngine.getAnsweredCount())
    }

    @Test
    fun exam_clearAnswer_thenReanswerWorks() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)
        val q = vm.examEngine.getCurrentQuestion()!!
        vm.examAnswer(q.question.correct)
        vm.examClearAnswer()
        val wrong = q.question.opciones.keys.firstOrNull { it != q.question.correct } ?: "B"
        vm.examAnswer(wrong)
        vm.finishExam()

        val r = vm.examResult.value!!
        assertEquals(0, r.correct)
        assertEquals(1, r.wrong)
    }

    // ========================
    // Exam: review questions
    // ========================

    @Test
    fun exam_review_getQuestionsReturnsAllQuestions() {
        vm.examEngine.loadExam(10)
        val questions = vm.getExamQuestions()
        assertEquals(10, questions.size)
    }

    @Test
    fun exam_review_questionsHaveUserAnswersAfterFinishing() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 5) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()

        val questions = vm.getExamQuestions()
        val answered = questions.filter { it.userAnswer != null }
        assertEquals(5, answered.size)
    }

    @Test
    fun exam_review_allQuestionsHaveCorrectAnswers() {
        vm.examEngine.loadExam(10)
        val questions = vm.getExamQuestions()
        for (q in questions) {
            assertTrue("Each question should have a correct answer", q.question.correct.isNotEmpty())
        }
    }

    // ========================
    // Simulacro: passed/failed threshold
    // ========================

    @Test
    fun simulacro_allCorrect_passes() {
        vm.loadSimulacroSync()
        val count = vm.examEngine.getQuestionCount()
        for (i in 0 until count) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()

        val r = vm.simulacroResult.value!!
        assertTrue("Simulacro with all correct should pass", r.passed)
        assertEquals(count, r.correct)
    }

    @Test
    fun simulacro_allWrong_fails() {
        vm.loadSimulacroSync()
        val count = vm.examEngine.getQuestionCount()
        for (i in 0 until count) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            val wrong = q.question.opciones.keys.firstOrNull { it != q.question.correct } ?: "B"
            vm.examAnswer(wrong)
        }
        vm.finishExam()

        val r = vm.simulacroResult.value!!
        assertFalse("Simulacro with all wrong should fail", r.passed)
    }

    @Test
    fun simulacro_allUnanswered_fails() {
        vm.loadSimulacroSync()
        vm.finishExam()

        val r = vm.simulacroResult.value!!
        assertFalse("Simulacro with no answers should fail", r.passed)
        assertEquals(100, r.unanswered)
    }

    @Test
    fun simulacro_halfCorrect_halfUnanswered_shouldPass() {
        vm.loadSimulacroSync()
        val count = vm.examEngine.getQuestionCount()
        // Answer half correctly
        for (i in 0 until count / 2) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()

        val r = vm.simulacroResult.value!!
        // 50 correct * 0.60 = 30 points. Passing score = 100 * 0.60 / 2 = 30
        // So 30 >= 30 should pass
        assertTrue("Simulacro with 50% correct should pass (points = passing score)",
            r.points >= r.passingScore)
    }

    @Test
    fun simulacro_pointsCalculationIsCorrect() {
        vm.loadSimulacroSync()
        val count = vm.examEngine.getQuestionCount()
        // Answer 60 correct, 40 wrong
        for (i in 0 until 60) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        for (i in 60 until count) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            val wrong = q.question.opciones.keys.firstOrNull { it != q.question.correct } ?: "B"
            vm.examAnswer(wrong)
        }
        vm.finishExam()

        val r = vm.simulacroResult.value!!
        // points = 60 * 0.60 - 40 * 0.15 = 36 - 6 = 30
        assertEquals(30.0f, r.points, 0.01f)
    }

    @Test
    fun simulacro_maxPointsIs60() {
        assertEquals(60.0f, ExamEngine.SIMULACRO_MAX_POINTS, 0.01f)
    }

    @Test
    fun simulacro_passingScoreIs30() {
        assertEquals(30.0f, ExamEngine.SIMULACRO_PASSING_SCORE, 0.01f)
    }

    // ========================
    // Simulacro: timer
    // ========================

    @Test
    fun simulacro_timer_startsAt6000Seconds() {
        vm.loadSimulacroSync()
        assertEquals(100 * 60, vm.getSimulacroTimer())
    }

    @Test
    fun simulacro_timer_decrementsOnTick() {
        vm.loadSimulacroSync()
        val initial = vm.getSimulacroTimer()
        vm.tickSimulacroTimer()
        assertEquals(initial - 1, vm.getSimulacroTimer())
    }

    @Test
    fun simulacro_timer_returnsTrueWhenZero() {
        vm.loadSimulacroSync()
        // Tick down to 1
        for (i in 0 until 100 * 60 - 1) {
            vm.tickSimulacroTimer()
        }
        assertEquals(1, vm.getSimulacroTimer())
        // Next tick should return true (expired)
        val expired = vm.tickSimulacroTimer()
        assertTrue("Timer should return true when it reaches 0", expired)
        assertEquals(0, vm.getSimulacroTimer())
    }

    @Test
    fun simulacro_timer_returnsTrueWhenAlreadyZero() {
        vm.loadSimulacroSync()
        // Tick all the way down
        for (i in 0 until 100 * 60) {
            vm.tickSimulacroTimer()
        }
        assertEquals(0, vm.getSimulacroTimer())
        // Tick again - should return true without going negative
        val expired = vm.tickSimulacroTimer()
        assertTrue("Timer should return true when already at 0", expired)
        assertEquals(0, vm.getSimulacroTimer())
    }

    // ========================
    // Simulacro: isSimulacroMode flag
    // ========================

    @Test
    fun simulacro_isSimulacroModeTrueAfterLoad() {
        vm.loadSimulacroSync()
        assertTrue(vm.isSimulacroMode.value)
    }

    @Test
    fun simulacro_isSimulacroModeFalseAfterClear() {
        vm.loadSimulacroSync()
        assertTrue(vm.isSimulacroMode.value)
        vm.clearExamResult()
        assertFalse(vm.isSimulacroMode.value)
    }

    @Test
    fun simulacro_resultIsNullAfterClear() {
        vm.loadSimulacroSync()
        vm.finishExam()
        assertNotNull(vm.simulacroResult.value)
        vm.clearExamResult()
        assertNull(vm.simulacroResult.value)
    }

    // ========================
    // Exam: second exam after first
    // ========================

    @Test
    fun exam_secondExamAfterClear_hasFreshQuestions() {
        // First exam
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        val r1 = vm.examResult.value!!
        assertEquals(10, r1.correct)

        // Clear and do second exam
        vm.clearExamResult()
        assertNull(vm.examResult.value)

        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        val r2 = vm.examResult.value!!
        assertEquals(10, r2.correct)
        assertEquals(10, r2.total)
    }

    @Test
    fun exam_secondExamWithDifferentQuestionCount() {
        // First exam with 10
        vm.examEngine.loadExam(10)
        vm.finishExam()
        assertEquals(10, vm.examResult.value!!.total)

        vm.clearExamResult()
        // Second exam with 20
        vm.examEngine.loadExam(20)
        vm.finishExam()
        assertEquals(20, vm.examResult.value!!.total)
    }

    // ========================
    // Exam: perLaw with realistic data
    // ========================

    @Test
    fun exam_perLawContainsRealLawNamesNotOtros() {
        vm.examEngine.loadExam(20)
        for (i in 0 until 20) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()

        val r = vm.examResult.value!!
        assertFalse("perLaw should not be only 'Otros'", r.perLaw.keys == setOf("Otros"))
        assertTrue("perLaw should contain at least one known law",
            r.perLaw.keys.any { it in listOf("LOPJ", "LEC", "LECrim", "Constitución/UE/Org", "Contencioso", "Social", "Registro Civil", "Concursal") })
    }

    @Test
    fun exam_perLawCorrectCountMatchesActualCorrect() {
        vm.examEngine.loadExam(15)
        for (i in 0 until 15) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()

        val r = vm.examResult.value!!
        val perLawCorrect = r.perLaw.values.sumOf { it.correct }
        assertEquals(15, perLawCorrect)
    }

    @Test
    fun exam_perLawWrongCountMatchesActualWrong() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            val wrong = q.question.opciones.keys.firstOrNull { it != q.question.correct } ?: "B"
            vm.examAnswer(wrong)
        }
        vm.finishExam()

        val r = vm.examResult.value!!
        val perLawWrong = r.perLaw.values.sumOf { it.wrong }
        assertEquals(10, perLawWrong)
    }

    @Test
    fun exam_perLawUnansweredCountMatchesActualUnanswered() {
        vm.examEngine.loadExam(10)
        // Answer only 3
        for (i in 0 until 3) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()

        val r = vm.examResult.value!!
        val perLawUnanswered = r.perLaw.values.sumOf { it.unanswered }
        assertEquals(7, perLawUnanswered)
    }

    // ========================
    // Exam: loadExam with different sizes
    // ========================

    @Test
    fun exam_loadExam5_loads5Questions() {
        vm.examEngine.loadExam(5)
        assertEquals(5, vm.examEngine.getQuestionCount())
    }

    @Test
    fun exam_loadExam20_loads20Questions() {
        vm.examEngine.loadExam(20)
        assertEquals(20, vm.examEngine.getQuestionCount())
    }

    @Test
    fun exam_loadExam0_loads0Questions() {
        vm.examEngine.loadExam(0)
        assertEquals(0, vm.examEngine.getQuestionCount())
    }

    // ========================
    // Exam: getCurrentQuestion
    // ========================

    @Test
    fun exam_getCurrentQuestion_returnsFirstAfterLoad() {
        vm.examEngine.loadExam(10)
        val q = vm.examEngine.getCurrentQuestion()
        assertNotNull(q)
        assertEquals(0, vm.examEngine.getCurrentIndex())
    }

    @Test
    fun exam_getCurrentQuestion_returnsNullBeforeLoad() {
        // Fresh engine, no exam loaded
        assertNull(vm.examEngine.getCurrentQuestion())
    }
}
