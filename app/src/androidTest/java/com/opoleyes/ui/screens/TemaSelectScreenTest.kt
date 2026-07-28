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
class TemaSelectScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun temaSelectScreen_displaysTitle() {
        composeRule.setContent {
            TemaSelectScreen(rememberNavController(), com.opoleyes.ui.navigation.GameViewModel(
                androidx.test.core.app.ApplicationProvider.getApplicationContext()
            ))
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Selecciona ley").assertIsDisplayed()
    }

    @Test
    fun temaSelectScreen_displaysAllLawsOption() {
        composeRule.setContent {
            TemaSelectScreen(rememberNavController(), com.opoleyes.ui.navigation.GameViewModel(
                androidx.test.core.app.ApplicationProvider.getApplicationContext()
            ))
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Todas las leyes").assertIsDisplayed()
    }

    @Test
    fun temaSelectScreen_displaysSearchBar() {
        composeRule.setContent {
            TemaSelectScreen(rememberNavController(), com.opoleyes.ui.navigation.GameViewModel(
                androidx.test.core.app.ApplicationProvider.getApplicationContext()
            ))
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Buscar ley...").assertIsDisplayed()
    }
}
