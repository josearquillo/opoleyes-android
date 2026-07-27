package com.opoleyes.data.repository

import android.content.Context
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.Mission
import com.opoleyes.data.model.MissionData
import java.time.LocalDate

class MissionRepository(private val context: Context) {
    private val prefs = PreferencesManager(context)
    private val statsRepo = StatsRepository(context)
    private val progressRepo = ProgressRepository(context)

    private fun seededRandom(seed: Long): () -> Double {
        var s = seed
        return {
            s = (s * 9301 + 49297) % 233280
            s / 233280.0
        }
    }

    private fun getTodayStr(): String = LocalDate.now().toString()

    fun getDailyMissions(): MissionData? = prefs.getDailyMissions()

    fun saveDailyMissions(data: MissionData) = prefs.saveDailyMissions(data)

    fun generateDailyMissions(): MissionData {
        val today = getTodayStr()
        val existing = getDailyMissions()
        if (existing != null && existing.date == today) {
            // Validate that cached mission testIds still exist
            val testDataMap = DataProvider.getTestDataMap(context)
            val allValid = existing.missions.all { m ->
                m.testId == null || testDataMap.containsKey(m.testId)
            }
            if (allValid) return existing
        }

        val seed = today.split("-").map { it.toLong() }.reduce { a, b -> a * 100 + b }
        val rng = seededRandom(seed)
        val stats = statsRepo.getStats()
        val temaTests = DataProvider.getTemaTests(context)

        val comboRecord = progressRepo.getMaxComboRecord()
        val comboTarget = maxOf(3, comboRecord + 2)

        var lowestLaw: com.opoleyes.data.model.Test? = null
        var lowestPct = 100
        for (t in temaTests) {
            val p = statsRepo.getLeyProgress(t.id)
            if (p < lowestPct) { lowestPct = p; lowestLaw = t }
        }

        val unplayedLaw = temaTests.find { statsRepo.getLeyProgress(it.id) == 0 }

        var wrongCount = 0
        for ((_, v) in stats) wrongCount += v?.wrong ?: 0

        val unlocks = progressRepo.getUnlocks()
        val candidates = mutableListOf<Mission>(
            Mission("quality", "🎯",
                "Acierta ${maxOf(3, Math.ceil(comboRecord * 0.7).toInt())} preguntas seguidas en Supervivencia (todas las leyes)",
                maxOf(3, Math.ceil(comboRecord * 0.7).toInt()), 0, false, 50, "streak"),
            Mission("progress", "📈",
                if (lowestLaw != null) "Sube el progreso de \"${lowestLaw.title.ifEmpty { lowestLaw.name }}\" al ${minOf(100, lowestPct + 5)}% en Supervivencia"
                else "Juega una partida de Supervivencia",
                if (lowestLaw != null) minOf(100, lowestPct + 5) else 1,
                if (lowestLaw != null) lowestPct else 0, false, 50,
                "progress_${lowestLaw?.id ?: "any"}", lowestLaw?.id),
        )
        if (unlocks.quick) {
            candidates.add(Mission("review", "🔄",
                "Responde ${minOf(20, maxOf(5, wrongCount))} preguntas en Repaso Express",
                minOf(20, maxOf(5, wrongCount)), 0, false, 50, "quick_review"))
        }
        candidates.add(Mission("variety", "🌍",
            if (unplayedLaw != null) "Juega Supervivencia en \"${unplayedLaw.title.ifEmpty { unplayedLaw.name }}\""
            else "Juega Supervivencia en cualquier ley",
            1, 0, false, 50, "variety_${unplayedLaw?.id ?: "any"}", unplayedLaw?.id))
        candidates.add(Mission("combo", "🔥",
            "Llega a combo x$comboTarget en Supervivencia (todas las leyes)",
            comboTarget, 0, false, 50, "combo"))

        val missionCount = progressRepo.getMissionCount()
        val pool = candidates.toMutableList()
        val selected = mutableListOf<Mission>()
        while (selected.size < missionCount && pool.isNotEmpty()) {
            val idx = (rng() * pool.size).toInt()
            selected.add(pool.removeAt(idx))
        }

        val data = MissionData(today, selected)
        saveDailyMissions(data)
        return data
    }

    fun updateProgress(type: String, value: Int) {
        val data = getDailyMissions() ?: return
        var anyCompleted = false
        for (m in data.missions) {
            if (m.completed) continue
            when {
                type == "streak" && m.key == "streak" -> m.current = maxOf(m.current, value)
                type == "combo" && m.key == "combo" -> m.current = maxOf(m.current, value)
                type == "quick_review" && m.key == "quick_review" -> m.current += value
                type == "progress" && m.key.startsWith("progress_") -> m.current = maxOf(m.current, value)
                type == "variety" && m.key.startsWith("variety_") -> m.current = maxOf(m.current, value)
            }
            if (m.current >= m.target && !m.completed) {
                m.completed = true
                progressRepo.addXP(m.reward)
                anyCompleted = true
            }
        }
        if (anyCompleted && data.missions.all { it.completed }) {
            progressRepo.addXP(200)
        }
        saveDailyMissions(data)
    }

    fun checkOnGameOver(mode: String, score: Int, maxCombo: Int, correctCount: Int, totalAnswered: Int, gameCategory: String) {
        val data = getDailyMissions() ?: return
        updateProgress("streak", maxCombo)
        updateProgress("combo", maxCombo)
        if (mode == "quick") updateProgress("quick_review", totalAnswered)
        for (m in data.missions) {
            if (m.key.startsWith("progress_")) {
                val lawId = m.key.removePrefix("progress_")
                if (lawId != "any") updateProgress("progress", statsRepo.getLeyProgress(lawId))
            }
            if (m.key.startsWith("variety_")) {
                val lawId = m.key.removePrefix("variety_")
                if (lawId == "any" || lawId == gameCategory) updateProgress("variety", 1)
            }
        }
    }
}
