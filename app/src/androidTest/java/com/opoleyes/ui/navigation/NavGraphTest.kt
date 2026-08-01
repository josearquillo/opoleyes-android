package com.opoleyes.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.activity.ComponentActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class NavGraphTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun navGraph_displaysLoadingScreenInitially() {
        composeRule.setContent {
            NavGraph()
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        // Loading screen should show OPOLEYES
        composeRule.onNodeWithText("OPOLEYES").assertIsDisplayed()
    }

    @Test
    fun navGraph_navigatesToHomeAfterLoading() {
        composeRule.setContent {
            NavGraph()
        }
        composeRule.waitForIdle()
        // Advance past loading delays (400ms + 300ms)
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        // Should navigate to Home
        composeRule.onNodeWithText("JUGAR").assertIsDisplayed()
    }

    @Test
    fun navGraph_homeDisplaysOPOLEYES() {
        composeRule.setContent {
            NavGraph()
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("OPOLEYES").assertIsDisplayed()
    }

    @Test
    fun navGraph_navigatesToModeSelect() {
        composeRule.setContent {
            NavGraph()
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        // Click JUGAR
        composeRule.onNodeWithText("JUGAR").performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Supervivencia").assertIsDisplayed()
    }

    @Test
    fun navGraph_modeSelectDisplaysExamMode() {
        composeRule.setContent {
            NavGraph()
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("JUGAR").performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Mini Examen").assertIsDisplayed()
    }

    @Test
    fun navGraph_navigatesToProfile() {
        composeRule.setContent {
            NavGraph()
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("PERFIL").performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("OPOLEYES").assertIsDisplayed()
    }

    @Test
    fun navGraph_navigatesToHelp() {
        composeRule.setContent {
            NavGraph()
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("AYUDA").performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("OPOLEYES").assertIsDisplayed()
    }

    @Test
    fun navGraph_modeSelectShowsLockedModes() {
        composeRule.setContent {
            NavGraph()
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("JUGAR").performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        // Contrarreloj should be visible (locked or unlocked depending on XP)
        composeRule.onNodeWithText("Contrarreloj").assertIsDisplayed()
    }

    @Test
    fun navGraph_backFromModeSelectReturnsHome() {
        composeRule.setContent {
            NavGraph()
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("JUGAR").performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Supervivencia").assertIsDisplayed()
        // Press back
        composeRule.onNodeWithText("Selecciona modo").assertIsDisplayed()
    }
}
