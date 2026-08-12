package com.opoleyes.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.ui.navigation.TestStrings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opoleyes.data.local.DataProvider

@RunWith(AndroidJUnit4::class)
class ErrorScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setupErrorScreen() {
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            ErrorScreen(rememberNavController())
        }
        composeRule.waitForIdle()
    }

    @Test
    fun errorScreen_displaysErrorText() {
        setupErrorScreen()
        composeRule.onNodeWithText(TestStrings.error).assertIsDisplayed()
    }

    @Test
    fun errorScreen_displaysBackButton() {
        setupErrorScreen()
        composeRule.onNodeWithText(TestStrings.back).assertIsDisplayed()
    }

    @Test
    fun errorScreen_displaysErrorIcon() {
        setupErrorScreen()
        composeRule.onNodeWithContentDescription(TestStrings.error).assertIsDisplayed()
    }
}
