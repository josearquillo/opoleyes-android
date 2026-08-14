package com.opoleyes.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class GameOverScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setupGameOverScreen() {
        val vm = GameViewModel(ApplicationProvider.getApplicationContext<Application>())
        vm.startAllLawsGame()
        vm.engine.lives = 0
        vm.onGameOver()
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            GameOverScreen(rememberNavController(), vm)
        }
        composeRule.waitUntil(timeoutMillis = 10000) {
            try { composeRule.onNodeWithText(TestStrings.gameOver).assertIsDisplayed(); true }
            catch (e: Exception) { false }
        }
    }

    private fun setupGameOverScreenWithScore() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        DataProvider.loadData(ctx)
        val vm = GameViewModel(ctx)
        vm.startAllLawsGame()
        // Answer some questions correctly to earn XP and score
        for (i in 0 until 5) {
            val q = vm.engine.currentQ
            if (q != null) {
                vm.engine.answer(q.correct)
                vm.engine.nextQuestion()
            }
        }
        vm.engine.score = 500
        vm.engine.lives = 0
        vm.onGameOver()
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            GameOverScreen(rememberNavController(), vm)
        }
        composeRule.waitUntil(timeoutMillis = 10000) {
            try { composeRule.onNodeWithText(TestStrings.gameOver).assertIsDisplayed(); true }
            catch (e: Exception) { false }
        }
    }

    @Test
    fun gameOverScreen_displaysTitle() {
        setupGameOverScreen()
        composeRule.onNodeWithText(TestStrings.gameOver).assertIsDisplayed()
    }

    @Test
    fun gameOverScreen_displaysPointsLabel() {
        setupGameOverScreen()
        composeRule.onNodeWithText(TestStrings.points).assertIsDisplayed()
    }

    @Test
    fun gameOverScreen_displaysAccuracy() {
        setupGameOverScreen()
        composeRule.onNodeWithText(TestStrings.accuracyLabel).assertIsDisplayed()
    }

    @Test
    fun gameOverScreen_displaysMaxCombo() {
        setupGameOverScreen()
        composeRule.onNodeWithText(TestStrings.maxComboLabel).assertIsDisplayed()
    }

    @Test
    fun gameOverScreen_displaysQuestionsLabel() {
        setupGameOverScreen()
        composeRule.onNodeWithText(TestStrings.questionsLabel).assertIsDisplayed()
    }

    @Test
    fun gameOverScreen_withScore_displaysPlayAgainButton() {
        setupGameOverScreenWithScore()
        // Wait for overlays to dismiss, then look for play again button
        composeRule.waitUntil(timeoutMillis = 15000) {
            try {
                composeRule.onNodeWithText(TestStrings.playAgain).assertIsDisplayed(); true
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun gameOverScreen_withScore_displaysMenuButton() {
        setupGameOverScreenWithScore()
        composeRule.waitUntil(timeoutMillis = 15000) {
            try {
                composeRule.onNodeWithText(TestStrings.menu).assertIsDisplayed(); true
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun gameOverScreen_displaysContinueLabel() {
        setupGameOverScreen()
        // XP summary overlay should show "Continuar" or "Saltar" button
        composeRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeRule.onNodeWithText(TestStrings.continueLabel).assertIsDisplayed(); true
            } catch (e: Throwable) {
                try {
                    composeRule.onNodeWithText("Saltar").assertIsDisplayed(); true
                } catch (e2: Throwable) { false }
            }
        }
    }
}
