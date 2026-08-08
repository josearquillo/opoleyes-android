package com.opoleyes

import com.opoleyes.data.IGameRepository
import com.opoleyes.data.IPreferencesManager
import com.opoleyes.data.IProgressRepository
import com.opoleyes.data.IStatsRepository
import com.opoleyes.data.model.MissionData
import com.opoleyes.data.model.QuestionEntry
import com.opoleyes.data.model.QuestionStat
import com.opoleyes.data.model.SimulacroHistoryEntry

object TestFakes {

    fun makeQuestion(
        correct: String = "A",
        enunciado: String = "Test question",
        opciones: Map<String, String> = mapOf("A" to "Option A", "B" to "Option B", "C" to "Option C", "D" to "Option D"),
        testId: String = "test1",
        origId: String = "1"
    ): QuestionEntry = QuestionEntry(
        enunciado = enunciado,
        opciones = opciones,
        correct = correct,
        weight = 50,
        testId = testId,
        origId = origId
    )

    fun makePool(n: Int = 20): List<QuestionEntry> = List(n) { i ->
        makeQuestion(
            correct = listOf("A", "B", "C", "D")[i % 4],
            origId = i.toString(),
            enunciado = "Question $i"
        )
    }
}

class FakeGameRepository(
    private val pool: List<QuestionEntry> = TestFakes.makePool()
) : IGameRepository {
    override fun startTemaGame(testId: String) = pool
    override fun startAllLawsGame() = pool
    override fun startQuickGame() = pool.take(5)
}

class FakeStatsRepository : IStatsRepository {
    val statsMap = mutableMapOf<String, QuestionStat>()
    override fun getStats(): Map<String, QuestionStat> = statsMap
    override fun updateStat(key: String, isCorrect: Boolean) {
        val s = statsMap.getOrPut(key) { QuestionStat(0, 0) }
        statsMap[key] = if (isCorrect) QuestionStat(s.correct + 1, s.wrong) else QuestionStat(s.correct, s.wrong + 1)
    }
    override fun getLeyProgress(testId: String) = 0
}

class FakeProgressRepository : IProgressRepository {
    var xp = 0
    // Default to Aprendiz (rank 2) so game-engine tests get the full mechanics:
    // 4 options, 3 lives, and all power-ups available. Tests that need a
    // different rank (e.g. Novato onboarding) can override _rankIndex.
    var _rankIndex = 2
    val unlocked = mutableSetOf<String>()
    override fun getXP() = xp
    override fun addXP(amount: Int): Int { xp += amount; return xp }
    override fun getRankIndex() = _rankIndex
    override fun isUnlocked(feature: String) = feature in unlocked
}

class FakePreferencesManager : IPreferencesManager {
    var _debugMode = false
    val masteredLaws = mutableSetOf<String>()
    var xp = 0
    var _multiplier = 1
    private val statsMap = mutableMapOf<String, QuestionStat>()
    private var gamesPlayed = 0
    private val records = mutableMapOf<String, Int>()
    private val comboRecords = mutableMapOf<String, Int>()
    private val accRecords = mutableMapOf<String, Int>()
    private val achievements = mutableMapOf<String, Long>()
    private var maxExamQuestions = 10
    private var simulacroUnlocked = false
    private val simulacroHistory = mutableListOf<SimulacroHistoryEntry>()
    private var lastKnownRankIndex = 0
    private val introShown = mutableSetOf<String>()
    private var dailyMissions: MissionData? = null

    override fun getXP() = xp
    override fun addXP(amount: Int): Int { xp += amount; return xp }
    override fun isDebugMode() = _debugMode
    override fun setDebugMode(enabled: Boolean) { _debugMode = enabled }
    override fun isLawMastered(testId: String) = testId in masteredLaws
    override fun setLawMastered(testId: String) { masteredLaws.add(testId) }
    override fun getMultiplier() = _multiplier
    override fun setMultiplier(value: Int) { _multiplier = value }
    override fun resetAll() {
        _debugMode = false; masteredLaws.clear(); xp = 0; _multiplier = 1
        statsMap.clear(); gamesPlayed = 0; records.clear(); comboRecords.clear()
        accRecords.clear(); achievements.clear(); maxExamQuestions = 10
        simulacroUnlocked = false; simulacroHistory.clear(); lastKnownRankIndex = 0
        dailyMissions = null; introShown.clear()
    }
    override fun getStats(): Map<String, QuestionStat> = statsMap.toMap()
    override fun saveStats(stats: Map<String, QuestionStat>) { statsMap.clear(); statsMap.putAll(stats) }
    override fun getGamesPlayed() = gamesPlayed
    override fun incrementGamesPlayed(): Int { gamesPlayed++; return gamesPlayed }
    override fun getRecord(mode: String) = records[mode] ?: 0
    override fun setRecord(mode: String, value: Int) { records[mode] = value }
    override fun getRecordCombo(mode: String) = comboRecords[mode] ?: 0
    override fun setRecordCombo(mode: String, value: Int) { comboRecords[mode] = value }
    override fun getRecordAcc(mode: String) = accRecords[mode] ?: 0
    override fun setRecordAcc(mode: String, value: Int) { accRecords[mode] = value }
    override fun getAchievements(): Map<String, Long> = achievements.toMap()
    override fun saveAchievements(achievements: Map<String, Long>) {
        this.achievements.clear(); this.achievements.putAll(achievements)
    }
    override fun getMaxExamQuestions() = maxExamQuestions
    override fun setMaxExamQuestions(value: Int) { maxExamQuestions = value }
    override fun isSimulacroUnlocked() = simulacroUnlocked
    override fun setSimulacroUnlocked() { simulacroUnlocked = true }
    override fun getSimulacroHistory(): List<SimulacroHistoryEntry> = simulacroHistory.toList()
    override fun addSimulacroHistory(entry: SimulacroHistoryEntry) { simulacroHistory.add(entry) }
    override fun getLastKnownRankIndex() = lastKnownRankIndex
    override fun setLastKnownRankIndex(index: Int) { lastKnownRankIndex = index }
    override fun getDailyMissions(): MissionData? = dailyMissions
    override fun saveDailyMissions(data: MissionData) { dailyMissions = data }
    override fun isIntroShown(key: String) = key in introShown
    override fun setIntroShown(key: String) { introShown.add(key) }
}
