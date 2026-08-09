package com.opoleyes

import com.opoleyes.data.model.Answer
import com.opoleyes.data.model.Question
import com.opoleyes.data.model.QuestionEntry
import com.opoleyes.data.model.TestData

/**
 * Builds test fixtures that mirror the exact structure of data.json:
 * - test.id follows the pattern "JUSTICIA__Examenes por temas__<Name>"
 * - question.test_id follows the pattern "JUSTICIA__Examenes por temas__Tema_NXX"
 * - test.id and question.test_id are DIFFERENT (this was the root cause of the "Otros" bug)
 * - test.name values are real names that mapTestToLaw recognizes
 *
 * This ensures tests reproduce real conditions, not simplified fake data.
 */
object RealisticFixtures {

    // Real test names from data.json and their expected law mapping
    data class TestSpec(
        val testId: String,
        val testName: String,
        val questionTestIds: List<String>,
        val expectedLaw: String,
        val questionCount: Int = 15
    )

    val REAL_TESTS = listOf(
        TestSpec(
            testId = "JUSTICIA__Examenes por temas__Constitución Española",
            testName = "Constitución Española",
            questionTestIds = listOf("JUSTICIA__Examenes por temas__Tema_N01"),
            expectedLaw = "Constitución/UE/Org"
        ),
        TestSpec(
            testId = "JUSTICIA__Examenes por temas__Poder Judicial y CGPJ",
            testName = "Poder Judicial y CGPJ",
            questionTestIds = listOf("JUSTICIA__Examenes por temas__Tema_N14"),
            expectedLaw = "LOPJ"
        ),
        TestSpec(
            testId = "JUSTICIA__Examenes por temas__Jueces, Magistrados y Ministerio Fiscal (LOPJ I)",
            testName = "Jueces, Magistrados y Ministerio Fiscal (LOPJ I)",
            questionTestIds = listOf("JUSTICIA__Examenes por temas__Tema_N15"),
            expectedLaw = "LOPJ"
        ),
        TestSpec(
            testId = "JUSTICIA__Examenes por temas__Juicio Ordinario",
            testName = "Juicio Ordinario",
            questionTestIds = listOf("JUSTICIA__Examenes por temas__Tema_N46"),
            expectedLaw = "LEC"
        ),
        TestSpec(
            testId = "JUSTICIA__Examenes por temas__Juicio Verbal",
            testName = "Juicio Verbal",
            questionTestIds = listOf("JUSTICIA__Examenes por temas__Tema_N49"),
            expectedLaw = "LEC"
        ),
        TestSpec(
            testId = "JUSTICIA__Examenes por temas__Procedimiento Abreviado",
            testName = "Procedimiento Abreviado",
            questionTestIds = listOf("JUSTICIA__Examenes por temas__Tema_N60"),
            expectedLaw = "LECrim"
        ),
        TestSpec(
            testId = "JUSTICIA__Examenes por temas__Procesos Contencioso-Administrativos",
            testName = "Procesos Contencioso-Administrativos",
            questionTestIds = listOf("JUSTICIA__Examenes por temas__Tema_N70"),
            expectedLaw = "Contencioso"
        ),
        TestSpec(
            testId = "JUSTICIA__Examenes por temas__Proceso Social",
            testName = "Proceso Social",
            questionTestIds = listOf("JUSTICIA__Examenes por temas__Tema_N75"),
            expectedLaw = "Social"
        ),
        TestSpec(
            testId = "JUSTICIA__Examenes por temas__Registro Civil",
            testName = "Registro Civil",
            questionTestIds = listOf("JUSTICIA__Examenes por temas__Tema_N80"),
            expectedLaw = "Registro Civil"
        ),
        TestSpec(
            testId = "JUSTICIA__Examenes por temas__Concursal",
            testName = "Concursal",
            questionTestIds = listOf("JUSTICIA__Examenes por temas__Tema_N85"),
            expectedLaw = "Concursal"
        ),
        TestSpec(
            testId = "JUSTICIA__Examenes por temas__Unión Europea",
            testName = "Unión Europea",
            questionTestIds = listOf("JUSTICIA__Examenes por temas__Tema_N12"),
            expectedLaw = "Constitución/UE/Org"
        ),
        TestSpec(
            testId = "JUSTICIA__Examenes por temas__Organización de Tribunales (LOPJ II)",
            testName = "Organización de Tribunales (LOPJ II)",
            questionTestIds = listOf("JUSTICIA__Examenes por temas__Tema_N17"),
            expectedLaw = "LOPJ"
        )
    )

    fun buildTestDataList(specs: List<TestSpec> = REAL_TESTS): List<TestData> {
        return specs.map { spec ->
            val questions = (1..spec.questionCount).map { i ->
                Question(
                    id = i,
                    test_id = spec.questionTestIds[0],
                    orig_id = i,
                    enunciado = "Pregunta $i de ${spec.testName}",
                    opciones = mapOf(
                        "A" to "Respuesta A",
                        "B" to "Respuesta B",
                        "C" to "Respuesta C",
                        "D" to "Respuesta D"
                    ),
                    difficulty = 3
                )
            }
            val answers = questions.map { Answer(id = it.id, correct = "A") }
            TestData(
                test = com.opoleyes.data.model.Test(
                    id = spec.testId,
                    name = spec.testName,
                    tema = 1
                ),
                questions = questions,
                answers = answers
            )
        }
    }

    /**
     * Builds QuestionEntry list for GameEngine tests with realistic test_id values.
     */
    fun buildRealisticPool(specs: List<TestSpec> = REAL_TESTS, perTest: Int = 10): List<QuestionEntry> {
        return specs.flatMap { spec ->
            (1..perTest).map { i ->
                QuestionEntry(
                    enunciado = "Pregunta $i de ${spec.testName}",
                    opciones = mapOf(
                        "A" to "Respuesta A",
                        "B" to "Respuesta B",
                        "C" to "Respuesta C",
                        "D" to "Respuesta D"
                    ),
                    correct = "A",
                    weight = 50,
                    testId = spec.questionTestIds[0],
                    origId = i.toString()
                )
            }
        }
    }
}
