package com.opoleyes.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import com.opoleyes.data.local.DataProvider
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.TestStrings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ExamResultScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setupExamResultScreen() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        DataProvider.loadData(ctx)
        val vm = GameViewModel(ctx)

        vm.examEngine.loadExam(10)
        vm.finishExam()

        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            ExamResultScreen(rememberNavController(), vm)
        }
        composeRule.waitUntil(timeoutMillis = 10000) {
            try { composeRule.onNodeWithText(TestStrings.resultados).assertIsDisplayed(); true }
            catch (e: Exception) { false }
        }
    }

    @Test
    fun examResultScreen_displaysTitle() {
        setupExamResultScreen()
        composeRule.onNodeWithText(TestStrings.resultados).assertIsDisplayed()
    }

    @Test
    fun examResultScreen_displaysRetryButton() {
        setupExamResultScreen()
        composeRule.onNodeWithText(TestStrings.retryLabel).performScrollTo()
        composeRule.onNodeWithText(TestStrings.retryLabel).assertIsDisplayed()
    }

    @Test
    fun examResultScreen_displaysMenuButton() {
        setupExamResultScreen()
        composeRule.onNodeWithText(TestStrings.menu).performScrollTo()
        composeRule.onNodeWithText(TestStrings.menu).assertIsDisplayed()
    }

    @Test
    fun examResultScreen_displaysReviewButton() {
        setupExamResultScreen()
        composeRule.onNodeWithText(TestStrings.reviewAnswers).assertIsDisplayed()
    }

    @Test
    fun examResultScreen_clickReview_showsReviewContent() {
        setupExamResultScreen()
        composeRule.onNodeWithText(TestStrings.reviewAnswers).performClick()
        composeRule.waitForIdle()
        // Review content should show question review cards
        composeRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeRule.onNodeWithText(TestStrings.hideReview).assertIsDisplayed(); true
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun examResultScreen_displaysScore() {
        setupExamResultScreen()
        // Should show "/ 10" text
        composeRule.onNodeWithText("/ 10").assertIsDisplayed()
    }

    @Test
    fun examResultScreen_displaysCorrectLabel() {
        setupExamResultScreen()
        composeRule.onNodeWithText("Aciertos").assertIsDisplayed()
    }

    @Test
    fun examResultScreen_displaysWrongLabel() {
        setupExamResultScreen()
        composeRule.onNodeWithText("Fallos").assertIsDisplayed()
    }

    @Test
    fun examResultScreen_displaysPerLawSection() {
        setupExamResultScreen()
        composeRule.onNodeWithText("Desglose por ley").performScrollTo()
        composeRule.onNodeWithText("Desglose por ley").assertIsDisplayed()
    }

    @Test
    fun examResultScreen_displaysXpEarned() {
        setupExamResultScreen()
        // XP earned text contains "XP ganados"
        composeRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeRule.onNodeWithText("XP ganados", substring = true).assertIsDisplayed(); true
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun examResultScreen_simulacroResult_displaysContent() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        DataProvider.loadData(ctx)
        val vm = GameViewModel(ctx)
        vm.loadSimulacroSync()
        // Answer a few questions
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam() // dispatches to finishSimulacro since isSimulacroMode

        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            ExamResultScreen(rememberNavController(), vm)
        }
        // Should show simulacro result content
        composeRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeRule.onNodeWithText("Aciertos").assertIsDisplayed(); true
            } catch (e: Throwable) { false }
        }
    }
}
