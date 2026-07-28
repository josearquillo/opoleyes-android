package com.opoleyes.ui.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opoleyes.data.model.*
import com.opoleyes.data.repository.GameRepository
import com.opoleyes.data.repository.MissionRepository
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository
import com.opoleyes.domain.AchievementChecker
import com.opoleyes.domain.AchievementContext
import com.opoleyes.domain.ChestSystem
import com.opoleyes.domain.ExamEngine
import com.opoleyes.domain.GameEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val progressRepo = ProgressRepository(application)
    private val statsRepo = StatsRepository(application)
    private val gameRepo = GameRepository(application)
    private val missionRepo = MissionRepository(application)
    private val achievementChecker = AchievementChecker(application)
    private val chestSystem = ChestSystem(application)
    private val prefs = com.opoleyes.data.local.PreferencesManager(application)

    val engine = GameEngine(application)
    val examEngine = ExamEngine(application)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _toasts = MutableStateFlow<List<Achievement>>(emptyList())
    val toasts: StateFlow<List<Achievement>> = _toasts.asStateFlow()

    private val _popups = MutableStateFlow<List<FloatingPopup>>(emptyList())
    val popups: StateFlow<List<FloatingPopup>> = _popups.asStateFlow()

    private val _powerUpToast = MutableStateFlow<PowerUpToast?>(null)
    val powerUpToast: StateFlow<PowerUpToast?> = _powerUpToast.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

    private val _examQuestionNum = MutableStateFlow(0)
    val examQuestionNum: StateFlow<Int> = _examQuestionNum.asStateFlow()

    private val _examAnswered = MutableStateFlow(0)
    val examAnswered: StateFlow<Int> = _examAnswered.asStateFlow()

    private val _examCurrentQuestion = MutableStateFlow<ExamEngine.ExamQuestion?>(null)
    val examCurrentQuestion: StateFlow<ExamEngine.ExamQuestion?> = _examCurrentQuestion.asStateFlow()

    private val _examTotalQuestions = MutableStateFlow(0)
    val examTotalQuestions: StateFlow<Int> = _examTotalQuestions.asStateFlow()

    private val _examResult = MutableStateFlow<ExamEngine.ExamResult?>(null)
    val examResult: StateFlow<ExamEngine.ExamResult?> = _examResult.asStateFlow()

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
        _popups.value = emptyList()
        _toasts.value = emptyList()
        val ok = engine.startQuickGame()
        if (ok) { engine.nextQuestion(); updateUiState() }
        return ok
    }

    var pendingMode: GameMode = GameMode.SURVIVAL

    fun startTemaGame(testId: String): Boolean {
        _popups.value = emptyList()
        _toasts.value = emptyList()
        val ok = engine.startTemaGame(testId, pendingMode)
        if (ok) { engine.nextQuestion(); updateUiState() }
        return ok
    }

    fun startAllLawsGame(): Boolean {
        _popups.value = emptyList()
        _toasts.value = emptyList()
        val ok = engine.startAllLawsGame(pendingMode)
        if (ok) { engine.nextQuestion(); updateUiState() }
        return ok
    }

    fun startChallengeGame(): Boolean {
        _popups.value = emptyList()
        _toasts.value = emptyList()
        val ok = engine.startChallengeGame()
        if (ok) { engine.nextQuestion(); updateUiState() }
        return ok
    }

    fun startQuickGameAsync(onDone: (Boolean) -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            val ok = withContext(Dispatchers.Default) { startQuickGame() }
            _isLoading.value = false
            onDone(ok)
        }
    }

    fun startTemaGameAsync(testId: String, onDone: (Boolean) -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            val ok = withContext(Dispatchers.Default) { startTemaGame(testId) }
            _isLoading.value = false
            onDone(ok)
        }
    }

    fun startAllLawsGameAsync(onDone: (Boolean) -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            val ok = withContext(Dispatchers.Default) { startAllLawsGame() }
            _isLoading.value = false
            onDone(ok)
        }
    }

    fun startChallengeGameAsync(onDone: (Boolean) -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            val ok = withContext(Dispatchers.Default) { startChallengeGame() }
            _isLoading.value = false
            onDone(ok)
        }
    }

    fun startExamAsync(questionCount: Int, onDone: (Boolean) -> Unit) {
        _isLoading.value = true
        _examResult.value = null
        viewModelScope.launch {
            withContext(Dispatchers.Default) { examEngine.loadExam(questionCount) }
            _examQuestionNum.value = 0
            _examAnswered.value = 0
            _examTotalQuestions.value = examEngine.getQuestionCount()
            _examCurrentQuestion.value = examEngine.getCurrentQuestion()
            _isLoading.value = false
            onDone(true)
        }
    }

    fun examAnswer(letter: String) {
        examEngine.answer(letter)
        _examAnswered.value = examEngine.getAnsweredCount()
        _examCurrentQuestion.value = examEngine.getCurrentQuestion()
    }

    fun examNavigate(index: Int) {
        examEngine.navigateTo(index)
        _examQuestionNum.value = examEngine.getCurrentIndex()
        _examCurrentQuestion.value = examEngine.getCurrentQuestion()
        if (_examTotalQuestions.value == 0) _examTotalQuestions.value = examEngine.getQuestionCount()
    }

    fun examNext(): Boolean {
        val ok = examEngine.next()
        _examQuestionNum.value = examEngine.getCurrentIndex()
        _examCurrentQuestion.value = examEngine.getCurrentQuestion()
        return ok
    }

    fun examPrev(): Boolean {
        val ok = examEngine.prev()
        _examQuestionNum.value = examEngine.getCurrentIndex()
        _examCurrentQuestion.value = examEngine.getCurrentQuestion()
        return ok
    }

    fun finishExam() {
        val result = examEngine.grade()
        _examResult.value = result
        progressRepo.incrementGamesPlayed()
        val xp = result.correct * 10
        progressRepo.addXP(xp)
        _xpGained.value = xp
    }

    fun clearExamResult() {
        _examResult.value = null
        _examQuestionNum.value = 0
        _examAnswered.value = 0
        _examCurrentQuestion.value = null
        _examTotalQuestions.value = 0
    }

    fun answer(letter: String): GameEngine.AnswerResult {
        val result = engine.answer(letter)
        updateUiState()

        when (result) {
            GameEngine.AnswerResult.CORRECT -> {
                addPopup("+${10 * engine.combo} pts", com.opoleyes.ui.theme.Success, 54, 0f)
                if (engine.combo >= 3) {
                    val comboColor = when {
                        engine.combo >= 20 -> com.opoleyes.ui.theme.Warning
                        engine.combo >= 10 -> com.opoleyes.ui.theme.Danger
                        else -> com.opoleyes.ui.theme.Orange
                    }
                    addPopup("🔥 COMBO x${engine.combo}", comboColor, 40, 0.15f)
                }
                if (engine.mode == GameMode.TIMETRIAL || engine.mode == GameMode.CHALLENGE) {
                    addPopup("+15s", com.opoleyes.ui.theme.Cyan, 44, 0.3f)
                }
                if (engine.streak > 0 && engine.streak % 5 == 0) {
                    val streakMsg = when {
                        engine.ctxLifeRecovered -> "❤️ ¡Vida recuperada! (Racha x${engine.streak})"
                        engine.mode == GameMode.TIMETRIAL || engine.mode == GameMode.CHALLENGE -> "⏱️ +20s (Racha x${engine.streak})"
                        else -> "⚡ Racha x${engine.streak}"
                    }
                    addPopup(streakMsg, com.opoleyes.ui.theme.Warning, 38, 0.45f)
                }
                if (engine.comboOverchargeActive && engine.comboOverchargeCharges == 3) {
                    addPopup("⚡ ¡OVERCHARGE!", com.opoleyes.ui.theme.Warning, 48, 0f)
                }
                checkAchievements(AchievementContext(firstCorrect = true, maxCombo = engine.maxCombo, score = engine.score, gameMode = engine.mode.name.lowercase()))
            }
            GameEngine.AnswerResult.WRONG -> {
                if (engine.mode == GameMode.SURVIVAL || engine.mode == GameMode.QUICK) {
                    addPopup("💔 -1 vida", com.opoleyes.ui.theme.Danger, 42, 0f)
                } else {
                    addPopup("-10s", com.opoleyes.ui.theme.Danger, 44, 0f)
                }
                checkAchievements(AchievementContext(maxCombo = engine.maxCombo, score = engine.score, gameMode = engine.mode.name.lowercase()))
            }
            GameEngine.AnswerResult.SHIELD_USED -> {
                addPopup("🛡️ Escudo usado!", com.opoleyes.ui.theme.Cyan, 44, 0f)
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
        engine.saveRemainingPowerUps()
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
            for (r in (rankBefore + 1)..rankAfter) {
                val rewards = com.opoleyes.data.Constants.RANK_POWERUP_REWARDS[r]
                if (rewards != null) {
                    val current = prefs.getFreePowerUps().toMutableList()
                    current.addAll(rewards)
                    prefs.setFreePowerUps(current)
                }
            }
            _rankUpOverlay.value = RankUpOverlay(
                com.opoleyes.data.Constants.getRankByIndex(rankBefore),
                com.opoleyes.data.Constants.getRankByIndex(rankAfter)
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

        missionRepo.checkOnGameOver(mode, engine.maxCombo, engine.totalAnswered, engine.category)

        val chest = chestSystem.generateChest(_newRecord.value, acc, engine.totalAnswered, engine.score)
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

    fun exitGame() {
        engine.saveRemainingPowerUps()
    }
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
