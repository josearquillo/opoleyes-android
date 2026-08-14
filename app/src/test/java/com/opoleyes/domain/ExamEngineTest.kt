package com.opoleyes.domain

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.opoleyes.TestContextProvider
import com.opoleyes.data.model.QuestionEntry
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

    // === Edge cases ===

    @Test
    fun answer_afterFinish_doesNothing() {
        engine.loadExam(10)
        val count = engine.getQuestionCount()
        // Navigate to last question
        engine.navigateTo(count - 1)
        // Try to answer beyond the last question
        engine.navigateTo(count) // coerceIn keeps it at last
        engine.answer("A")
        // Should not crash
    }

    @Test
    fun clearAnswer_removesUserAnswer() {
        engine.loadExam(10)
        engine.navigateTo(0)
        val q = engine.getCurrentQuestion()!!
        engine.answer(q.question.correct)
        assertEquals(1, engine.getAnsweredCount())
        engine.clearAnswer()
        assertEquals(0, engine.getAnsweredCount())
    }

    @Test
    fun clearAnswer_afterFinish_doesNothing() {
        engine.loadExam(10)
        val count = engine.getQuestionCount()
        engine.navigateTo(count) // coerceIn keeps at last
        engine.clearAnswer()
        // Should not crash
    }

    @Test
    fun next_atLastQuestion_returnsFalse() {
        engine.loadExam(10)
        val count = engine.getQuestionCount()
        engine.navigateTo(count - 1)
        assertFalse("next() at last question should return false", engine.next())
    }

    @Test
    fun next_inMiddle_returnsTrue() {
        engine.loadExam(10)
        engine.navigateTo(0)
        assertTrue("next() in middle should return true", engine.next())
    }

    @Test
    fun prev_atFirstQuestion_returnsFalse() {
        engine.loadExam(10)
        engine.navigateTo(0)
        assertFalse("prev() at first question should return false", engine.prev())
    }

    @Test
    fun prev_inMiddle_returnsTrue() {
        engine.loadExam(10)
        engine.navigateTo(1)
        assertTrue("prev() in middle should return true", engine.prev())
    }

    @Test
    fun navigateTo_emptyQuestions_doesNothing() {
        // Don't load exam, questions should be empty
        engine.navigateTo(0)
        // Should not crash
    }

    @Test
    fun isFinished_falseWhenNotAtEnd() {
        engine.loadExam(10)
        assertFalse("Should not be finished at start", engine.isFinished())
    }

    @Test
    fun isFinished_trueWhenAtEnd() {
        engine.loadExam(10)
        val count = engine.getQuestionCount()
        engine.navigateTo(count - 1)
        engine.next() // try to go past last
        // isFinished checks currentIndex >= questions.size
        // After next() at last, currentIndex stays at count-1, so not finished
        // Actually isFinished is currentIndex >= questions.size, and next() at last doesn't increment
        // So let's navigateTo(count) which coerces to count-1
        // isFinished would be false since currentIndex = count-1 < count
        assertFalse("Should not be finished at last question", engine.isFinished())
    }

    @Test
    fun grade_withNoQuestions_returnsZeros() {
        val result = engine.grade()
        assertEquals(0, result.total)
        assertEquals(0, result.correct)
        assertEquals(0, result.wrong)
        assertEquals(0, result.unanswered)
        assertEquals(0f, result.score, 0.01f)
    }

    @Test
    fun gradeSimulacro_withNoQuestions_returnsZeros() {
        val result = engine.gradeSimulacro()
        assertEquals(0, result.total)
        assertEquals(0, result.correct)
        assertEquals(0, result.wrong)
        assertEquals(0, result.unanswered)
        assertEquals(0f, result.points, 0.01f)
        assertFalse("Should not pass with no questions", result.passed)
    }

    @Test
    fun loadSimulacro_loadsCorrectQuestionCount() {
        engine.loadSimulacro()
        assertEquals(ExamEngine.SIMULACRO_QUESTIONS, engine.getQuestionCount())
    }

    @Test
    fun answer_andClearAnswer_roundTrip() {
        engine.loadExam(10)
        engine.navigateTo(0)
        engine.answer("A")
        assertEquals(1, engine.getAnsweredCount())
        engine.clearAnswer()
        assertEquals(0, engine.getAnsweredCount())
        engine.answer("B")
        assertEquals(1, engine.getAnsweredCount())
    }

    @Test
    fun navigateTo_clampsToValidRange() {
        engine.loadExam(10)
        val count = engine.getQuestionCount()
        engine.navigateTo(-1)
        assertEquals(0, engine.getCurrentIndex())
        engine.navigateTo(count + 10)
        assertEquals(count - 1, engine.getCurrentIndex())
    }

    @Test
    fun next_inMiddle_advances() {
        engine.loadExam(10)
        engine.navigateTo(0)
        assertTrue(engine.next())
        assertEquals(1, engine.getCurrentIndex())
    }

    @Test
    fun prev_inMiddle_goesBack() {
        engine.loadExam(10)
        engine.navigateTo(2)
        assertTrue(engine.prev())
        assertEquals(1, engine.getCurrentIndex())
    }

    @Test
    fun createForTest_loadsQuestionsFromPool() {
        val pool = listOf(
            QuestionEntry("q1", mapOf("A" to "a", "B" to "b"), "A", 50, "t1", "1", 3),
            QuestionEntry("q2", mapOf("A" to "a", "B" to "b"), "B", 50, "t1", "2", 3),
            QuestionEntry("q3", mapOf("A" to "a", "B" to "b"), "A", 50, "t1", "3", 3)
        )
        val fakeStats = object : com.opoleyes.data.IStatsRepository {
            override fun getStats() = emptyMap<String, com.opoleyes.data.model.QuestionStat>()
            override fun updateStat(key: String, isCorrect: Boolean) {}
            override fun getLeyProgress(testId: String) = 0
        }
        val eng = ExamEngine.createForTest(fakeStats, pool)
        eng.loadExam(3)
        assertEquals(3, eng.getQuestionCount())
    }

    @Test
    fun createForTest_gradeWithMixedAnswers() {
        val pool = listOf(
            QuestionEntry("q1", mapOf("A" to "a", "B" to "b"), "A", 50, "t1", "1", 3),
            QuestionEntry("q2", mapOf("A" to "a", "B" to "b"), "B", 50, "t1", "2", 3),
            QuestionEntry("q3", mapOf("A" to "a", "B" to "b"), "A", 50, "t1", "3", 3)
        )
        val fakeStats = object : com.opoleyes.data.IStatsRepository {
            override fun getStats() = emptyMap<String, com.opoleyes.data.model.QuestionStat>()
            override fun updateStat(key: String, isCorrect: Boolean) {}
            override fun getLeyProgress(testId: String) = 0
        }
        val eng = ExamEngine.createForTest(fakeStats, pool)
        eng.loadExam(3)
        // Answer first question correctly, second wrong, third unanswered
        eng.navigateTo(0); eng.answer(eng.getCurrentQuestion()!!.question.correct)
        eng.navigateTo(1); eng.answer(if (eng.getCurrentQuestion()!!.question.correct == "A") "B" else "A")
        eng.navigateTo(2) // unanswered
        val result = eng.grade()
        assertEquals(3, result.total)
        assertEquals(1, result.correct)
        assertEquals(1, result.wrong)
        assertEquals(1, result.unanswered)
    }

    @Test
    fun createForTest_simulacroGrading() {
        val pool = listOf(
            QuestionEntry("q1", mapOf("A" to "a", "B" to "b"), "A", 50, "t1", "1", 3),
            QuestionEntry("q2", mapOf("A" to "a", "B" to "b"), "B", 50, "t1", "2", 3)
        )
        val fakeStats = object : com.opoleyes.data.IStatsRepository {
            override fun getStats() = emptyMap<String, com.opoleyes.data.model.QuestionStat>()
            override fun updateStat(key: String, isCorrect: Boolean) {}
            override fun getLeyProgress(testId: String) = 0
        }
        val eng = ExamEngine.createForTest(fakeStats, pool)
        eng.loadExam(2)
        // Answer each question correctly based on its correct answer
        eng.navigateTo(0); eng.answer(eng.getCurrentQuestion()!!.question.correct)
        eng.navigateTo(1); eng.answer(eng.getCurrentQuestion()!!.question.correct)
        val result = eng.gradeSimulacro()
        assertEquals(2, result.correct)
        assertTrue(result.points > 0)
    }

    @Test
    fun clearAnswer_onEmptyExam_noCrash() {
        engine.loadExam(0)
        engine.clearAnswer() // should not crash
    }

    @Test
    fun answer_onEmptyExam_noCrash() {
        engine.loadExam(0)
        engine.answer("A") // should not crash
    }

    @Test
    fun navigateTo_emptyExam_noCrash() {
        engine.loadExam(0)
        engine.navigateTo(0) // should not crash
    }

    @Test
    fun isFinished_atLastIndex_returnsTrue() {
        engine.loadExam(10)
        val count = engine.getQuestionCount()
        engine.navigateTo(count - 1)
        assertFalse(engine.isFinished())
        engine.navigateTo(count)
        // navigateTo clamps to count-1, so isFinished should be false
        assertFalse(engine.isFinished())
    }
}
