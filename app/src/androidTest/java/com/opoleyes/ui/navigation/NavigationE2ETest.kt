package com.opoleyes.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.repository.ProgressRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class NavigationE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val appContext = ApplicationProvider.getApplicationContext<Application>()
    private val actualRank = ProgressRepository(appContext).getRank()

    private fun preparePrefs() {
        val prefs = PreferencesManager(appContext)
        // Toggle debug mode off then on to clean up state from previous tests.
        // This restores saved exam/simulacro values, then re-enables debug mode.
        prefs.setDebugMode(false)
        prefs.setDebugMode(true)
        // Write intro flags directly to SharedPreferences to bypass the
        // write block when debug mode is active.
        val rankIndex = ProgressRepository(appContext).getRankIndex()
        val rawPrefs = appContext.getSharedPreferences("opoleyes_prefs", android.content.Context.MODE_PRIVATE)
        rawPrefs.edit()
            .putBoolean("intro_shown_intro_survival_rank_${minOf(rankIndex, 2)}", true)
            .putBoolean("intro_shown_intro_timetrial", true)
            .putBoolean("intro_shown_intro_quick", true)
            .putBoolean("intro_shown_intro_exam", true)
            .apply()
    }

    private fun startAtHome() {
        DataProvider.loadData(appContext)
        preparePrefs()
        val vm = GameViewModel(appContext)
        vm.preloadHomeData()
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent { NavGraph(startDestination = Routes.HOME, gameViewModel = vm) }
        composeRule.waitUntil(timeoutMillis = 10000) {
            try { composeRule.onNodeWithText(TestStrings.play).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    private fun navigateToGameViaSurvival() {
        composeRule.onNodeWithText(TestStrings.play).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.modeSurvival).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        composeRule.onNodeWithText(TestStrings.modeSurvival).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.allLaws).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        composeRule.onNodeWithText(TestStrings.allLaws).performClick()
        // Wait for game screen — use "50/50" which is available at rank 0
        composeRule.waitUntil(timeoutMillis = 15000) {
            try { composeRule.onNodeWithText(TestStrings.fiftyFifty).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    private fun setupGameOverState(): GameViewModel {
        DataProvider.loadData(appContext)
        preparePrefs()
        val vm = GameViewModel(appContext)
        vm.startAllLawsGame()
        // Answer wrong until game over (rank 0 has 7 lives)
        while (!vm.isGameOver()) {
            val q = vm.engine.currentQ!!
            val wrongAnswer = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
            vm.answer(wrongAnswer)
            vm.nextQuestion()
        }
        vm.onGameOver()
        return vm
    }

    private fun setupExamResultState(): GameViewModel {
        DataProvider.loadData(appContext)
        preparePrefs()
        val vm = GameViewModel(appContext)
        vm.examEngine.loadExam(10)
        vm.finishExam()
        return vm
    }

    private fun dismissOverlaysAndWaitForButton(buttonText: String, timeoutMs: Long = 20000) {
        composeRule.waitUntil(timeoutMillis = timeoutMs) {
            try { composeRule.onNodeWithText("Saltar ⏭").performClick() }
            catch (e: Throwable) {}
            try { composeRule.onNodeWithText(TestStrings.continueLabel).performClick() }
            catch (e: Throwable) {}
            try {
                composeRule.onNodeWithText(buttonText).performScrollTo()
                composeRule.onNodeWithText(buttonText).assertIsDisplayed()
                true
            } catch (e: Throwable) {
                false
            }
        }
    }

    @Test
    fun home_showsPlayButtonAndRank() {
        startAtHome()
        composeRule.onNodeWithText(TestStrings.play).assertIsDisplayed()
        composeRule.onNodeWithText(actualRank.name).assertIsDisplayed()
    }

    @Test
    fun home_clickPlay_showsModeSelect() {
        startAtHome()
        composeRule.onNodeWithText(TestStrings.play).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.modeSurvival).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    @Test
    fun modeSelect_backButton_returnsHome() {
        startAtHome()
        composeRule.onNodeWithText(TestStrings.play).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.modeSurvival).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        composeRule.onNodeWithContentDescription(TestStrings.back).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.play).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    @Test
    fun modeSelect_clickSurvival_showsTemaSelect() {
        startAtHome()
        composeRule.onNodeWithText(TestStrings.play).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.modeSurvival).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        composeRule.onNodeWithText(TestStrings.modeSurvival).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.allLaws).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    @Test
    fun home_clickRank_showsProfile() {
        startAtHome()
        composeRule.onNodeWithText(actualRank.name).performClick()
        composeRule.waitUntil(timeoutMillis = 10000) {
            try { composeRule.onNodeWithText(TestStrings.records).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    @Test
    fun profile_backButton_returnsHome() {
        startAtHome()
        composeRule.onNodeWithText(actualRank.name).performClick()
        composeRule.waitUntil(timeoutMillis = 10000) {
            try { composeRule.onNodeWithText(TestStrings.records).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        composeRule.onNodeWithContentDescription(TestStrings.back).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.play).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    @Test
    fun home_clickHelp_showsHelpScreen() {
        startAtHome()
        composeRule.onNodeWithContentDescription(TestStrings.help).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.helpObjective, substring = true).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    @Test
    fun help_backButton_returnsHome() {
        startAtHome()
        composeRule.onNodeWithContentDescription(TestStrings.help).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.helpObjective, substring = true).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        composeRule.onNodeWithContentDescription(TestStrings.back).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.play).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    // --- LOADING → HOME ---
    @Test
    fun loadingScreen_navigatesToHome() {
        DataProvider.loadData(appContext)
        preparePrefs()
        val vm = GameViewModel(appContext)
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent { NavGraph(startDestination = Routes.LOADING, gameViewModel = vm) }
        composeRule.waitUntil(timeoutMillis = 30000) {
            try { composeRule.onNodeWithText(TestStrings.play).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    // --- ERROR → HOME ---
    @Test
    fun errorScreen_backButton_navigatesToHome() {
        DataProvider.loadData(appContext)
        preparePrefs()
        val vm = GameViewModel(appContext)
        vm.preloadHomeData()
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent { NavGraph(startDestination = Routes.ERROR, gameViewModel = vm) }
        composeRule.waitUntil(timeoutMillis = 10000) {
            try { composeRule.onNodeWithText(TestStrings.error).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        composeRule.onNodeWithText(TestStrings.back).performClick()
        composeRule.waitUntil(timeoutMillis = 15000) {
            try { composeRule.onNodeWithText(TestStrings.play).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    // --- HOME → MODE_SELECT → SIMULACRO_INTRO ---
    @Test
    fun modeSelect_clickSimulacro_showsSimulacroIntro() {
        startAtHome()
        composeRule.onNodeWithText(TestStrings.play).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.modeSimulacro).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        composeRule.onNodeWithText(TestStrings.modeSimulacro).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.simulacroStart).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    // --- TEMA_SELECT → GAME (All Laws) ---
    @Test
    fun temaSelect_clickAllLaws_navigatesToGame() {
        startAtHome()
        navigateToGameViaSurvival()
        composeRule.onNodeWithText(TestStrings.fiftyFifty).assertIsDisplayed()
    }

    // --- GAME → HOME (exit via back press) ---
    @Test
    fun gameScreen_exitDialog_returnsHome() {
        startAtHome()
        navigateToGameViaSurvival()
        composeRule.runOnUiThread { composeRule.activity.onBackPressed() }
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.exit).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        composeRule.onNodeWithText(TestStrings.exit).performClick()
        composeRule.waitUntil(timeoutMillis = 10000) {
            try { composeRule.onNodeWithText(TestStrings.play).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    // --- GAME_OVER screen is displayed ---
    @Test
    fun gameOverScreen_isDisplayed() {
        val vm = setupGameOverState()
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent { NavGraph(startDestination = Routes.GAME_OVER, gameViewModel = vm) }
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.gameOver).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    // --- GAME_OVER → HOME ---
    @Test
    fun gameOverScreen_clickMenu_returnsHome() {
        val vm = setupGameOverState()
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent { NavGraph(startDestination = Routes.GAME_OVER, gameViewModel = vm) }
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.gameOver).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        dismissOverlaysAndWaitForButton(TestStrings.menu)
        composeRule.onNodeWithText(TestStrings.menu).performScrollTo()
        composeRule.onNodeWithText(TestStrings.menu).performClick()
        composeRule.waitUntil(timeoutMillis = 10000) {
            try { composeRule.onNodeWithText(TestStrings.play).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    // --- GAME_OVER → GAME (retry) ---
    @Test
    fun gameOverScreen_clickRetry_returnsToGame() {
        val vm = setupGameOverState()
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent { NavGraph(startDestination = Routes.GAME_OVER, gameViewModel = vm) }
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.gameOver).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        dismissOverlaysAndWaitForButton(TestStrings.playAgain)
        composeRule.onNodeWithText(TestStrings.playAgain).performScrollTo()
        composeRule.onNodeWithText(TestStrings.playAgain).performClick()
        composeRule.waitUntil(timeoutMillis = 15000) {
            try { composeRule.onNodeWithText(TestStrings.fiftyFifty).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    // --- EXAM_RESULT screen is displayed ---
    @Test
    fun examResultScreen_isDisplayed() {
        val vm = setupExamResultState()
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent { NavGraph(startDestination = Routes.EXAM_RESULT, gameViewModel = vm) }
        composeRule.waitUntil(timeoutMillis = 10000) {
            try { composeRule.onNodeWithText(TestStrings.resultados).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    // --- EXAM_RESULT → HOME ---
    @Test
    fun examResult_clickMenu_returnsHome() {
        val vm = setupExamResultState()
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent { NavGraph(startDestination = Routes.EXAM_RESULT, gameViewModel = vm) }
        composeRule.waitUntil(timeoutMillis = 10000) {
            try { composeRule.onNodeWithText(TestStrings.resultados).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        composeRule.onNodeWithText(TestStrings.menu).performScrollTo()
        composeRule.onNodeWithText(TestStrings.menu).performClick()
        composeRule.waitUntil(timeoutMillis = 15000) {
            try { composeRule.onNodeWithText(TestStrings.play).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    // --- EXAM_RESULT → EXAM (retry restarts exam) ---
    @Test
    fun examResult_clickRetry_restartsExam() {
        val vm = setupExamResultState()
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent { NavGraph(startDestination = Routes.EXAM_RESULT, gameViewModel = vm) }
        composeRule.waitUntil(timeoutMillis = 10000) {
            try { composeRule.onNodeWithText(TestStrings.resultados).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        composeRule.onNodeWithText(TestStrings.retryLabel).performScrollTo()
        composeRule.onNodeWithText(TestStrings.retryLabel).performClick()
        // Retry should navigate away from result screen — wait for it to disappear
        composeRule.waitUntil(timeoutMillis = 15000) {
            try {
                composeRule.onNodeWithText(TestStrings.resultados).assertIsDisplayed()
                false // Still visible, keep waiting
            } catch (e: Throwable) {
                true // Gone — navigation happened
            }
        }
    }

    // --- SIMULACRO_INTRO → EXAM ---
    @Test
    fun simulacroIntro_clickStart_navigatesToExam() {
        startAtHome()
        composeRule.onNodeWithText(TestStrings.play).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.modeSimulacro).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        composeRule.onNodeWithText(TestStrings.modeSimulacro).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            try { composeRule.onNodeWithText(TestStrings.simulacroStart).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        composeRule.onNodeWithText(TestStrings.simulacroStart).performClick()
        composeRule.waitUntil(timeoutMillis = 20000) {
            try { composeRule.onNodeWithText(TestStrings.modeSimulacro).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
    }

    // --- SIMULACRO_RESULT → EXAM (retry restarts simulacro) ---
    @Test
    fun simulacroResult_clickRetry_restartsSimulacro() {
        DataProvider.loadData(appContext)
        preparePrefs()
        val vm = GameViewModel(appContext)
        vm.loadSimulacroSync()
        for (i in 0 until 5) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent { NavGraph(startDestination = Routes.EXAM_RESULT, gameViewModel = vm) }
        composeRule.waitUntil(timeoutMillis = 10000) {
            try { composeRule.onNodeWithText(TestStrings.resultados).assertIsDisplayed(); true }
            catch (e: Throwable) { false }
        }
        composeRule.onNodeWithText(TestStrings.retryLabel).performScrollTo()
        composeRule.onNodeWithText(TestStrings.retryLabel).performClick()
        // Retry should navigate away from result screen — wait for it to disappear
        composeRule.waitUntil(timeoutMillis = 15000) {
            try {
                composeRule.onNodeWithText(TestStrings.resultados).assertIsDisplayed()
                false // Still visible, keep waiting
            } catch (e: Throwable) {
                true // Gone — navigation happened
            }
        }
    }
}
