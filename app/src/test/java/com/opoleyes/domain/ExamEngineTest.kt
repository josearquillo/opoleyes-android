package com.opoleyes.domain

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.opoleyes.TestContextProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExamEngineTest {

    private lateinit var engine: ExamEngine

    @Before
    fun setup() {
        engine = ExamEngine(TestContextProvider.getContext())
    }

    @Test
    fun grade_allCorrect_scoreIsTen() {
        engine.loadExam(10)
        val count = engine.getQuestionCount()
        for (i in 0 until count) {
            engine.navigateTo(i)
            val q = engine.getCurrentQuestion()!!
            engine.answer(q.question.correct)
        }
        val result = engine.grade()
        assertEquals(count, result.correct)
        assertEquals(0, result.wrong)
        assertEquals(0, result.unanswered)
        assertEquals(10f, result.score, 0.01f)
    }

    @Test
    fun grade_halfCorrect_scoreIsFive() {
        engine.loadExam(10)
        val count = engine.getQuestionCount()
        for (i in 0 until count) {
            engine.navigateTo(i)
            val q = engine.getCurrentQuestion()!!
            if (i % 2 == 0) {
                engine.answer(q.question.correct)
            } else {
                val wrong = listOf("A", "B", "C", "D").first { it != q.question.correct }
                engine.answer(wrong)
            }
        }
        val result = engine.grade()
        assertEquals(5f, result.score, 0.01f)
    }

    @Test
    fun gradeSimulacro_correctScoring() {
        // SIMULACRO_CORRECT_POINTS = 0.60, SIMULACRO_WRONG_PENALTY = 0.15
        engine.loadExam(10)
        val count = engine.getQuestionCount()
        // Answer 6 correct, 2 wrong, 2 unanswered
        for (i in 0 until count) {
            engine.navigateTo(i)
            val q = engine.getCurrentQuestion()!!
            when {
                i < 6 -> engine.answer(q.question.correct)
                i < 8 -> {
                    val wrong = listOf("A", "B", "C", "D").first { it != q.question.correct }
                    engine.answer(wrong)
                }
                // i >= 8: leave unanswered
            }
        }
        val result = engine.gradeSimulacro()
        assertEquals(6, result.correct)
        assertEquals(2, result.wrong)
        assertEquals(2, result.unanswered)
        val expectedPoints = 6 * 0.60f - 2 * 0.15f
        assertEquals(expectedPoints, result.points, 0.01f)
    }

    @Test
    fun gradeSimulacro_passingScoreIsHalfOfMax() {
        // SIMULACRO_MAX_POINTS = 100 * 0.60 = 60.0
        // SIMULACRO_PASSING_SCORE = 60.0 / 2 = 30.0
        assertEquals(60.0f, ExamEngine.SIMULACRO_MAX_POINTS, 0.01f)
        assertEquals(30.0f, ExamEngine.SIMULACRO_PASSING_SCORE, 0.01f)
    }

    @Test
    fun gradeSimulacro_unansweredNotPenalized() {
        engine.loadExam(10)
        // Leave all unanswered
        val result = engine.gradeSimulacro()
        assertEquals(0, result.correct)
        assertEquals(0, result.wrong)
        assertEquals(engine.getQuestionCount(), result.unanswered)
        assertEquals(0f, result.points, 0.01f)
        assertFalse("Should not pass with 0 points", result.passed)
    }

    @Test
    fun gradeSimulacro_passesAtThreshold() {
        // To pass: points >= 30.0, so need at least 50 correct (50 * 0.60 = 30.0)
        engine.loadExam(100)
        val count = engine.getQuestionCount()
        var answered = 0
        for (i in 0 until count) {
            engine.navigateTo(i)
            val q = engine.getCurrentQuestion()!!
            if (answered < 50) {
                engine.answer(q.question.correct)
                answered++
            }
            // rest unanswered
        }
        val result = engine.gradeSimulacro()
        assertEquals(50, result.correct)
        val expectedPoints = 50 * 0.60f
        assertEquals(expectedPoints, result.points, 0.01f)
        assertTrue("Should pass with exactly 50 correct ($expectedPoints >= ${ExamEngine.SIMULACRO_PASSING_SCORE})",
            result.passed)
    }

    @Test
    fun loadExam_distributesByLawWeights() {
        engine.loadExam(100)
        // Verify that questions were loaded
        assertTrue("Should load questions", engine.getQuestionCount() > 0)
        // Answer all correctly to populate perLaw in grade result
        val count = engine.getQuestionCount()
        for (i in 0 until count) {
            engine.navigateTo(i)
            val q = engine.getCurrentQuestion()!!
            engine.answer(q.question.correct)
        }
        val result = engine.grade()
        // Verify perLaw has at least one law with questions
        assertTrue("perLaw should have at least one law", result.perLaw.isNotEmpty())
        // Verify total matches
        val totalAllocated = result.perLaw.values.sumOf { it.total }
        assertEquals(engine.getQuestionCount(), totalAllocated)
    }
}
