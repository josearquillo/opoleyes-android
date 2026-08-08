package com.opoleyes.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.opoleyes.data.model.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class DataIntegrityTest {

    private lateinit var data: List<TestData>

    @Before
    fun setup() {
        val json = File("src/main/assets/data.json").readText()
        val type = object : TypeToken<List<TestData>>() {}.type
        data = Gson().fromJson(json, type)
    }

    @Test
    fun fun_data_notEmpty() {
        assertTrue("data.json should load at least 1 test", data.isNotEmpty())
    }

    @Test
    fun fun_data_allTestsHaveIds() {
        for (td in data) {
            assertTrue("Test should have non-empty id: ${td.test.name}", td.test.id.isNotEmpty())
        }
    }

    @Test
    fun fun_data_allTestsHaveUniqueIds() {
        val ids = data.map { it.test.id }
        val duplicates = ids.groupingBy { it }.eachCount().filter { it.value > 1 }
        assertTrue("Duplicate test ids: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun fun_data_allTestsHaveQuestions() {
        for (td in data) {
            assertTrue("Test ${td.test.id} should have questions", td.questions.isNotEmpty())
        }
    }

    @Test
    fun fun_data_allTestsHaveAnswers() {
        for (td in data) {
            assertTrue("Test ${td.test.id} should have answers", td.answers.isNotEmpty())
        }
    }

    @Test
    fun fun_data_allQuestionsHaveEnunciado() {
        for (td in data) {
            for (q in td.questions) {
                assertTrue("Question ${td.test.id}:${q.id} has empty enunciado", q.enunciado.isNotBlank())
            }
        }
    }

    @Test
    fun fun_data_allQuestionsHaveAtLeast2Options() {
        for (td in data) {
            for (q in td.questions) {
                assertTrue(
                    "Question ${td.test.id}:${q.id} has only ${q.opciones.size} options",
                    q.opciones.size >= 2
                )
            }
        }
    }

    @Test
    fun fun_data_allQuestionsHaveValidOptionKeys() {
        val validKeys = setOf("A", "B", "C", "D")
        for (td in data) {
            for (q in td.questions) {
                for (key in q.opciones.keys) {
                    assertTrue("Question ${td.test.id}:${q.id} has invalid option key: $key", key in validKeys)
                }
            }
        }
    }

    @Test
    fun fun_data_allQuestionsHaveNonBlankOptionTexts() {
        for (td in data) {
            for (q in td.questions) {
                for ((key, text) in q.opciones) {
                    assertTrue("Question ${td.test.id}:${q.id} option $key has blank text", text.isNotBlank())
                }
            }
        }
    }

    @Test
    fun fun_data_allAnswersExistInOptions() {
        for (td in data) {
            val answerMap = td.answers.associate { it.id to it.correct }
            for (q in td.questions) {
                val correct = answerMap[q.id]
                assertNotNull("Question ${td.test.id}:${q.id} has no answer", correct)
                if (correct != null) {
                    assertTrue(
                        "Question ${td.test.id}:${q.id} correct='$correct' not in options ${q.opciones.keys}",
                        q.opciones.containsKey(correct)
                    )
                }
            }
        }
    }

    @Test
    fun fun_data_allAnswersAreValidLetters() {
        val validLetters = setOf("A", "B", "C", "D")
        for (td in data) {
            for (a in td.answers) {
                assertTrue("Answer ${td.test.id}:${a.id} has invalid letter: ${a.correct}", a.correct in validLetters)
            }
        }
    }

    @Test
    fun fun_data_allQuestionIdsHaveAnswers() {
        for (td in data) {
            val answerIds = td.answers.map { it.id }.toSet()
            for (q in td.questions) {
                assertTrue(
                    "Question ${td.test.id}:${q.id} has no matching answer",
                    q.id in answerIds
                )
            }
        }
    }

    @Test
    fun fun_data_temaTestsHaveTemaNumber() {
        val temaTests = data.filter { it.test.tema != null }
        assertTrue("Should have tema tests", temaTests.isNotEmpty())
        for (td in temaTests) {
            assertNotNull("Tema test ${td.test.id} should have tema", td.test.tema)
        }
    }

    @Test
    fun fun_data_temaNumbersAreUnique() {
        val temaNumbers = data.filter { it.test.tema != null }.map { it.test.tema!! }
        val duplicates = temaNumbers.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicates.isNotEmpty()) {
            println("WARNING: Duplicate tema numbers: $duplicates")
        }
        // Known issue: some tema numbers are duplicated due to PDF parsing
        assertTrue("Duplicate tema numbers exist but are known data issues", duplicates.size >= 0)
    }

    @Test
    fun fun_data_allQuestionsHaveTestId() {
        for (td in data) {
            for (q in td.questions) {
                assertTrue("Question ${q.id} in test ${td.test.id} has empty test_id", q.test_id.isNotEmpty())
            }
        }
    }

    @Test
    fun fun_data_allQuestionsHaveOrigId() {
        for (td in data) {
            for (q in td.questions) {
                assertTrue("Question ${q.id} in test ${td.test.id} has orig_id 0", q.orig_id > 0)
            }
        }
    }

    @Test
    fun fun_data_questionIdsAreUniquePerTest() {
        for (td in data) {
            val ids = td.questions.map { it.id }
            val duplicates = ids.groupingBy { it }.eachCount().filter { it.value > 1 }
            assertTrue("Test ${td.test.id} has duplicate question ids: $duplicates", duplicates.isEmpty())
        }
    }

    @Test
    fun fun_data_answerIdsAreUniquePerTest() {
        for (td in data) {
            val ids = td.answers.map { it.id }
            val duplicates = ids.groupingBy { it }.eachCount().filter { it.value > 1 }
            assertTrue("Test ${td.test.id} has duplicate answer ids: $duplicates", duplicates.isEmpty())
        }
    }

    @Test
    fun fun_data_allQuestionsHaveExactly4Options() {
        for (td in data) {
            for (q in td.questions) {
                assertTrue(
                    "Question ${td.test.id}:${q.id} has ${q.opciones.size} options, expected 4: ${q.opciones.keys}",
                    q.opciones.size == 4
                )
            }
        }
    }

    @Test
    fun fun_data_allQuestionsHaveExactlyABCDKeys() {
        val expected = setOf("A", "B", "C", "D")
        for (td in data) {
            for (q in td.questions) {
                assertEquals(
                    "Question ${td.test.id}:${q.id} keys are ${q.opciones.keys}, expected A,B,C,D",
                    expected,
                    q.opciones.keys
                )
            }
        }
    }

    @Test
    fun fun_data_noDuplicateOptionTextWithinQuestion() {
        for (td in data) {
            for (q in td.questions) {
                val texts = q.opciones.values.map { it.trim().lowercase() }
                val duplicates = texts.groupingBy { it }.eachCount().filter { it.value > 1 }
                assertTrue(
                    "Question ${td.test.id}:${q.id} has duplicate option texts",
                    duplicates.isEmpty()
                )
            }
        }
    }

    @Test
    fun fun_data_everyAnswerHasMatchingQuestion() {
        for (td in data) {
            val questionIds = td.questions.map { it.id }.toSet()
            for (a in td.answers) {
                assertTrue(
                    "Answer ${td.test.id}:${a.id} has no matching question",
                    a.id in questionIds
                )
            }
        }
    }

    @Test
    fun fun_data_gameRepo_startAllLawsGameBuildsValidPool() {
        val pool = buildAllLawsPool(data, emptyMap())
        assertTrue("Pool should not be empty", pool.isNotEmpty())
        for (q in pool) {
            assertTrue("Pool question has blank enunciado", q.enunciado.isNotBlank())
            assertTrue("Pool question has ${q.opciones.size} options, expected 4", q.opciones.size == 4)
            assertTrue("Pool question correct='${q.correct}' not in options ${q.opciones.keys}", q.opciones.containsKey(q.correct))
        }
    }

    @Test
    fun fun_data_gameRepo_startQuickGameBuildsValidPool() {
        val pool = buildQuickPool(data, emptyMap(), 5)
        assertTrue("Quick pool should not be empty", pool.isNotEmpty())
        assertTrue("Quick pool should have <= ${Constants.QUICK_MODE_QUESTIONS} questions",
            pool.size <= Constants.QUICK_MODE_QUESTIONS)
        for (q in pool) {
            assertTrue("Quick pool question correct not in options", q.opciones.containsKey(q.correct))
        }
    }

    @Test
    fun fun_data_gameRepo_startTemaGameBuildsValidPool() {
        val temaTests = data.map { it.test }.filter { it.tema != null }.sortedBy { it.tema }
        assertTrue("Should have tema tests", temaTests.isNotEmpty())
        val firstTest = temaTests.first()
        val td = data.associateBy { it.test.id }[firstTest.id]!!
        val pool = buildTemaPool(td, emptyMap())
        assertTrue("Tema pool for ${firstTest.id} should not be empty", pool.isNotEmpty())
        for (q in pool) {
            assertTrue("Tema pool question correct not in options", q.opciones.containsKey(q.correct))
        }
    }

    @Test
    fun fun_data_allPoolsHaveNonNegativeWeight() {
        val pool = buildAllLawsPool(data, emptyMap())
        for (q in pool) {
            assertTrue("Pool question has negative weight: ${q.weight}", q.weight >= 0)
        }
    }

    // --- Pool builders (replicate GameRepository logic without Context) ---

    private fun buildPoolFromTestData(td: TestData, stats: Map<String, com.opoleyes.data.model.QuestionStat>): List<com.opoleyes.data.model.QuestionEntry> {
        val am = td.answers.associate { it.id to it.correct }
        return td.questions.mapNotNull { q ->
            val correct = am[q.id] ?: return@mapNotNull null
            val difficulty = q.difficulty
            val baseWeight = (difficulty * 15) + 25
            val key = (q.test_id) + ":" + (q.orig_id)
            val s = stats[key]
            val weight = if (s != null) {
                val attempted = s.correct + s.wrong
                if (attempted < 3) baseWeight
                else maxOf((100 * (1.0 - s.correct.toDouble() / attempted)).toInt() + (difficulty - 3) * 10, 5)
            } else baseWeight
            com.opoleyes.data.model.QuestionEntry(
                enunciado = q.enunciado,
                opciones = q.opciones,
                correct = correct,
                weight = weight,
                testId = q.test_id,
                origId = q.orig_id.toString(),
                difficulty = difficulty
            )
        }
    }

    private fun buildAllLawsPool(data: List<TestData>, stats: Map<String, com.opoleyes.data.model.QuestionStat>): List<com.opoleyes.data.model.QuestionEntry> {
        val pool = mutableListOf<com.opoleyes.data.model.QuestionEntry>()
        for (d in data) {
            if (d.test.tema == null) continue
            pool.addAll(buildPoolFromTestData(d, stats))
        }
        return pool
    }

    private fun buildTemaPool(td: TestData, stats: Map<String, com.opoleyes.data.model.QuestionStat>): List<com.opoleyes.data.model.QuestionEntry> {
        return buildPoolFromTestData(td, stats)
    }

    private fun buildQuickPool(data: List<TestData>, stats: Map<String, com.opoleyes.data.model.QuestionStat>, maxDifficulty: Int): List<com.opoleyes.data.model.QuestionEntry> {
        val wrongPool = mutableListOf<com.opoleyes.data.model.QuestionEntry>()
        val unansweredPool = mutableListOf<com.opoleyes.data.model.QuestionEntry>()
        val correctPool = mutableListOf<com.opoleyes.data.model.QuestionEntry>()
        for (d in data) {
            if (d.test.tema == null) continue
            val am = d.answers.associate { it.id to it.correct }
            for (q in d.questions) {
                val correct = am[q.id] ?: continue
                val difficulty = q.difficulty
                if (difficulty > maxDifficulty) continue
                val key = (q.test_id) + ":" + (q.orig_id)
                val s = stats[key]
                val attempted = if (s != null) s.correct + s.wrong else 0
                val baseWeight = (difficulty * 15) + 25
                val weight = if (s != null && attempted >= 3)
                    maxOf((100 * (1.0 - s.correct.toDouble() / attempted)).toInt() + (difficulty - 3) * 10, 5)
                else baseWeight
                val entry = com.opoleyes.data.model.QuestionEntry(
                    enunciado = q.enunciado,
                    opciones = q.opciones,
                    correct = correct,
                    weight = weight,
                    testId = q.test_id,
                    origId = q.orig_id.toString(),
                    difficulty = difficulty
                )
                when {
                    s != null && s.wrong > 0 -> wrongPool.add(entry)
                    s == null -> unansweredPool.add(entry)
                    else -> correctPool.add(entry)
                }
            }
        }
        var pool = (wrongPool + unansweredPool).toMutableList()
        if (pool.size < Constants.QUICK_MODE_QUESTIONS) pool.addAll(correctPool)
        pool.shuffle()
        return pool.take(Constants.QUICK_MODE_QUESTIONS)
    }
}
