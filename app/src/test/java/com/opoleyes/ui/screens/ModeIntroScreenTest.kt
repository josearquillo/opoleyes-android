package com.opoleyes.ui.screens

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.GameMode
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ModeIntroScreenTest {

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
    fun teardown() { prefs.resetAll() }

    private fun advance() {
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
    }

    private fun startSurvivalAndRender() {
        vm.pendingMode = GameMode.SURVIVAL
        val ok = vm.startAllLawsGame()
        assertTrue("Game should start successfully (pool not empty)", ok)
        composeRule.setContent {
            val nav = rememberNavController()
            NavHost(nav, startDestination = Routes.MODE_INTRO) {
                composable(Routes.MODE_INTRO) { ModeIntroScreen(nav, vm) }
                composable(Routes.GAME) { Text("GAME") }
                composable(Routes.EXAM) { Text("EXAM") }
            }
        }
        advance()
    }

    private fun assertTextPresent(text: String) {
        val nodes = composeRule.onAllNodesWithText(text).fetchSemanticsNodes()
        assertTrue("Should find text '$text', got ${nodes.size} nodes", nodes.isNotEmpty())
    }

    @Test
    fun modeIntro_showsSurvivalTitleAndKeyCards() {
        startSurvivalAndRender()
        // Rank 0 (Novato): subtitle is "Rango: Novato"
        assertTextPresent("Rango: Novato")
        assertTextPresent("Corazones")
        assertTextPresent("Opciones por pregunta")
        assertTextPresent("Dificultad de las preguntas")
        assertTextPresent("Power-ups")
    }

    @Test
    fun modeIntro_showsDontShowAgainCheckbox() {
        startSurvivalAndRender()
        assertTextPresent("No mostrar más")
    }

    @Test
    fun modeIntro_showsPlayButton() {
        startSurvivalAndRender()
        assertTextPresent("Jugar")
    }

    @Test
    fun modeIntro_playButton_dismissesIntroWhenCheckboxChecked() {
        startSurvivalAndRender()
        assertTrue("Intro should be visible before dismissing", vm.shouldShowModeIntro(GameMode.SURVIVAL))

        // Check the "don't show again" checkbox
        composeRule.onNodeWithText("No mostrar más").performScrollTo().performClick()
        advance()

        // Press play
        composeRule.onNodeWithText("Jugar").performScrollTo().performClick()
        advance()

        assertFalse("Intro should be dismissed after play with checkbox", vm.shouldShowModeIntro(GameMode.SURVIVAL))
    }

    @Test
    fun modeIntro_playButton_doesNotDismissWhenCheckboxUnchecked() {
        startSurvivalAndRender()
        assertTrue(vm.shouldShowModeIntro(GameMode.SURVIVAL))

        // Press play without checking the checkbox
        composeRule.onNodeWithText("Jugar").performScrollTo().performClick()
        advance()

        assertTrue("Intro should still show if checkbox not checked", vm.shouldShowModeIntro(GameMode.SURVIVAL))
    }

    @Test
    fun modeIntro_playButton_navigatesToGameRoute() {
        vm.pendingMode = GameMode.SURVIVAL
        val ok = vm.startAllLawsGame()
        assertTrue(ok)
        var navigatedRoute: String? = null
        composeRule.setContent {
            val nav = rememberNavController()
            nav.addOnDestinationChangedListener { _, destination, _ ->
                navigatedRoute = destination.route
            }
            NavHost(nav, startDestination = Routes.MODE_INTRO) {
                composable(Routes.MODE_INTRO) { ModeIntroScreen(nav, vm) }
                composable(Routes.GAME) { Text("GAME") }
                composable(Routes.EXAM) { Text("EXAM") }
            }
        }
        advance()

        composeRule.onNodeWithText("Jugar").performScrollTo().performClick()
        advance()

        assertTrue("Should navigate to GAME route, got $navigatedRoute", navigatedRoute == Routes.GAME)
    }

    @Test
    fun modeIntro_playButton_navigatesToExamRouteForExamMode() {
        // ModeIntroScreen only needs pendingMode and rankIndex, not the actual exam loaded
        vm.pendingMode = GameMode.EXAM
        var navigatedRoute: String? = null
        composeRule.setContent {
            val nav = rememberNavController()
            nav.addOnDestinationChangedListener { _, destination, _ ->
                navigatedRoute = destination.route
            }
            NavHost(nav, startDestination = Routes.MODE_INTRO) {
                composable(Routes.MODE_INTRO) { ModeIntroScreen(nav, vm) }
                composable(Routes.GAME) { Text("GAME") }
                composable(Routes.EXAM) { Text("EXAM") }
            }
        }
        advance()

        composeRule.onNodeWithText("Jugar").performScrollTo().performClick()
        advance()

        assertTrue("Should navigate to EXAM route for exam mode, got $navigatedRoute", navigatedRoute == Routes.EXAM)
    }

    @Test
    fun modeIntro_rank0_showsNovatoContent() {
        startSurvivalAndRender()
        // Rank 0 (Novato): 5 hearts, 2 options
        assertTextPresent("Rango: Novato")
        assertTextPresent("Corazones")
        assertTextPresent("Opciones por pregunta")
    }

    @Test
    fun modeIntro_rank2_showsModoCompletoTitle() {
        // Promote to rank 2 (Aprendiz) for the "modo completo" intro
        prefs.addXP(800)
        vm.pendingMode = GameMode.SURVIVAL
        val ok = vm.startAllLawsGame()
        assertTrue(ok)
        composeRule.setContent {
            val nav = rememberNavController()
            NavHost(nav, startDestination = Routes.MODE_INTRO) {
                composable(Routes.MODE_INTRO) { ModeIntroScreen(nav, vm) }
                composable(Routes.GAME) { Text("GAME") }
            }
        }
        advance()

        assertTextPresent("Rango: Aprendiz")
        assertTextPresent("Corazones")
    }

    @Test
    fun modeIntro_timetrial_showsTimetrialContent() {
        vm.pendingMode = GameMode.TIMETRIAL
        val ok = vm.startAllLawsGame()
        assertTrue(ok)
        composeRule.setContent {
            val nav = rememberNavController()
            NavHost(nav, startDestination = Routes.MODE_INTRO) {
                composable(Routes.MODE_INTRO) { ModeIntroScreen(nav, vm) }
                composable(Routes.GAME) { Text("GAME") }
            }
        }
        advance()

        assertTextPresent("Responde rápido y con precisión")
        assertTextPresent("Tiempo limitado")
        assertTextPresent("Acierto")
        assertTextPresent("Fallo")
    }

    @Test
    fun modeIntro_quick_showsQuickContent() {
        val ok = vm.startQuickGame()
        assertTrue(ok)
        composeRule.setContent {
            val nav = rememberNavController()
            NavHost(nav, startDestination = Routes.MODE_INTRO) {
                composable(Routes.MODE_INTRO) { ModeIntroScreen(nav, vm) }
                composable(Routes.GAME) { Text("GAME") }
            }
        }
        advance()

        assertTextPresent("Repasa tus errores rápidamente")
        assertTextPresent("5 preguntas")
        assertTextPresent("Recompensa 5/5")
    }
}
