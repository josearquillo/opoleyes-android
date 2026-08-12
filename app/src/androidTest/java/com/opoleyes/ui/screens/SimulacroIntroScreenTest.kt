package com.opoleyes.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
class SimulacroIntroScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setupSimulacroIntro() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        DataProvider.loadData(ctx)
        val vm = GameViewModel(ctx)
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            SimulacroIntroScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun simulacroIntro_displaysTitle() {
        setupSimulacroIntro()
        composeRule.onNodeWithText(TestStrings.simulacroIntroTitle).assertIsDisplayed()
    }

    @Test
    fun simulacroIntro_displaysStartButton() {
        setupSimulacroIntro()
        composeRule.onNodeWithText(TestStrings.simulacroStart).assertIsDisplayed()
    }

    @Test
    fun simulacroIntro_displaysBackButton() {
        setupSimulacroIntro()
        composeRule.onNodeWithContentDescription(TestStrings.back).assertIsDisplayed()
    }
}
