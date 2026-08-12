package com.opoleyes.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithText
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
}
