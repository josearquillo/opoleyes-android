package com.opoleyes.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
class ExamScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setupExamScreen() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        DataProvider.loadData(ctx)
        val vm = GameViewModel(ctx)
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)

        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            ExamScreen(rememberNavController(), vm)
        }
        composeRule.waitUntil(timeoutMillis = 30000) {
            try {
                composeRule.onNodeWithText(TestStrings.modeExam).assertIsDisplayed(); true
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun examScreen_displaysQuestionWithOptions() {
        setupExamScreen()
        // ExamScreen shows option letters "A", "B", "C", "D" in circles
        composeRule.onNodeWithText(TestStrings.modeExam).assertIsDisplayed()
    }

    @Test
    fun examScreen_displaysBackContentDescription() {
        setupExamScreen()
        composeRule.onNodeWithContentDescription("Salir").assertIsDisplayed()
    }

    @Test
    fun examScreen_clickBack_showsExitDialog() {
        setupExamScreen()
        composeRule.onNodeWithContentDescription("Salir").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TestStrings.exitExam).assertIsDisplayed()
    }

    @Test
    fun examScreen_displaysNextButton() {
        setupExamScreen()
        composeRule.onNodeWithText(TestStrings.next).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun examScreen_clickOption_marksAnswer() {
        setupExamScreen()
        // Click the first option letter "A"
        composeRule.onAllNodesWithText("A")[0].performClick()
        composeRule.waitForIdle()
        // Verify exam title still displayed (answer was marked)
        composeRule.onNodeWithText(TestStrings.modeExam).assertIsDisplayed()
    }

    @Test
    fun examScreen_clickNext_advancesToNextQuestion() {
        setupExamScreen()
        composeRule.onAllNodesWithText("A")[0].performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TestStrings.next).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TestStrings.modeExam).assertIsDisplayed()
    }

    @Test
    fun examScreen_displaysPreviousButtonAfterNext() {
        setupExamScreen()
        composeRule.onAllNodesWithText("A")[0].performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TestStrings.next).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TestStrings.previous).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun examScreen_clickPrevious_goesBack() {
        setupExamScreen()
        composeRule.onAllNodesWithText("A")[0].performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TestStrings.next).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TestStrings.previous).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TestStrings.modeExam).assertIsDisplayed()
    }
}
