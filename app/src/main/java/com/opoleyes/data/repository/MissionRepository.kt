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
            val expectedCount = progressRepo.getMissionCount()
            if (allValid && existing.missions.size == expectedCount) return existing
        }

        val seed = today.split("-").map { it.toLong() }.reduce { a, b -> a * 100 + b }
        val rng = seededRandom(seed)
        val stats = statsRepo.getStats()
        val temaTests = DataProvider.getTemaTests(context)

        val rankIndex = progressRepo.getRankIndex()
        val missionReward = 50 * (1 + rankIndex)

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
        for ((_, v) in stats) wrongCount += v.wrong

        val unlocks = progressRepo.getUnlocks()
        val varietyTarget = 5 + rankIndex
        val candidates = mutableListOf<Mission>(
            Mission("quality", "🎯",
                "Acierta ${maxOf(3, Math.ceil(comboRecord * 0.7).toInt())} preguntas seguidas en Supervivencia (todas las leyes)",
                maxOf(3, Math.ceil(comboRecord * 0.7).toInt()), 0, false, missionReward, "streak"),
            Mission("progress", "📈",
                if (lowestLaw != null) "Sube el progreso de \"${lowestLaw.title.ifEmpty { lowestLaw.name }}\" al ${minOf(100, lowestPct + 5)}% en Supervivencia"
                else "Acierta al menos $varietyTarget preguntas en Supervivencia (cualquier ley)",
                if (lowestLaw != null) minOf(100, lowestPct + 5) else varietyTarget,
                if (lowestLaw != null) lowestPct else 0, false, missionReward,
                "progress_${lowestLaw?.id ?: "any"}", lowestLaw?.id),
        )
        if (unlocks.quick) {
            candidates.add(Mission("review", "🔄",
                "Responde ${minOf(20, maxOf(5, wrongCount))} preguntas en Repaso Express",
                minOf(20, maxOf(5, wrongCount)), 0, false, missionReward, "quick_review"))
        }
        candidates.add(Mission("variety", "🌍",
            if (unplayedLaw != null) "Acierta al menos $varietyTarget preguntas en Supervivencia en \"${unplayedLaw.title.ifEmpty { unplayedLaw.name }}\""
            else "Acierta al menos $varietyTarget preguntas en Supervivencia en cualquier ley",
            varietyTarget, 0, false, missionReward, "variety_${unplayedLaw?.id ?: "any"}", unplayedLaw?.id))
        candidates.add(Mission("combo", "🔥",
            "Llega a combo x$comboTarget en Supervivencia (todas las leyes)",
            comboTarget, 0, false, missionReward, "combo"))
        if (unlocks.timetrial) {
            val ttTarget = 300 + rankIndex * 100
            candidates.add(Mission("timetrial", "⏱️",
                "Alcanza $ttTarget puntos en Contrarreloj (todas las leyes)",
                ttTarget, 0, false, missionReward, "timetrial_score"))
        }
        if (unlocks.exam) {
            candidates.add(Mission("exam", "📝",
                "Completa un examen con al menos un ${50 + rankIndex * 2}% de aciertos",
                50 + rankIndex * 2, 0, false, missionReward, "exam_score"))
        }
        if (unlocks.challenge) {
            val chTarget = 200 + rankIndex * 50
            candidates.add(Mission("challenge", "🏆",
                "Alcanza $chTarget puntos en Modo Reto",
                chTarget, 0, false, missionReward, "challenge_score"))
        }

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
                type == "progress" && m.key.startsWith("progress_") && m.key != "progress_any" -> m.current = maxOf(m.current, value)
                type == "progress_any" && m.key == "progress_any" -> m.current += value
                type == "variety" && m.key.startsWith("variety_") -> m.current += value
                type == "timetrial_score" && m.key == "timetrial_score" -> m.current = maxOf(m.current, value)
                type == "challenge_score" && m.key == "challenge_score" -> m.current = maxOf(m.current, value)
                type == "exam_score" && m.key == "exam_score" -> m.current = maxOf(m.current, value)
            }
            if (m.current >= m.target && !m.completed) {
                m.completed = true
                progressRepo.addXP(m.reward)
                anyCompleted = true
            }
        }
        if (anyCompleted && data.missions.all { it.completed }) {
            val rankIndex2 = progressRepo.getRankIndex()
            progressRepo.addXP(200 * (1 + rankIndex2))
        }
        saveDailyMissions(data)
    }

    fun checkOnGameOver(mode: String, maxCombo: Int, totalAnswered: Int, gameCategory: String, correctCount: Int, score: Int = 0) {
        val data = getDailyMissions() ?: return
        updateProgress("streak", maxCombo)
        updateProgress("combo", maxCombo)
        if (mode == "quick") updateProgress("quick_review", totalAnswered)
        if (mode == "timetrial") updateProgress("timetrial_score", score)
        if (mode == "challenge") updateProgress("challenge_score", score)
        for (m in data.missions) {
            if (m.key.startsWith("progress_")) {
                val lawId = m.key.removePrefix("progress_")
                if (lawId == "any") updateProgress("progress_any", correctCount)
                else updateProgress("progress", statsRepo.getLeyProgress(lawId))
            }
            if (m.key.startsWith("variety_")) {
                val lawId = m.key.removePrefix("variety_")
                if (lawId == "any" || lawId == gameCategory) updateProgress("variety", correctCount)
            }
        }
    }

    fun checkExamResult(scorePct: Int) {
        updateProgress("exam_score", scorePct)
    }
}
