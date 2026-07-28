package com.opoleyes.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class HelpScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun helpScreen_displaysTitle() {
        composeRule.setContent {
            HelpScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ayuda").assertIsDisplayed()
    }

    @Test
    fun helpScreen_displaysSections() {
        composeRule.setContent {
            HelpScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Objetivo").assertIsDisplayed()
    }

    @Test
    fun helpScreen_displaysGameModesSection() {
        composeRule.setContent {
            HelpScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Modos de juego").assertIsDisplayed()
    }

    @Test
    fun helpScreen_displaysPowerUpsSection() {
        composeRule.setContent {
            HelpScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ayudas (Power-ups)").assertIsDisplayed()
    }

    @Test
    fun helpScreen_displaysComboSection() {
        composeRule.setContent {
            HelpScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Combo").assertIsDisplayed()
    }

    @Test
    fun helpScreen_displaysRanksSection() {
        composeRule.setContent {
            HelpScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Rangos").assertIsDisplayed()
    }

    @Test
    fun helpScreen_displaysChestsSection() {
        composeRule.setContent {
            HelpScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cofres").assertIsDisplayed()
    }

    @Test
    fun helpScreen_displaysMissionsSection() {
        composeRule.setContent {
            HelpScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Misiones diarias").assertIsDisplayed()
    }
}
