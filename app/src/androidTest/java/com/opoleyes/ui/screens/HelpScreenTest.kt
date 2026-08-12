package com.opoleyes.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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
class HelpScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setupHelpScreen() {
        val vm = GameViewModel(ApplicationProvider.getApplicationContext<Application>())
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            HelpScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun helpScreen_displaysTitle() {
        setupHelpScreen()
        composeRule.onAllNodesWithText(TestStrings.help)[0].assertIsDisplayed()
    }

    @Test
    fun helpScreen_displaysObjectiveSection() {
        setupHelpScreen()
        composeRule.onNodeWithText(TestStrings.helpObjective, substring = true).assertIsDisplayed()
    }

    @Test
    fun helpScreen_displaysModesSection() {
        setupHelpScreen()
        composeRule.onNodeWithText(TestStrings.helpModes, substring = true).assertIsDisplayed()
    }

    @Test
    fun helpScreen_displaysPowerUpsSection() {
        setupHelpScreen()
        composeRule.onNodeWithText(TestStrings.helpPowerups, substring = true).performScrollTo()
        composeRule.onNodeWithText(TestStrings.helpPowerups, substring = true).assertIsDisplayed()
    }

    @Test
    fun helpScreen_displaysComboSection() {
        setupHelpScreen()
        composeRule.onNodeWithText(TestStrings.helpCombo, substring = true).performScrollTo()
        composeRule.onNodeWithText(TestStrings.helpCombo, substring = true).assertIsDisplayed()
    }

    @Test
    fun helpScreen_displaysRanksSection() {
        setupHelpScreen()
        composeRule.onNodeWithText(TestStrings.helpRanks, substring = true).performScrollTo()
        composeRule.onNodeWithText(TestStrings.helpRanks, substring = true).assertIsDisplayed()
    }

    @Test
    fun helpScreen_displaysBonusSection() {
        setupHelpScreen()
        composeRule.onAllNodesWithText(TestStrings.helpBonus, substring = true)[0].performScrollTo()
        composeRule.onAllNodesWithText(TestStrings.helpBonus, substring = true)[0].assertIsDisplayed()
    }

    @Test
    fun helpScreen_displaysMissionsSection() {
        setupHelpScreen()
        composeRule.onNodeWithText(TestStrings.helpMissions, substring = true).performScrollTo()
        composeRule.onNodeWithText(TestStrings.helpMissions, substring = true).assertIsDisplayed()
    }
}
