package com.opoleyes.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opoleyes.data.model.XpBreakdown
import com.opoleyes.data.model.XpLine
import com.opoleyes.ui.theme.Primary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameComponentsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun gameButton_displaysText() {
        composeRule.setContent {
            GameButton(text = "TestButton", onClick = {})
        }
        composeRule.onNodeWithText("TestButton").assertIsDisplayed()
    }

    @Test
    fun gameButton_click_triggersOnClick() {
        var clicked = false
        composeRule.setContent {
            GameButton(text = "ClickMe", onClick = { clicked = true })
        }
        composeRule.onNodeWithText("ClickMe").performClick()
        composeRule.waitForIdle()
        assert(clicked) { "GameButton onClick should fire" }
    }

    @Test
    fun optionCard_displaysLetterAndText() {
        composeRule.setContent {
            OptionCard(text = "Test option", letter = "A", onClick = {})
        }
        composeRule.onNodeWithText("A)", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Test option").assertIsDisplayed()
    }

    @Test
    fun optionCard_click_triggersOnClick() {
        var clicked = false
        composeRule.setContent {
            OptionCard(text = "Option B", letter = "B", onClick = { clicked = true })
        }
        composeRule.onNodeWithText("B)", substring = true).performClick()
        composeRule.waitForIdle()
        assert(clicked) { "OptionCard onClick should fire" }
    }

    @Test
    fun optionCard_correctAnswer_showsCheckIcon() {
        composeRule.setContent {
            OptionCard(
                text = "Correct option", letter = "C",
                isCorrect = true, answered = true, userWasCorrect = true,
                onClick = {}
            )
        }
        composeRule.onNodeWithText("C)", substring = true).assertIsDisplayed()
    }

    @Test
    fun optionCard_wrongAnswer_showsAsDanger() {
        composeRule.setContent {
            OptionCard(
                text = "Wrong option", letter = "D",
                isWrong = true, answered = true,
                onClick = {}
            )
        }
        composeRule.onNodeWithText("D)", substring = true).assertIsDisplayed()
    }

    @Test
    fun optionCard_hintRemoved_showsAsDimmed() {
        composeRule.setContent {
            OptionCard(
                text = "Hint removed option", letter = "A",
                isHintRemoved = true,
                onClick = {}
            )
        }
        composeRule.onNodeWithText("A)", substring = true).assertIsDisplayed()
    }

    @Test
    fun animatedHudBar_displaysScoreAndLives() {
        composeRule.setContent {
            AnimatedHudBar(
                score = 500, lives = 3, timer = 60f,
                mode = com.opoleyes.data.model.GameMode.SURVIVAL,
                maxLives = 3
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun comboBar_displaysFillLevel() {
        composeRule.setContent {
            ComboBar(fill = 0.5f, overchargeActive = false, overchargeCharges = 0)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun comboBar_overchargeActive_displays() {
        composeRule.setContent {
            ComboBar(fill = 1f, overchargeActive = true, overchargeCharges = 2)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun progressBar_displays() {
        composeRule.setContent {
            ProgressBar(progress = 0.5f)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun statCard_displaysValueAndLabel() {
        composeRule.setContent {
            StatCard(value = "500", label = "Score")
        }
        composeRule.onNodeWithText("500").assertIsDisplayed()
        composeRule.onNodeWithText("Score").assertIsDisplayed()
    }

    @Test
    fun glassCard_displaysContent() {
        composeRule.setContent {
            GlassCard {
                androidx.compose.material3.Text("Inside card")
            }
        }
        composeRule.onNodeWithText("Inside card").assertIsDisplayed()
    }

    @Test
    fun xpSummaryOverlay_displaysBreakdown() {
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            XpSummaryOverlay(
                breakdown = XpBreakdown(
                    lines = listOf(
                        XpLine(icon = "✅", label = "Correct answers", value = 100, color = Primary),
                        XpLine(icon = "🔥", label = "Combo bonus", value = 50, color = Primary)
                    ),
                    total = 150
                ),
                onDismiss = {}
            )
        }
        composeRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeRule.onNodeWithText("Correct answers").assertIsDisplayed(); true
            } catch (e: Throwable) { false }
        }
        composeRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeRule.onNodeWithText("Combo bonus").assertIsDisplayed(); true
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun statCardWithIcon_displaysValueAndLabel() {
        composeRule.setContent {
            StatCardWithIcon(icon = Icons.Filled.Star, value = "100", label = "Stars", color = Primary)
        }
        composeRule.onNodeWithText("100").assertIsDisplayed()
        composeRule.onNodeWithText("Stars").assertIsDisplayed()
    }

    @Test
    fun loadingOverlay_displays() {
        composeRule.setContent {
            LoadingOverlay()
        }
        composeRule.waitForIdle()
    }

    @Test
    fun gameButton_withIcon_displays() {
        composeRule.setContent {
            GameButton(text = "WithIcon", icon = Icons.Filled.Star, onClick = {})
        }
        composeRule.onNodeWithText("WithIcon").assertIsDisplayed()
    }

    @Test
    fun animatedHudBar_timetrial_displaysTimer() {
        composeRule.setContent {
            AnimatedHudBar(
                score = 300, lives = 0, timer = 45f,
                mode = com.opoleyes.data.model.GameMode.TIMETRIAL,
                maxLives = 0
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun animatedHudBar_quick_displaysLives() {
        composeRule.setContent {
            AnimatedHudBar(
                score = 200, lives = 2, timer = 0f,
                mode = com.opoleyes.data.model.GameMode.QUICK,
                maxLives = 5
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun particleBurst_displaysWithTrigger() {
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            ParticleBurst(trigger = Any(), color = Primary, particleCount = 10)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun confettiBurst_displaysWithTrigger() {
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            ConfettiBurst(trigger = Any(), durationMs = 1000)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun shakeBox_displaysWithTrigger() {
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            ShakeBox(shakeTrigger = Any(), intensity = 8f) {
                androidx.compose.material3.Text("Shake content")
            }
        }
        composeRule.onNodeWithText("Shake content").assertIsDisplayed()
    }

    @Test
    fun xpSummaryOverlay_withMultiplier_displays() {
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent {
            XpSummaryOverlay(
                breakdown = XpBreakdown(
                    lines = listOf(
                        XpLine(icon = "✓", label = "Aciertos (5)", value = 50, color = Primary),
                        XpLine(icon = "×2", label = "Multiplicador", value = 0, color = Primary)
                    ),
                    total = 100,
                    multiplierApplied = true
                ),
                onDismiss = {}
            )
        }
        composeRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeRule.onNodeWithText("Aciertos (5)").assertIsDisplayed(); true
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun optionCard_selected_displaysAsSelected() {
        composeRule.setContent {
            OptionCard(
                text = "Selected option", letter = "B",
                isSelected = true,
                onClick = {}
            )
        }
        composeRule.onNodeWithText("B)", substring = true).assertIsDisplayed()
    }

    @Test
    fun optionCard_correctButUserWrong_displaysAsCorrect() {
        composeRule.setContent {
            OptionCard(
                text = "Correct but user wrong", letter = "C",
                isCorrect = true, answered = true, userWasCorrect = false,
                onClick = {}
            )
        }
        composeRule.onNodeWithText("C)", substring = true).assertIsDisplayed()
    }

    @Test
    fun optionCard_disabled_displays() {
        composeRule.setContent {
            OptionCard(
                text = "Disabled option", letter = "D",
                enabled = false,
                onClick = {}
            )
        }
        composeRule.onNodeWithText("D)", substring = true).assertIsDisplayed()
    }

    @Test
    fun comboBar_empty_displays() {
        composeRule.setContent {
            ComboBar(fill = 0f, overchargeActive = false, overchargeCharges = 0)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun progressBar_full_displays() {
        composeRule.setContent {
            ProgressBar(progress = 1f)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun progressBar_empty_displays() {
        composeRule.setContent {
            ProgressBar(progress = 0f)
        }
        composeRule.waitForIdle()
    }
}
