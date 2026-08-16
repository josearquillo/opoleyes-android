package com.opoleyes.data.repository

import android.content.Context
import com.opoleyes.data.Constants
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.Achievement
import com.opoleyes.data.model.Rank
import com.opoleyes.data.model.SimulacroHistoryEntry
import com.opoleyes.data.model.XPProgress

open class ProgressRepository(
    private val prefs: com.opoleyes.data.IPreferencesManager
) : com.opoleyes.data.IProgressRepository {

    constructor(context: Context) : this(PreferencesManager(context))

    override fun getXP(): Int = if (prefs.isDebugMode()) 100000 else prefs.getXP()
    override fun addXP(amount: Int): Int {
        if (prefs.isDebugMode()) return 100000
        return prefs.addXP(amount)
    }

    override fun getRankIndex(): Int {
        val xp = getXP()
        return getRankIndexForXP(xp)
    }

    fun getRankIndexForXP(xp: Int): Int {
        for (i in Constants.RANKS.indices.reversed()) {
            if (xp >= Constants.RANKS[i].xp) return i
        }
        return 0
    }

    fun getRank(): Rank = Constants.getRankByIndex(getRankIndex())

    fun getXPProgress(): XPProgress = getXPProgressFor(getXP())

    fun getXPProgressFor(xp: Int): XPProgress {
        val idx = getRankIndexForXP(xp)
        val curXp = Constants.RANKS[idx].xp
        if (idx >= Constants.RANKS.size - 1) return XPProgress(100, xp - curXp, 0, curXp)
        val nextXp = Constants.RANKS[idx + 1].xp
        val intoRank = xp - curXp
        val rankSpan = nextXp - curXp
        val pct = minOf(100, intoRank * 100 / rankSpan)
        return XPProgress(pct, intoRank, rankSpan, nextXp)
    }

    fun getUnlocks(): Unlocks {
        if (prefs.isDebugMode()) {
            return Unlocks(
                survival = true,
                timetrial = true,
                quick = true,
                exam = true,
                simulacro = true,
                powerUps = true,
                hint = true,
                shield = false,
                fiftyFifty = true,
                lifeRecovery = true,
                doubleScore = false,
                dailyMissions = 3
            )
        }
        val r = getRankIndex()
        return Unlocks(
            survival = true,
            timetrial = r >= 3,
            quick = r >= 5,
            exam = r >= 7,
            simulacro = isSimulacroUnlocked(),
            powerUps = true,
            hint = r >= 1,
            shield = false,
            fiftyFifty = true,
            lifeRecovery = true,
            doubleScore = false,
            dailyMissions = if (r >= 2) 3 else 2
        )
    }

    override fun isUnlocked(feature: String): Boolean {
        val u = getUnlocks()
        return when (feature) {
            "survival" -> u.survival
            "timetrial" -> u.timetrial
            "quick" -> u.quick
            "exam" -> u.exam
            "simulacro" -> u.simulacro
            "powerUps" -> u.powerUps
            "hint" -> u.hint
            "shield" -> u.shield
            "fiftyFifty" -> u.fiftyFifty
            "lifeRecovery" -> u.lifeRecovery
            "doubleScore" -> u.doubleScore
            else -> false
        }
    }

    fun getMissionCount(): Int = getUnlocks().dailyMissions

    fun getMaxExamQuestions(): Int = if (prefs.isDebugMode()) 50 else prefs.getMaxExamQuestions()

    fun unlockNextExamQuestions() {
        val current = prefs.getMaxExamQuestions()
        val presets = PreferencesManager.EXAM_QUESTION_PRESETS
        val idx = presets.indexOf(current)
        if (idx >= 0 && idx < presets.size - 1) {
            prefs.setMaxExamQuestions(presets[idx + 1])
        }
    }

    fun isSimulacroUnlocked(): Boolean = if (prefs.isDebugMode()) true else prefs.isSimulacroUnlocked()

    fun unlockSimulacro() {
        if (!prefs.isSimulacroUnlocked()) {
            prefs.setSimulacroUnlocked()
        }
    }

    fun getSimulacroHistory(): List<SimulacroHistoryEntry> = prefs.getSimulacroHistory()

    fun addSimulacroHistory(entry: SimulacroHistoryEntry) {
        prefs.addSimulacroHistory(entry)
    }

    fun getAchievements(): Map<String, Long> = prefs.getAchievements()

    fun unlockAchievement(id: String): Achievement? {
        val ach = getAchievements().toMutableMap()
        if (ach.containsKey(id)) return null
        ach[id] = System.currentTimeMillis()
        prefs.saveAchievements(ach)
        return Constants.ACHIEVEMENTS.find { it.id == id }
    }

    fun getGamesPlayed(): Int = prefs.getGamesPlayed()
    fun incrementGamesPlayed(): Int = prefs.incrementGamesPlayed()

    fun getRecord(mode: String): Int = prefs.getRecord(mode)
    fun setRecord(mode: String, value: Int) = prefs.setRecord(mode, value)
    fun getRecordCombo(mode: String): Int = prefs.getRecordCombo(mode)
    fun setRecordCombo(mode: String, value: Int) = prefs.setRecordCombo(mode, value)
    fun getRecordAcc(mode: String): Int = prefs.getRecordAcc(mode)
    fun setRecordAcc(mode: String, value: Int) = prefs.setRecordAcc(mode, value)

    fun getMaxComboRecord(): Int {
        val modes = listOf("survival", "timetrial", "quick")
        return modes.maxOf { getRecordCombo(it) }
    }

    fun getLastKnownRankIndex(): Int = prefs.getLastKnownRankIndex()
    fun setLastKnownRankIndex(index: Int) = prefs.setLastKnownRankIndex(index)

    fun resetAll() = prefs.resetAll()
}

data class Unlocks(
    val survival: Boolean,
    val timetrial: Boolean,
    val quick: Boolean,
    val exam: Boolean,
    val simulacro: Boolean,
    val powerUps: Boolean,
    val hint: Boolean,
    val shield: Boolean,
    val fiftyFifty: Boolean,
    val lifeRecovery: Boolean,
    val doubleScore: Boolean,
    val dailyMissions: Int
)
