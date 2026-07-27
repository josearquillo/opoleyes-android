package com.opoleyes.ui.navigation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
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
class TrainingViewModelTest {

    private lateinit var vm: TrainingViewModel
    private lateinit var prefs: PreferencesManager
    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        prefs = PreferencesManager(app)
        prefs.resetAll()
        vm = TrainingViewModel(app)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun fun_startTraining_validTestReturnsTrue() {
        val temaTests = com.opoleyes.data.local.DataProvider.getTemaTests(app)
        if (temaTests.isNotEmpty()) {
            val ok = vm.startTraining(temaTests[0].id)
            assertTrue(ok)
            assertNotNull(vm.currentTestData)
            assertEquals(0, vm.currentQuestionIndex)
        }
    }

    @Test
    fun fun_startTraining_nonexistentReturnsFalse() {
        val ok = vm.startTraining("nonexistent")
        assertFalse(ok)
        assertTrue(vm.currentTestData?.questions.isNullOrEmpty())
    }

    @Test
    fun fun_startTrainingCustom_returnsTrue() {
        val ok = vm.startTrainingCustom("", 10)
        assertTrue(ok)
        assertNotNull(vm.currentTestData)
    }

    @Test
    fun fun_answerQuestion_storesAnswer() {
        val temaTests = com.opoleyes.data.local.DataProvider.getTemaTests(app)
        if (temaTests.isNotEmpty()) {
            vm.startTraining(temaTests[0].id)
            val q = vm.currentTestData!!.questions[0]
            vm.answerQuestion(q.id, "A")
            assertEquals("A", vm.userAnswers[q.id])
        }
    }

    @Test
    fun fun_toggleFlag_addsAndRemoves() {
        vm.flaggedQuestions.clear()
        vm.toggleFlag(1)
        assertTrue(vm.flaggedQuestions.contains(1))
        vm.toggleFlag(1)
        assertFalse(vm.flaggedQuestions.contains(1))
    }

    @Test
    fun fun_nextQuestion_incrementsIndex() {
        val temaTests = com.opoleyes.data.local.DataProvider.getTemaTests(app)
        if (temaTests.isNotEmpty()) {
            vm.startTraining(temaTests[0].id)
            val initial = vm.currentQuestionIndex
            if (vm.currentTestData!!.questions.size > 1) {
                vm.nextQuestion()
                assertEquals(initial + 1, vm.currentQuestionIndex)
            }
        }
    }

    @Test
    fun fun_prevQuestion_decrementsIndex() {
        vm.currentQuestionIndex = 5
        vm.prevQuestion()
        assertEquals(4, vm.currentQuestionIndex)
    }

    @Test
    fun fun_prevQuestion_atZeroDoesNothing() {
        vm.currentQuestionIndex = 0
        vm.prevQuestion()
        assertEquals(0, vm.currentQuestionIndex)
    }

    @Test
    fun fun_goToQuestion_setsIndex() {
        vm.currentQuestionIndex = 0
        vm.goToQuestion(7)
        assertEquals(7, vm.currentQuestionIndex)
    }

    @Test
    fun fun_buildFlaggedList() {
        vm.flaggedQuestions.clear()
        vm.flaggedQuestions.add(5)
        vm.flaggedQuestions.add(10)
        vm.currentTestData = com.opoleyes.data.model.TestData(
            questions = listOf(
                com.opoleyes.data.model.Question(id = 1),
                com.opoleyes.data.model.Question(id = 5),
                com.opoleyes.data.model.Question(id = 10)
            )
        )
        vm.buildFlaggedList()
        assertEquals(2, vm.flaggedList.size)
        assertEquals(1, vm.flaggedList[0])
        assertEquals(2, vm.flaggedList[1])
    }

    @Test
    fun fun_buildWrongList() {
        vm.currentTestData = com.opoleyes.data.model.TestData(
            questions = listOf(
                com.opoleyes.data.model.Question(id = 1, test_id = "t1", orig_id = 1),
                com.opoleyes.data.model.Question(id = 2, test_id = "t1", orig_id = 2)
            ),
            answers = listOf(
                com.opoleyes.data.model.Answer(id = 1, correct = "A"),
                com.opoleyes.data.model.Answer(id = 2, correct = "B")
            )
        )
        vm.userAnswers[1] = "A"
        vm.userAnswers[2] = "A"
        vm.buildWrongList()
        assertEquals(1, vm.reviewWrongList.size)
        assertEquals(1, vm.reviewWrongList[0])
    }

