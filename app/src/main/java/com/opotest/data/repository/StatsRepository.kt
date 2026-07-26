package com.opotest.data.repository

import android.content.Context
import com.opotest.data.local.DataProvider
import com.opotest.data.local.PreferencesManager
import com.opotest.data.model.QuestionStat

class StatsRepository(private val context: Context) {
    private val prefs = PreferencesManager(context)

    fun getStats(): Map<String, QuestionStat> = prefs.getStats()
    fun saveStats(stats: Map<String, QuestionStat>) = prefs.saveStats(stats)

    fun getWeight(key: String): Int {
        val stats = getStats()
        val s = stats[key] ?: return 50
        val attempted = s.correct + s.wrong
        if (attempted < 3) return 50
        val w = (100 * (1.0 - s.correct.toDouble() / attempted)).toInt()
        return maxOf(w, 5)
    }

    fun updateStat(key: String, isCorrect: Boolean) {
        val stats = getStats().toMutableMap()
        val s = stats.getOrPut(key) { QuestionStat() }
        stats[key] = if (isCorrect) s.copy(correct = s.correct + 1) else s.copy(wrong = s.wrong + 1)
        saveStats(stats)
    }

    fun getLeyProgress(testId: String): Int {
        val testDataMap = DataProvider.getTestDataMap(context)
        val td = testDataMap[testId] ?: return 0
        if (td.questions.isEmpty()) return 0
        val stats = getStats()
        var mastered = 0
        for (q in td.questions) {
            val key = (q.test_id) + ":" + (q.orig_id)
            val s = stats[key]
            if (s != null && s.correct > s.wrong) mastered++
        }
        return (mastered * 100 / td.questions.size)
    }

    fun getGlobalProgress(): Int {
        val allData = DataProvider.loadData(context)
        val stats = getStats()
        var totalQ = 0
        var mastered = 0
        for (d in allData) {
            if (d.test.tema == null) continue
            for (q in d.questions) {
                totalQ++
                val key = (q.test_id) + ":" + (q.orig_id)
                val s = stats[key]
                if (s != null && s.correct > s.wrong) mastered++
            }
        }
        return if (totalQ > 0) mastered * 100 / totalQ else 0
    }

    fun getTotalCorrect(): Int {
        return getStats().values.sumOf { it.correct }
    }

    fun getTotalWrong(): Int {
        return getStats().values.sumOf { it.wrong }
    }
}
