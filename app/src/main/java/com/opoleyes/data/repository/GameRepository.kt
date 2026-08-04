package com.opoleyes.data.repository

import android.content.Context
import com.opoleyes.data.Constants
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.model.QuestionEntry
import com.opoleyes.data.model.TestData

open class GameRepository(private val context: Context) : com.opoleyes.data.IGameRepository {
    private val statsRepo = StatsRepository(context)
    private val prefs = PreferencesManager(context)

    private fun buildPoolFromTestData(td: TestData, stats: Map<String, com.opoleyes.data.model.QuestionStat>): List<QuestionEntry> {
        val am = td.answers.associate { it.id to it.correct }
        return td.questions.mapNotNull { q ->
            val correct = am[q.id] ?: return@mapNotNull null
            val key = (q.test_id) + ":" + (q.orig_id)
            val s = stats[key]
            val difficulty = q.difficulty
            val baseWeight = (difficulty * 15) + 25
            val weight = if (s != null) {
                val attempted = s.correct + s.wrong
                if (attempted < 3) baseWeight
                else maxOf((100 * (1.0 - s.correct.toDouble() / attempted)).toInt() + (difficulty - 3) * 10, 5)
            } else baseWeight
            QuestionEntry(
                enunciado = q.enunciado,
                opciones = q.opciones,
                correct = correct,
                weight = weight,
                testId = q.test_id,
                origId = q.orig_id.toString(),
                difficulty = difficulty
            )
        }
    }

    override fun startTemaGame(testId: String): List<QuestionEntry> {
        val td = DataProvider.getTestDataMap(context)[testId] ?: return emptyList()
        return buildPoolFromTestData(td, statsRepo.getStats())
    }

    override fun startAllLawsGame(): List<QuestionEntry> {
        val stats = statsRepo.getStats()
        val pool = mutableListOf<QuestionEntry>()
        for (d in DataProvider.loadData(context)) {
            if (d.test.tema == null) continue
            pool.addAll(buildPoolFromTestData(d, stats))
        }
        return pool
    }

    override fun startQuickGame(): List<QuestionEntry> {
        val stats = statsRepo.getStats()
        val wrongPool = mutableListOf<QuestionEntry>()
        val unansweredPool = mutableListOf<QuestionEntry>()
        val correctPool = mutableListOf<QuestionEntry>()

        for (d in DataProvider.loadData(context)) {
            if (d.test.tema == null) continue
            val am = d.answers.associate { it.id to it.correct }
            for (q in d.questions) {
                val correct = am[q.id] ?: continue
                val key = (q.test_id) + ":" + (q.orig_id)
                val s = stats[key]
                val difficulty = q.difficulty
                val attempted = if (s != null) s.correct + s.wrong else 0
                val baseWeight = (difficulty * 15) + 25
                val weight = if (s != null && attempted >= 3)
                    maxOf((100 * (1.0 - s.correct.toDouble() / attempted)).toInt() + (difficulty - 3) * 10, 5)
                else baseWeight
                val entry = QuestionEntry(
                    enunciado = q.enunciado,
                    opciones = q.opciones,
                    correct = correct,
                    weight = weight,
                    testId = q.test_id,
                    origId = q.orig_id.toString(),
                    difficulty = difficulty
                )
                when {
                    s != null && s.wrong > 0 -> wrongPool.add(entry)
                    s == null -> unansweredPool.add(entry)
                    else -> correctPool.add(entry)
                }
            }
        }
        var pool = (wrongPool + unansweredPool).toMutableList()
        if (pool.size < Constants.QUICK_MODE_QUESTIONS) pool.addAll(correctPool)
        pool.shuffle()
        return pool.take(Constants.QUICK_MODE_QUESTIONS)
    }

    fun getFreePowerUps(): List<String> = prefs.getFreePowerUps()
    fun clearFreePowerUps() = prefs.clearFreePowerUps()
    fun addFreePowerUps(list: List<String>) {
        val current = prefs.getFreePowerUps().toMutableList()
        current.addAll(list)
        prefs.setFreePowerUps(current)
    }

    fun getMultiplier(): Int = prefs.getMultiplier()
    fun setMultiplier(value: Int) = prefs.setMultiplier(value)
}
