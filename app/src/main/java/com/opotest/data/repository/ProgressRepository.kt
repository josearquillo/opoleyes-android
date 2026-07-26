package com.opotest.data.repository

import android.content.Context
import com.opotest.data.Constants
import com.opotest.data.local.PreferencesManager
import com.opotest.data.model.Achievement
import com.opotest.data.model.Rank
import com.opotest.data.model.XPProgress

class ProgressRepository(private val context: Context) {
    private val prefs = PreferencesManager(context)

    fun getXP(): Int = prefs.getXP()
    fun addXP(amount: Int): Int = prefs.addXP(amount)

    fun getRankIndex(): Int {
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
        val r = getRankIndex()
        return Unlocks(
            survival = true,
            timetrial = r >= 1,
            quick = r >= 2,
            challenge = r >= 4,
            powerUps = r >= 3,
            hint = r >= 2,
            shield = r >= 1,
            fiftyFifty = r >= 3,
            lifeRecovery = r >= 4,
            doubleScore = r >= 5,
            freezeTime = r >= 6,
            dailyMissions = if (r >= 8) 3 else if (r >= 2) 2 else 1
        )
    }

    fun isUnlocked(feature: String): Boolean {
        val u = getUnlocks()
        return when (feature) {
            "survival" -> u.survival
            "timetrial" -> u.timetrial
            "quick" -> u.quick
            "challenge" -> u.challenge
            "powerUps" -> u.powerUps
            "hint" -> u.hint
            "shield" -> u.shield
            "fiftyFifty" -> u.fiftyFifty
            "lifeRecovery" -> u.lifeRecovery
            "doubleScore" -> u.doubleScore
            "freezeTime" -> u.freezeTime
            else -> false
        }
    }

    fun getMissionCount(): Int = getUnlocks().dailyMissions

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
    fun getTrainingsDone(): Int = prefs.getTrainingsDone()
    fun incrementTrainingsDone(): Int = prefs.incrementTrainingsDone()

    fun getRecord(mode: String): Int = prefs.getRecord(mode)
    fun setRecord(mode: String, value: Int) = prefs.setRecord(mode, value)
    fun getRecordCombo(mode: String): Int = prefs.getRecordCombo(mode)
    fun setRecordCombo(mode: String, value: Int) = prefs.setRecordCombo(mode, value)
    fun getRecordAcc(mode: String): Int = prefs.getRecordAcc(mode)
    fun setRecordAcc(mode: String, value: Int) = prefs.setRecordAcc(mode, value)

    fun getMaxComboRecord(): Int {
        val modes = listOf("survival", "timetrial", "quick", "challenge")
        return modes.maxOf { getRecordCombo(it) }
    }

    fun resetAll() = prefs.resetAll()
}

data class Unlocks(
    val survival: Boolean,
    val timetrial: Boolean,
    val quick: Boolean,
    val challenge: Boolean,
    val powerUps: Boolean,
    val hint: Boolean,
    val shield: Boolean,
    val fiftyFifty: Boolean,
    val lifeRecovery: Boolean,
    val doubleScore: Boolean,
    val freezeTime: Boolean,
    val dailyMissions: Int
)
