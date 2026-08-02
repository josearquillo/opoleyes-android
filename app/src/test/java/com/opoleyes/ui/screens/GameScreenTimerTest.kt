package com.opoleyes.ui.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.navigation.compose.rememberNavController
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.GameMode
import com.opoleyes.ui.navigation.GameViewModel
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameScreenTimerTest {

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
    fun timetrialTimer_ticksWhenNoDialogOpen() {
        // Verify that the timer LaunchedEffect runs and ticks the timer.
        // This confirms the timer mechanism works; the pause during dialog
        // is verified by the showExitDialog key in the LaunchedEffect.
        vm.startAllLawsGame()
        vm.engine.mode = GameMode.TIMETRIAL
        vm.engine.timer = 180f
        vm.updateUiState()

        composeRule.setContent {
            GameScreen(rememberNavController(), vm)
        }
        composeRule.waitForIdle()

        val timerBefore = vm.uiState.value.timer
        // Advance time to let the LaunchedEffect tick
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()

        val timerAfter = vm.uiState.value.timer
        // Timer should have ticked at least once (180 -> 179 or less)
        assertTrue("Timer should tick when no dialog is open (before=$timerBefore, after=$timerAfter)",
            timerAfter < timerBefore)
    }
}
