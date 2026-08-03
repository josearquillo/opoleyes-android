package com.opoleyes.ui.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opoleyes.data.model.*
import com.opoleyes.data.Constants
import com.opoleyes.data.repository.GameRepository
import com.opoleyes.data.repository.MissionRepository
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository
import com.opoleyes.domain.AchievementChecker
import com.opoleyes.domain.AchievementContext
import com.opoleyes.domain.ChestSystem
import com.opoleyes.domain.ExamEngine
import com.opoleyes.domain.GameEngine
import com.opoleyes.ui.theme.Primary
import com.opoleyes.ui.theme.AccentLight
import com.opoleyes.ui.theme.Success
import com.opoleyes.ui.theme.PrimaryLight
import com.opoleyes.ui.theme.Warning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

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

    // Preloaded data for HomeScreen (computed during loading screen)
    data class HomePreload(
        val rank: com.opoleyes.data.model.Rank,
        val xpProgress: com.opoleyes.data.model.XPProgress,
        val missions: com.opoleyes.data.model.MissionData,
        val totalCorrect: Int,
        val totalWrong: Int,
        val maxCombo: Int
    )
    private var _homePreload: HomePreload? = null
    val homePreload: HomePreload? get() = _homePreload

    fun preloadHomeData() {
        if (_homePreload != null) return
        val rank = progressRepo.getRank()
        val xpProgress = progressRepo.getXPProgress()
        val missions = missionRepo.generateDailyMissions()
        val totalCorrect = statsRepo.getTotalCorrect()
        val totalWrong = statsRepo.getTotalWrong()
        val maxCombo = progressRepo.getMaxComboRecord()
        _homePreload = HomePreload(rank, xpProgress, missions, totalCorrect, totalWrong, maxCombo)
    }

    // Preloaded data for ProfileScreen
    data class ProfileData(
        val rank: com.opoleyes.data.model.Rank,
        val xpProgress: com.opoleyes.data.model.XPProgress,
        val achievements: Map<String, Long>,
        val gamesPlayed: Int,
        val totalCorrect: Int,
        val totalWrong: Int,
        val globalProgress: Int,
        val temaTests: List<com.opoleyes.data.model.Test>,
        val dominatedLaws: Int,
        val powerUps: List<String>,
        val records: Map<String, Int>,
        val unlockedModes: Map<String, Boolean>
    )
    private var _profileData: ProfileData? = null
    val profileData: ProfileData? get() = _profileData

    fun preloadProfileData() {
        if (_profileData != null) return
        val temaTests = com.opoleyes.data.local.DataProvider.getTemaTests(getApplication())
        val records = linkedMapOf(
            "survival" to progressRepo.getRecord("survival"),
            "timetrial" to progressRepo.getRecord("timetrial"),
            "quick" to progressRepo.getRecord("quick")
        )
        val unlockedModes = linkedMapOf(
            "survival" to progressRepo.isUnlocked("survival"),
            "timetrial" to progressRepo.isUnlocked("timetrial"),
            "quick" to progressRepo.isUnlocked("quick")
        )
        _profileData = ProfileData(
            rank = progressRepo.getRank(),
            xpProgress = progressRepo.getXPProgress(),
            achievements = progressRepo.getAchievements(),
            gamesPlayed = progressRepo.getGamesPlayed(),
            totalCorrect = statsRepo.getTotalCorrect(),
            totalWrong = statsRepo.getTotalWrong(),
            globalProgress = statsRepo.getGlobalProgress(),
            temaTests = temaTests,
            dominatedLaws = temaTests.count { statsRepo.getLeyProgress(it.id) >= 100 },
            powerUps = prefs.getFreePowerUps(),
            records = records,
            unlockedModes = unlockedModes
        )
    }

    fun getLeyProgress(testId: String): Int = statsRepo.getLeyProgress(testId)
    fun getTemaTests(): List<com.opoleyes.data.model.Test> =
        com.opoleyes.data.local.DataProvider.getTemaTests(getApplication())
    fun getUnlocks(): com.opoleyes.data.repository.Unlocks = progressRepo.getUnlocks()

    fun resetProgress() {
        progressRepo.resetAll()
        statsRepo.invalidateCache()
        prefs.initPowerUpsIfNeeded()
        _homePreload = null
        _profileData = null
    }

    fun isDebugMode(): Boolean = prefs.isDebugMode()
    fun setDebugMode(enabled: Boolean) {
        prefs.setDebugMode(enabled)
        // Invalidate all caches so UI reflects the new state
        _homePreload = null
        _profileData = null
        statsRepo.invalidateCache()
        // Regenerate daily missions for the new unlock state
        missionRepo.generateDailyMissions()
    }

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

    private val _quickRewardPowerUp = MutableStateFlow<String?>(null)
    val quickRewardPowerUp: StateFlow<String?> = _quickRewardPowerUp.asStateFlow()

    private val _quickRewardEarned = MutableStateFlow(false)
    val quickRewardEarned: StateFlow<Boolean> = _quickRewardEarned.asStateFlow()

    private val _quickRewardMissed = MutableStateFlow(false)
    val quickRewardMissed: StateFlow<Boolean> = _quickRewardMissed.asStateFlow()

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

    private val _xpBreakdown = MutableStateFlow<XpBreakdown?>(null)
    val xpBreakdown: StateFlow<XpBreakdown?> = _xpBreakdown.asStateFlow()

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

    private val _isSimulacroMode = MutableStateFlow(false)
    val isSimulacroMode: StateFlow<Boolean> = _isSimulacroMode.asStateFlow()

    private val _simulacroResult = MutableStateFlow<ExamEngine.SimulacroResult?>(null)
    val simulacroResult: StateFlow<ExamEngine.SimulacroResult?> = _simulacroResult.asStateFlow()

    private val _simulacroTimer = MutableStateFlow(0)
    val simulacroTimer: StateFlow<Int> = _simulacroTimer.asStateFlow()

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
            shieldActive = engine.shieldActive,
            powerUpUsedThisQuestion = engine.powerUpUsedThisQuestion,
            currentQ = engine.currentQ,
            mode = engine.mode,
            totalAnswered = engine.totalAnswered,
            correctCount = engine.correctCount
        )
    }

    fun startQuickGame(): Boolean {
        _popups.value = emptyList()
        _toasts.value = emptyList()
        _quickRewardEarned.value = false
        gameOverProcessed = false
        _xpBreakdown.value = null
        missionRepo.clearSessionCompletedMissions()
        val ok = engine.startQuickGame()
        if (ok) {
            _quickRewardPowerUp.value = generateQuickReward()
            engine.nextQuestion(); updateUiState()
        }
        return ok
    }

    private fun generateQuickReward(): String {
        val avgWeight = if (engine.pool.isNotEmpty()) engine.pool.map { it.weight }.average() else 50.0
        return when {
            avgWeight >= 70 -> "doubleScore"
            avgWeight >= 50 -> "fiftyFifty"
            avgWeight >= 30 -> "shield"
            else -> "hint"
        }
    }

    fun getQuickRewardPowerUp(): String? = _quickRewardPowerUp.value

    fun clearQuickReward() {
        _quickRewardPowerUp.value = null
        _quickRewardEarned.value = false
        _quickRewardMissed.value = false
    }

    var pendingMode: GameMode = GameMode.SURVIVAL

    fun startTemaGame(testId: String): Boolean {
        _popups.value = emptyList()
        _toasts.value = emptyList()
        gameOverProcessed = false
        _xpBreakdown.value = null
        missionRepo.clearSessionCompletedMissions()
        val ok = engine.startTemaGame(testId, pendingMode)
        if (ok) { engine.nextQuestion(); updateUiState() }
        return ok
    }

    fun startAllLawsGame(): Boolean {
        _popups.value = emptyList()
        _toasts.value = emptyList()
        gameOverProcessed = false
        _xpBreakdown.value = null
        missionRepo.clearSessionCompletedMissions()
        val ok = engine.startAllLawsGame(pendingMode)
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

    fun startExamAsync(questionCount: Int, onDone: (Boolean) -> Unit) {
        _isLoading.value = true
        _examResult.value = null
        _isSimulacroMode.value = false
        _xpBreakdown.value = null
        missionRepo.clearSessionCompletedMissions()
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

    fun startSimulacroAsync(onDone: (Boolean) -> Unit) {
        _isLoading.value = true
        _examResult.value = null
        _simulacroResult.value = null
        _isSimulacroMode.value = true
        _xpBreakdown.value = null
        missionRepo.clearSessionCompletedMissions()
        viewModelScope.launch {
            withContext(Dispatchers.Default) { examEngine.loadSimulacro() }
            _examQuestionNum.value = 0
            _examAnswered.value = 0
            _examTotalQuestions.value = examEngine.getQuestionCount()
            _examCurrentQuestion.value = examEngine.getCurrentQuestion()
            _simulacroTimer.value = ExamEngine.SIMULACRO_TIME_SECONDS
            _isLoading.value = false
            onDone(true)
        }
    }

    fun examAnswer(letter: String) {
        examEngine.answer(letter)
        _examAnswered.value = examEngine.getAnsweredCount()
        _examCurrentQuestion.value = examEngine.getCurrentQuestion()
    }

    fun examClearAnswer() {
        examEngine.clearAnswer()
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
        // Guard against double submission (e.g. user presses back from result screen
        // and finishes the exam again)
        if (_examResult.value != null || _simulacroResult.value != null) return
        if (_isSimulacroMode.value) {
            finishSimulacro()
            return
        }
        val rankBefore = progressRepo.getRankIndex()
        val result = examEngine.grade()
        _examResult.value = result
        progressRepo.incrementGamesPlayed()
        val xp = result.correct * 10
        progressRepo.addXP(xp)
        _xpGained.value = xp
        val scorePct = if (result.total > 0) (result.correct * 100 / result.total) else 0
        missionRepo.checkExamResult(scorePct)
        if (result.score >= 5.0) {
            progressRepo.unlockNextExamQuestions()
            if (result.total >= 50 && !progressRepo.isSimulacroUnlocked()) {
                progressRepo.unlockSimulacro()
            }
        }
        checkRankUp(rankBefore)
        _xpBreakdown.value = buildExamXpBreakdown(result.correct, result.total)
        _homePreload = null
        _profileData = null
    }

    private fun finishSimulacro() {
        val rankBefore = progressRepo.getRankIndex()
        val result = examEngine.gradeSimulacro()
        _simulacroResult.value = result
        progressRepo.incrementGamesPlayed()
        val xp = (result.points * 10).toInt().coerceAtLeast(0)
        progressRepo.addXP(xp)
        _xpGained.value = xp
        progressRepo.addSimulacroHistory(
            SimulacroHistoryEntry(
                date = LocalDate.now().toString(),
                points = result.points,
                correct = result.correct,
                wrong = result.wrong,
                unanswered = result.unanswered,
                passed = result.passed
            )
        )
        missionRepo.checkSimulacroResult(result.passed)
        checkRankUp(rankBefore)
        _xpBreakdown.value = buildSimulacroXpBreakdown(result.points, result.correct)
        _homePreload = null
        _profileData = null
    }

    fun tickSimulacroTimer(): Boolean {
        val current = _simulacroTimer.value
        if (current <= 0) return true
        _simulacroTimer.value = current - 1
        return _simulacroTimer.value <= 0
    }

    fun getSimulacroTimer(): Int = _simulacroTimer.value

    fun clearExamResult() {
        _examResult.value = null
        _simulacroResult.value = null
        _isSimulacroMode.value = false
        _simulacroTimer.value = 0
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
                if (engine.combo >= 3) {
                    val comboColor = when {
                        engine.combo >= 20 -> com.opoleyes.ui.theme.Warning
                        engine.combo >= 10 -> com.opoleyes.ui.theme.Danger
                        else -> com.opoleyes.ui.theme.Orange
                    }
                    addPopup("COMBO x${engine.combo}", comboColor, 40, 0.15f, "🔥")
                }
                if (engine.streak > 0 && engine.streak % 5 == 0) {
                    val streakMsg = when {
                        engine.ctxLifeRecovered -> "¡Vida recuperada! (Racha x${engine.streak})"
                        engine.mode == GameMode.TIMETRIAL -> "+20s (Racha x${engine.streak})"
                        else -> "Racha x${engine.streak}"
                    }
                    val streakIcon = when {
                        engine.ctxLifeRecovered -> "❤️"
                        engine.mode == GameMode.TIMETRIAL -> "⏱️"
                        else -> "⚡"
                    }
                    addPopup(streakMsg, com.opoleyes.ui.theme.Warning, 38, 0.45f, streakIcon)
                }
                if (engine.comboOverchargeActive && engine.comboOverchargeCharges == 3) {
                    addPopup("¡OVERCHARGE!", com.opoleyes.ui.theme.Warning, 48, 0f, "⚡")
                }
                checkAchievementsPerQuestion(AchievementContext(firstCorrect = true, maxCombo = engine.maxCombo, fiftyFiftyUsed = engine.ctxFiftyFiftyUsed, lifeRecovered = engine.ctxLifeRecovered))
            }
            GameEngine.AnswerResult.WRONG -> {
                checkAchievementsPerQuestion(AchievementContext(maxCombo = engine.maxCombo))
            }
            GameEngine.AnswerResult.SHIELD_USED -> {
                addPopup("Escudo usado!", Primary, 44, 0f, "🛡️")
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
    fun activateDoubleScore() { engine.activateDoubleScore(); _powerUpToast.value = PowerUpToast("¡Doble puntuación activada!", "✨"); updateUiState() }
    fun activateShield() { engine.activateShield(); _powerUpToast.value = PowerUpToast("¡Escudo activado!", "🛡️"); updateUiState() }
    fun useHint() { engine.useHint(); updateUiState() }

    fun clearPowerUpToast() { _powerUpToast.value = null }

    private fun addPopup(text: String, color: androidx.compose.ui.graphics.Color, size: Int, delay: Float, icon: String = "") {
        _popups.value = _popups.value + FloatingPopup(text, color, size, delay, icon)
    }

    private fun checkAchievementsPerQuestion(ctx: AchievementContext) {
        val unlocked = achievementChecker.checkPerQuestion(ctx)
        if (unlocked.isNotEmpty()) _toasts.value = _toasts.value + unlocked
    }

    private fun checkAchievementsGameOver(ctx: AchievementContext) {
        val unlocked = achievementChecker.checkGameOver(ctx)
        if (unlocked.isNotEmpty()) _toasts.value = _toasts.value + unlocked
    }

    fun clearToasts() { _toasts.value = emptyList() }

    private var gameOverProcessed = false

    fun onGameOver() {
        if (gameOverProcessed) return
        gameOverProcessed = true
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

        _accuracy.value = acc
        _medal.value = when {
            engine.score >= 1000 -> "🥇"
            engine.score >= 600 -> "🥈"
            engine.score >= 300 -> "🥉"
            else -> ""
        }

        val perfectGame = engine.totalAnswered >= 10 && acc == 100
        val sharpshooter = engine.totalAnswered >= 10 && acc >= 90
        checkAchievementsGameOver(AchievementContext(
            gameOver = true, maxCombo = engine.maxCombo, score = engine.score,
            gameMode = mode, newRecord = _newRecord.value,
            perfectGame = perfectGame, sharpshooter = sharpshooter,
            fiftyFiftyUsed = engine.ctxFiftyFiftyUsed, lifeRecovered = engine.ctxLifeRecovered
        ))

        missionRepo.checkOnGameOver(mode, engine.maxCombo, engine.totalAnswered, engine.category, engine.correctCount, engine.score)

        _xpGained.value = progressRepo.getXP() - engine.startXP
        _xpBreakdown.value = buildGameXpBreakdown()

        checkRankUp(engine.startRankIndex)

        _homePreload = null
        _profileData = null

        if (engine.mode == GameMode.QUICK && engine.totalAnswered >= Constants.QUICK_MODE_QUESTIONS) {
            if (engine.correctCount == engine.totalAnswered) {
                _quickRewardPowerUp.value?.let { reward ->
                    val current = prefs.getFreePowerUps().toMutableList()
                    current.add(reward)
                    prefs.setFreePowerUps(current)
                    _quickRewardEarned.value = true
                }
            } else {
                _quickRewardMissed.value = true
            }
        }

        val chest = chestSystem.generateChest(_newRecord.value, acc, engine.totalAnswered, engine.score)
        _chestReward.value = chest
    }

    private fun buildGameXpBreakdown(): XpBreakdown {
        val lines = mutableListOf<XpLine>()
        val multiplierApplied = engine.xpMultiplier > 1

        if (engine.xpFromCorrect > 0) {
            lines.add(XpLine(
                icon = "✓",
                label = "Aciertos (${engine.correctCount})",
                value = engine.xpFromCorrect,
                color = AccentLight
            ))
        }
        if (engine.xpFromLawMastery > 0) {
            val label = if (engine.lawsMasteredThisGame > 1)
                "Leyes dominadas (${engine.lawsMasteredThisGame})"
            else "Ley dominada"
            lines.add(XpLine(
                icon = "📚",
                label = label,
                value = engine.xpFromLawMastery,
                color = Success
            ))
        }
        missionRepo.getSessionCompletedMissions().forEach { m ->
            lines.add(XpLine(
                icon = m.icon,
                label = "Misión: ${m.text.take(28)}${if (m.text.length > 28) "…" else ""}",
                value = m.reward,
                color = PrimaryLight
            ))
        }
        if (multiplierApplied) {
            lines.add(XpLine(
                icon = "×${engine.xpMultiplier}",
                label = "Multiplicador",
                value = 0,
                color = Warning
            ))
        }
        return XpBreakdown(lines = lines, total = _xpGained.value, multiplierApplied = multiplierApplied)
    }

    private fun buildExamXpBreakdown(correct: Int, total: Int): XpBreakdown {
        val lines = mutableListOf<XpLine>()
        if (correct > 0) {
            lines.add(XpLine(
                icon = "✓",
                label = "Aciertos ($correct/$total)",
                value = correct * 10,
                color = AccentLight
            ))
        }
        missionRepo.getSessionCompletedMissions().forEach { m ->
            lines.add(XpLine(
                icon = m.icon,
                label = "Misión: ${m.text.take(28)}${if (m.text.length > 28) "…" else ""}",
                value = m.reward,
                color = PrimaryLight
            ))
        }
        return XpBreakdown(lines = lines, total = _xpGained.value, multiplierApplied = false)
    }

    private fun buildSimulacroXpBreakdown(points: Float, correct: Int): XpBreakdown {
        val lines = mutableListOf<XpLine>()
        val xp = (points * 10).toInt().coerceAtLeast(0)
        if (xp > 0) {
            lines.add(XpLine(
                icon = "🎯",
                label = "Puntuación ($correct aciertos)",
                value = xp,
                color = AccentLight
            ))
        }
        missionRepo.getSessionCompletedMissions().forEach { m ->
            lines.add(XpLine(
                icon = m.icon,
                label = "Misión: ${m.text.take(28)}${if (m.text.length > 28) "…" else ""}",
                value = m.reward,
                color = PrimaryLight
            ))
        }
        return XpBreakdown(lines = lines, total = _xpGained.value, multiplierApplied = false)
    }

    private fun checkRankUp(rankBefore: Int) {
        val rankAfter = progressRepo.getRankIndex()
        if (rankAfter > rankBefore) {
            val allRewards = mutableListOf<String>()
            for (r in (rankBefore + 1)..rankAfter) {
                val rewards = com.opoleyes.data.Constants.RANK_POWERUP_REWARDS[r]
                if (rewards != null) {
                    allRewards.addAll(rewards)
                    val current = prefs.getFreePowerUps().toMutableList()
                    current.addAll(rewards)
                    prefs.setFreePowerUps(current)
                }
            }
            _rankUpOverlay.value = RankUpOverlay(
                com.opoleyes.data.Constants.getRankByIndex(rankBefore),
                com.opoleyes.data.Constants.getRankByIndex(rankAfter),
                allRewards
            )
        }
    }

    fun openChest() {
        _chestReward.value?.let { chestSystem.openChest(it) }
    }

    fun clearChest() { _chestReward.value = null }
    fun clearRankUp() { _rankUpOverlay.value = null }
    fun clearPopups() { _popups.value = emptyList() }

    fun exitGame() {
        engine.saveRemainingPowerUps()
    }

    // --- Encapsulated engine access for UI ---
    // The UI must never mutate engine state directly; it goes through these methods.
    // `engine`/`examEngine` remain public only to allow tests to set up state.

    /** @return true if the game is over after the tick (timer reached 0). */
    fun tickTimer(): Boolean {
        engine.timer = (engine.timer - 1f).coerceAtLeast(0f)
        updateUiState()
        return engine.timer <= 0
    }

    /** Adjusts the timer after the app was paused; @return true if game over. */
    fun applyPausedElapsed(seconds: Float): Boolean {
        engine.timer = (engine.timer - seconds).coerceAtLeast(0f)
        updateUiState()
        return engine.timer <= 0
    }

    fun isTimedMode(): Boolean =
        engine.mode == GameMode.TIMETRIAL

    fun getMode(): GameMode = engine.mode
    fun getCategory(): String = engine.category
    fun getExamQuestions(): List<ExamEngine.ExamQuestion> = examEngine.getQuestions()
    fun getMaxExamQuestions(): Int = progressRepo.getMaxExamQuestions()
    fun getSimulacroHistory(): List<SimulacroHistoryEntry> = progressRepo.getSimulacroHistory()
    val examQuestionPresets = com.opoleyes.data.local.PreferencesManager.EXAM_QUESTION_PRESETS
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
    val shieldActive: Boolean = false,
    val powerUpUsedThisQuestion: Boolean = false,
    val currentQ: QuestionEntry? = null,
    val mode: GameMode = GameMode.SURVIVAL,
    val totalAnswered: Int = 0,
    val correctCount: Int = 0
)
