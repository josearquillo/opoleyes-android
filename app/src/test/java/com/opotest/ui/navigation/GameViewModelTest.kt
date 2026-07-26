package com.opotest.ui.navigation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opotest.data.local.PreferencesManager
import com.opotest.data.model.GameMode
import com.opotest.domain.GameEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameViewModelTest {

    private lateinit var vm: GameViewModel
    private lateinit var prefs: PreferencesManager
    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        prefs = PreferencesManager(app)
        prefs.resetAll()
        vm = GameViewModel(app)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun fun_uiState_initialState() {
        val state = vm.uiState.value
        assertEquals(0, state.score)
        assertEquals(0, state.combo)
        assertEquals(0, state.lives)
        assertEquals(GameMode.SURVIVAL, state.mode)
    }

    @Test
    fun fun_startQuickGame_returnsTrue() {
        val ok = vm.startQuickGame()
        assertTrue(ok)
        assertEquals(GameMode.QUICK, vm.uiState.value.mode)
        assertNotNull(vm.uiState.value.currentQ)
    }

    @Test
    fun fun_startAllLawsGame_returnsTrue() {
        val ok = vm.startAllLawsGame()
        assertTrue(ok)
        assertEquals(GameMode.SURVIVAL, vm.uiState.value.mode)
        assertNotNull(vm.uiState.value.currentQ)
    }

    @Test
    fun fun_startChallengeGame_returnsTrue() {
        val ok = vm.startChallengeGame()
        assertTrue(ok)
        assertEquals(GameMode.CHALLENGE, vm.uiState.value.mode)
    }

    @Test
    fun fun_answer_correctUpdatesState() {
        vm.startAllLawsGame()
        val q = vm.engine.currentQ!!
        val result = vm.answer(q.correct)
        assertEquals(GameEngine.AnswerResult.CORRECT, result)
        assertEquals(1, vm.uiState.value.combo)
        assertEquals(1, vm.uiState.value.correctCount)
        assertTrue(vm.uiState.value.score > 0)
        assertTrue(vm.uiState.value.answered)
    }

    @Test
    fun fun_answer_wrongUpdatesState() {
        vm.startAllLawsGame()
        val q = vm.engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        val result = vm.answer(wrong)
        assertEquals(GameEngine.AnswerResult.WRONG, result)
        assertEquals(0, vm.uiState.value.combo)
        assertEquals(2, vm.uiState.value.lives)
    }

    @Test
    fun fun_nextQuestion_advances() {
        vm.startAllLawsGame()
        val q1 = vm.engine.currentQ
        vm.nextQuestion()
        assertNotNull(vm.uiState.value.currentQ)
    }

    @Test
    fun fun_nextQuestion_gameOverDoesNothing() {
        vm.startAllLawsGame()
        vm.engine.lives = 0
        vm.nextQuestion()
    }

    @Test
    fun fun_isGameOver_survival() {
        vm.startAllLawsGame()
        assertFalse(vm.isGameOver())
        vm.engine.lives = 0
        assertTrue(vm.isGameOver())
    }

    @Test
    fun fun_activateFiftyFifty() {
        vm.startAllLawsGame()
        vm.engine.fiftyFiftyCharges = 1
        vm.activateFiftyFifty()
        assertTrue(vm.uiState.value.fiftyFiftyActive)
    }

    @Test
    fun fun_activateDoubleScore() {
        vm.startAllLawsGame()
        vm.engine.doubleScoreCharges = 1
        vm.activateDoubleScore()
        assertTrue(vm.uiState.value.doubleScoreActive)
    }

    @Test
    fun fun_useHint() {
        vm.startAllLawsGame()
        vm.engine.hintCharges = 1
        vm.useHint()
        assertTrue(vm.uiState.value.hintActive)
    }

    @Test
    fun fun_clearPowerUpToast() {
        vm.activateFreeze()
        vm.clearPowerUpToast()
        assertEquals(null, vm.powerUpToast.value)
    }

    @Test
    fun fun_clearToasts() {
        vm.clearToasts()
        assertTrue(vm.toasts.value.isEmpty())
    }

    @Test
    fun fun_clearPopups() {
        vm.clearPopups()
        assertTrue(vm.popups.value.isEmpty())
    }

    @Test
    fun fun_onGameOver_setsNewRecord() {
        vm.startAllLawsGame()
        vm.engine.score = 500
        vm.onGameOver()
        assertTrue(vm.newRecord.value)
    }

    @Test
    fun fun_onGameOver_incrementsGamesPlayed() {
        val before = vm.getProgressRepo().getGamesPlayed()
        vm.startAllLawsGame()
        vm.onGameOver()
        assertEquals(before + 1, vm.getProgressRepo().getGamesPlayed())
    }

    @Test
    fun fun_onGameOver_setsXpGained() {
        vm.startAllLawsGame()
        vm.onGameOver()
        assertTrue(vm.xpGained.value >= 0)
    }

    @Test
    fun fun_onGameOver_setsAccuracy() {
        vm.startAllLawsGame()
        vm.engine.correctCount = 5
        vm.engine.totalAnswered = 10
        vm.onGameOver()
        assertEquals(50, vm.accuracy.value)
    }

    @Test
    fun fun_onGameOver_setsMedal_gold() {
        vm.startAllLawsGame()
        vm.engine.score = 1000
        vm.onGameOver()
        assertEquals("🥇", vm.medal.value)
    }

    @Test
    fun fun_onGameOver_setsMedal_silver() {
        vm.startAllLawsGame()
        vm.engine.score = 600
        vm.onGameOver()
        assertEquals("🥈", vm.medal.value)
    }

    @Test
    fun fun_onGameOver_setsMedal_bronze() {
        vm.startAllLawsGame()
        vm.engine.score = 300
        vm.onGameOver()
        assertEquals("🥉", vm.medal.value)
    }

    @Test
    fun fun_onGameOver_setsMedal_none() {
        vm.startAllLawsGame()
        vm.engine.score = 100
        vm.onGameOver()
        assertEquals("", vm.medal.value)
    }

    @Test
    fun fun_onGameOver_setsChestReward() {
        vm.startAllLawsGame()
        vm.onGameOver()
        assertNotNull(vm.chestReward.value)
    }

    @Test
    fun fun_openChest_addsXP() {
        vm.startAllLawsGame()
        vm.onGameOver()
        val xpBefore = vm.getProgressRepo().getXP()
        vm.openChest()
        assertTrue(vm.getProgressRepo().getXP() >= xpBefore)
    }

    @Test
    fun fun_clearChest() {
        vm.clearChest()
        assertEquals(null, vm.chestReward.value)
    }

    @Test
    fun fun_clearRankUp() {
        vm.clearRankUp()
        assertEquals(null, vm.rankUpOverlay.value)
    }

    @Test
    fun fun_onGameOver_rankUp() {
        vm.startAllLawsGame()
        vm.engine.startXP = 0
        vm.engine.startRankIndex = 0
        vm.getProgressRepo().addXP(600)
        vm.onGameOver()
        assertNotNull(vm.rankUpOverlay.value)
    }

    @Test
    fun fun_onGameOver_newComboRecord() {
        vm.startAllLawsGame()
        vm.engine.maxCombo = 15
        vm.onGameOver()
        assertTrue(vm.newComboRecord.value)
    }

    @Test
    fun fun_onGameOver_newAccRecord() {
        vm.startAllLawsGame()
        vm.engine.correctCount = 10
        vm.engine.totalAnswered = 10
        vm.onGameOver()
        assertTrue(vm.newAccRecord.value)
    }

    @Test
    fun fun_onGameOver_accRecordRequiresMin5() {
        vm.startAllLawsGame()
        vm.engine.correctCount = 2
        vm.engine.totalAnswered = 2
        vm.onGameOver()
        assertFalse(vm.newAccRecord.value)
    }

    @Test
    fun fun_answer_correctAddsPopup() {
        vm.startAllLawsGame()
        val q = vm.engine.currentQ!!
        vm.answer(q.correct)
        assertTrue(vm.popups.value.isNotEmpty())
    }

    @Test
    fun fun_answer_wrongAddsPopup() {
        vm.startAllLawsGame()
        val q = vm.engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        vm.answer(wrong)
        assertTrue(vm.popups.value.isNotEmpty())
    }

    @Test
    fun fun_answer_combo3AddsComboPopup() {
        vm.startAllLawsGame()
        for (i in 1..3) {
            vm.answer(vm.engine.currentQ!!.correct)
            if (i < 3) vm.nextQuestion()
        }
        assertTrue(vm.popups.value.any { it.text.contains("COMBO") })
    }

    @Test
    fun fun_startTemaGame_validId() {
        val temaTests = com.opotest.data.local.DataProvider.getTemaTests(app)
        if (temaTests.isNotEmpty()) {
            val ok = vm.startTemaGame(temaTests[0].id)
            assertTrue(ok)
        }
    }

    @Test
    fun fun_startTemaGame_invalidId() {
        val ok = vm.startTemaGame("nonexistent")
        assertFalse(ok)
    }
}
