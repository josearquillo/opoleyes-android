package com.opoleyes.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import com.opoleyes.ui.navigation.GameViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import android.app.Application

@RunWith(AndroidJUnit4::class)
class ExamResultScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setupVmWithExamResult(): GameViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = GameViewModel(app)
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)
        // Answer all correctly
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        return vm
    }

    @Test
    fun examResultScreen_displaysTitle() {
        val vm = setupVmWithExamResult()
        composeRule.setContent {
            ExamResultScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Resultado del Examen").assertIsDisplayed()
    }

    @Test
    fun examResultScreen_displaysScore() {
        val vm = setupVmWithExamResult()
        composeRule.setContent {
            ExamResultScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        // Score should be 10.0 for all correct
        composeRule.onNodeWithText("10,0").assertIsDisplayed()
    }

    @Test
    fun examResultScreen_displaysGrade() {
        val vm = setupVmWithExamResult()
        composeRule.setContent {
            ExamResultScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sobresaliente").assertIsDisplayed()
    }

    @Test
    fun examResultScreen_displaysLawBreakdown() {
        val vm = setupVmWithExamResult()
        composeRule.setContent {
            ExamResultScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Desglose por ley").assertIsDisplayed()
    }

    @Test
    fun examResultScreen_displaysXpGained() {
        val vm = setupVmWithExamResult()
        composeRule.setContent {
            ExamResultScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("+100 XP ganados").assertIsDisplayed()
    }

    @Test
    fun examResultScreen_displaysReviewButton() {
        val vm = setupVmWithExamResult()
        composeRule.setContent {
            ExamResultScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Revisar respuestas").assertIsDisplayed()
    }

    @Test
    fun examResultScreen_displaysHomeAndExamButtons() {
        val vm = setupVmWithExamResult()
        composeRule.setContent {
            ExamResultScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Inicio").assertIsDisplayed()
        composeRule.onNodeWithText("Otro examen").assertIsDisplayed()
    }

    @Test
    fun examResultScreen_displaysCorrectStats() {
        val vm = setupVmWithExamResult()
        composeRule.setContent {
            ExamResultScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Aciertos").assertIsDisplayed()
        composeRule.onNodeWithText("Fallos").assertIsDisplayed()
    }
}
