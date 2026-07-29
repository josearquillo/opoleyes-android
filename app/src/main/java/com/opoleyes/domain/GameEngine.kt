package com.opoleyes.domain

import android.content.Context
import com.opoleyes.data.Constants
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.model.QuestionEntry
import com.opoleyes.data.repository.GameRepository
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository

class GameEngine(private val context: Context) {
    private val gameRepo = GameRepository(context)
    private val statsRepo = StatsRepository(context)
    private val progressRepo = ProgressRepository(context)
    private val prefs = PreferencesManager(context)

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

    var fiftyFiftyCharges: Int = 0
    var fiftyFiftyActive: Boolean = false
    var fiftyFiftyRemoved: List<String> = emptyList()
    var doubleScoreCharges: Int = 0
    var doubleScoreActive: Boolean = false
    var hintCharges: Int = 0
    var hintActive: Boolean = false
    var hintRemoved: List<String> = emptyList()
    var shieldCharges: Int = 0
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
        shieldCharges = 0; ctxFiftyFiftyUsed = false; ctxLifeRecovered = false
        powerUpUsedThisQuestion = false
        startRankIndex = progressRepo.getRankIndex()
        startXP = progressRepo.getXP()

        val freePowerUps = prefs.getFreePowerUps()
        for (pu in freePowerUps) {
            when (pu) {
                "shield" -> shieldCharges++
                "fiftyFifty" -> fiftyFiftyCharges++
                "hint" -> hintCharges++
                "doubleScore" -> doubleScoreCharges++
            }
        }
        if (!prefs.isDebugMode()) {
            prefs.clearFreePowerUps()
        }

        when (mode) {
            GameMode.TIMETRIAL -> { timer = 180f; lives = 0 }
            GameMode.CHALLENGE -> { lives = 0; timer = 120f }
            else -> { lives = 3; timer = 0f }
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

    fun startChallengeGame(): Boolean {
        mode = GameMode.CHALLENGE; category = ""
        pool = gameRepo.startAllLawsGame()
        if (pool.isEmpty()) return false
        initGameStats()
        return true
    }

    fun nextQuestion(): Boolean {
        if (mode == GameMode.SURVIVAL && lives <= 0) return false
        if (mode == GameMode.QUICK && (lives <= 0 || questionNum >= Constants.QUICK_MODE_QUESTIONS)) return false
        if (mode == GameMode.TIMETRIAL && timer <= 0) return false
        if (mode == GameMode.CHALLENGE && (questionNum >= 15 || timer <= 0)) return false

        val available = pool.filter { !askedIds.contains("${it.testId}:${it.origId}") }
        val usePool = if (available.isNotEmpty()) available else {
            askedIds.clear()
            pool
        }
        val tw = usePool.sumOf { it.weight }
        currentQ = if (tw == 0) {
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
        askedIds.add("${currentQ!!.testId}:${currentQ!!.origId}")
        answered = false; selectedOption = null; questionNum++
        fiftyFiftyActive = false; fiftyFiftyRemoved = emptyList()
        hintActive = false; hintRemoved = emptyList()
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
                comboBarFill = minOf(1f, comboBarFill + 0.1f)
                if (comboBarFill >= 1f) {
                    comboOverchargeActive = true; comboOverchargeCharges = 3; comboBarFill = 0f
                }
            } else {
                comboOverchargeCharges--
                if (mode == GameMode.SURVIVAL || mode == GameMode.QUICK) {
                    if (lives < 3) lives++
                } else {
                    timer = minOf(300f, timer + 30f)
                }
                if (comboOverchargeCharges <= 0) comboOverchargeActive = false
            }

            var pts = 10 * combo
            if (doubleScoreActive) { pts *= 2; doubleScoreActive = false }
            score += pts
            correctCount++
            progressRepo.addXP(pts)

            streak++
            if (streak > 0 && streak % 5 == 0) {
                val lifeRecoveryUnlocked = progressRepo.isUnlocked("lifeRecovery")
                if (mode == GameMode.SURVIVAL && lifeRecoveryUnlocked) {
                    if (lives < 3) {
                        lives++; ctxLifeRecovered = true
                    } else {
                        fiftyFiftyCharges++
                    }
                } else if (mode == GameMode.TIMETRIAL || mode == GameMode.CHALLENGE) {
                    timer = minOf(300f, timer + 20f)
                }
                if (mode == GameMode.TIMETRIAL) fiftyFiftyCharges++
                if (streak % 15 == 0 && mode != GameMode.CHALLENGE && mode != GameMode.QUICK) doubleScoreCharges++
            }

            if (mode == GameMode.TIMETRIAL || mode == GameMode.CHALLENGE) {
                timer = minOf(300f, timer + 15f)
            }

            if (q.testId.isNotEmpty()) {
                val newPct = statsRepo.getLeyProgress(q.testId)
                if (newPct >= 100 && !prefs.isLawMastered(q.testId)) {
                    prefs.setLawMastered(q.testId)
                    progressRepo.addXP(200)
                }
            }

            return AnswerResult.CORRECT
        } else {
            if (shieldCharges > 0) {
                shieldCharges--
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
        if (doubleScoreCharges <= 0 || doubleScoreActive || answered || powerUpUsedThisQuestion) return
        doubleScoreCharges--; doubleScoreActive = true
        powerUpUsedThisQuestion = true
    }

    fun useHint() {
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
        if (mode == GameMode.CHALLENGE && (questionNum >= 15 || timer <= 0)) return true
        return false
    }

    fun getAccuracy(): Int =
        if (totalAnswered > 0) correctCount * 100 / totalAnswered else 0

    fun saveRemainingPowerUps() {
        if (prefs.isDebugMode()) return
        val remaining = mutableListOf<String>()
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
