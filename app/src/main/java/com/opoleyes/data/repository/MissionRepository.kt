package com.opoleyes.data.repository

import android.content.Context
import com.opoleyes.data.Constants
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.Mission
import com.opoleyes.data.model.MissionData
import com.opoleyes.data.model.MissionDifficulty
import java.time.LocalDate

class MissionRepository private constructor(
    private val context: Context?,
    private val prefs: com.opoleyes.data.IPreferencesManager,
    private val statsRepo: StatsRepository,
    private val progressRepo: ProgressRepository
) {

    constructor(context: Context) : this(
        context,
        PreferencesManager(context),
        StatsRepository(context),
        ProgressRepository(context)
    )

    constructor(prefs: com.opoleyes.data.IPreferencesManager) : this(
        null,
        prefs,
        StatsRepository(prefs),
        ProgressRepository(prefs)
    )

    // Missions completed during the current game session (cleared by clearSessionCompletedMissions).
    // Used by the ViewModel to build the XP breakdown shown on GameOver.
    private val sessionCompletedMissions = mutableListOf<Mission>()

    fun clearSessionCompletedMissions() { sessionCompletedMissions.clear() }
    fun getSessionCompletedMissions(): List<Mission> = sessionCompletedMissions.toList()

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
            val testDataMap = context?.let { DataProvider.getTestDataMap(it) } ?: emptyMap()
            val allValid = existing.missions.all { m ->
                m.testId == null || testDataMap.containsKey(m.testId)
            }
            val missionCount = progressRepo.getMissionCount()
            if (allValid && existing.missions.size == missionCount) return existing
        }

        val seed = today.split("-").map { it.toLong() }.reduce { a, b -> a * 100 + b }
        val rng = seededRandom(seed)
        val stats = statsRepo.getStats()
        val temaTests = context?.let { DataProvider.getTemaTests(it) } ?: emptyList()

        val rankIndex = progressRepo.getRankIndex()
        val easyReward = 30 * (10 + rankIndex * 3) / 10
        val mediumReward = 60 * (10 + rankIndex * 3) / 10
        val hardReward = 100 * (10 + rankIndex * 3) / 10

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
        val isBeginner = rankIndex <= 1

        // === EASY pool ===
        val easyPool = mutableListOf<Mission>()

        if (isBeginner) {
            val correctTarget = if (rankIndex == 0) 3 else 5
            easyPool.add(Mission("variety", "🌍",
                "Acierta $correctTarget preguntas en Supervivencia",
                correctTarget, 0, false, easyReward, "variety_any",
                null, MissionDifficulty.EASY))
            easyPool.add(Mission("quality", "🎯",
                "Acierta ${if (rankIndex == 0) 2 else 3} preguntas seguidas en Supervivencia",
                if (rankIndex == 0) 2 else 3, 0, false, easyReward, "streak",
                null, MissionDifficulty.EASY))
            easyPool.add(Mission("play_count", "🎮",
                "Juega ${if (rankIndex == 0) 2 else 3} partidas hoy",
                if (rankIndex == 0) 2 else 3, 0, false, easyReward, "play_count",
                null, MissionDifficulty.EASY))
        } else {
            val varietyTargetEasy = 3 + rankIndex
            easyPool.add(Mission("variety", "🌍",
                if (unplayedLaw != null) "Acierta $varietyTargetEasy preguntas en Supervivencia en \"${unplayedLaw.title.ifEmpty { unplayedLaw.name }}\""
                else "Acierta $varietyTargetEasy preguntas en Supervivencia en cualquier ley",
                varietyTargetEasy, 0, false, easyReward,
                "variety_${unplayedLaw?.id ?: "any"}", unplayedLaw?.id, MissionDifficulty.EASY))
            easyPool.add(Mission("variety", "📈",
                if (lowestLaw != null) "Responde $varietyTargetEasy preguntas correctamente en \"${lowestLaw.title.ifEmpty { lowestLaw.name }}\" en Supervivencia"
                else "Acierta al menos $varietyTargetEasy preguntas en Supervivencia (cualquier ley)",
                varietyTargetEasy, 0, false, easyReward,
                "variety_${lowestLaw?.id ?: "any"}", lowestLaw?.id, MissionDifficulty.EASY))
            easyPool.add(Mission("play_count", "🎮",
                "Juega ${2 + rankIndex / 3} partidas hoy (cualquier modo)",
                2 + rankIndex / 3, 0, false, easyReward, "play_count",
                null, MissionDifficulty.EASY))
            if (unlocks.quick) {
                easyPool.add(Mission("review", "🔄",
                    "Responde ${minOf(15, maxOf(5, wrongCount / 2))} preguntas en Repaso Express",
                    minOf(15, maxOf(5, wrongCount / 2)), 0, false, easyReward, "quick_review",
                    null, MissionDifficulty.EASY))
            }
            if (unlocks.timetrial) {
                easyPool.add(Mission("timetrial", "⏱️",
                    "Juega una partida de Contrarreloj",
                    1, 0, false, easyReward, "timetrial_play",
                    null, MissionDifficulty.EASY))
            }
        }

        // === MEDIUM pool ===
        val mediumPool = mutableListOf<Mission>()

        if (rankIndex <= 2) {
            val streakTarget = maxOf(3, rankIndex)
            mediumPool.add(Mission("quality", "🎯",
                "Acierta $streakTarget preguntas seguidas en Supervivencia (todas las leyes)",
                streakTarget, 0, false, mediumReward, "streak",
                null, MissionDifficulty.MEDIUM))
            mediumPool.add(Mission("combo", "🔥",
                "Llega a combo x${maxOf(3, streakTarget + 1)} en Supervivencia (todas las leyes)",
                maxOf(3, streakTarget + 1), 0, false, mediumReward, "combo",
                null, MissionDifficulty.MEDIUM))
            mediumPool.add(Mission("no_powerups", "🚫",
                "Completa una partida de Supervivencia sin usar power-ups",
                1, 0, false, mediumReward, "no_powerups",
                null, MissionDifficulty.MEDIUM))
        } else {
            val varietyTargetMedium = 5 + rankIndex
            mediumPool.add(Mission("quality", "🎯",
                "Acierta $streakTargetMedium preguntas seguidas en Supervivencia (todas las leyes)",
                streakTargetMedium, 0, false, mediumReward, "streak",
                null, MissionDifficulty.MEDIUM))
            mediumPool.add(Mission("combo", "🔥",
                "Llega a combo x$comboTargetMedium en Supervivencia (todas las leyes)",
                comboTargetMedium, 0, false, mediumReward, "combo",
                null, MissionDifficulty.MEDIUM))
            mediumPool.add(Mission("variety", "🌍",
                "Acierta $varietyTargetMedium preguntas en Supervivencia en \"${(lowestLaw ?: temaTests.firstOrNull())?.title?.ifEmpty { (lowestLaw ?: temaTests.firstOrNull())?.name } ?: "cualquier ley"}\"",
                varietyTargetMedium, 0, false, mediumReward,
                "variety_${(lowestLaw ?: temaTests.firstOrNull())?.id ?: "any"}",
                (lowestLaw ?: temaTests.firstOrNull())?.id, MissionDifficulty.MEDIUM))
            if (unlocks.timetrial) {
                val ttTarget = 300 + rankIndex * 100
                mediumPool.add(Mission("timetrial", "⏱️",
                    "Alcanza $ttTarget puntos en Contrarreloj (todas las leyes)",
                    ttTarget, 0, false, mediumReward, "timetrial_score",
                    null, MissionDifficulty.MEDIUM))
            }
            mediumPool.add(Mission("no_powerups", "🚫",
                "Completa una partida de Supervivencia sin usar power-ups",
                1, 0, false, mediumReward, "no_powerups",
                null, MissionDifficulty.MEDIUM))
            if (unlocks.quick) {
                mediumPool.add(Mission("review", "🔄",
                    "Completa un Repaso Express sin fallar ninguna pregunta",
                    1, 0, false, mediumReward, "perfect_quick",
                    null, MissionDifficulty.MEDIUM))
            }
        }

        // === HARD pool ===
        val hardPool = mutableListOf<Mission>()

        // First hard mission: rotate between streak, combo, and no_powerups
        val hardRotation = rng() % 3
        when (hardRotation.toInt()) {
            0 -> hardPool.add(Mission("quality", "🎯",
                "Acierta $streakTargetHard preguntas seguidas en Supervivencia (todas las leyes)",
                streakTargetHard, 0, false, hardReward, "streak",
                null, MissionDifficulty.HARD))
            1 -> hardPool.add(Mission("combo", "🔥",
                "Llega a combo x$comboTargetHard en Supervivencia (todas las leyes)",
                comboTargetHard, 0, false, hardReward, "combo",
                null, MissionDifficulty.HARD))
            2 -> hardPool.add(Mission("no_powerups", "🚫",
                "Llega a combo x${maxOf(3, comboTargetHard - 2)} en Supervivencia sin usar power-ups",
                maxOf(3, comboTargetHard - 2), 0, false, hardReward, "no_powerups_combo",
                null, MissionDifficulty.HARD))
        }

        // Second hard mission: perfect game or high streak
        val hardRotation2 = rng() % 2
        when (hardRotation2.toInt()) {
            0 -> hardPool.add(Mission("perfect_game", "💎",
                "Acierta ${10 + rankIndex} preguntas seguidas en Supervivencia sin fallar ninguna",
                10 + rankIndex, 0, false, hardReward, "perfect_game",
                null, MissionDifficulty.HARD))
            1 -> hardPool.add(Mission("quality", "🎯",
                "Acierta ${streakTargetHard + 2} preguntas seguidas en Supervivencia (todas las leyes)",
                streakTargetHard + 2, 0, false, hardReward, "streak",
                null, MissionDifficulty.HARD))
        }

        // Third hard mission: dynamic based on highest unlocked mode
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
                    "Completa 3 Repasos Express hoy",
                    3, 0, false, hardReward, "quick_complete",
                    null, MissionDifficulty.HARD))
            }
            unlocks.timetrial -> {
                val ttTarget = 500 + rankIndex * 100
                hardPool.add(Mission("timetrial", "⏱️",
                    "Alcanza $ttTarget puntos en Contrarreloj (todas las leyes)",
                    ttTarget, 0, false, hardReward, "timetrial_score",
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
                type == "variety" && m.key.startsWith("variety_") -> m.current += value
                type == "timetrial_score" && m.key == "timetrial_score" -> m.current = maxOf(m.current, value)
                type == "exam_score" && m.key == "exam_score" -> m.current = maxOf(m.current, value)
                type == "simulacro_complete" && m.key == "simulacro_complete" -> m.current = maxOf(m.current, value)
                type == "quick_complete" && m.key == "quick_complete" -> m.current += value
                type == "play_count" && m.key == "play_count" -> m.current += value
                type == "no_powerups" && m.key == "no_powerups" && value == 1 -> m.current = 1
                type == "no_powerups_combo" && m.key == "no_powerups_combo" -> m.current = maxOf(m.current, value)
                type == "perfect_quick" && m.key == "perfect_quick" && value == 1 -> m.current = 1
                type == "timetrial_play" && m.key == "timetrial_play" && value == 1 -> m.current = 1
            }
            if (m.current >= m.target && !m.completed) {
                m.completed = true
                progressRepo.addXP(m.reward)
                sessionCompletedMissions.add(m)
            }
        }
        saveDailyMissions(data)
    }

    fun checkLiveProgress(mode: String, wrongCount: Int, totalAnswered: Int) {
        if (mode != "survival") return
        val data = getDailyMissions() ?: return
        for (m in data.missions) {
            if (m.completed) continue
            if (m.key == "perfect_game" && wrongCount == 0 && totalAnswered >= m.target) {
                m.current = m.target
                m.completed = true
                progressRepo.addXP(m.reward)
                sessionCompletedMissions.add(m)
            }
        }
        saveDailyMissions(data)
    }

    fun checkOnGameOver(mode: String, maxCombo: Int, maxStreak: Int, totalAnswered: Int, gameCategory: String, correctCount: Int, score: Int = 0, powerUpsUsed: Int = 0, wrongCount: Int = 0) {
        val data = getDailyMissions() ?: return
        // play_count tracks any game mode
        updateProgress("play_count", 1)
        if (mode == "survival") {
            updateProgress("streak", maxStreak)
            updateProgress("combo", maxCombo)
            for (m in data.missions) {
                if (m.key.startsWith("variety_")) {
                    val lawId = m.key.removePrefix("variety_")
                    if (lawId == "any" || lawId == gameCategory) updateProgress("variety", correctCount)
                }
            }
            // no_powerups: completed a survival game without using power-ups
            if (powerUpsUsed == 0 && totalAnswered >= 3) {
                updateProgress("no_powerups", 1)
                updateProgress("no_powerups_combo", maxCombo)
            }
        }
        if (mode == "quick") {
            updateProgress("quick_review", totalAnswered)
            if (totalAnswered >= Constants.QUICK_MODE_QUESTIONS) {
                updateProgress("quick_complete", 1)
                if (wrongCount == 0) updateProgress("perfect_quick", 1)
            }
        }
        if (mode == "timetrial") {
            updateProgress("timetrial_score", score)
            updateProgress("timetrial_play", 1)
        }
    }

    fun checkExamResult(scorePct: Int) {
        updateProgress("exam_score", scorePct)
    }

    fun checkSimulacroResult(passed: Boolean) {
        if (passed) updateProgress("simulacro_complete", 1)
    }
}
