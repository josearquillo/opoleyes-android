package com.opoleyes.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.opoleyes.data.model.MissionData
import com.opoleyes.data.model.QuestionStat
import com.opoleyes.data.model.SimulacroHistoryEntry

open class PreferencesManager(private val context: Context) : com.opoleyes.data.IPreferencesManager {
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("opoleyes_prefs", Context.MODE_PRIVATE)

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
        const val LOGO_PREF = "logo_pref"
        const val LOGO_CHOSEN = "logo_chosen"
        const val MAX_EXAM_QUESTIONS = "max_exam_questions"
        const val SIMULACRO_UNLOCKED = "simulacro_unlocked"
        const val SIMULACRO_HISTORY_JSON = "simulacro_history_json"
        fun recordKey(mode: String) = "record_$mode"
        fun recordComboKey(mode: String) = "record_combo_$mode"
        fun recordAccKey(mode: String) = "record_acc_$mode"
        fun lawMasteredKey(testId: String) = "law_mastered_$testId"
    }

    fun initPowerUpsIfNeeded() {
        if (!prefs.getBoolean(POWERUPS_INITIALIZED, false)) {
            val initial = listOf("shield", "fiftyFifty", "hint", "doubleScore")
            setFreePowerUps(initial)
            prefs.edit().putBoolean(POWERUPS_INITIALIZED, true).apply()
        }
    }

    override fun getXP(): Int = prefs.getInt(XP, 0)

    override fun addXP(amount: Int): Int {
        val multiplier = getMultiplier()
        val newXp = getXP() + amount * multiplier
        val editor = prefs.edit()
        editor.putInt(XP, newXp)
        if (multiplier > 1) editor.putInt(XP_MULTIPLIER, 1)
        editor.apply()
        return newXp
    }

    fun getMultiplier(): Int = prefs.getInt(XP_MULTIPLIER, 1)

    fun setMultiplier(value: Int) {
        prefs.edit().putInt(XP_MULTIPLIER, value).apply()
    }

    fun getStats(): Map<String, QuestionStat> {
        val json = prefs.getString(STATS_JSON, "{}")
        val type = object : TypeToken<Map<String, QuestionStat>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    fun saveStats(stats: Map<String, QuestionStat>) {
        prefs.edit().putString(STATS_JSON, gson.toJson(stats)).apply()
    }

    fun getGamesPlayed(): Int = prefs.getInt(GAMES_PLAYED, 0)

    fun incrementGamesPlayed(): Int {
        val v = getGamesPlayed() + 1
        prefs.edit().putInt(GAMES_PLAYED, v).apply()
        return v
    }

    fun getRecord(mode: String): Int = prefs.getInt(recordKey(mode), 0)
    fun setRecord(mode: String, value: Int) { prefs.edit().putInt(recordKey(mode), value).apply() }

    fun getRecordCombo(mode: String): Int = prefs.getInt(recordComboKey(mode), 0)
    fun setRecordCombo(mode: String, value: Int) { prefs.edit().putInt(recordComboKey(mode), value).apply() }

    fun getRecordAcc(mode: String): Int = prefs.getInt(recordAccKey(mode), 0)
    fun setRecordAcc(mode: String, value: Int) { prefs.edit().putInt(recordAccKey(mode), value).apply() }

    override fun getFreePowerUps(): List<String> {
        val json = prefs.getString(FREE_POWERUPS_JSON, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    override fun setFreePowerUps(list: List<String>) {
        prefs.edit().putString(FREE_POWERUPS_JSON, gson.toJson(list)).apply()
    }

    override fun clearFreePowerUps() {
        prefs.edit().remove(FREE_POWERUPS_JSON).apply()
    }

    fun getAchievements(): Map<String, Long> {
        val json = prefs.getString(ACHIEVEMENTS_JSON, "{}")
        val type = object : TypeToken<Map<String, Long>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    fun saveAchievements(achievements: Map<String, Long>) {
        prefs.edit().putString(ACHIEVEMENTS_JSON, gson.toJson(achievements)).apply()
    }

    fun getDailyMissions(): MissionData? {
        val json = prefs.getString(DAILY_MISSIONS_JSON, null) ?: return null
        return gson.fromJson(json, MissionData::class.java)
    }

    fun saveDailyMissions(data: MissionData) {
        prefs.edit().putString(DAILY_MISSIONS_JSON, gson.toJson(data)).apply()
    }

    override fun isLawMastered(testId: String): Boolean =
        prefs.getString(lawMasteredKey(testId), null) != null

    override fun setLawMastered(testId: String) {
        prefs.edit().putString(lawMasteredKey(testId), "1").apply()
    }

    override fun isDebugMode(): Boolean = prefs.getBoolean(DEBUG_MODE, false)

    override fun setDebugMode(enabled: Boolean) {
        val editor = prefs.edit()
        if (enabled) {
            // Save current real state before setting debug state
            val currentPowerUps = getFreePowerUps()
            editor.putString(SAVED_POWERUPS_JSON, gson.toJson(currentPowerUps))
            editor.putInt(SAVED_MAX_EXAM_QUESTIONS, getMaxExamQuestions())
            editor.putBoolean(SAVED_SIMULACRO_UNLOCKED, isSimulacroUnlocked())
            // Set debug state: infinite power-ups, max exam questions, simulacro unlocked
            val debugPowerUps = mutableListOf<String>()
            repeat(99) { debugPowerUps.add("shield") }
            repeat(99) { debugPowerUps.add("fiftyFifty") }
            repeat(99) { debugPowerUps.add("hint") }
            repeat(99) { debugPowerUps.add("doubleScore") }
            setFreePowerUps(debugPowerUps)
            editor.putInt(MAX_EXAM_QUESTIONS, 50)
            editor.putBoolean(SIMULACRO_UNLOCKED, true)
        } else {
            // Restore saved state
            val savedPowerUpsJson = prefs.getString(SAVED_POWERUPS_JSON, null)
            if (savedPowerUpsJson != null) {
                val type = object : TypeToken<List<String>>() {}.type
                val saved: List<String> = gson.fromJson(savedPowerUpsJson, type) ?: emptyList()
                setFreePowerUps(saved)
                editor.remove(SAVED_POWERUPS_JSON)
            }
            val savedMaxExam = prefs.getInt(SAVED_MAX_EXAM_QUESTIONS, 10)
            editor.putInt(MAX_EXAM_QUESTIONS, savedMaxExam)
            editor.remove(SAVED_MAX_EXAM_QUESTIONS)

            val savedSimulacro = prefs.getBoolean(SAVED_SIMULACRO_UNLOCKED, false)
            editor.putBoolean(SIMULACRO_UNLOCKED, savedSimulacro)
            editor.remove(SAVED_SIMULACRO_UNLOCKED)
        }
        editor.putBoolean(DEBUG_MODE, enabled).apply()
    }

    fun getLogoPref(): String = prefs.getString(LOGO_PREF, "ol_v1") ?: "ol_v1"

    fun setLogoPref(logo: String) {
        prefs.edit().putString(LOGO_PREF, logo).putBoolean(LOGO_CHOSEN, true).apply()
    }

    fun isLogoChosen(): Boolean = prefs.getBoolean(LOGO_CHOSEN, false)

    val EXAM_QUESTION_PRESETS = listOf(10, 20, 30, 40, 50)

    fun getMaxExamQuestions(): Int = prefs.getInt(MAX_EXAM_QUESTIONS, 10)

    fun setMaxExamQuestions(value: Int) {
        prefs.edit().putInt(MAX_EXAM_QUESTIONS, value).apply()
    }

    fun isSimulacroUnlocked(): Boolean = prefs.getBoolean(SIMULACRO_UNLOCKED, false)

    fun setSimulacroUnlocked() {
        prefs.edit().putBoolean(SIMULACRO_UNLOCKED, true).apply()
    }

    fun getSimulacroHistory(): List<SimulacroHistoryEntry> {
        val json = prefs.getString(SIMULACRO_HISTORY_JSON, "[]")
        val type = object : TypeToken<List<SimulacroHistoryEntry>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun addSimulacroHistory(entry: SimulacroHistoryEntry) {
        val history = getSimulacroHistory().toMutableList()
        history.add(entry)
        prefs.edit().putString(SIMULACRO_HISTORY_JSON, gson.toJson(history)).apply()
    }

    override fun resetAll() {
        prefs.edit().clear().commit()
    }
}

