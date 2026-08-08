package com.opoleyes.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.opoleyes.data.Constants
import com.opoleyes.data.model.MissionData
import com.opoleyes.data.model.QuestionStat
import com.opoleyes.data.model.SimulacroHistoryEntry

open class PreferencesManager(private val context: Context) : com.opoleyes.data.IPreferencesManager {
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("opoleyes_prefs", Context.MODE_PRIVATE)

    // When debug mode is active, all persistent writes that represent real game
    // progress are blocked so the debug session acts as a sandbox. The internalWrite
    // flag is set only while setDebugMode is mutating state, so those writes are allowed.
    @Volatile
    private var internalWrite = false
    private fun isWriteBlocked(): Boolean = isDebugMode() && !internalWrite

    companion object {
        const val XP = "xp"
        const val STATS_JSON = "stats_json"
        const val ACHIEVEMENTS_JSON = "achievements_json"
        const val GAMES_PLAYED = "games_played"
        const val FREE_POWERUPS_JSON = "free_powerups_json"
        const val XP_MULTIPLIER = "xp_multiplier"
        const val DAILY_MISSIONS_JSON = "daily_missions_json"
        const val DEBUG_MODE = "debug_mode"
        const val SAVED_POWERUPS_JSON = "saved_powerups_json"
        const val SAVED_MAX_EXAM_QUESTIONS = "saved_max_exam_questions"
        const val SAVED_SIMULACRO_UNLOCKED = "saved_simulacro_unlocked"
        const val POWERUPS_INITIALIZED = "powerups_initialized"
        const val MAX_EXAM_QUESTIONS = "max_exam_questions"
        const val SIMULACRO_UNLOCKED = "simulacro_unlocked"
        const val SIMULACRO_HISTORY_JSON = "simulacro_history_json"
        const val LAST_KNOWN_RANK_INDEX = "last_known_rank_index"
        val EXAM_QUESTION_PRESETS = listOf(10, 20, 30, 40, 50)
        fun recordKey(mode: String) = "record_$mode"
        fun recordComboKey(mode: String) = "record_combo_$mode"
        fun recordAccKey(mode: String) = "record_acc_$mode"
        fun lawMasteredKey(testId: String) = "law_mastered_$testId"
        fun introShownKey(key: String) = "intro_shown_$key"
    }

    override fun getXP(): Int = prefs.getInt(XP, 0)

    override fun addXP(amount: Int): Int {
        if (isWriteBlocked()) return getXP()
        val newXp = getXP() + amount
        prefs.edit().putInt(XP, newXp).apply()
        return newXp
    }

    override fun getMultiplier(): Int = prefs.getInt(XP_MULTIPLIER, 1)

    override fun setMultiplier(value: Int) {
        if (isWriteBlocked()) return
        prefs.edit().putInt(XP_MULTIPLIER, value).apply()
    }

