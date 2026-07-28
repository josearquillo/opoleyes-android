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
class LoadingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingScreen_displaysAppName() {
        composeRule.setContent {
            LoadingScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("OPOLEYES").assertIsDisplayed()
    }

    @Test
    fun loadingScreen_displaysCargandoText() {
        composeRule.setContent {
            LoadingScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cargando...").assertIsDisplayed()
    }
}
