package com.opoleyes.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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
class ProfileScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setupProfileScreen() {
        val vm = GameViewModel(ApplicationProvider.getApplicationContext<Application>())
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            ProfileScreen(rememberNavController(), vm)
        }
        composeRule.waitUntil(timeoutMillis = 10000) {
            try { composeRule.onNodeWithText(TestStrings.profile).assertIsDisplayed(); true }
            catch (e: Exception) { false }
        }
    }

    @Test
    fun profileScreen_displaysTitle() {
        setupProfileScreen()
        composeRule.onNodeWithText(TestStrings.profile).assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysRecordsSection() {
        setupProfileScreen()
        composeRule.onNodeWithText(TestStrings.records).assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysAchievementsSection() {
        setupProfileScreen()
        composeRule.onNodeWithText("Logros", substring = true).performScrollTo()
        composeRule.onNodeWithText("Logros", substring = true).assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysStatisticsSection() {
        setupProfileScreen()
        composeRule.onNodeWithText(TestStrings.statistics).performScrollTo()
        composeRule.onNodeWithText(TestStrings.statistics).assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysResetButton() {
        setupProfileScreen()
        composeRule.onNodeWithText(TestStrings.resetProgress).performScrollTo()
        composeRule.onNodeWithText(TestStrings.resetProgress).assertIsDisplayed()
    }

    @Test
    fun profileScreen_clickReset_showsConfirmDialog() {
        setupProfileScreen()
        composeRule.onNodeWithText(TestStrings.resetProgress).performScrollTo()
        composeRule.onNodeWithText(TestStrings.resetProgress).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TestStrings.reset).assertIsDisplayed()
        composeRule.onNodeWithText(TestStrings.cancel).assertIsDisplayed()
    }
}
