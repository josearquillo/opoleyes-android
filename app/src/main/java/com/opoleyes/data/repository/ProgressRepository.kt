package com.opoleyes.data.repository

import android.content.Context
import com.opoleyes.data.Constants
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.Achievement
import com.opoleyes.data.model.Rank
import com.opoleyes.data.model.XPProgress

open class ProgressRepository(private val context: Context) : com.opoleyes.data.IProgressRepository {
    private val prefs = PreferencesManager(context)

    override fun getXP(): Int = if (prefs.isDebugMode()) 100000 else prefs.getXP()
    override fun addXP(amount: Int): Int = prefs.addXP(amount)

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
                shield = true,
                fiftyFifty = true,
                lifeRecovery = true,
                doubleScore = true,
                dailyMissions = 3
            )
        }
        val r = getRankIndex()
        return Unlocks(
            survival = true,
            timetrial = r >= 1,
            quick = r >= 3,
            exam = r >= 5,
            simulacro = isSimulacroUnlocked(),
            powerUps = true,
            hint = true,
            shield = true,
            fiftyFifty = true,
            lifeRecovery = true,
            doubleScore = true,
            dailyMissions = if (r >= 4) 3 else if (r >= 2) 2 else 1
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

    fun getMaxExamQuestions(): Int = prefs.getMaxExamQuestions()

    fun unlockNextExamQuestions() {
        val current = prefs.getMaxExamQuestions()
        val presets = prefs.EXAM_QUESTION_PRESETS
        val idx = presets.indexOf(current)
        if (idx >= 0 && idx < presets.size - 1) {
            prefs.setMaxExamQuestions(presets[idx + 1])
        }
    }

    fun isSimulacroUnlocked(): Boolean = prefs.isSimulacroUnlocked()

    fun unlockSimulacro() {
        if (!prefs.isSimulacroUnlocked()) {
            prefs.setSimulacroUnlocked()
        }
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
