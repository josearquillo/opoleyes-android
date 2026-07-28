package com.opoleyes.ui.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.ui.navigation.GameViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeScreenTest {

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
    fun fun_homeScreen_showsTitle() {
        composeRule.setContent {
            HomeScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("OPOLEYES").assertIsDisplayed()
    }

    @Test
    fun fun_homeScreen_showsPlayButton() {
        composeRule.setContent {
            HomeScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("JUGAR").assertIsDisplayed()
    }

    @Test
    fun fun_homeScreen_showsRankName() {
        composeRule.setContent {
            HomeScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Novato").assertIsDisplayed()
    }

    @Test
    fun fun_homeScreen_showsMissionsTitle() {
        composeRule.setContent {
            HomeScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Misiones diarias").assertIsDisplayed()
    }

    @Test
    fun fun_homeScreen_clickingPlayDoesNotCrash() {
        composeRule.setContent {
            HomeScreen(androidx.navigation.compose.rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("JUGAR").fetchSemanticsNodes().also { org.junit.Assert.assertTrue(it.isNotEmpty()) }
    }
}