    @Test
    fun fun_submitResults_correct() {
        vm.currentTestData = com.opoleyes.data.model.TestData(
            questions = listOf(
                com.opoleyes.data.model.Question(id = 1, test_id = "t1", orig_id = 1),
                com.opoleyes.data.model.Question(id = 2, test_id = "t1", orig_id = 2)
            ),
            answers = listOf(
                com.opoleyes.data.model.Answer(id = 1, correct = "A"),
                com.opoleyes.data.model.Answer(id = 2, correct = "B")
            )
        )
        vm.userAnswers[1] = "A"
        vm.userAnswers[2] = "B"
        val result = vm.submitResults()
        assertEquals(2, result.correct)
        assertEquals(0, result.wrong)
        assertEquals(0, result.unanswered)
    }

    @Test
    fun fun_submitResults_wrong() {
        vm.currentTestData = com.opoleyes.data.model.TestData(
            questions = listOf(
                com.opoleyes.data.model.Question(id = 1, test_id = "t1", orig_id = 1)
            ),
            answers = listOf(
                com.opoleyes.data.model.Answer(id = 1, correct = "A")
            )
        )
        vm.userAnswers[1] = "B"
        val result = vm.submitResults()
        assertEquals(0, result.correct)
        assertEquals(1, result.wrong)
        assertEquals(0, result.unanswered)
    }

    @Test
    fun fun_submitResults_unanswered() {
        vm.currentTestData = com.opoleyes.data.model.TestData(
            questions = listOf(
                com.opoleyes.data.model.Question(id = 1, test_id = "t1", orig_id = 1)
            ),
            answers = listOf(
                com.opoleyes.data.model.Answer(id = 1, correct = "A")
            )
        )
        val result = vm.submitResults()
        assertEquals(0, result.correct)
        assertEquals(0, result.wrong)
        assertEquals(1, result.unanswered)
    }

    @Test
    fun fun_submitResults_nullDataReturnsZeros() {
        vm.currentTestData = null
        val result = vm.submitResults()
        assertEquals(0, result.correct)
        assertEquals(0, result.wrong)
        assertEquals(0, result.unanswered)
    }

    @Test
    fun fun_getScorePercent() {
        vm.currentTestData = com.opoleyes.data.model.TestData(
            questions = listOf(
                com.opoleyes.data.model.Question(id = 1),
                com.opoleyes.data.model.Question(id = 2)
            ),
            answers = listOf(
                com.opoleyes.data.model.Answer(id = 1, correct = "A"),
                com.opoleyes.data.model.Answer(id = 2, correct = "B")
            )
        )
        vm.userAnswers[1] = "A"
        vm.userAnswers[2] = "A"
        assertEquals(50, vm.getScorePercent())
    }

    @Test
    fun fun_getScorePercent_noData() {
        vm.currentTestData = null
        assertEquals(0, vm.getScorePercent())
    }

    @Test
    fun fun_getScorePercent_emptyQuestions() {
        vm.currentTestData = com.opoleyes.data.model.TestData()
        assertEquals(0, vm.getScorePercent())
    }

    @Test
    fun fun_submitResults_addsXP() {
        vm.currentTestData = com.opoleyes.data.model.TestData(
            questions = listOf(
                com.opoleyes.data.model.Question(id = 1, test_id = "t1", orig_id = 1)
            ),
            answers = listOf(
                com.opoleyes.data.model.Answer(id = 1, correct = "A")
            )
        )
        vm.userAnswers[1] = "A"
        val xpBefore = vm.getProgressRepo().getXP()
        vm.submitResults()
        assertTrue(vm.getProgressRepo().getXP() > xpBefore)
    }

    @Test
    fun fun_submitResults_incrementsTrainings() {
        vm.currentTestData = com.opoleyes.data.model.TestData(
            questions = listOf(
                com.opoleyes.data.model.Question(id = 1, test_id = "t1", orig_id = 1)
            ),
            answers = listOf(
                com.opoleyes.data.model.Answer(id = 1, correct = "A")
            )
        )
        vm.userAnswers[1] = "A"
        val before = vm.getProgressRepo().getTrainingsDone()
        vm.submitResults()
        assertEquals(before + 1, vm.getProgressRepo().getTrainingsDone())
    }

    @Test
    fun fun_uiState_initialState() {
        val state = vm.uiState.value
        assertEquals(0, state.currentIndex)
        assertEquals(0, state.totalQuestions)
        assertNull(state.selectedOption)
        assertFalse(state.isFlagged)
    }

    @Test
    fun fun_startTrainingCustom_withCategory() {
        val temaTests = com.opoleyes.data.local.DataProvider.getTemaTests(app)
        if (temaTests.isNotEmpty()) {
            val ok = vm.startTrainingCustom(temaTests[0].category, 5)
            if (ok) {
                assertTrue(vm.currentTestData!!.questions.size <= 5)
            }
        }
    }
}
