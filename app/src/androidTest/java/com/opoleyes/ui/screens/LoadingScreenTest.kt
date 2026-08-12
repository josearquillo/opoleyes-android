package com.opoleyes.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.TestStrings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class LoadingScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingScreen_displaysLoadingText() {
        val vm = GameViewModel(ApplicationProvider.getApplicationContext<Application>())
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            LoadingScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TestStrings.loadingApp).assertIsDisplayed()
    }
}
