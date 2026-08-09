package com.opoleyes.ui.navigation

import com.opoleyes.FakeGameRepository
import com.opoleyes.FakePreferencesManager
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
class ExamFlowIntegrationTest {

    private lateinit var vm: GameViewModel
    private lateinit var prefs: FakePreferencesManager
    private lateinit var progressRepo: ProgressRepository
    private lateinit var examEngine: ExamEngine

    companion object {
        private val testDispatcher = StandardTestDispatcher()

        @JvmStatic
        @BeforeClass
        fun setUpClass() { Dispatchers.setMain(testDispatcher) }

        @JvmStatic
        @AfterClass
        fun tearDownClass() { Dispatchers.resetMain() }
    }

    private fun makeTestData(testId: String, testName: String, questionCount: Int = 10): TestData {
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
        makeTestData("test_cont", "Contencioso-Administrativa: Demanda y Contestación"),
        makeTestData("test_social", "Social: Principios Laborales"),
        makeTestData("test_registro", "Registro Civil (Ley 2011) - Títulos"),
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
        examEngine = ExamEngine.createForTestData(statsRepo, makeRealisticTestData())
        vm = GameViewModel.createForTest(
            progressRepo, statsRepo, missionRepo,
            AchievementChecker(prefs), ChestSystem(prefs),
            prefs, engine, examEngine
        )
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    private fun wrongAnswerFor(q: com.opoleyes.data.model.QuestionEntry): String =
        q.opciones.keys.firstOrNull { it != q.correct }
            ?: listOf("A", "B", "C", "D").first { it != q.correct }

    private fun answerAllWrong(count: Int) {
        for (i in 0 until count) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(wrongAnswerFor(q.question))
        }
    }

    // ========================
    // Test 1: Basic exam flow - finish exam and get results with correct law mapping
    // ========================

    @org.junit.Test
    fun examFlow_finishExam_resultsAreNotNullAndPerLawIsCorrect() {
        vm.examEngine.loadExam(10)
        answerAllWrong(10)
        vm.finishExam()

        val r = vm.examResult.value
        assertNotNull("examResult should be set after finishExam", r)
        assertEquals(10, r!!.total)
        assertEquals(0, r.correct)
        assertEquals(10, r.wrong)
        assertTrue("perLaw should be populated", r.perLaw.isNotEmpty())
        assertFalse("perLaw should not contain only 'Otros': ${r.perLaw.keys}",
            r.perLaw.keys == setOf("Otros"))
    }

    // ========================
    // Test 2: Second exam after clearExamResult should work
    // ========================

    @org.junit.Test
    fun examFlow_secondExamAfterClearResultWorks() {
        // First exam
        vm.examEngine.loadExam(10)
        answerAllWrong(10)
        vm.finishExam()
        assertNotNull(vm.examResult.value)

        // Clear result (simulates navigating away from result screen)
        vm.clearExamResult()
        assertNull(vm.examResult.value)

        // Second exam
        vm.examEngine.loadExam(10)
        answerAllWrong(10)
        vm.finishExam()

        val r = vm.examResult.value
        assertNotNull("examResult should be set after second exam", r)
        assertEquals(10, r!!.total)
        assertEquals(0, r.correct)
        assertTrue("Second exam perLaw should be populated", r.perLaw.isNotEmpty())
    }

    // ========================
    // Test 3: finishExam is idempotent (guard against double submission)
    // ========================

    @org.junit.Test
    fun examFlow_doubleFinishExamDoesNotOverwriteResult() {
        vm.examEngine.loadExam(10)
        answerAllWrong(10)
        vm.finishExam()

        val firstResult = vm.examResult.value
        assertNotNull(firstResult)

        vm.finishExam() // Should be a no-op

        val secondResult = vm.examResult.value
        assertEquals("Second finishExam should not change result", firstResult, secondResult)
    }

    // ========================
    // Test 4: clearExamResult resets all exam state
    // ========================

    @org.junit.Test
    fun examFlow_clearExamResultResetsAllState() {
        vm.examEngine.loadExam(10)
        answerAllWrong(10)
        vm.finishExam()
        assertNotNull(vm.examResult.value)

        vm.clearExamResult()
        assertNull(vm.examResult.value)
        assertNull(vm.simulacroResult.value)
        assertFalse(vm.isSimulacroMode.value)
    }

    // ========================
    // Test 5: Exam with unanswered questions still maps to correct laws
    // ========================

    @org.junit.Test
    fun examFlow_unansweredQuestionsMappedToCorrectLaws() {
        vm.examEngine.loadExam(10)
        // Don't answer any
        vm.finishExam()

        val r = vm.examResult.value!!
        assertEquals(10, r.unanswered)
        assertFalse("Unanswered questions should not all be 'Otros': ${r.perLaw.keys}",
            r.perLaw.keys == setOf("Otros"))
    }

    // ========================
    // Test 6: perLaw totals always match total questions
    // ========================

    @org.junit.Test
    fun examFlow_perLawTotalsMatchQuestionCount() {
        for (count in listOf(5, 10, 20)) {
            vm.clearExamResult()
            vm.examEngine.loadExam(count)
            answerAllWrong(count)
            vm.finishExam()

            val r = vm.examResult.value!!
            assertEquals("perLaw totals should match question count for $count questions",
                count, r.perLaw.values.sumOf { it.total })
        }
    }
}
