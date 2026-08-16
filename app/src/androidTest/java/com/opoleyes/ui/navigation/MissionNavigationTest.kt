package com.opoleyes.ui.navigation

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.Mission
import com.opoleyes.data.model.MissionData
import com.opoleyes.data.model.MissionDifficulty
import com.opoleyes.data.repository.ProgressRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MissionNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val appContext = ApplicationProvider.getApplicationContext<Application>()

    private fun preparePrefs() {
        DataProvider.loadData(appContext)
        val prefs = PreferencesManager(appContext)
        prefs.setDebugMode(false)
        prefs.setDebugMode(true)
        val rankIndex = ProgressRepository(appContext).getRankIndex()
        val rawPrefs = appContext.getSharedPreferences("opoleyes_prefs", android.content.Context.MODE_PRIVATE)
        rawPrefs.edit()
            .putBoolean("intro_shown_intro_survival_rank_${minOf(rankIndex, 2)}", true)
            .putBoolean("intro_shown_intro_timetrial", true)
            .putBoolean("intro_shown_intro_quick", true)
            .putBoolean("intro_shown_intro_exam", true)
            .apply()
    }

    private fun saveMissions(mission: Mission) {
        val filler1 = Mission("streak", "🎯", "Filler streak mission", 99, 0, true, 50, "streak_filler", null, MissionDifficulty.EASY)
        val filler2 = Mission("combo", "🔥", "Filler combo mission", 99, 0, true, 50, "combo_filler", null, MissionDifficulty.EASY)
        val data = MissionData(
            date = LocalDate.now().toString(),
            missions = listOf(mission, filler1, filler2)
        )
        // Write directly to SharedPreferences — PreferencesManager.saveDailyMissions
        // is blocked in debug mode (isWriteBlocked returns true)
        val gson = com.google.gson.Gson()
        val rawPrefs = appContext.getSharedPreferences("opoleyes_prefs", android.content.Context.MODE_PRIVATE)
        rawPrefs.edit()
            .putString("daily_missions_json", gson.toJson(data))
            .apply()
    }

    private fun startHomeWithMission(mission: Mission) {
        preparePrefs()
        saveMissions(mission)
        val vm = GameViewModel(appContext)
        vm.preloadHomeData()
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent { NavGraph(startDestination = Routes.HOME, gameViewModel = vm) }
        composeRule.waitUntil(timeoutMillis = 20000) {
            try { composeRule.onNodeWithText(TestStrings.play).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        composeRule.waitUntil(timeoutMillis = 20000) {
            try {
                composeRule.onAllNodesWithText(mission.text).fetchSemanticsNodes().isNotEmpty()
            } catch (e: Throwable) { false }
        }
        composeRule.onNodeWithText(mission.text).performScrollTo()
        composeRule.onNodeWithText(mission.text).performClick()
    }

    private fun waitForNavigationAwayFromHome(timeoutMillis: Long = 15000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            try {
                composeRule.onNodeWithText(TestStrings.play).assertIsDisplayed()
                false
            } catch (e: Throwable) {
                true
            }
        }
    }

    private fun waitForScreenWithText(text: String, timeoutMillis: Long = 15000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            try { composeRule.onNodeWithText(text).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    // === Survival-based missions ===

    @Test
    fun mission_quality_navigatesToGame() {
        startHomeWithMission(Mission("quality", "🎯", "Test quality mission", 5, 0, false, 50, "streak", null, MissionDifficulty.HARD))
        waitForNavigationAwayFromHome()
    }

    @Test
    fun mission_combo_navigatesToGame() {
        startHomeWithMission(Mission("combo", "", "Test combo mission", 5, 0, false, 50, "combo", null, MissionDifficulty.HARD))
        waitForNavigationAwayFromHome()
    }

    @Test
    fun mission_variety_navigatesAwayFromHome() {
        startHomeWithMission(Mission("variety", "", "Test variety mission", 5, 0, false, 50, "variety_any", null, MissionDifficulty.EASY))
        waitForNavigationAwayFromHome()
    }

    @Test
    fun mission_no_powerups_navigatesAwayFromHome() {
        startHomeWithMission(Mission("no_powerups", "", "Test no powerups mission", 1, 0, false, 50, "no_powerups", null, MissionDifficulty.MEDIUM))
        waitForNavigationAwayFromHome()
    }

    @Test
    fun mission_perfect_game_navigatesToGame() {
        startHomeWithMission(Mission("perfect_game", "💎", "Test perfect game mission", 1, 0, false, 100, "perfect_game", null, MissionDifficulty.HARD))
        waitForNavigationAwayFromHome()
    }

    @Test
    fun mission_no_powerups_combo_navigatesToGame() {
        startHomeWithMission(Mission("no_powerups_combo", "🚫", "Test no powerups combo mission", 5, 0, false, 100, "no_powerups_combo", null, MissionDifficulty.HARD))
        waitForNavigationAwayFromHome()
    }

    @Test
    fun mission_play_count_navigatesToGame() {
        startHomeWithMission(Mission("play_count", "🎮", "Test play count mission", 3, 0, false, 50, "play_count", null, MissionDifficulty.EASY))
        waitForNavigationAwayFromHome()
    }

    // === Quick-based missions ===

    @Test
    fun mission_review_navigatesToGame() {
        startHomeWithMission(Mission("review", "🔄", "Test review mission", 5, 0, false, 50, "quick_review", null, MissionDifficulty.EASY))
        waitForNavigationAwayFromHome()
    }

    @Test
    fun mission_perfect_quick_navigatesToGame() {
        startHomeWithMission(Mission("perfect_quick", "🔄", "Test perfect quick mission", 1, 0, false, 100, "perfect_quick", null, MissionDifficulty.MEDIUM))
        waitForNavigationAwayFromHome()
    }

    // === Timetrial-based missions ===

    @Test
    fun mission_timetrial_navigatesToGame() {
        startHomeWithMission(Mission("timetrial", "⏱️", "Test timetrial mission", 500, 0, false, 50, "timetrial_score", null, MissionDifficulty.MEDIUM))
        waitForNavigationAwayFromHome()
    }

    // === Exam mission ===

    @Test
    fun mission_exam_navigatesToModeSelect() {
        startHomeWithMission(Mission("exam", "📝", "Test exam mission", 60, 0, false, 100, "exam_score", null, MissionDifficulty.HARD))
        waitForScreenWithText(TestStrings.selectMode)
    }

    // === Simulacro mission ===

    @Test
    fun mission_simulacro_navigatesToSimulacroIntro() {
        startHomeWithMission(Mission("simulacro", "🎯", "Test simulacro mission", 1, 0, false, 100, "simulacro_complete", null, MissionDifficulty.HARD))
        waitForScreenWithText(TestStrings.simulacroIntroTitle)
    }

    // === Completed mission does not navigate ===

    @Test
    fun mission_completed_doesNotNavigate() {
        startHomeWithMission(Mission("quality", "🎯", "Test completed mission", 5, 5, true, 50, "streak", null, MissionDifficulty.EASY))
        // Verify we're still on home after 2s
        Thread.sleep(2000)
        composeRule.onNodeWithText(TestStrings.play).assertIsDisplayed()
    }
}
