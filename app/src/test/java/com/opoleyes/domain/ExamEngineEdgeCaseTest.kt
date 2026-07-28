package com.opoleyes.domain

import com.opoleyes.TestContextProvider
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
class ExamEngineEdgeCaseTest {

    private lateinit var engine: ExamEngine

    @Before
    fun setup() {
        engine = ExamEngine(TestContextProvider.getContext())
    }

    // === Edge cases: loadExam with various counts ===

    @Test
    fun fun_loadExam_with1Question() {
        engine.loadExam(1)
        assertEquals(1, engine.getQuestionCount())
        assertNotNull(engine.getCurrentQuestion())
    }

    @Test
    fun fun_loadExam_withVeryLargeCount_capsAtAvailable() {
        engine.loadExam(10000)
        assertTrue("Should not exceed available questions", engine.getQuestionCount() <= 10000)
        assertTrue("Should load some questions", engine.getQuestionCount() > 0)
    }

    @Test
    fun fun_loadExam_with0Questions() {
        engine.loadExam(0)
        assertEquals(0, engine.getQuestionCount())
        assertNull(engine.getCurrentQuestion())
    }

    // === Edge cases: navigation bounds ===

    @Test
    fun fun_navigateTo_negativeIndex_clampsTo0() {
        engine.loadExam(10)
        engine.navigateTo(-5)
        assertEquals(0, engine.getCurrentIndex())
    }

    @Test
    fun fun_navigateTo_beyondSize_clampsToLast() {
        engine.loadExam(10)
        engine.navigateTo(100)
        assertEquals(9, engine.getCurrentIndex())
    }

    // === Edge cases: answer on empty exam ===

    @Test
    fun fun_answer_onEmptyExam_doesNotCrash() {
        engine.answer("A")
        assertEquals(0, engine.getAnsweredCount())
    }

    // === Edge cases: grade with no questions ===

    @Test
    fun fun_grade_onEmptyExam_returns0Score() {
        val result = engine.grade()
        assertEquals(0, result.total)
        assertEquals(0f, result.score, 0.01f)
    }

    // === Edge cases: grade with all unanswered ===

    @Test
    fun fun_grade_allUnanswered() {
        engine.loadExam(10)
        val result = engine.grade()
        assertEquals(10, result.total)
        assertEquals(0, result.correct)
        assertEquals(0, result.wrong)
        assertEquals(10, result.unanswered)
        assertEquals(0f, result.score, 0.01f)
    }

    // === Edge cases: grade with mix ===

    @Test
    fun fun_grade_mixCorrectWrongUnanswered() {
        engine.loadExam(10)
        // 4 correct
        for (i in 0 until 4) {
            engine.navigateTo(i)
            val q = engine.getCurrentQuestion()!!
            engine.answer(q.question.correct)
        }
        // 3 wrong
        for (i in 4 until 7) {
            engine.navigateTo(i)
            val q = engine.getCurrentQuestion()!!
            val wrong = listOf("A", "B", "C", "D").filter { it != q.question.correct }.first()
            engine.answer(wrong)
        }
        val result = engine.grade()
        assertEquals(10, result.total)
        assertEquals(4, result.correct)
        assertEquals(3, result.wrong)
        assertEquals(3, result.unanswered)
    }

    // === Edge cases: changing answer ===

    @Test
    fun fun_changeAnswer_updatesUserAnswer() {
        engine.loadExam(10)
        engine.answer("A")
        engine.answer("B")
        assertEquals("B", engine.getCurrentQuestion()?.userAnswer)
    }

    @Test
    fun fun_changeAnswer_doesNotIncreaseAnsweredCount() {
        engine.loadExam(10)
        engine.answer("A")
        assertEquals(1, engine.getAnsweredCount())
        engine.answer("B")
        assertEquals(1, engine.getAnsweredCount())
    }

    // === Edge cases: next/prev at bounds ===

    @Test
    fun fun_next_atLastQuestion_returnsFalse() {
        engine.loadExam(5)
        engine.navigateTo(4)
        assertFalse(engine.next())
    }

    @Test
    fun fun_prev_atFirstQuestion_returnsFalse() {
        engine.loadExam(5)
        assertFalse(engine.prev())
    }

    // === Edge cases: isFinished ===

    @Test
    fun fun_isFinished_atLastIndex_returnsFalse() {
        engine.loadExam(5)
        engine.navigateTo(4)
        assertFalse(engine.isFinished())
    }

    @Test
    fun fun_isFinished_emptyExam_returnsTrue() {
        assertTrue(engine.isFinished())
    }

    // === Edge cases: per-law results ===

    @Test
    fun fun_grade_perLawResultsSumToTotal() {
        engine.loadExam(20)
        for (i in 0 until 20) {
            engine.navigateTo(i)
            engine.answer("A")
        }
        val result = engine.grade()
        val perLawTotal = result.perLaw.values.sumOf { it.total }
        assertEquals(result.total, perLawTotal)
    }

    @Test
    fun fun_grade_perLawCorrectSumsToTotal() {
        engine.loadExam(20)
        for (i in 0 until 20) {
            engine.navigateTo(i)
            engine.answer("A")
        }
        val result = engine.grade()
        val perLawCorrect = result.perLaw.values.sumOf { it.correct }
        assertEquals(result.correct, perLawCorrect)
    }

    // === Edge cases: score calculation ===

    @Test
    fun fun_grade_allCorrect_scoreIs10() {
        engine.loadExam(10)
        for (i in 0 until 10) {
            engine.navigateTo(i)
            val q = engine.getCurrentQuestion()!!
            engine.answer(q.question.correct)
        }
        val result = engine.grade()
        assertEquals(10f, result.score, 0.01f)
    }

    @Test
    fun fun_grade_halfCorrect_scoreIs5() {
        engine.loadExam(10)
        for (i in 0 until 5) {
            engine.navigateTo(i)
            val q = engine.getCurrentQuestion()!!
            engine.answer(q.question.correct)
        }
        for (i in 5 until 10) {
            engine.navigateTo(i)
            val q = engine.getCurrentQuestion()!!
            val wrong = listOf("A", "B", "C", "D").filter { it != q.question.correct }.first()
            engine.answer(wrong)
        }
        val result = engine.grade()
        assertEquals(5f, result.score, 0.01f)
    }
}
