package com.opoleyes

import com.opoleyes.data.IGameRepository
import com.opoleyes.data.IPreferencesManager
import com.opoleyes.data.IProgressRepository
import com.opoleyes.data.IStatsRepository
import com.opoleyes.data.model.QuestionEntry
import com.opoleyes.data.model.QuestionStat

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
    var _rankIndex = 0
    val unlocked = mutableSetOf<String>()
    override fun getXP() = xp
    override fun addXP(amount: Int): Int { xp += amount; return xp }
    override fun getRankIndex() = _rankIndex
    override fun isUnlocked(feature: String) = feature in unlocked
}

class FakePreferencesManager : IPreferencesManager {
    val _freePowerUps = mutableListOf<String>()
    var _debugMode = false
    val masteredLaws = mutableSetOf<String>()
    var xp = 0
    override fun getXP() = xp
    override fun addXP(amount: Int): Int { xp += amount; return xp }
    override fun getFreePowerUps() = _freePowerUps.toList()
    override fun setFreePowerUps(list: List<String>) { _freePowerUps.clear(); _freePowerUps.addAll(list) }
    override fun clearFreePowerUps() { _freePowerUps.clear() }
    override fun isDebugMode() = _debugMode
    override fun setDebugMode(enabled: Boolean) { _debugMode = enabled }
    override fun isLawMastered(testId: String) = testId in masteredLaws
    override fun setLawMastered(testId: String) { masteredLaws.add(testId) }
    override fun resetAll() { _freePowerUps.clear(); _debugMode = false; masteredLaws.clear(); xp = 0 }
}
