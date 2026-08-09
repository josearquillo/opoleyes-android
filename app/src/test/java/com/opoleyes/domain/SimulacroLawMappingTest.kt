package com.opoleyes.domain

import com.opoleyes.FakeStatsRepository
import com.opoleyes.data.model.Answer
import com.opoleyes.data.model.Question
import com.opoleyes.data.model.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulacroLawMappingTest {

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
        makeTestData("test_cont", "Contencioso-Administrativa: Demanda y Contestación"),
        makeTestData("test_social", "Social: Principios Laborales"),
        makeTestData("test_registro", "Registro Civil (Ley 2011) - Títulos"),
        makeTestData("test_concursal", "Concursal"),
        makeTestData("test_ue", "Unión Europea")
    )

    @Test
    fun simulacro_perLawDoesNotContainOnlyOtros() {
        val testData = makeRealisticTestData()
        val statsRepo = FakeStatsRepository()
        val engine = ExamEngine.createForTestData(statsRepo, testData)

        engine.loadSimulacro()
        val result = engine.gradeSimulacro()

        assertNotNull(result)
        assertTrue("perLaw should be populated", result.perLaw.isNotEmpty())
        assertFalse("perLaw should not contain only 'Otros': ${result.perLaw.keys}",
            result.perLaw.keys == setOf("Otros"))
    }

    @Test
    fun simulacro_perLawTotalsMatch100Questions() {
        val testData = makeRealisticTestData()
        val statsRepo = FakeStatsRepository()
        val engine = ExamEngine.createForTestData(statsRepo, testData)

        engine.loadSimulacro()
        val result = engine.gradeSimulacro()

        assertEquals(100, result.perLaw.values.sumOf { it.total })
    }

    @Test
    fun simulacro_allCorrect_perLawHasCorrectCounts() {
        val testData = makeRealisticTestData()
        val statsRepo = FakeStatsRepository()
        val engine = ExamEngine.createForTestData(statsRepo, testData)

        engine.loadSimulacro()
        val count = engine.getQuestionCount()
        // Answer all correctly
        for (i in 0 until count) {
            engine.navigateTo(i)
            val q = engine.getCurrentQuestion()!!
            engine.answer(q.question.correct)
        }
        val result = engine.gradeSimulacro()

        assertEquals(count, result.correct)
        assertEquals(0, result.wrong)
        assertEquals(0, result.unanswered)
        // Each law entry should have correct == total
        result.perLaw.forEach { (law, lr) ->
            assertEquals("Law $law: correct should equal total", lr.total, lr.correct)
        }
    }

    @Test
    fun simulacro_allWrong_perLawHasWrongCounts() {
        val testData = makeRealisticTestData()
        val statsRepo = FakeStatsRepository()
        val engine = ExamEngine.createForTestData(statsRepo, testData)

        engine.loadSimulacro()
        val count = engine.getQuestionCount()
        // Answer all wrong
        for (i in 0 until count) {
            engine.navigateTo(i)
            val q = engine.getCurrentQuestion()!!
            val wrong = q.question.opciones.keys.firstOrNull { it != q.question.correct } ?: "B"
            engine.answer(wrong)
        }
        val result = engine.gradeSimulacro()

        assertEquals(count, result.wrong)
        assertEquals(0, result.correct)
        result.perLaw.forEach { (law, lr) ->
            assertEquals("Law $law: wrong should equal total", lr.total, lr.wrong)
        }
    }

    @Test
    fun simulacro_allUnanswered_perLawStillMappedCorrectly() {
        val testData = makeRealisticTestData()
        val statsRepo = FakeStatsRepository()
        val engine = ExamEngine.createForTestData(statsRepo, testData)

        engine.loadSimulacro()
        // Don't answer any
        val result = engine.gradeSimulacro()

        assertEquals(100, result.unanswered)
        assertFalse("Unanswered simulacro should not all be 'Otros': ${result.perLaw.keys}",
            result.perLaw.keys == setOf("Otros"))
    }

    @Test
    fun simulacro_secondSimulacroDoesNotReuseStaleLawMap() {
        val testData = makeRealisticTestData()
        val statsRepo = FakeStatsRepository()
        val engine = ExamEngine.createForTestData(statsRepo, testData)

        // First simulacro
        engine.loadSimulacro()
        val result1 = engine.gradeSimulacro()
        assertTrue(result1.perLaw.isNotEmpty())

        // Second simulacro
        engine.loadSimulacro()
        val result2 = engine.gradeSimulacro()

        assertNotNull(result2)
        assertTrue("Second simulacro perLaw should be populated", result2.perLaw.isNotEmpty())
        assertFalse("Second simulacro should not have stale 'Otros' only",
            result2.perLaw.keys == setOf("Otros"))
    }
}
