package com.opoleyes.ui.navigation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.GameMode
import com.opoleyes.domain.GameEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
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
        vm.engine.shieldCharges = 0
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
        vm.startAllLawsGame()
        vm.engine.doubleScoreCharges = 1
        vm.activateDoubleScore()
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
        repeat(6) {
            vm.engine.nextQuestion()
            vm.engine.answer(vm.engine.currentQ!!.correct)
        }
        vm.onGameOver()
        assertNotNull(vm.chestReward.value)
    }

    @Test
    fun fun_openChest_addsXP() {
        vm.startAllLawsGame()
        repeat(6) {
            vm.engine.nextQuestion()
            vm.engine.answer(vm.engine.currentQ!!.correct)
        }
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
        val temaTests = com.opoleyes.data.local.DataProvider.getTemaTests(app)
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

    // === Rank-up power-up reward tests ===

    @Test
    fun fun_onGameOver_rankUp_grantsPowerUps() {
        vm.startAllLawsGame()
        vm.engine.startXP = 0
        vm.engine.startRankIndex = 0
        vm.getProgressRepo().addXP(600)
        val powerUpsBefore = vm.getPrefs().getFreePowerUps().size
        vm.onGameOver()
        val powerUpsAfter = vm.getPrefs().getFreePowerUps().size
        assertTrue("Power-ups should increase on rank-up", powerUpsAfter > powerUpsBefore)
    }

    @Test
    fun fun_onGameOver_rankUp_grantsCorrectPowerUpsForRank1() {
        vm.startAllLawsGame()
        vm.engine.startXP = 0
        vm.engine.startRankIndex = 0
        vm.getProgressRepo().addXP(600)
        vm.onGameOver()
        val powerUps = vm.getPrefs().getFreePowerUps()
        val rank1Rewards = com.opoleyes.data.Constants.RANK_POWERUP_REWARDS[1]!!
        assertEquals(rank1Rewards.size, powerUps.size)
        rank1Rewards.forEach { pu ->
            assertTrue("Should contain $pu", powerUps.contains(pu))
        }
    }

    @Test
    fun fun_onGameOver_noRankUp_doesNotGrantPowerUps() {
        vm.startAllLawsGame()
        vm.engine.startXP = vm.getProgressRepo().getXP()
        vm.engine.startRankIndex = vm.getProgressRepo().getRankIndex()
        val powerUpsBefore = vm.getPrefs().getFreePowerUps().size
        vm.onGameOver()
        assertEquals(powerUpsBefore, vm.getPrefs().getFreePowerUps().size)
    }

    @Test
    fun fun_onGameOver_multiRankUp_grantsAllRewards() {
        vm.startAllLawsGame()
        vm.engine.startXP = 0
        vm.engine.startRankIndex = 0
        vm.getProgressRepo().addXP(1600)
        vm.onGameOver()
        val powerUps = vm.getPrefs().getFreePowerUps()
        val expected = (com.opoleyes.data.Constants.RANK_POWERUP_REWARDS[1]!! +
                com.opoleyes.data.Constants.RANK_POWERUP_REWARDS[2]!!)
        assertEquals(expected.size, powerUps.size)
    }

    // === Exam ViewModel tests ===

    @Test
    fun fun_startExam_loadsQuestions() {
        vm.examEngine.loadExam(25)
        assertEquals(25, vm.examEngine.getQuestionCount())
    }

    @Test
    fun fun_examAnswer_storesAnswer() {
        vm.examEngine.loadExam(10)
        vm.examAnswer("A")
        val q = vm.examEngine.getCurrentQuestion()
        assertEquals("A", q?.userAnswer)
        assertEquals(1, vm.examAnswered.value)
    }

    @Test
    fun fun_examNext_advancesIndex() {
        vm.examEngine.loadExam(10)
        vm.examNext()
        assertEquals(1, vm.examQuestionNum.value)
    }

    @Test
    fun fun_examPrev_decreasesIndex() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(3)
        vm.examPrev()
        assertEquals(2, vm.examQuestionNum.value)
    }

    @Test
    fun fun_examNavigate_setsIndex() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(5)
        assertEquals(5, vm.examQuestionNum.value)
    }

    @Test
    fun fun_examNext_returnsFalseAtLast() {
        vm.examEngine.loadExam(5)
        vm.examNavigate(4)
        assertFalse(vm.examNext())
    }

    @Test
    fun fun_examPrev_returnsFalseAtFirst() {
        vm.examEngine.loadExam(5)
        assertFalse(vm.examPrev())
    }

    @Test
    fun fun_finishExam_setsResult() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        vm.finishExam()
        assertNotNull(vm.examResult.value)
        assertEquals(10, vm.examResult.value!!.correct)
    }

    @Test
    fun fun_finishExam_grantsXP() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            vm.examAnswer(q.question.correct)
        }
        val xpBefore = vm.getProgressRepo().getXP()
        vm.finishExam()
        assertTrue(vm.getProgressRepo().getXP() > xpBefore)
    }

    @Test
    fun fun_finishExam_incrementsGamesPlayed() {
        vm.examEngine.loadExam(5)
        val before = vm.getProgressRepo().getGamesPlayed()
        vm.finishExam()
        assertEquals(before + 1, vm.getProgressRepo().getGamesPlayed())
    }

    @Test
    fun fun_clearExamResult_resetsState() {
        vm.examEngine.loadExam(10)
        vm.examAnswer("A")
        vm.finishExam()
        vm.clearExamResult()
        assertEquals(null, vm.examResult.value)
        assertEquals(0, vm.examQuestionNum.value)
        assertEquals(0, vm.examAnswered.value)
    }

    // === Bug regression tests ===

    @Test
    fun fun_onGameOver_savesRemainingPowerUps() {
        vm.startAllLawsGame()
        vm.engine.shieldCharges = 2
        vm.engine.fiftyFiftyCharges = 1
        vm.onGameOver()
        val powerUps = vm.getPrefs().getFreePowerUps()
        assertTrue("Remaining power-ups should be saved on game over",
            powerUps.contains("shield"))
        assertTrue("Remaining power-ups should be saved on game over",
            powerUps.contains("fiftyFifty"))
    }

    @Test
    fun fun_onGameOver_doesNotSavePowerUpsWhenNoneRemaining() {
        vm.startAllLawsGame()
        vm.engine.shieldCharges = 0
        vm.engine.fiftyFiftyCharges = 0
        vm.engine.hintCharges = 0
        vm.engine.doubleScoreCharges = 0
        vm.onGameOver()
        val powerUps = vm.getPrefs().getFreePowerUps()
        assertTrue("No power-ups should be saved when none remaining",
            !powerUps.contains("shield") && !powerUps.contains("fiftyFifty"))
    }

    @Test
    fun fun_exitGame_savesRemainingPowerUps() {
        vm.startAllLawsGame()
        vm.engine.shieldCharges = 1
        val powerUpsBefore = vm.getPrefs().getFreePowerUps().size
        vm.exitGame()
        val powerUpsAfter = vm.getPrefs().getFreePowerUps().size
        assertTrue("exitGame should save remaining power-ups", powerUpsAfter > powerUpsBefore)
    }

    @Test
    fun fun_clearChest_setsNull() {
        vm.startAllLawsGame()
        repeat(6) {
            vm.engine.nextQuestion()
            vm.engine.answer(vm.engine.currentQ!!.correct)
        }
        vm.onGameOver()
        assertNotNull(vm.chestReward.value)
        vm.clearChest()
        assertEquals(null, vm.chestReward.value)
    }

    @Test
    fun fun_clearRankUp_setsNull() {
        vm.startAllLawsGame()
        vm.engine.startXP = 0
        vm.engine.startRankIndex = 0
        vm.getProgressRepo().addXP(600)
        vm.onGameOver()
        assertNotNull(vm.rankUpOverlay.value)
        vm.clearRankUp()
        assertEquals(null, vm.rankUpOverlay.value)
    }

    @Test
    fun fun_finishExam_withZeroCorrect() {
        vm.examEngine.loadExam(10)
        for (i in 0 until 10) {
            vm.examNavigate(i)
            val q = vm.examEngine.getCurrentQuestion()!!
            val wrong = listOf("A", "B", "C", "D").filter { it != q.question.correct }.first()
            vm.examAnswer(wrong)
        }
        vm.finishExam()
        assertNotNull(vm.examResult.value)
        assertEquals(0, vm.examResult.value!!.correct)
        assertEquals(10, vm.examResult.value!!.wrong)
    }

    @Test
    fun fun_finishExam_withAllUnanswered() {
        vm.examEngine.loadExam(10)
        vm.finishExam()
        assertNotNull(vm.examResult.value)
        assertEquals(0, vm.examResult.value!!.correct)
        assertEquals(10, vm.examResult.value!!.unanswered)
    }

    @Test
    fun fun_answer_shieldUsedShowsInState() {
        vm.startAllLawsGame()
        vm.engine.shieldCharges = 1
        val q = vm.engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        vm.answer(wrong)
        assertEquals(0, vm.uiState.value.shieldCharges)
    }

    @Test
    fun fun_answer_shieldMultipleInState() {
        vm.startAllLawsGame()
        vm.engine.shieldCharges = 3
        val q = vm.engine.currentQ!!
        val wrong = listOf("A", "B", "C", "D").filter { it != q.correct }.first()
        vm.answer(wrong)
        assertEquals("Shield should show 2 remaining in UI state", 2, vm.uiState.value.shieldCharges)
    }

    @Test
    fun fun_startGame_loadsPowerUpsIntoState() {
        prefs.setFreePowerUps(listOf("shield", "shield", "hint"))
        vm.startAllLawsGame()
        assertEquals(2, vm.uiState.value.shieldCharges)
        assertEquals(1, vm.uiState.value.hintCharges)
    }

    @Test
    fun fun_onGameOver_setsMedalForHighScore() {
        vm.startAllLawsGame()
        vm.engine.score = 1500
        vm.onGameOver()
        assertEquals("🥇", vm.medal.value)
    }

    @Test
    fun fun_onGameOver_chestGeneratedForValidGame() {
        vm.startAllLawsGame()
        repeat(6) {
            vm.engine.nextQuestion()
            vm.engine.answer(vm.engine.currentQ!!.correct)
        }
        vm.onGameOver()
        assertNotNull("Chest should be generated after game over", vm.chestReward.value)
    }

    @Test
    fun fun_onGameOver_newRecordSetCorrectly() {
        vm.startAllLawsGame()
        vm.engine.score = 500
        vm.onGameOver()
        assertTrue(vm.newRecord.value)
        // Second game with lower score should not set new record
        vm.startAllLawsGame()
        vm.engine.score = 100
        vm.onGameOver()
        assertFalse(vm.newRecord.value)
    }

    @Test
    fun fun_examAnswer_canChangeAnswerAndCountStaysSame() {
        vm.examEngine.loadExam(10)
        vm.examAnswer("A")
        assertEquals(1, vm.examAnswered.value)
        vm.examAnswer("B")
        assertEquals(1, vm.examAnswered.value)
    }

    @Test
    fun fun_updateUiState_reflectsEngineState() {
        vm.startAllLawsGame()
        vm.engine.score = 250
        vm.engine.combo = 5
        vm.engine.lives = 2
        vm.updateUiState()
        assertEquals(250, vm.uiState.value.score)
        assertEquals(5, vm.uiState.value.combo)
        assertEquals(2, vm.uiState.value.lives)
    }

    // === Exam StateFlow observability regression tests ===

    @Test
    fun fun_examAnswer_updatesExamCurrentQuestion() {
        vm.examEngine.loadExam(10)
        vm.examAnswer("A")
        assertNotNull("examCurrentQuestion should be updated after answer",
            vm.examCurrentQuestion.value)
        assertEquals("A", vm.examCurrentQuestion.value?.userAnswer)
    }

    @Test
    fun fun_examNext_updatesExamCurrentQuestion() {
        vm.examEngine.loadExam(10)
        val q0 = vm.examCurrentQuestion.value
        vm.examNext()
        val q1 = vm.examCurrentQuestion.value
        assertNotNull(q1)
        assertNotSame("examCurrentQuestion should be different after next", q0, q1)
    }

    @Test
    fun fun_examPrev_updatesExamCurrentQuestion() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(3)
        val q3 = vm.examCurrentQuestion.value
        vm.examPrev()
        val q2 = vm.examCurrentQuestion.value
        assertNotNull(q2)
        assertNotSame("examCurrentQuestion should be different after prev", q3, q2)
    }

    @Test
    fun fun_examNavigate_updatesExamCurrentQuestion() {
        vm.examEngine.loadExam(10)
        vm.examNavigate(5)
        assertNotNull(vm.examCurrentQuestion.value)
        assertEquals(5, vm.examQuestionNum.value)
    }

    @Test
    fun fun_startExam_setsExamTotalQuestions() {
        vm.examEngine.loadExam(25)
        assertEquals(25, vm.examEngine.getQuestionCount())
    }

    @Test
    fun fun_clearExamResult_resetsExamCurrentQuestion() {
        vm.examEngine.loadExam(10)
        vm.examAnswer("A")
        vm.finishExam()
        vm.clearExamResult()
        assertEquals(null, vm.examCurrentQuestion.value)
        assertEquals(0, vm.examTotalQuestions.value)
    }
}
