package com.opotest.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.opotest.data.model.MissionData
import com.opotest.data.model.QuestionStat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "opotest_prefs")

class PreferencesManager(private val context: Context) {
    private val gson = Gson()

    companion object {
        val XP = intPreferencesKey("xp")
        val STATS_JSON = stringPreferencesKey("stats_json")
        val ACHIEVEMENTS_JSON = stringPreferencesKey("achievements_json")
        val GAMES_PLAYED = intPreferencesKey("games_played")
        val TRAININGS_DONE = intPreferencesKey("trainings_done")
        val FREE_POWERUPS_JSON = stringPreferencesKey("free_powerups_json")
        val XP_MULTIPLIER = intPreferencesKey("xp_multiplier")
        val DAILY_MISSIONS_JSON = stringPreferencesKey("daily_missions_json")
        fun recordKey(mode: String) = intPreferencesKey("record_$mode")
        fun recordComboKey(mode: String) = intPreferencesKey("record_combo_$mode")
        fun recordAccKey(mode: String) = intPreferencesKey("record_acc_$mode")
        fun lawMasteredKey(testId: String) = stringPreferencesKey("law_mastered_$testId")
    }

    fun getXP(): Int = runBlocking { context.dataStore.data.first()[XP] ?: 0 }

    fun addXP(amount: Int): Int {
        val multiplier = getMultiplier()
        val newXp = getXP() + amount * multiplier
        runBlocking {
            context.dataStore.edit {
                it[XP] = newXp
                if (multiplier > 1) it[XP_MULTIPLIER] = 1
            }
        }
        return newXp
    }

    fun getMultiplier(): Int = runBlocking { context.dataStore.data.first()[XP_MULTIPLIER] ?: 1 }

    fun setMultiplier(value: Int) {
        runBlocking { context.dataStore.edit { it[XP_MULTIPLIER] = value } }
    }

    fun getStats(): Map<String, QuestionStat> {
        val json = runBlocking { context.dataStore.data.first()[STATS_JSON] } ?: "{}"
        val type = object : TypeToken<Map<String, QuestionStat>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    fun saveStats(stats: Map<String, QuestionStat>) {
        runBlocking { context.dataStore.edit { it[STATS_JSON] = gson.toJson(stats) } }
    }

    fun getGamesPlayed(): Int = runBlocking { context.dataStore.data.first()[GAMES_PLAYED] ?: 0 }

    fun incrementGamesPlayed(): Int {
        val v = getGamesPlayed() + 1
        runBlocking { context.dataStore.edit { it[GAMES_PLAYED] = v } }
        return v
    }

    fun getTrainingsDone(): Int = runBlocking { context.dataStore.data.first()[TRAININGS_DONE] ?: 0 }

    fun incrementTrainingsDone(): Int {
        val v = getTrainingsDone() + 1
        runBlocking { context.dataStore.edit { it[TRAININGS_DONE] = v } }
        return v
    }

    fun getRecord(mode: String): Int = runBlocking { context.dataStore.data.first()[recordKey(mode)] } ?: 0
    fun setRecord(mode: String, value: Int) { runBlocking { context.dataStore.edit { it[recordKey(mode)] = value } } }

    fun getRecordCombo(mode: String): Int = runBlocking { context.dataStore.data.first()[recordComboKey(mode)] } ?: 0
    fun setRecordCombo(mode: String, value: Int) { runBlocking { context.dataStore.edit { it[recordComboKey(mode)] = value } } }

    fun getRecordAcc(mode: String): Int = runBlocking { context.dataStore.data.first()[recordAccKey(mode)] } ?: 0
    fun setRecordAcc(mode: String, value: Int) { runBlocking { context.dataStore.edit { it[recordAccKey(mode)] = value } } }

    fun getFreePowerUps(): List<String> {
        val json = runBlocking { context.dataStore.data.first()[FREE_POWERUPS_JSON] } ?: "[]"
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun setFreePowerUps(list: List<String>) {
        runBlocking { context.dataStore.edit { it[FREE_POWERUPS_JSON] = gson.toJson(list) } }
    }

    fun clearFreePowerUps() {
        runBlocking { context.dataStore.edit { it.remove(FREE_POWERUPS_JSON) } }
    }

    fun getAchievements(): Map<String, Long> {
        val json = runBlocking { context.dataStore.data.first()[ACHIEVEMENTS_JSON] } ?: "{}"
        val type = object : TypeToken<Map<String, Long>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    fun saveAchievements(achievements: Map<String, Long>) {
        runBlocking { context.dataStore.edit { it[ACHIEVEMENTS_JSON] = gson.toJson(achievements) } }
    }

    fun getDailyMissions(): MissionData? {
        val json = runBlocking { context.dataStore.data.first()[DAILY_MISSIONS_JSON] } ?: return null
        return gson.fromJson(json, MissionData::class.java)
    }

    fun saveDailyMissions(data: MissionData) {
        runBlocking { context.dataStore.edit { it[DAILY_MISSIONS_JSON] = gson.toJson(data) } }
    }

    fun isLawMastered(testId: String): Boolean =
        runBlocking { context.dataStore.data.first()[lawMasteredKey(testId)] } != null

    fun setLawMastered(testId: String) {
        runBlocking { context.dataStore.edit { it[lawMasteredKey(testId)] = "1" } }
    }

    fun resetAll() {
        runBlocking {
            context.dataStore.edit { it.clear() }
        }
    }
}
