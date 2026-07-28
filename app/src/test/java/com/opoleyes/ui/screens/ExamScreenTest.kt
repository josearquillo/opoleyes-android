package com.opoleyes.ui.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExamScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var vm: GameViewModel
    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        prefs = PreferencesManager(app)
        prefs.resetAll()
        vm = GameViewModel(app)
        // Load exam and initialize StateFlows through ViewModel
        vm.examEngine.loadExam(10)
        // Manually trigger the state update since we're not using startExamAsync
        vm.examNavigate(0)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun fun_examScreen_showsQuestionText() {
        composeRule.setContent {
            ExamScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        val q = vm.examEngine.getCurrentQuestion()!!
        composeRule.onNodeWithText(q.question.enunciado).assertIsDisplayed()
    }

    @Test
    fun fun_examScreen_showsQuestionNumber() {
        composeRule.setContent {
            ExamScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Pregunta 1 de 10").assertIsDisplayed()
    }

    @Test
    fun fun_examScreen_showsAnsweredCount() {
        composeRule.setContent {
            ExamScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("0/10").assertIsDisplayed()
    }

    @Test
    fun fun_examScreen_clickingOptionUpdatesAnswer() {
        composeRule.setContent {
            ExamScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        val q = vm.examEngine.getCurrentQuestion()!!
        val optionText = q.question.opciones["A"]!!
        composeRule.onNodeWithText(optionText).performClick()
        assertEquals("A", vm.examCurrentQuestion.value?.userAnswer)
        assertEquals(1, vm.examAnswered.value)
    }

    @Test
    fun fun_examScreen_showsNextButton() {
        composeRule.setContent {
            ExamScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Siguiente").fetchSemanticsNodes().also { org.junit.Assert.assertTrue(it.isNotEmpty()) }
    }

    @Test
    fun fun_examScreen_showsFinishButtonOnLastQuestion() {
        vm.examNavigate(9)
        composeRule.setContent {
            ExamScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Finalizar").fetchSemanticsNodes().also { org.junit.Assert.assertTrue(it.isNotEmpty()) }
    }

    @Test
    fun fun_examScreen_showsPreviousButton() {
        vm.examNavigate(1)
        composeRule.setContent {
            ExamScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Anterior").fetchSemanticsNodes().also { org.junit.Assert.assertTrue(it.isNotEmpty()) }
    }

    @Test
    fun fun_examScreen_showsExitDialogTitle() {
        composeRule.setContent {
            ExamScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Modo Examen").assertIsDisplayed()
    }
}
