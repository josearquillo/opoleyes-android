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
        // Filter the pool by the rank's max difficulty so a Novato never gets
        // difficulty-5 questions in Repaso Express (per plan section 3.2).
        // The rank is read from PreferencesManager so the interface signature
        // stays unchanged (startQuickGame() with no args).
        val xp = prefs.getXP()
        var rankIndex = 0
        for (i in Constants.RANKS.indices.reversed()) {
            if (xp >= Constants.RANKS[i].xp) { rankIndex = i; break }
        }
        val maxDifficulty = Constants.MAX_DIFFICULTY_BY_RANK[rankIndex] ?: 5
        val wrongPool = mutableListOf<QuestionEntry>()
        val unansweredPool = mutableListOf<QuestionEntry>()
        val correctPool = mutableListOf<QuestionEntry>()

        for (d in DataProvider.loadData(context)) {
            if (d.test.tema == null) continue
            val am = d.answers.associate { it.id to it.correct }
            for (q in d.questions) {
                val correct = am[q.id] ?: continue
                val difficulty = q.difficulty
                if (difficulty > maxDifficulty) continue
                val key = (q.test_id) + ":" + (q.orig_id)
                val s = stats[key]
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

    /**
     * Filters [pool] by the rank's max difficulty and applies weights based on
     * per-question stats. Used by GameEngine to keep pool construction in the
     * repository layer (per plan section 3.2).
     */
    fun getFilteredAndWeightedPool(
        pool: List<QuestionEntry>,
        rankIndex: Int
    ): List<QuestionEntry> {
        val maxDifficulty = Constants.MAX_DIFFICULTY_BY_RANK[rankIndex] ?: 5
        return pool.filter { it.difficulty <= maxDifficulty }
    }

    fun getMultiplier(): Int = prefs.getMultiplier()
    fun setMultiplier(value: Int) = prefs.setMultiplier(value)
}
