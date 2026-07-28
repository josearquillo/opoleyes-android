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
class GameOverScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setupVm(): GameViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        return GameViewModel(app)
    }

    @Test
    fun gameOverScreen_displaysTitle() {
        val vm = setupVm()
        vm.startAllLawsGame()
        vm.engine.lives = 0
        vm.onGameOver()
        composeRule.setContent {
            GameOverScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Fin de partida").assertIsDisplayed()
    }

    @Test
    fun gameOverScreen_displaysPointsLabel() {
        val vm = setupVm()
        vm.startAllLawsGame()
        vm.engine.lives = 0
        vm.onGameOver()
        composeRule.setContent {
            GameOverScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("puntos").assertIsDisplayed()
    }

    @Test
    fun gameOverScreen_displaysPlayAndMenuButtons() {
        val vm = setupVm()
        vm.startAllLawsGame()
        vm.engine.lives = 0
        vm.onGameOver()
        composeRule.setContent {
            GameOverScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Jugar").assertIsDisplayed()
        composeRule.onNodeWithText("Menú").assertIsDisplayed()
    }

    @Test
    fun gameOverScreen_displaysAccuracyStat() {
        val vm = setupVm()
        vm.startAllLawsGame()
        vm.engine.lives = 0
        vm.onGameOver()
        composeRule.setContent {
            GameOverScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Precisión").assertIsDisplayed()
    }

    @Test
    fun gameOverScreen_displaysComboMaxStat() {
        val vm = setupVm()
        vm.startAllLawsGame()
        vm.engine.lives = 0
        vm.onGameOver()
        composeRule.setContent {
            GameOverScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Combo máx").assertIsDisplayed()
    }

    @Test
    fun gameOverScreen_displaysQuestionsStat() {
        val vm = setupVm()
        vm.startAllLawsGame()
        vm.engine.lives = 0
        vm.onGameOver()
        composeRule.setContent {
            GameOverScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Preguntas").assertIsDisplayed()
    }
}
