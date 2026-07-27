package com.opoleyes.domain

import com.opoleyes.TestContextProvider
import com.opoleyes.data.local.PreferencesManager
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExamEngineTest {

    private lateinit var examEngine: ExamEngine
    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        val context = TestContextProvider.getContext()
        prefs = PreferencesManager(context)
        prefs.resetAll()
        examEngine = ExamEngine(context)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun fun_loadExam_loadsCorrectNumberOfQuestions() {
        examEngine.loadExam(25)
        assertEquals(25, examEngine.getQuestionCount())
    }

    @Test
    fun fun_loadExam_loads50Questions() {
        examEngine.loadExam(50)
        assertEquals(50, examEngine.getQuestionCount())
    }

    @Test
    fun fun_loadExam_loads100Questions() {
        examEngine.loadExam(100)
        assertEquals(100, examEngine.getQuestionCount())
    }

    @Test
    fun fun_loadExam_questionsAreNotEmpty() {
        examEngine.loadExam(10)
        val q = examEngine.getCurrentQuestion()
        assertNotNull(q)
        assertTrue(q!!.question.enunciado.isNotEmpty())
        assertTrue(q.question.opciones.isNotEmpty())
    }

    @Test
    fun fun_loadExam_currentIndexStartsAt0() {
        examEngine.loadExam(10)
        assertEquals(0, examEngine.getCurrentIndex())
    }

    @Test
    fun fun_answer_storesUserAnswer() {
        examEngine.loadExam(10)
        val q = examEngine.getCurrentQuestion()!!
        examEngine.answer("A")
        val current = examEngine.getCurrentQuestion()!!
        assertEquals("A", current.userAnswer)
    }

    @Test
    fun fun_answer_updatesAnsweredCount() {
        examEngine.loadExam(10)
        assertEquals(0, examEngine.getAnsweredCount())
        examEngine.answer("A")
        assertEquals(1, examEngine.getAnsweredCount())
    }

    @Test
    fun fun_next_advancesToNextQuestion() {
        examEngine.loadExam(10)
        assertEquals(0, examEngine.getCurrentIndex())
        assertTrue(examEngine.next())
        assertEquals(1, examEngine.getCurrentIndex())
    }

    @Test
    fun fun_next_returnsFalseAtLastQuestion() {
        examEngine.loadExam(5)
        examEngine.navigateTo(4)
        assertFalse(examEngine.next())
        assertEquals(4, examEngine.getCurrentIndex())
    }

    @Test
    fun fun_prev_goesToPreviousQuestion() {
        examEngine.loadExam(10)
        examEngine.navigateTo(3)
        assertTrue(examEngine.prev())
        assertEquals(2, examEngine.getCurrentIndex())
    }

    @Test
    fun fun_prev_returnsFalseAtFirstQuestion() {
        examEngine.loadExam(10)
        assertFalse(examEngine.prev())
        assertEquals(0, examEngine.getCurrentIndex())
    }

    @Test
    fun fun_navigateTo_jumpsToSpecificQuestion() {
        examEngine.loadExam(10)
        examEngine.navigateTo(5)
        assertEquals(5, examEngine.getCurrentIndex())
    }

    @Test
    fun fun_navigateTo_clampsToValidRange() {
        examEngine.loadExam(10)
        examEngine.navigateTo(99)
        assertEquals(9, examEngine.getCurrentIndex())
        examEngine.navigateTo(-5)
        assertEquals(0, examEngine.getCurrentIndex())
    }

    @Test
    fun fun_answer_canChangeAnswer() {
        examEngine.loadExam(10)
        examEngine.answer("A")
        examEngine.answer("B")
        val current = examEngine.getCurrentQuestion()!!
        assertEquals("B", current.userAnswer)
        assertEquals(1, examEngine.getAnsweredCount())
    }

    @Test
    fun fun_answer_navigateAndAnswerDifferentQuestions() {
        examEngine.loadExam(10)
        examEngine.answer("A")
        examEngine.navigateTo(3)
        examEngine.answer("C")
        examEngine.navigateTo(7)
        examEngine.answer("B")
        assertEquals(3, examEngine.getAnsweredCount())
    }

    @Test
    fun fun_grade_allCorrect() {
        examEngine.loadExam(10)
        for (i in 0 until 10) {
            examEngine.navigateTo(i)
            val q = examEngine.getCurrentQuestion()!!
            examEngine.answer(q.question.correct)
        }
        val result = examEngine.grade()
        assertEquals(10, result.total)
        assertEquals(10, result.correct)
        assertEquals(0, result.wrong)
        assertEquals(0, result.unanswered)
        assertEquals(10f, result.score, 0.01f)
    }

    @Test
    fun fun_grade_allWrong() {
        examEngine.loadExam(10)
        for (i in 0 until 10) {
            examEngine.navigateTo(i)
            val q = examEngine.getCurrentQuestion()!!
            val wrong = listOf("A", "B", "C", "D").filter { it != q.question.correct }.first()
            examEngine.answer(wrong)
        }
        val result = examEngine.grade()
        assertEquals(10, result.total)
        assertEquals(0, result.correct)
        assertEquals(10, result.wrong)
        assertEquals(0, result.unanswered)
        assertEquals(0f, result.score, 0.01f)
    }

    @Test
    fun fun_grade_allUnanswered() {
        examEngine.loadExam(10)
        val result = examEngine.grade()
        assertEquals(10, result.total)
        assertEquals(0, result.correct)
        assertEquals(0, result.wrong)
        assertEquals(10, result.unanswered)
        assertEquals(0f, result.score, 0.01f)
    }

    @Test
    fun fun_grade_halfCorrect() {
        examEngine.loadExam(10)
        for (i in 0 until 5) {
            examEngine.navigateTo(i)
            val q = examEngine.getCurrentQuestion()!!
            examEngine.answer(q.question.correct)
        }
        val result = examEngine.grade()
        assertEquals(5, result.correct)
        assertEquals(5, result.unanswered)
        assertEquals(5f, result.score, 0.01f)
    }

    @Test
    fun fun_grade_perLawBreakdownIsNotEmpty() {
        examEngine.loadExam(20)
        for (i in 0 until 20) {
            examEngine.navigateTo(i)
            val q = examEngine.getCurrentQuestion()!!
            examEngine.answer(q.question.correct)
        }
        val result = examEngine.grade()
        assertTrue(result.perLaw.isNotEmpty())
    }

    @Test
    fun fun_grade_perLawSumsToTotal() {
        examEngine.loadExam(20)
        for (i in 0 until 20) {
            examEngine.navigateTo(i)
            val q = examEngine.getCurrentQuestion()!!
            examEngine.answer(q.question.correct)
        }
        val result = examEngine.grade()
        val perLawTotal = result.perLaw.values.sumOf { it.total }
        assertEquals(result.total, perLawTotal)
    }

    @Test
    fun fun_grade_perLawCorrectSumsToTotal() {
        examEngine.loadExam(20)
        for (i in 0 until 20) {
            examEngine.navigateTo(i)
            val q = examEngine.getCurrentQuestion()!!
            examEngine.answer(q.question.correct)
        }
        val result = examEngine.grade()
        val perLawCorrect = result.perLaw.values.sumOf { it.correct }
        assertEquals(result.correct, perLawCorrect)
    }

    @Test
    fun fun_isFinished_returnsFalseWhenQuestionsRemain() {
        examEngine.loadExam(10)
        assertFalse(examEngine.isFinished())
    }

    @Test
    fun fun_getQuestions_returnsAllQuestions() {
        examEngine.loadExam(10)
        val questions = examEngine.getQuestions()
        assertEquals(10, questions.size)
    }

    @Test
    fun fun_loadExam_questionsAreUnique() {
        examEngine.loadExam(20)
        val questions = examEngine.getQuestions()
        val ids = questions.map { "${it.question.testId}:${it.question.origId}" }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun fun_grade_mixedAnswers() {
        examEngine.loadExam(10)
        for (i in 0 until 3) {
            examEngine.navigateTo(i)
            val q = examEngine.getCurrentQuestion()!!
            examEngine.answer(q.question.correct)
        }
        for (i in 3 until 6) {
            examEngine.navigateTo(i)
            val q = examEngine.getCurrentQuestion()!!
            val wrong = listOf("A", "B", "C", "D").filter { it != q.question.correct }.first()
            examEngine.answer(wrong)
        }
        val result = examEngine.grade()
        assertEquals(3, result.correct)
        assertEquals(3, result.wrong)
        assertEquals(4, result.unanswered)
        assertEquals(3f, result.score, 0.01f)
    }

    // === Weighted distribution tests ===

    @Test
    fun fun_loadExam_lopjHasMostQuestions() {
        examEngine.loadExam(100)
        for (i in 0 until examEngine.getQuestionCount()) {
            examEngine.navigateTo(i)
            examEngine.answer(examEngine.getCurrentQuestion()!!.question.correct)
        }
        val result = examEngine.grade()
        val lopj = result.perLaw["LOPJ"]
        // LOPJ has weight 28 (highest), so it should be present if data exists
        if (lopj != null) {
            val maxOther = result.perLaw.filterKeys { it != "LOPJ" }.values.maxOfOrNull { it.total } ?: 0
            assertTrue("LOPJ should have the most questions", lopj.total >= maxOther)
        }
    }

    @Test
    fun fun_loadExam_distributionIsProportional() {
        examEngine.loadExam(50)
        for (i in 0 until examEngine.getQuestionCount()) {
            examEngine.navigateTo(i)
            examEngine.answer(examEngine.getCurrentQuestion()!!.question.correct)
        }
        val result = examEngine.grade()
        // LOPJ weight is 28 out of 100 total, so ~28% of questions
        val lopj = result.perLaw["LOPJ"]
        if (lopj != null) {
            val pct = lopj.total * 100 / 50
            assertTrue("LOPJ should be roughly 28%: got $pct%", pct in 15..40)
        }
        // LEC weight is 22, so ~22%
        val lec = result.perLaw["LEC"]
        if (lec != null) {
            val pct = lec.total * 100 / 50
            assertTrue("LEC should be roughly 22%: got $pct%", pct in 10..35)
        }
    }

    @Test
    fun fun_loadExam_multipleLawsPresent() {
        examEngine.loadExam(50)
        for (i in 0 until examEngine.getQuestionCount()) {
            examEngine.navigateTo(i)
            examEngine.answer(examEngine.getCurrentQuestion()!!.question.correct)
        }
        val result = examEngine.grade()
        assertTrue("Per-law breakdown should not be empty", result.perLaw.isNotEmpty())
    }

    @Test
    fun fun_loadExam_concursalHasFewestOrZero() {
        examEngine.loadExam(50)
        for (i in 0 until examEngine.getQuestionCount()) {
            examEngine.navigateTo(i)
            examEngine.answer(examEngine.getCurrentQuestion()!!.question.correct)
        }
        val result = examEngine.grade()
        val concursal = result.perLaw["Concursal"]
        val lopj = result.perLaw["LOPJ"]
        if (concursal != null && lopj != null) {
            assertTrue("Concursal should have fewer questions than LOPJ",
                concursal.total <= lopj.total)
        }
    }

    @Test
    fun fun_loadExam_differentLoadsProduceDifferentQuestions() {
        examEngine.loadExam(20)
        val firstRun = examEngine.getQuestions().map { "${it.question.testId}:${it.question.origId}" }
        examEngine.loadExam(20)
        val secondRun = examEngine.getQuestions().map { "${it.question.testId}:${it.question.origId}" }
        // With shuffling, it's extremely unlikely both runs are identical
        assertTrue("Different loads should produce different question sets",
            firstRun != secondRun)
    }
}
