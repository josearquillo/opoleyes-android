package com.opoleyes.domain

import com.opoleyes.FakeStatsRepository
import com.opoleyes.data.model.Answer
import com.opoleyes.data.model.Question
import com.opoleyes.data.model.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

class ExamEngineLawMappingTest {

    private fun makeTestData(
        testId: String,
        testName: String,
        tema: Int? = 1,
        questionCount: Int = 5
    ): TestData {
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
            test = com.opoleyes.data.model.Test(id = testId, name = testName, tema = tema),
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

    @org.junit.Test
    fun loadExam_perLawDoesNotContainOtros_whenAllTestsMapToKnownLaws() {
        val testData = makeRealisticTestData()
        val statsRepo = FakeStatsRepository()
        val engine = ExamEngine.createForTestData(statsRepo, testData)

        engine.loadExam(10)
        val result = engine.grade()

        assertNotNull(result)
        assertTrue("perLaw should be populated", result.perLaw.isNotEmpty())
        assertFalse("perLaw should not contain 'Otros' when all tests map to known laws, but got: ${result.perLaw.keys}",
            result.perLaw.containsKey("Otros"))
    }

    @org.junit.Test
    fun loadExam_perLawContainsExpectedLawNames() {
        val testData = makeRealisticTestData()
        val statsRepo = FakeStatsRepository()
        val engine = ExamEngine.createForTestData(statsRepo, testData)

        engine.loadExam(20)
        val result = engine.grade()

        val expectedLaws = setOf("LOPJ", "LEC", "LECrim", "Constitución/UE/Org", "Contencioso", "Social", "Registro Civil", "Concursal")
        val actualLaws = result.perLaw.keys
        assertTrue("perLaw should contain at least some expected law names, but got: $actualLaws",
            actualLaws.any { it in expectedLaws })
    }

    @org.junit.Test
    fun loadExam_perLawTotalsMatchQuestionCount() {
        val testData = makeRealisticTestData()
        val statsRepo = FakeStatsRepository()
        val engine = ExamEngine.createForTestData(statsRepo, testData)

        engine.loadExam(15)
        val result = engine.grade()

        assertEquals(15, result.perLaw.values.sumOf { it.total })
    }

    @org.junit.Test
    fun loadExam_testLawMapUsesQuestionTestIdNotTestId() {
        val testData = makeRealisticTestData()
        val statsRepo = FakeStatsRepository()
        val engine = ExamEngine.createForTestData(statsRepo, testData)

        engine.loadExam(10)
        val result = engine.grade()

        // The key bug was that testLawMap was keyed by test.id but queried by q.test_id.
        // If the mapping is correct, perLaw should NOT be all "Otros".
        val otrosCount = result.perLaw["Otros"]?.total ?: 0
        assertTrue("Most questions should map to known laws, not 'Otros'. Otros count: $otrosCount out of ${result.total}",
            otrosCount < result.total)
    }

    @org.junit.Test
    fun loadExam_unansweredQuestionsStillMappedToCorrectLaw() {
        val testData = makeRealisticTestData()
        val statsRepo = FakeStatsRepository()
        val engine = ExamEngine.createForTestData(statsRepo, testData)

        engine.loadExam(10)
        // Don't answer any questions
        val result = engine.grade()

        assertEquals(10, result.unanswered)
        assertFalse("Unanswered questions should still be mapped to laws, not all 'Otros'",
            result.perLaw.keys == setOf("Otros"))
    }

    @org.junit.Test
    fun loadExam_secondExamDoesNotReuseStaleLawMap() {
        val testData = makeRealisticTestData()
        val statsRepo = FakeStatsRepository()
        val engine = ExamEngine.createForTestData(statsRepo, testData)

        // First exam
        engine.loadExam(10)
        val result1 = engine.grade()
        assertTrue(result1.perLaw.isNotEmpty())

        // Second exam with different data
        val testData2 = listOf(makeTestData("test_lec2", "Juicio Verbal", questionCount = 10))
        val engine2 = ExamEngine.createForTestData(statsRepo, testData2)
        engine2.loadExam(5)
        val result2 = engine2.grade()

        assertNotNull(result2)
        assertTrue("Second exam should have perLaw populated", result2.perLaw.isNotEmpty())
        assertFalse("Second exam should not have stale 'Otros' from first exam",
            result2.perLaw.keys == setOf("Otros"))
    }
}
