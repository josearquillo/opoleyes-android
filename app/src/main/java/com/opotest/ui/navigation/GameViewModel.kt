package com.opotest.ui.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opotest.data.model.*
import com.opotest.data.repository.GameRepository
import com.opotest.data.repository.MissionRepository
import com.opotest.data.repository.ProgressRepository
import com.opotest.data.repository.StatsRepository
import com.opotest.domain.AchievementChecker
import com.opotest.domain.AchievementContext
import com.opotest.domain.ChestSystem
import com.opotest.domain.GameEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val progressRepo = ProgressRepository(application)
    private val statsRepo = StatsRepository(application)
    private val gameRepo = GameRepository(application)
    private val missionRepo = MissionRepository(application)
    private val achievementChecker = AchievementChecker(application)
    private val chestSystem = ChestSystem(application)
    private val prefs = com.opotest.data.local.PreferencesManager(application)

    val engine = GameEngine(application)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _toasts = MutableStateFlow<List<Achievement>>(emptyList())
    val toasts: StateFlow<List<Achievement>> = _toasts.asStateFlow()

    private val _popups = MutableStateFlow<List<FloatingPopup>>(emptyList())
    val popups: StateFlow<List<FloatingPopup>> = _popups.asStateFlow()

    private val _powerUpToast = MutableStateFlow<PowerUpToast?>(null)
    val powerUpToast: StateFlow<PowerUpToast?> = _powerUpToast.asStateFlow()

    private val _chestReward = MutableStateFlow<ChestReward?>(null)
    val chestReward: StateFlow<ChestReward?> = _chestReward.asStateFlow()

    private val _rankUpOverlay = MutableStateFlow<RankUpOverlay?>(null)
    val rankUpOverlay: StateFlow<RankUpOverlay?> = _rankUpOverlay.asStateFlow()

    private val _newRecord = MutableStateFlow(false)
    val newRecord: StateFlow<Boolean> = _newRecord.asStateFlow()

    private val _newComboRecord = MutableStateFlow(false)
    val newComboRecord: StateFlow<Boolean> = _newComboRecord.asStateFlow()

    private val _newAccRecord = MutableStateFlow(false)
    val newAccRecord: StateFlow<Boolean> = _newAccRecord.asStateFlow()

    private val _xpGained = MutableStateFlow(0)
    val xpGained: StateFlow<Int> = _xpGained.asStateFlow()

    private val _medal = MutableStateFlow("")
    val medal: StateFlow<String> = _medal.asStateFlow()

    private val _accuracy = MutableStateFlow(0)
    val accuracy: StateFlow<Int> = _accuracy.asStateFlow()

    fun updateUiState() {
        _uiState.value = GameUiState(
            score = engine.score,
            combo = engine.combo,
            maxCombo = engine.maxCombo,
            lives = engine.lives,
            timer = engine.timer,
            questionNum = engine.questionNum,
            answered = engine.answered,
            selectedOption = engine.selectedOption,
            comboBarFill = engine.comboBarFill,
            comboOverchargeActive = engine.comboOverchargeActive,
            comboOverchargeCharges = engine.comboOverchargeCharges,
            streak = engine.streak,
            fiftyFiftyCharges = engine.fiftyFiftyCharges,
            fiftyFiftyActive = engine.fiftyFiftyActive,
            fiftyFiftyRemoved = engine.fiftyFiftyRemoved,
            freezeCharges = engine.freezeCharges,
            freezeActive = engine.freezeActive,
            doubleScoreCharges = engine.doubleScoreCharges,
            doubleScoreActive = engine.doubleScoreActive,
            hintCharges = engine.hintCharges,
            hintActive = engine.hintActive,
            hintRemoved = engine.hintRemoved,
            shieldCharges = engine.shieldCharges,
            currentQ = engine.currentQ,
            mode = engine.mode,
            totalAnswered = engine.totalAnswered,
            correctCount = engine.correctCount
        )
    }

    fun startQuickGame(): Boolean {
        val ok = engine.startQuickGame()
        if (ok) { engine.nextQuestion(); updateUiState() }
        return ok
    }

    fun startTemaGame(testId: String): Boolean {
        val ok = engine.startTemaGame(testId)
        if (ok) { engine.nextQuestion(); updateUiState() }
        return ok
    }

    fun startAllLawsGame(): Boolean {
        val ok = engine.startAllLawsGame()
        if (ok) { engine.nextQuestion(); updateUiState() }
        return ok
    }

    fun startChallengeGame(): Boolean {
        val ok = engine.startChallengeGame()
        if (ok) { engine.nextQuestion(); updateUiState() }
        return ok
    }

    fun answer(letter: String): GameEngine.AnswerResult {
        val result = engine.answer(letter)
        updateUiState()

        when (result) {
            GameEngine.AnswerResult.CORRECT -> {
                addPopup("+${10 * engine.combo} pts", com.opotest.ui.theme.Success, 54, 0f)
                if (engine.combo >= 3) {
                    val comboColor = when {
                        engine.combo >= 20 -> com.opotest.ui.theme.Warning
                        engine.combo >= 10 -> com.opotest.ui.theme.Danger
                        else -> com.opotest.ui.theme.Orange
                    }
                    addPopup("🔥 COMBO x${engine.combo}", comboColor, 40, 0.15f)
                }
                if (engine.mode == GameMode.TIMETRIAL || engine.mode == GameMode.CHALLENGE) {
                    addPopup("+15s", com.opotest.ui.theme.Cyan, 44, 0.3f)
                }
                if (engine.streak > 0 && engine.streak % 5 == 0) {
                    addPopup("⚡ Racha x${engine.streak}", com.opotest.ui.theme.Warning, 42, 0.45f)
                }
                if (engine.comboOverchargeActive && engine.comboOverchargeCharges == 3) {
                    addPopup("⚡ ¡OVERCHARGE!", com.opotest.ui.theme.Warning, 48, 0f)
                }
                checkAchievements(AchievementContext(firstCorrect = true, maxCombo = engine.maxCombo, score = engine.score, gameMode = engine.mode.name.lowercase()))
            }
            GameEngine.AnswerResult.WRONG -> {
                if (engine.mode == GameMode.SURVIVAL || engine.mode == GameMode.QUICK) {
                    addPopup("💔 -1 vida", com.opotest.ui.theme.Danger, 42, 0f)
                } else {
                    addPopup("-10s", com.opotest.ui.theme.Danger, 44, 0f)
                }
                checkAchievements(AchievementContext(maxCombo = engine.maxCombo, score = engine.score, gameMode = engine.mode.name.lowercase()))
            }
            GameEngine.AnswerResult.SHIELD_USED -> {
                addPopup("🛡️ Escudo usado!", com.opotest.ui.theme.Cyan, 44, 0f)
            }
            else -> {}
        }
        return result
    }

    fun nextQuestion() {
        if (engine.isGameOver()) return
        engine.nextQuestion()
        _popups.value = emptyList()
        updateUiState()
    }

    fun isGameOver(): Boolean = engine.isGameOver()

    fun activateFiftyFifty() { engine.activateFiftyFifty(); updateUiState() }
    fun activateFreeze() { engine.activateFreeze(); _powerUpToast.value = PowerUpToast("🧊 ¡Tiempo congelado 10s!", "🧊"); updateUiState() }
    fun activateDoubleScore() { engine.activateDoubleScore(); _powerUpToast.value = PowerUpToast("✨ ¡Doble puntuación activada!", "✨"); updateUiState() }
    fun useHint() { engine.useHint(); updateUiState() }

    fun clearPowerUpToast() { _powerUpToast.value = null }

    private fun addPopup(text: String, color: androidx.compose.ui.graphics.Color, size: Int, delay: Float) {
        _popups.value = _popups.value + FloatingPopup(text, color, size, delay)
    }

    private fun checkAchievements(ctx: AchievementContext) {
        val unlocked = achievementChecker.check(ctx)
        if (unlocked.isNotEmpty()) _toasts.value = _toasts.value + unlocked
    }

    fun clearToasts() { _toasts.value = emptyList() }

    fun onGameOver() {
        val mode = engine.mode.name.lowercase()
        val record = progressRepo.getRecord(mode)
        _newRecord.value = engine.score > record
        if (_newRecord.value) progressRepo.setRecord(mode, engine.score)

        val comboRecord = progressRepo.getRecordCombo(mode)
        _newComboRecord.value = engine.maxCombo > comboRecord
        if (_newComboRecord.value) progressRepo.setRecordCombo(mode, engine.maxCombo)

        val acc = engine.getAccuracy()
        val accRecord = progressRepo.getRecordAcc(mode)
        _newAccRecord.value = engine.totalAnswered >= 5 && acc > accRecord
        if (_newAccRecord.value) progressRepo.setRecordAcc(mode, acc)

        progressRepo.incrementGamesPlayed()
        _xpGained.value = progressRepo.getXP() - engine.startXP

        val rankBefore = engine.startRankIndex
        val rankAfter = progressRepo.getRankIndex()
        if (rankAfter > rankBefore) {
            _rankUpOverlay.value = RankUpOverlay(
                com.opotest.data.Constants.getRankByIndex(rankBefore),
                com.opotest.data.Constants.getRankByIndex(rankAfter)
            )
        }

        _accuracy.value = acc
        _medal.value = when {
            engine.score >= 1000 -> "🥇"
            engine.score >= 600 -> "🥈"
            engine.score >= 300 -> "🥉"
            else -> ""
        }

        val perfectGame = engine.totalAnswered >= 10 && acc == 100
        val sharpshooter = engine.totalAnswered >= 10 && acc >= 90
        checkAchievements(AchievementContext(
            gameOver = true, maxCombo = engine.maxCombo, score = engine.score,
            gameMode = mode, newRecord = _newRecord.value,
            perfectGame = perfectGame, sharpshooter = sharpshooter,
            fiftyFiftyUsed = engine.ctxFiftyFiftyUsed, lifeRecovered = engine.ctxLifeRecovered
        ))

        missionRepo.checkOnGameOver(mode, engine.score, engine.maxCombo, engine.correctCount, engine.totalAnswered, engine.category)

        val chest = chestSystem.generateChest(_newRecord.value, acc, engine.totalAnswered)
        _chestReward.value = chest
    }

    fun openChest() {
        _chestReward.value?.let { chestSystem.openChest(it) }
    }

    fun clearChest() { _chestReward.value = null }
    fun clearRankUp() { _rankUpOverlay.value = null }
    fun clearPopups() { _popups.value = emptyList() }

    fun getProgressRepo() = progressRepo
    fun getStatsRepo() = statsRepo
    fun getGameRepo() = gameRepo
    fun getMissionRepo() = missionRepo
    fun getPrefs() = prefs
}

data class GameUiState(
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val lives: Int = 0,
    val timer: Float = 0f,
    val questionNum: Int = 0,
    val answered: Boolean = false,
    val selectedOption: String? = null,
    val comboBarFill: Float = 0f,
    val comboOverchargeActive: Boolean = false,
    val comboOverchargeCharges: Int = 0,
    val streak: Int = 0,
    val fiftyFiftyCharges: Int = 0,
    val fiftyFiftyActive: Boolean = false,
    val fiftyFiftyRemoved: List<String> = emptyList(),
    val freezeCharges: Int = 0,
    val freezeActive: Boolean = false,
    val doubleScoreCharges: Int = 0,
    val doubleScoreActive: Boolean = false,
    val hintCharges: Int = 0,
    val hintActive: Boolean = false,
    val hintRemoved: List<String> = emptyList(),
    val shieldCharges: Int = 0,
    val currentQ: QuestionEntry? = null,
    val mode: GameMode = GameMode.SURVIVAL,
    val totalAnswered: Int = 0,
    val correctCount: Int = 0
)
