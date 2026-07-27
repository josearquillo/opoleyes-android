package com.opoleyes.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.opoleyes.data.model.MissionData
import com.opoleyes.data.model.QuestionStat

class PreferencesManager(private val context: Context) {
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
        fun recordKey(mode: String) = "record_$mode"
        fun recordComboKey(mode: String) = "record_combo_$mode"
        fun recordAccKey(mode: String) = "record_acc_$mode"
        fun lawMasteredKey(testId: String) = "law_mastered_$testId"
    }

    fun getXP(): Int = prefs.getInt(XP, 0)

    fun addXP(amount: Int): Int {
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

    fun getFreePowerUps(): List<String> {
        val json = prefs.getString(FREE_POWERUPS_JSON, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun setFreePowerUps(list: List<String>) {
        prefs.edit().putString(FREE_POWERUPS_JSON, gson.toJson(list)).apply()
    }

    fun clearFreePowerUps() {
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

    fun isLawMastered(testId: String): Boolean =
        prefs.getString(lawMasteredKey(testId), null) != null

    fun setLawMastered(testId: String) {
        prefs.edit().putString(lawMasteredKey(testId), "1").apply()
    }

    fun isDebugMode(): Boolean = prefs.getBoolean(DEBUG_MODE, false)

    fun setDebugMode(enabled: Boolean) {
        prefs.edit().putBoolean(DEBUG_MODE, enabled).apply()
    }

    fun resetAll() {
        prefs.edit().clear().apply()
    }
}

