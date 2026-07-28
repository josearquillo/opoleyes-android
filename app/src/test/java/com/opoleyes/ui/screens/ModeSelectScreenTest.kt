package com.opoleyes.ui.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
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
class ModeSelectScreenTest {

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
    fun fun_modeSelectScreen_showsTitle() {
        composeRule.setContent {
            ModeSelectScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Selecciona modo").assertIsDisplayed()
    }

    @Test
    fun fun_modeSelectScreen_showsSurvivalMode() {
        composeRule.setContent {
            ModeSelectScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Supervivencia").assertIsDisplayed()
    }

    @Test
    fun fun_modeSelectScreen_showsExamMode() {
        composeRule.setContent {
            ModeSelectScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Modo Examen").assertIsDisplayed()
    }

    @Test
    fun fun_modeSelectScreen_showsQuickMode() {
        composeRule.setContent {
            ModeSelectScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Repaso Express").assertIsDisplayed()
    }

    @Test
    fun fun_modeSelectScreen_showsChallengeMode() {
        composeRule.setContent {
            ModeSelectScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Modo Reto").fetchSemanticsNodes().also { org.junit.Assert.assertTrue(it.isNotEmpty()) }
    }
}
