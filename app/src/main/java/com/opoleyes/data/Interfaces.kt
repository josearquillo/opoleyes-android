package com.opoleyes.data

import com.opoleyes.data.model.QuestionEntry
import com.opoleyes.data.model.QuestionStat

interface IGameRepository {
    fun startTemaGame(testId: String): List<QuestionEntry>
    fun startAllLawsGame(): List<QuestionEntry>
    fun startQuickGame(): List<QuestionEntry>
}

interface IStatsRepository {
    fun getStats(): Map<String, QuestionStat>
    fun updateStat(key: String, isCorrect: Boolean)
    fun getLeyProgress(testId: String): Int
}

interface IProgressRepository {
    fun getXP(): Int
    fun addXP(amount: Int): Int
    fun getRankIndex(): Int
    fun isUnlocked(feature: String): Boolean
}

interface IPreferencesManager {
    fun getXP(): Int
    fun addXP(amount: Int): Int
    fun isDebugMode(): Boolean
    fun setDebugMode(enabled: Boolean)
    fun isLawMastered(testId: String): Boolean
    fun setLawMastered(testId: String)
    fun getMultiplier(): Int
    fun setMultiplier(value: Int)
    fun resetAll()
}
