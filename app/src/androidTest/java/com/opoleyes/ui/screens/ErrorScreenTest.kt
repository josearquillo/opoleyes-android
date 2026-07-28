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
class ErrorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun errorScreen_displaysErrorText() {
        composeRule.setContent {
            ErrorScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun errorScreen_displaysBackButton() {
        composeRule.setContent {
            ErrorScreen(rememberNavController())
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Volver").assertIsDisplayed()
    }
}
