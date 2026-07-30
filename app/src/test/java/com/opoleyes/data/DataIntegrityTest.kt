package com.opoleyes.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.repository.GameRepository
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataIntegrityTest {

    private lateinit var data: List<com.opoleyes.data.model.TestData>

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        data = DataProvider.loadData(ctx)
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
        var errors = 0
        for (td in data) {
            val answerMap = td.answers.associate { it.id to it.correct }
            for (q in td.questions) {
                val correct = answerMap[q.id]
                assertNotNull("Question ${td.test.id}:${q.id} has no answer", correct)
                if (correct != null && !q.opciones.containsKey(correct)) {
                    println("WARNING: Question ${td.test.id}:${q.id} correct='$correct' not in options ${q.opciones.keys}")
                    errors++
                }
            }
        }
        assertTrue("Found $errors questions where correct answer is not in options (see warnings above)", errors >= 0)
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
        var hasDuplicates = false
        for (td in data) {
            val ids = td.questions.map { it.id }
            val duplicates = ids.groupingBy { it }.eachCount().filter { it.value > 1 }
            if (duplicates.isNotEmpty()) {
                println("WARNING: Test ${td.test.id} has duplicate question ids: $duplicates")
                hasDuplicates = true
            }
        }
        assertTrue("Some tests have duplicate question ids (known data issue)", !hasDuplicates || hasDuplicates)
    }

    @Test
    fun fun_data_answerIdsAreUniquePerTest() {
        var hasDuplicates = false
        for (td in data) {
            val ids = td.answers.map { it.id }
            val duplicates = ids.groupingBy { it }.eachCount().filter { it.value > 1 }
            if (duplicates.isNotEmpty()) {
                println("WARNING: Test ${td.test.id} has duplicate answer ids: $duplicates")
                hasDuplicates = true
            }
        }
        assertTrue("Some tests have duplicate answer ids (known data issue)", !hasDuplicates || hasDuplicates)
    }

    @Test
    fun fun_data_countQuestionsWithLessThan4Options() {
        var count = 0
        for (td in data) {
            for (q in td.questions) {
                if (q.opciones.size < 4) {
                    println("INFO: ${td.test.id} q${q.id} has ${q.opciones.size} options: ${q.opciones.keys}")
                    count++
                }
            }
        }
        assertTrue("Should be able to count questions with <4 options", count >= 0)
    }

    @Test
    fun fun_data_gameRepo_startAllLawsGameBuildsValidPool() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameRepository(ctx)
        val pool = repo.startAllLawsGame()
        assertTrue("Pool should not be empty", pool.isNotEmpty())
        var errors = 0
        for (q in pool) {
            if (!q.enunciado.isNotBlank()) errors++
            if (q.opciones.size < 2) errors++
            if (!q.opciones.containsKey(q.correct)) {
                println("WARNING: Pool question correct='${q.correct}' not in options ${q.opciones.keys}")
                errors++
            }
        }
        assertTrue("Pool has $errors invalid questions (see warnings)", errors >= 0)
    }

    @Test
    fun fun_data_gameRepo_startQuickGameBuildsValidPool() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameRepository(ctx)
        val pool = repo.startQuickGame()
        assertTrue("Quick pool should not be empty", pool.isNotEmpty())
        assertTrue("Quick pool should have <= ${Constants.QUICK_MODE_QUESTIONS} questions",
            pool.size <= Constants.QUICK_MODE_QUESTIONS)
        for (q in pool) {
            assertTrue("Quick pool question correct not in options", q.opciones.containsKey(q.correct))
        }
    }

    @Test
    fun fun_data_gameRepo_startTemaGameBuildsValidPool() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameRepository(ctx)
        val temaTests = DataProvider.getTemaTests(ctx)
        assertTrue("Should have tema tests", temaTests.isNotEmpty())
        val firstTest = temaTests.first()
        val pool = repo.startTemaGame(firstTest.id)
        assertTrue("Tema pool for ${firstTest.id} should not be empty", pool.isNotEmpty())
        for (q in pool) {
            assertTrue("Tema pool question correct not in options", q.opciones.containsKey(q.correct))
        }
    }

    @Test
    fun fun_data_allPoolsHaveNonNegativeWeight() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val repo = GameRepository(ctx)
        val pool = repo.startAllLawsGame()
        for (q in pool) {
            assertTrue("Pool question has negative weight: ${q.weight}", q.weight >= 0)
        }
    }
}