    override fun getStats(): Map<String, QuestionStat> {
        val json = prefs.getString(STATS_JSON, "{}")
        val type = object : TypeToken<Map<String, QuestionStat>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    override fun saveStats(stats: Map<String, QuestionStat>) {
        if (isWriteBlocked()) return
        prefs.edit().putString(STATS_JSON, gson.toJson(stats)).apply()
    }

    override fun getGamesPlayed(): Int = prefs.getInt(GAMES_PLAYED, 0)

    override fun incrementGamesPlayed(): Int {
        if (isWriteBlocked()) return getGamesPlayed()
        val v = getGamesPlayed() + 1
        prefs.edit().putInt(GAMES_PLAYED, v).apply()
        return v
    }

    override fun getRecord(mode: String): Int = prefs.getInt(recordKey(mode), 0)
    override fun setRecord(mode: String, value: Int) { if (isWriteBlocked()) return; prefs.edit().putInt(recordKey(mode), value).apply() }

    override fun getRecordCombo(mode: String): Int = prefs.getInt(recordComboKey(mode), 0)
    override fun setRecordCombo(mode: String, value: Int) { if (isWriteBlocked()) return; prefs.edit().putInt(recordComboKey(mode), value).apply() }

    override fun getRecordAcc(mode: String): Int = prefs.getInt(recordAccKey(mode), 0)
    override fun setRecordAcc(mode: String, value: Int) { if (isWriteBlocked()) return; prefs.edit().putInt(recordAccKey(mode), value).apply() }

    override fun getAchievements(): Map<String, Long> {
        val json = prefs.getString(ACHIEVEMENTS_JSON, "{}")
        val type = object : TypeToken<Map<String, Long>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    override fun saveAchievements(achievements: Map<String, Long>) {
        val wasInternal = internalWrite
        internalWrite = true
        try {
            prefs.edit().putString(ACHIEVEMENTS_JSON, gson.toJson(achievements)).apply()
        } finally {
            internalWrite = wasInternal
        }
    }

    override fun getDailyMissions(): MissionData? {
        val json = prefs.getString(DAILY_MISSIONS_JSON, null) ?: return null
        return gson.fromJson(json, MissionData::class.java)
    }

    override fun saveDailyMissions(data: MissionData) {
        if (isWriteBlocked()) return
        prefs.edit().putString(DAILY_MISSIONS_JSON, gson.toJson(data)).apply()
    }

    override fun isLawMastered(testId: String): Boolean =
        prefs.getString(lawMasteredKey(testId), null) != null

    override fun setLawMastered(testId: String) {
        if (isWriteBlocked()) return
        prefs.edit().putString(lawMasteredKey(testId), "1").apply()
    }

    override fun isDebugMode(): Boolean = prefs.getBoolean(DEBUG_MODE, false)

    override fun setDebugMode(enabled: Boolean) {
        // internalWrite allows the writes below to bypass the debug write-block,
        // and ensures the whole transition is a single atomic SharedPreferences apply.
        internalWrite = true
        try {
            val editor = prefs.edit()
            if (enabled) {
                // Save current real state before setting debug state
                editor.putInt(SAVED_MAX_EXAM_QUESTIONS, getMaxExamQuestions())
                editor.putBoolean(SAVED_SIMULACRO_UNLOCKED, isSimulacroUnlocked())
                // Set debug state: max exam questions, simulacro unlocked
                editor.putInt(MAX_EXAM_QUESTIONS, 50)
                editor.putBoolean(SIMULACRO_UNLOCKED, true)
            } else {
                // Restore saved state, with safe fallbacks if the saved snapshot is missing
                val savedMaxExam = prefs.getInt(SAVED_MAX_EXAM_QUESTIONS, 10)
                editor.putInt(MAX_EXAM_QUESTIONS, savedMaxExam)
                editor.remove(SAVED_MAX_EXAM_QUESTIONS)

                val savedSimulacro = prefs.getBoolean(SAVED_SIMULACRO_UNLOCKED, false)
                editor.putBoolean(SIMULACRO_UNLOCKED, savedSimulacro)
                editor.remove(SAVED_SIMULACRO_UNLOCKED)

                // Force daily missions to regenerate so they match the real unlock state
                editor.remove(DAILY_MISSIONS_JSON)
            }
            // Force daily missions to regenerate for the new unlock state (debug or normal)
            if (enabled) editor.remove(DAILY_MISSIONS_JSON)
            editor.putBoolean(DEBUG_MODE, enabled).apply()
        } finally {
            internalWrite = false
        }
    }

    override fun getMaxExamQuestions(): Int = prefs.getInt(MAX_EXAM_QUESTIONS, 10)

    override fun setMaxExamQuestions(value: Int) {
        if (isWriteBlocked()) return
        prefs.edit().putInt(MAX_EXAM_QUESTIONS, value).apply()
    }

    override fun isSimulacroUnlocked(): Boolean = prefs.getBoolean(SIMULACRO_UNLOCKED, false)

    override fun setSimulacroUnlocked() {
        if (isWriteBlocked()) return
        prefs.edit().putBoolean(SIMULACRO_UNLOCKED, true).apply()
    }

    override fun getSimulacroHistory(): List<SimulacroHistoryEntry> {
        val json = prefs.getString(SIMULACRO_HISTORY_JSON, "[]")
        val type = object : TypeToken<List<SimulacroHistoryEntry>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    override fun addSimulacroHistory(entry: SimulacroHistoryEntry) {
        if (isWriteBlocked()) return
        val history = getSimulacroHistory().toMutableList()
        history.add(entry)
        prefs.edit().putString(SIMULACRO_HISTORY_JSON, gson.toJson(history)).apply()
    }

    override fun getLastKnownRankIndex(): Int = prefs.getInt(LAST_KNOWN_RANK_INDEX, 0)

    override fun setLastKnownRankIndex(index: Int) {
        if (isWriteBlocked()) return
        prefs.edit().putInt(LAST_KNOWN_RANK_INDEX, index).apply()
    }

    override fun isIntroShown(key: String): Boolean = prefs.getBoolean(introShownKey(key), false)

    override fun setIntroShown(key: String) {
        if (isWriteBlocked()) return
        prefs.edit().putBoolean(introShownKey(key), true).apply()
    }

    override fun resetAll() {
        // If debug mode is active, first disable it so the real saved state is
        // restored before wiping everything. Otherwise clear() would silently
        // discard the SAVED_* snapshot and leave the user with no real progress.
        if (isDebugMode()) {
            internalWrite = true
            try {
                prefs.edit().putBoolean(DEBUG_MODE, false).apply()
            } finally {
                internalWrite = false
            }
        }
        prefs.edit().clear().apply()
    }
}

