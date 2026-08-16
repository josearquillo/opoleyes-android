package com.opoleyes.ui.navigation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opoleyes.data.IPreferencesManager
import com.opoleyes.data.model.*
import com.opoleyes.data.Constants
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
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
import com.opoleyes.ui.theme.Orange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class GameViewModel private constructor(
    private val progressRepo: ProgressRepository,
    private val statsRepo: StatsRepository,
    private val missionRepo: MissionRepository,
    private val achievementChecker: AchievementChecker,
    private val chestSystem: ChestSystem,
    private val prefs: IPreferencesManager,
    val engine: GameEngine,
    val examEngine: ExamEngine,
    private val temaTestsProvider: () -> List<com.opoleyes.data.model.Test>
) : ViewModel() {

    constructor(application: Application) : this(
        ProgressRepository(application),
        StatsRepository(application),
        MissionRepository(application),
        AchievementChecker(application),
        ChestSystem(application),
        PreferencesManager(application),
        GameEngine(application),
        ExamEngine(application),
        { DataProvider.getTemaTests(application) }
    )

    companion object {
        fun createForTest(
            progressRepo: ProgressRepository,
            statsRepo: StatsRepository,
            missionRepo: MissionRepository,
            achievementChecker: AchievementChecker,
            chestSystem: ChestSystem,
            prefs: IPreferencesManager,
            engine: GameEngine,
            examEngine: ExamEngine,
            temaTestsProvider: () -> List<com.opoleyes.data.model.Test> = { emptyList() }
        ) = GameViewModel(
            progressRepo, statsRepo, missionRepo, achievementChecker,
            chestSystem, prefs, engine, examEngine, temaTestsProvider
        )
    }

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
        checkRankUp(progressRepo.getLastKnownRankIndex())
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
        val records: Map<String, Int>,
        val unlockedModes: Map<String, Boolean>
    )
    private var _profileData: ProfileData? = null
    val profileData: ProfileData? get() = _profileData

    fun preloadProfileData() {
        if (_profileData != null) return
        val temaTests = temaTestsProvider()
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
            records = records,
            unlockedModes = unlockedModes
        )
    }

    fun getLeyProgress(testId: String): Int = statsRepo.getLeyProgress(testId)
    fun getTemaTests(): List<com.opoleyes.data.model.Test> = temaTestsProvider()
    fun getUnlocks(): com.opoleyes.data.repository.Unlocks = progressRepo.getUnlocks()

    fun resetProgress() {
        progressRepo.resetAll()
        statsRepo.invalidateCache()
        _homePreload = null
        _profileData = null
        _rankUpOverlay.value = null
        _chestReward.value = null
        chestOpened = false
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

    // === Mode intro / onboarding ===
    // Survival gets a per-rank intro at ranks 0, 1, 2 (mechanics change).
    // Other modes get a single intro the first time they are played.
    // Simulacro has its own dedicated intro screen and is excluded here.
    fun getIntroKey(mode: GameMode, rankIndex: Int): String = when (mode) {
        GameMode.SURVIVAL -> "intro_survival_rank_${minOf(rankIndex, 2)}"
        GameMode.TIMETRIAL -> "intro_timetrial"
        GameMode.QUICK -> "intro_quick"
        GameMode.EXAM -> "intro_exam"
        GameMode.SIMULACRO -> "intro_simulacro"
    }

    fun shouldShowModeIntro(mode: GameMode): Boolean {
        if (mode == GameMode.SIMULACRO) return false
        val rankIndex = progressRepo.getRankIndex()
        return !prefs.isIntroShown(getIntroKey(mode, rankIndex))
    }

    fun dismissModeIntro(mode: GameMode, dontShowAgain: Boolean) {
        if (!dontShowAgain) return
        val rankIndex = progressRepo.getRankIndex()
        prefs.setIntroShown(getIntroKey(mode, rankIndex))
    }

    fun getEngineMode(): GameMode = engine.mode
    fun getEngineRankIndex(): Int = engine.rankIndex

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

    var lastExamQuestionCount: Int = 10
        private set

    var isRetrying = false
        private set

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

    private val _motivationalMessage = MutableStateFlow("")
    val motivationalMessage: StateFlow<String> = _motivationalMessage.asStateFlow()

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
            fiftyFiftyActive = engine.fiftyFiftyActive,
            fiftyFiftyRemoved = engine.fiftyFiftyRemoved,
            hintActive = engine.hintActive,
            hintRemoved = engine.hintRemoved,
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
        _quickRewardMissed.value = false
        gameOverProcessed = false
        _xpBreakdown.value = null
        _rankUpOverlay.value = null
        _chestReward.value = null
        chestOpened = false
        missionRepo.clearSessionCompletedMissions()
        pendingMode = GameMode.QUICK
        val ok = engine.startQuickGame()
        if (ok) {
            engine.nextQuestion(); updateUiState()
        }
        return ok
    }

    var pendingMode: GameMode = GameMode.SURVIVAL

    fun clearQuickReward() {
        _quickRewardEarned.value = false
        _quickRewardMissed.value = false
    }

    fun startTemaGame(testId: String): Boolean {
        _popups.value = emptyList()
        _toasts.value = emptyList()
        gameOverProcessed = false
        _xpBreakdown.value = null
        _rankUpOverlay.value = null
        _chestReward.value = null
        chestOpened = false
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
        _rankUpOverlay.value = null
        _chestReward.value = null
        chestOpened = false
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
        _isSimulacroMode.value = false
        _xpBreakdown.value = null
        missionRepo.clearSessionCompletedMissions()
        pendingMode = GameMode.EXAM
        lastExamQuestionCount = questionCount
        isRetrying = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) { examEngine.loadExam(questionCount) }
                _examResult.value = null
                _examQuestionNum.value = 0
                _examAnswered.value = 0
                _examTotalQuestions.value = examEngine.getQuestionCount()
                _examCurrentQuestion.value = examEngine.getCurrentQuestion()
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            } finally {
                _isLoading.value = false
                isRetrying = false
            }
        }
    }

    fun startSimulacroAsync(onDone: (Boolean) -> Unit) {
        _isLoading.value = true
        _isSimulacroMode.value = true
        _xpBreakdown.value = null
        missionRepo.clearSessionCompletedMissions()
        isRetrying = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) { examEngine.loadSimulacro() }
                _examResult.value = null
                _simulacroResult.value = null
                _examQuestionNum.value = 0
                _examAnswered.value = 0
                _examTotalQuestions.value = examEngine.getQuestionCount()
                _examCurrentQuestion.value = examEngine.getCurrentQuestion()
                _simulacroTimer.value = ExamEngine.SIMULACRO_TIME_SECONDS
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            } finally {
                _isLoading.value = false
                isRetrying = false
            }
        }
    }

    fun loadSimulacroSync() {
        _examResult.value = null
        _simulacroResult.value = null
        _isSimulacroMode.value = true
        _xpBreakdown.value = null
        missionRepo.clearSessionCompletedMissions()
        examEngine.loadSimulacro()
        _examQuestionNum.value = 0
        _examAnswered.value = 0
        _examTotalQuestions.value = examEngine.getQuestionCount()
        _examCurrentQuestion.value = examEngine.getCurrentQuestion()
        _simulacroTimer.value = ExamEngine.SIMULACRO_TIME_SECONDS
    }

    fun loadExamSync(questionCount: Int) {
        _examResult.value = null
        _isSimulacroMode.value = false
        _xpBreakdown.value = null
        missionRepo.clearSessionCompletedMissions()
        pendingMode = GameMode.EXAM
        lastExamQuestionCount = questionCount
        examEngine.loadExam(questionCount)
        _examQuestionNum.value = 0
        _examAnswered.value = 0
        _examTotalQuestions.value = examEngine.getQuestionCount()
        _examCurrentQuestion.value = examEngine.getCurrentQuestion()
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
        val multiplier = prefs.getMultiplier()
        if (multiplier > 1) prefs.setMultiplier(1)
        val rankScale = (4 + progressRepo.getRankIndex()) / 4f
        val xp = (result.correct * 10 * rankScale * multiplier).toInt()
        progressRepo.addXP(xp)
        val scorePct = if (result.total > 0) (result.correct * 100 / result.total) else 0
        missionRepo.checkExamResult(scorePct)
        val missionRewards = missionRepo.getSessionCompletedMissions().sumOf { it.reward }
        _xpGained.value = xp + missionRewards
        if (result.score >= 5.0) {
            progressRepo.unlockNextExamQuestions()
            if (result.total >= 50 && !progressRepo.isSimulacroUnlocked()) {
                progressRepo.unlockSimulacro()
            }
        }
        checkRankUp(rankBefore)
        _xpBreakdown.value = buildExamXpBreakdown(result.correct, result.total, multiplier)
        checkAchievementsGameOver(AchievementContext(
            gameOver = true, gameMode = "exam",
            examPassed = result.score >= 5.0f,
            examPerfect = result.correct == result.total
        ))
        _homePreload = null
        _profileData = null
    }

    private fun finishSimulacro() {
        val rankBefore = progressRepo.getRankIndex()
        val result = examEngine.gradeSimulacro()
        _simulacroResult.value = result
        progressRepo.incrementGamesPlayed()
        val multiplier = prefs.getMultiplier()
        if (multiplier > 1) prefs.setMultiplier(1)
        val rankScale = (4 + progressRepo.getRankIndex()) / 4f
        val xp = ((result.points * 10 * rankScale).toInt().coerceAtLeast(0)) * multiplier
        progressRepo.addXP(xp)
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
        val missionRewards = missionRepo.getSessionCompletedMissions().sumOf { it.reward }
        _xpGained.value = xp + missionRewards
        checkRankUp(rankBefore)
        _xpBreakdown.value = buildSimulacroXpBreakdown(result.points, result.correct, multiplier)
        checkAchievementsGameOver(AchievementContext(
            gameOver = true, gameMode = "simulacro",
            simulacroPassed = result.passed,
            simulacroPerfect = result.correct == result.total
        ))
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
                val pts = engine.lastPtsEarned
                val powerUpLabel = when (engine.powerUpUsedType) {
                    "fiftyFifty" -> " (50/50)"
                    "hint" -> " (Pista)"
                    else -> ""
                }
                addPopup("+$pts pts$powerUpLabel", com.opoleyes.ui.theme.AccentLight, 36, 0f, "✅")
                if (engine.combo >= 3) {
                    val comboColor = when {
                        engine.combo >= 20 -> com.opoleyes.ui.theme.Warning
                        engine.combo >= 10 -> com.opoleyes.ui.theme.Danger
                        else -> com.opoleyes.ui.theme.Orange
                    }
                    addPopup("COMBO x${engine.combo}", comboColor, 40, 0f, "🔥")
                }
                val streakThreshold = Constants.STREAK_RECOVERY_THRESHOLD_BY_RANK[engine.rankIndex] ?: 5
                if (engine.streak > 0 && engine.streak % streakThreshold == 0) {
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
                    addPopup(streakMsg, com.opoleyes.ui.theme.Warning, 38, 0f, streakIcon)
                }
                if (engine.comboOverchargeActive && engine.comboOverchargeCharges == 3) {
                    addPopup("¡OVERCHARGE!", com.opoleyes.ui.theme.Warning, 48, 0f, "⚡")
                }
                checkAchievementsPerQuestion(AchievementContext(firstCorrect = true, maxCombo = engine.maxCombo, fiftyFiftyUsed = engine.ctxFiftyFiftyUsed, lifeRecovered = engine.ctxLifeRecovered, maxOptions = engine.maxOptions))
                missionRepo.checkLiveProgress(engine.mode.name.lowercase(), engine.totalAnswered - engine.correctCount, engine.totalAnswered)
            }
            GameEngine.AnswerResult.WRONG -> {
                if (engine.ctxFirstMistakeForgiven) {
                    addPopup("¡Primer fallo sin contar! Estás aprendiendo 💪", com.opoleyes.ui.theme.Success, 38, 0f, "🛡️")
                }
                checkAchievementsPerQuestion(AchievementContext(maxCombo = engine.maxCombo, maxOptions = engine.maxOptions))
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
        _motivationalMessage.value = computeMotivationalMessage(acc, engine.totalAnswered)
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
            fiftyFiftyUsed = engine.ctxFiftyFiftyUsed, lifeRecovered = engine.ctxLifeRecovered,
            maxOptions = engine.maxOptions
        ))

        missionRepo.checkOnGameOver(mode, engine.maxCombo, engine.maxStreak, engine.totalAnswered, engine.category, engine.correctCount, engine.score, engine.powerUpsUsedCount, engine.totalAnswered - engine.correctCount)

        if (engine.mode == GameMode.QUICK && engine.totalAnswered >= Constants.QUICK_MODE_QUESTIONS) {
            if (engine.correctCount == engine.totalAnswered) {
                _quickRewardEarned.value = true
                val quickReward = 40 * (2 + engine.rankIndex) / 2
                progressRepo.addXP(quickReward)
            } else {
                _quickRewardMissed.value = true
            }
        }

        val missionRewards = missionRepo.getSessionCompletedMissions().sumOf { it.reward }
        val quickReward = if (_quickRewardEarned.value) 40 * (2 + engine.rankIndex) / 2 else 0
        _xpGained.value = engine.xpFromCorrect + engine.xpFromLawMastery + engine.xpFromConsolation + missionRewards + quickReward
        _xpBreakdown.value = buildGameXpBreakdown()

        checkRankUp(engine.startRankIndex)

        _homePreload = null
        _profileData = null

        val chest = chestSystem.generateChest(_newRecord.value, acc, engine.totalAnswered)
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
        if (engine.xpFromConsolation > 0) {
            val wrongCount = engine.totalAnswered - engine.correctCount
            lines.add(XpLine(
                icon = "💪",
                label = "Esfuerzo ($wrongCount intentos)",
                value = engine.xpFromConsolation,
                color = Orange
            ))
        }
        if (_quickRewardEarned.value) {
            val quickReward = 40 * (2 + engine.rankIndex) / 2
            lines.add(XpLine(
                icon = "⚡",
                label = "Repaso Express perfecto",
                value = quickReward,
                color = Warning
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

    private fun buildExamXpBreakdown(correct: Int, total: Int, multiplier: Int = 1): XpBreakdown {
        val lines = mutableListOf<XpLine>()
        if (correct > 0) {
            val rankScale = (3 + progressRepo.getRankIndex()) / 3f
            lines.add(XpLine(
                icon = "✓",
                label = "Aciertos ($correct/$total)",
                value = (correct * 10 * rankScale * multiplier).toInt(),
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
        if (multiplier > 1) {
            lines.add(XpLine(
                icon = "×$multiplier",
                label = "Multiplicador",
                value = 0,
                color = Warning
            ))
        }
        return XpBreakdown(lines = lines, total = _xpGained.value, multiplierApplied = multiplier > 1)
    }

    private fun buildSimulacroXpBreakdown(points: Float, correct: Int, multiplier: Int = 1): XpBreakdown {
        val lines = mutableListOf<XpLine>()
        val rankScale = (3 + progressRepo.getRankIndex()) / 3f
        val xp = ((points * 10 * rankScale).toInt().coerceAtLeast(0)) * multiplier
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
        if (multiplier > 1) {
            lines.add(XpLine(
                icon = "×$multiplier",
                label = "Multiplicador",
                value = 0,
                color = Warning
            ))
        }
        return XpBreakdown(lines = lines, total = _xpGained.value, multiplierApplied = multiplier > 1)
    }

    private fun computeMotivationalMessage(acc: Int, totalAnswered: Int): String {
        val gamesPlayed = progressRepo.getGamesPlayed()
        return when {
            gamesPlayed <= 1 -> "¡Has dado el primer paso! 🌱"
            totalAnswered == 0 -> "¡Sigue intentándolo! 💪"
            acc == 0 -> "¡Cada error te enseña algo nuevo! 💪"
            acc < 40 -> "¡Cada error te acerca al acierto! Sigue 💪"
            acc < 70 -> "¡Vas por buen camino! 🎯"
            acc < 90 -> "¡Lo estás dominando! ⚡"
            else -> "¡Excelente precisión! 🏆"
        }
    }

    private fun checkRankUp(rankBefore: Int) {
        val rankAfter = progressRepo.getRankIndex()
        if (rankAfter > rankBefore) {
            progressRepo.setLastKnownRankIndex(rankAfter)
            _rankUpOverlay.value = RankUpOverlay(
                com.opoleyes.data.Constants.getRankByIndex(rankBefore),
                com.opoleyes.data.Constants.getRankByIndex(rankAfter)
            )
        }
    }

    private var chestOpened = false

    fun openChest() {
        if (chestOpened) return
        chestOpened = true
        _chestReward.value?.let {
            chestSystem.openChest(it)
            _xpGained.value = _xpGained.value + it.xp
            _xpBreakdown.value = _xpBreakdown.value?.let { breakdown ->
                val lines = breakdown.lines.toMutableList()
                lines.add(XpLine(
                    icon = it.type.icon,
                    label = "Cofre ${it.type.label}",
                    value = it.xp,
                    color = Warning
                ))
                XpBreakdown(lines = lines, total = _xpGained.value, multiplierApplied = breakdown.multiplierApplied)
            }
            checkRankUp(engine.startRankIndex)
        }
    }

    fun clearChest() { _chestReward.value = null; chestOpened = false }
    fun clearRankUp() { _rankUpOverlay.value = null }
    fun clearPopups() { _popups.value = emptyList() }

    fun exitGame() {
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
    val fiftyFiftyActive: Boolean = false,
    val fiftyFiftyRemoved: List<String> = emptyList(),
    val hintActive: Boolean = false,
    val hintRemoved: List<String> = emptyList(),
    val powerUpUsedThisQuestion: Boolean = false,
    val currentQ: QuestionEntry? = null,
    val mode: GameMode = GameMode.SURVIVAL,
    val totalAnswered: Int = 0,
    val correctCount: Int = 0
)
