package com.opoleyes.domain

import android.content.Context
import com.opoleyes.data.Constants
import com.opoleyes.data.IGameRepository
import com.opoleyes.data.IPreferencesManager
import com.opoleyes.data.IProgressRepository
import com.opoleyes.data.IStatsRepository
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.model.QuestionEntry
import com.opoleyes.data.repository.GameRepository
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository

class GameEngine private constructor(
    private val context: Context?,
    private val gameRepo: IGameRepository,
    private val statsRepo: IStatsRepository,
    private val progressRepo: IProgressRepository,
    private val prefs: IPreferencesManager
) {
    constructor(context: Context) : this(
        context,
        GameRepository(context),
        StatsRepository(context),
        ProgressRepository(context),
        PreferencesManager(context)
    )

    companion object {
        fun createForTest(
            gameRepo: IGameRepository,
            statsRepo: IStatsRepository,
            progressRepo: IProgressRepository,
            prefs: IPreferencesManager
        ) = GameEngine(null, gameRepo, statsRepo, progressRepo, prefs)
    }

    var mode: GameMode = GameMode.SURVIVAL
    var category: String = ""
    var pool: List<QuestionEntry> = emptyList()
    var currentQ: QuestionEntry? = null
    var askedIds: MutableSet<String> = mutableSetOf()
    var questionNum: Int = 0
    var score: Int = 0
    var combo: Int = 0
    var maxCombo: Int = 0
    var correctCount: Int = 0
    var totalAnswered: Int = 0
    var streak: Int = 0
    var lives: Int = 0
    var timer: Float = 0f
    var answered: Boolean = false
    var selectedOption: String? = null
    var comboBarFill: Float = 0f
    var comboOverchargeActive: Boolean = false
    var comboOverchargeCharges: Int = 0
    var startRankIndex: Int = 0
    var startXP: Int = 0
    var powerUpsSaved: Boolean = false
    var xpMultiplier: Int = 1
    // XP breakdown accumulators (reset in initGameStats)
    var xpFromCorrect: Int = 0
    var xpFromLawMastery: Int = 0
    var lawsMasteredThisGame: Int = 0

    // Rank-based mechanics
    var rankIndex: Int = 0
    var maxOptions: Int = 4
    var maxLives: Int = 3
    var maxDifficulty: Int = 5
    var sessionDifficultyCap: Int = 1
    var availablePowerUps: List<String> = listOf("shield", "doubleScore", "fiftyFifty", "hint")

    var fiftyFiftyCharges: Int = 0
    var fiftyFiftyActive: Boolean = false
    var fiftyFiftyRemoved: List<String> = emptyList()
    var doubleScoreCharges: Int = 0
    var doubleScoreActive: Boolean = false
    var hintCharges: Int = 0
    var hintActive: Boolean = false
    var hintRemoved: List<String> = emptyList()
    var shieldCharges: Int = 0
    var shieldActive: Boolean = false
    var ctxFiftyFiftyUsed: Boolean = false
    var ctxLifeRecovered: Boolean = false
    var powerUpUsedThisQuestion: Boolean = false

    fun initGameStats() {
        score = 0; combo = 0; maxCombo = 0; correctCount = 0; totalAnswered = 0; streak = 0
        comboBarFill = 0f; comboOverchargeActive = false; comboOverchargeCharges = 0
        answered = false; selectedOption = null; questionNum = 0
        askedIds.clear()
        fiftyFiftyCharges = 0; fiftyFiftyActive = false; fiftyFiftyRemoved = emptyList()
        doubleScoreCharges = 0; doubleScoreActive = false
        hintCharges = 0; hintActive = false; hintRemoved = emptyList()
        shieldCharges = 0; shieldActive = false; ctxFiftyFiftyUsed = false; ctxLifeRecovered = false
        powerUpUsedThisQuestion = false
        powerUpsSaved = false
        startRankIndex = progressRepo.getRankIndex()
        startXP = progressRepo.getXP()
        xpFromCorrect = 0
        xpFromLawMastery = 0
        lawsMasteredThisGame = 0

        // Configure rank-based mechanics.
        rankIndex = startRankIndex.coerceIn(0, Constants.RANKS.size - 1)
        maxOptions = Constants.MAX_OPTIONS_BY_RANK[rankIndex] ?: 4
        maxLives = Constants.MAX_LIVES_BY_RANK[rankIndex] ?: 3
        maxDifficulty = Constants.MAX_DIFFICULTY_BY_RANK[rankIndex] ?: 5
        availablePowerUps = Constants.AVAILABLE_POWERUPS_BY_RANK[rankIndex] ?: listOf("shield", "doubleScore", "fiftyFifty", "hint")
        sessionDifficultyCap = 1

        // Read and consume the pending XP multiplier from a gold chest so it
        // applies to ALL XP earned during this game, not just the first answer.
        xpMultiplier = prefs.getMultiplier()
        if (xpMultiplier > 1) prefs.setMultiplier(1)

        val freePowerUps = prefs.getFreePowerUps()
        for (pu in freePowerUps) {
            if (pu in availablePowerUps) {
                when (pu) {
                    "shield" -> shieldCharges++
                    "fiftyFifty" -> fiftyFiftyCharges++
                    "hint" -> hintCharges++
                    "doubleScore" -> doubleScoreCharges++
                }
            }
        }
        if (!prefs.isDebugMode()) {
            prefs.clearFreePowerUps()
        }

        when (mode) {
            GameMode.TIMETRIAL -> { timer = 180f; lives = 0 }
            else -> { lives = maxLives; timer = 0f }
        }
    }

    fun startQuickGame(): Boolean {
        mode = GameMode.QUICK; category = ""
        pool = gameRepo.startQuickGame()
        if (pool.isEmpty()) return false
        initGameStats()
        return true
    }

    fun startTemaGame(testId: String, gameMode: GameMode = GameMode.SURVIVAL): Boolean {
        category = testId
        mode = gameMode
        pool = gameRepo.startTemaGame(testId)
        if (pool.isEmpty()) return false
        initGameStats()
        return true
    }

    fun startAllLawsGame(gameMode: GameMode = GameMode.SURVIVAL): Boolean {
        category = ""
        mode = gameMode
        pool = gameRepo.startAllLawsGame()
        if (pool.isEmpty()) return false
        initGameStats()
        return true
    }

    fun nextQuestion(): Boolean {
        if (mode == GameMode.SURVIVAL && lives <= 0) return false
        if (mode == GameMode.QUICK && (lives <= 0 || questionNum >= Constants.QUICK_MODE_QUESTIONS)) return false
        if (mode == GameMode.TIMETRIAL && timer <= 0) return false

        val cap = minOf(sessionDifficultyCap, maxDifficulty)
        var available = pool.filter { !askedIds.contains("${it.testId}:${it.origId}") && it.difficulty <= cap }
        if (available.isEmpty()) {
            askedIds.clear()
            available = pool.filter { it.difficulty <= cap }
        }
        // Hard fallback: never serve questions above the rank's max difficulty,
        // even if the pool is exhausted at the current session cap.
        val usePool = if (available.isNotEmpty()) available else {
            askedIds.clear()
            pool.filter { it.difficulty <= maxDifficulty }.ifEmpty { pool }
        }

        currentQ = if (rankIndex <= 1) {
            // Novice / Beginner: serve easiest questions first.
            usePool.sortedBy { it.difficulty }.firstOrNull()
                ?: usePool.random()
        } else {
            val tw = usePool.sumOf { it.weight }
            if (tw == 0) {
                usePool.random()
            } else {
                var r = (0 until tw).random()
                var cum = 0
                var selected: QuestionEntry? = null
                for (item in usePool) {
                    cum += item.weight
                    if (r < cum) { selected = item; break }
                }
                selected ?: usePool.random()
            }
        }
        askedIds.add("${currentQ!!.testId}:${currentQ!!.origId}")
        answered = false; selectedOption = null; questionNum++
        fiftyFiftyActive = false; fiftyFiftyRemoved = emptyList()
        hintActive = false; hintRemoved = emptyList()
        doubleScoreActive = false
        // Shield persists across questions until the user fails (per help text)
        powerUpUsedThisQuestion = false
        return true
    }

    fun answer(letter: String): AnswerResult {
        if (answered) return AnswerResult.ALREADY_ANSWERED
        selectedOption = letter
        answered = true
        totalAnswered++
        ctxLifeRecovered = false
        val q = currentQ ?: return AnswerResult.ERROR
        val isCorrect = letter == q.correct
        val key = "${q.testId}:${q.origId}"

        if (isCorrect) {
            statsRepo.updateStat(key, true)
            combo++
            maxCombo = maxOf(maxCombo, combo)

            if (!comboOverchargeActive) {
                comboBarFill = minOf(1f, comboBarFill + 0.2f)
                if (comboBarFill >= 1f) {
                    comboOverchargeActive = true; comboOverchargeCharges = 3; comboBarFill = 0f
                }
            } else {
                comboOverchargeCharges--
                if (mode == GameMode.SURVIVAL || mode == GameMode.QUICK) {
                    if (lives < maxLives) lives++
                } else {
                    timer = minOf(300f, timer + 30f)
                }
                if (comboOverchargeCharges <= 0) comboOverchargeActive = false
            }

            var pts = if (mode == GameMode.QUICK) 15 * combo else 10 * combo
            if (doubleScoreActive) { pts *= 2; doubleScoreActive = false }
            score += pts
            correctCount++
            // Increase session difficulty cap every 5 correct answers (per plan 2.2).
            if (correctCount % 5 == 0 && sessionDifficultyCap < maxDifficulty) {
                sessionDifficultyCap++
            }
            progressRepo.addXP(pts * xpMultiplier)
            xpFromCorrect += pts * xpMultiplier

            streak++
            if (streak > 0 && streak % 5 == 0) {
                val lifeRecoveryUnlocked = progressRepo.isUnlocked("lifeRecovery")
                if (mode == GameMode.SURVIVAL && lifeRecoveryUnlocked) {
                    if (lives < maxLives) {
                        lives++; ctxLifeRecovered = true
                    } else if ("fiftyFifty" in availablePowerUps) {
                        fiftyFiftyCharges++
                    }
                } else if (mode == GameMode.TIMETRIAL) {
                    timer = minOf(300f, timer + 20f)
                }
                if (streak % 15 == 0 && mode != GameMode.QUICK && "doubleScore" in availablePowerUps) doubleScoreCharges++
            }

            if (mode == GameMode.TIMETRIAL) {
                timer = minOf(300f, timer + 15f)
            }

            if (q.testId.isNotEmpty()) {
                val newPct = statsRepo.getLeyProgress(q.testId)
                if (newPct >= 100 && !prefs.isLawMastered(q.testId)) {
                    prefs.setLawMastered(q.testId)
                    progressRepo.addXP(200 * xpMultiplier)
                    xpFromLawMastery += 200 * xpMultiplier
                    lawsMasteredThisGame++
                }
            }

            return AnswerResult.CORRECT
        } else {
            if (shieldActive) {
                shieldActive = false
                statsRepo.updateStat(key, false)
                return AnswerResult.SHIELD_USED
            }
            streak = 0
            statsRepo.updateStat(key, false)
            combo = 0; comboBarFill = 0f; comboOverchargeActive = false; comboOverchargeCharges = 0
            if (mode == GameMode.SURVIVAL || mode == GameMode.QUICK) {
                lives--
            } else {
                timer = maxOf(0f, timer - 10f)
            }
            return AnswerResult.WRONG
        }
    }

    fun activateFiftyFifty() {
        if ("fiftyFifty" !in availablePowerUps || maxOptions < 4) return
        if (fiftyFiftyCharges <= 0 || fiftyFiftyActive || answered || powerUpUsedThisQuestion) return
        val q = currentQ ?: return
        val allOptions = listOf("A", "B", "C", "D").filter { q.opciones[it] != null }
        val wrong = allOptions.filter { it != q.correct }
        // Remove exactly 2 wrong options (or fewer if not enough wrong options)
        // Always leave at least 2 options visible (correct + 1 wrong)
        val toRemove = minOf(2, wrong.size - 1).coerceAtLeast(0)
        if (toRemove <= 0) return
        val removed = wrong.shuffled().take(toRemove)
        // Hard guarantee: correct answer is never removed
        // Hard guarantee: at least 2 options remain visible
        if (allOptions.size - removed.size < 2) return
        fiftyFiftyCharges--; fiftyFiftyActive = true; ctxFiftyFiftyUsed = true
        powerUpUsedThisQuestion = true
        fiftyFiftyRemoved = removed
    }

    fun activateDoubleScore() {
        if ("doubleScore" !in availablePowerUps) return
        if (doubleScoreCharges <= 0 || doubleScoreActive || answered || powerUpUsedThisQuestion) return
        doubleScoreCharges--; doubleScoreActive = true
        powerUpUsedThisQuestion = true
    }

    fun activateShield() {
        if ("shield" !in availablePowerUps) return
        if (shieldCharges <= 0 || shieldActive || answered || powerUpUsedThisQuestion) return
        shieldCharges--; shieldActive = true
        powerUpUsedThisQuestion = true
    }

    fun useHint() {
        if ("hint" !in availablePowerUps || maxOptions < 4) return
        if (hintCharges <= 0 || hintActive || answered || powerUpUsedThisQuestion) return
        val q = currentQ ?: return
        val allOptions = listOf("A", "B", "C", "D").filter { q.opciones[it] != null }
        // Don't remove if it would leave fewer than 2 visible options
        if (allOptions.size - 1 < 2) return
        val wrong = allOptions.filter { it != q.correct }
        if (wrong.isEmpty()) return
        val remove = wrong.random()
        hintRemoved = listOf(remove)
        hintActive = true; hintCharges--
        powerUpUsedThisQuestion = true
    }

    fun isGameOver(): Boolean {
        if (mode == GameMode.SURVIVAL && lives <= 0) return true
        if (mode == GameMode.QUICK && (lives <= 0 || questionNum >= Constants.QUICK_MODE_QUESTIONS)) return true
        if (mode == GameMode.TIMETRIAL && timer <= 0) return true
        return false
    }

    fun getAccuracy(): Int =
        if (totalAnswered > 0) correctCount * 100 / totalAnswered else 0

    fun saveRemainingPowerUps() {
        if (prefs.isDebugMode()) return
        if (powerUpsSaved) return
        powerUpsSaved = true
        val remaining = mutableListOf<String>()
        if (shieldActive) remaining.add("shield")
        repeat(shieldCharges) { remaining.add("shield") }
        repeat(fiftyFiftyCharges) { remaining.add("fiftyFifty") }
        repeat(hintCharges) { remaining.add("hint") }
        repeat(doubleScoreCharges) { remaining.add("doubleScore") }
        if (remaining.isNotEmpty()) {
            val current = prefs.getFreePowerUps().toMutableList()
            current.addAll(remaining)
            prefs.setFreePowerUps(current)
        }
    }

    enum class AnswerResult { CORRECT, WRONG, SHIELD_USED, ALREADY_ANSWERED, ERROR }
}
