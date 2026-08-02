package com.opoleyes.data.repository

import android.content.Context
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.Mission
import com.opoleyes.data.model.MissionData
import com.opoleyes.data.model.MissionDifficulty
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
            val testDataMap = DataProvider.getTestDataMap(context)
            val allValid = existing.missions.all { m ->
                m.testId == null || testDataMap.containsKey(m.testId)
            }
            val missionCount = progressRepo.getMissionCount()
            if (allValid && existing.missions.size == missionCount) return existing
        }

        val seed = today.split("-").map { it.toLong() }.reduce { a, b -> a * 100 + b }
        val rng = seededRandom(seed)
        val stats = statsRepo.getStats()
        val temaTests = DataProvider.getTemaTests(context)

        val rankIndex = progressRepo.getRankIndex()
        val easyReward = 30 * (1 + rankIndex)
        val mediumReward = 60 * (1 + rankIndex)
        val hardReward = 100 * (1 + rankIndex)

        val comboRecord = progressRepo.getMaxComboRecord()
        val comboTargetMedium = maxOf(3, comboRecord + 2)
        val comboTargetHard = maxOf(5, comboRecord + 5)
        val streakTargetMedium = maxOf(3, Math.ceil(comboRecord * 0.7).toInt())
        val streakTargetHard = maxOf(5, Math.ceil(comboRecord * 0.9).toInt())

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
        val varietyTargetEasy = 3 + rankIndex
        val varietyTargetMedium = 5 + rankIndex

        // === EASY pool ===
        val easyPool = mutableListOf<Mission>(
            Mission("variety", "🌍",
                if (unplayedLaw != null) "Acierta $varietyTargetEasy preguntas en Supervivencia en \"${unplayedLaw.title.ifEmpty { unplayedLaw.name }}\""
                else "Acierta $varietyTargetEasy preguntas en Supervivencia en cualquier ley",
                varietyTargetEasy, 0, false, easyReward,
                "variety_${unplayedLaw?.id ?: "any"}", unplayedLaw?.id, MissionDifficulty.EASY),
            Mission("progress", "📈",
                if (lowestLaw != null) "Sube el progreso de \"${lowestLaw.title.ifEmpty { lowestLaw.name }}\" al ${minOf(100, lowestPct + 5)}% en Supervivencia"
                else "Acierta al menos $varietyTargetEasy preguntas en Supervivencia (cualquier ley)",
                if (lowestLaw != null) minOf(100, lowestPct + 5) else varietyTargetEasy,
                if (lowestLaw != null) lowestPct else 0, false, easyReward,
                "progress_${lowestLaw?.id ?: "any"}", lowestLaw?.id, MissionDifficulty.EASY),
        )
        if (unlocks.quick) {
            easyPool.add(Mission("review", "🔄",
                "Responde ${minOf(15, maxOf(5, wrongCount / 2))} preguntas en Repaso Express",
                minOf(15, maxOf(5, wrongCount / 2)), 0, false, easyReward, "quick_review",
                null, MissionDifficulty.EASY))
        }

        // === MEDIUM pool ===
        val mediumPool = mutableListOf<Mission>(
            Mission("quality", "🎯",
                "Acierta $streakTargetMedium preguntas seguidas en Supervivencia (todas las leyes)",
                streakTargetMedium, 0, false, mediumReward, "streak",
                null, MissionDifficulty.MEDIUM),
            Mission("combo", "🔥",
                "Llega a combo x$comboTargetMedium en Supervivencia (todas las leyes)",
                comboTargetMedium, 0, false, mediumReward, "combo",
                null, MissionDifficulty.MEDIUM),
            Mission("variety", "🌍",
                "Acierta $varietyTargetMedium preguntas en Supervivencia en \"${(lowestLaw ?: temaTests.firstOrNull())?.title?.ifEmpty { (lowestLaw ?: temaTests.firstOrNull())?.name } ?: "cualquier ley"}\"",
                varietyTargetMedium, 0, false, mediumReward,
                "variety_${(lowestLaw ?: temaTests.firstOrNull())?.id ?: "any"}",
                (lowestLaw ?: temaTests.firstOrNull())?.id, MissionDifficulty.MEDIUM),
        )
        if (unlocks.timetrial) {
            val ttTarget = 300 + rankIndex * 100
            mediumPool.add(Mission("timetrial", "⏱️",
                "Alcanza $ttTarget puntos en Contrarreloj (todas las leyes)",
                ttTarget, 0, false, mediumReward, "timetrial_score",
                null, MissionDifficulty.MEDIUM))
        }

        // === HARD pool ===
        val hardPool = mutableListOf<Mission>(
            Mission("quality", "🎯",
                "Acierta $streakTargetHard preguntas seguidas en Supervivencia (todas las leyes)",
                streakTargetHard, 0, false, hardReward, "streak",
                null, MissionDifficulty.HARD),
            Mission("combo", "🔥",
                "Llega a combo x$comboTargetHard en Supervivencia (todas las leyes)",
                comboTargetHard, 0, false, hardReward, "combo",
                null, MissionDifficulty.HARD),
        )

        // Dynamic mission 3 based on highest unlocked mode
        when {
            unlocks.simulacro -> {
                hardPool.add(Mission("simulacro", "🎯",
                    "Completa el Simulacro del día",
                    1, 0, false, hardReward, "simulacro_complete",
                    null, MissionDifficulty.HARD))
            }
            unlocks.exam -> {
                val maxExamQ = progressRepo.getMaxExamQuestions()
                hardPool.add(Mission("exam", "📝",
                    "Completa un Mini Examen de $maxExamQ preguntas con al menos un ${60 + rankIndex * 2}% de aciertos",
                    60 + rankIndex * 2, 0, false, hardReward, "exam_score",
                    null, MissionDifficulty.HARD))
            }
            unlocks.quick -> {
                hardPool.add(Mission("review", "🔄",
                    "Completa un Repaso Express",
                    20, 0, false, hardReward, "quick_complete",
                    null, MissionDifficulty.HARD))
            }
            else -> {
                hardPool.add(Mission("quality", "🎯",
                    "Acierta $streakTargetHard preguntas seguidas en Supervivencia (todas las leyes)",
                    streakTargetHard, 0, false, hardReward, "streak",
                    null, MissionDifficulty.HARD))
            }
        }

        val missionCount = progressRepo.getMissionCount()
        val selected = mutableListOf<Mission>()
        selected.add(pickRandom(easyPool, rng))
        if (missionCount >= 2) selected.add(pickRandom(mediumPool, rng))
        if (missionCount >= 3) selected.add(pickRandom(hardPool, rng))

        val data = MissionData(today, selected)
        saveDailyMissions(data)
        return data
    }

    private fun pickRandom(pool: MutableList<Mission>, rng: () -> Double): Mission {
        val idx = (rng() * pool.size).toInt()
        return pool.removeAt(idx)
    }

    fun updateProgress(type: String, value: Int) {
        val data = getDailyMissions() ?: return
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
                type == "exam_score" && m.key == "exam_score" -> m.current = maxOf(m.current, value)
                type == "simulacro_complete" && m.key == "simulacro_complete" -> m.current = maxOf(m.current, value)
                type == "quick_complete" && m.key == "quick_complete" -> m.current = maxOf(m.current, value)
            }
            if (m.current >= m.target && !m.completed) {
                m.completed = true
                progressRepo.addXP(m.reward)
            }
        }
        saveDailyMissions(data)
    }

    fun checkOnGameOver(mode: String, maxCombo: Int, totalAnswered: Int, gameCategory: String, correctCount: Int, score: Int = 0) {
        val data = getDailyMissions() ?: return
        updateProgress("streak", maxCombo)
        updateProgress("combo", maxCombo)
        if (mode == "quick") {
            updateProgress("quick_review", totalAnswered)
            updateProgress("quick_complete", 1)
        }
        if (mode == "timetrial") updateProgress("timetrial_score", score)
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

    fun checkSimulacroResult(passed: Boolean) {
        if (passed) updateProgress("simulacro_complete", 1)
    }
}
