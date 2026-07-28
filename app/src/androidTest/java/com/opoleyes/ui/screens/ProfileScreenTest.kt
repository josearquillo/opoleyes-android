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
class ProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun profileScreen_displaysTitle() {
        composeRule.setContent {
            ProfileScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Perfil").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysRecordsSection() {
        composeRule.setContent {
            ProfileScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Récords").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysSurvivalRecord() {
        composeRule.setContent {
            ProfileScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Supervivencia").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysPowerUpsSection() {
        composeRule.setContent {
            ProfileScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ayudas disponibles").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysAchievementsSection() {
        composeRule.setContent {
            ProfileScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Logros").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysStatsSection() {
        composeRule.setContent {
            ProfileScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Estadísticas").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysGamesPlayed() {
        composeRule.setContent {
            ProfileScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Partidas jugadas").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysResetButton() {
        composeRule.setContent {
            ProfileScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Reiniciar progreso").assertIsDisplayed()
    }
}
