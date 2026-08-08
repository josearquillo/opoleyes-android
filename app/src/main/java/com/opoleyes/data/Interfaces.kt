package com.opoleyes.data

import com.opoleyes.data.model.MissionData
import com.opoleyes.data.model.QuestionEntry
import com.opoleyes.data.model.QuestionStat
import com.opoleyes.data.model.SimulacroHistoryEntry

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
    fun getStats(): Map<String, QuestionStat>
    fun saveStats(stats: Map<String, QuestionStat>)
    fun getGamesPlayed(): Int
    fun incrementGamesPlayed(): Int
    fun getRecord(mode: String): Int
    fun setRecord(mode: String, value: Int)
    fun getRecordCombo(mode: String): Int
    fun setRecordCombo(mode: String, value: Int)
    fun getRecordAcc(mode: String): Int
    fun setRecordAcc(mode: String, value: Int)
    fun getAchievements(): Map<String, Long>
    fun saveAchievements(achievements: Map<String, Long>)
    fun getMaxExamQuestions(): Int
    fun setMaxExamQuestions(value: Int)
    fun isSimulacroUnlocked(): Boolean
    fun setSimulacroUnlocked()
    fun getSimulacroHistory(): List<SimulacroHistoryEntry>
    fun addSimulacroHistory(entry: SimulacroHistoryEntry)
    fun getLastKnownRankIndex(): Int
    fun setLastKnownRankIndex(index: Int)
    fun getDailyMissions(): MissionData?
    fun saveDailyMissions(data: MissionData)
}
