package com.opoleyes.ui.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.navigation.compose.rememberNavController
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.ui.navigation.GameViewModel
import org.junit.After
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
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun examScreen_displaysQuestionNumber() {
        // Start an exam synchronously
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)

        composeRule.setContent {
            ExamScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()

        // Should display "Pregunta 1 de 10" (first question)
        composeRule.onNodeWithText("Pregunta 1 de 10").assertIsDisplayed()
    }

    @Test
    fun examScreen_showsNavigationButtons() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(0)

        composeRule.setContent {
            ExamScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()

        // Should display the exam mode title
        composeRule.onNodeWithText("Mini Examen").assertIsDisplayed()
    }
}
