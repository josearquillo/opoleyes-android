package com.opoleyes.data.local

import androidx.test.core.app.ApplicationProvider
import com.opoleyes.TestContextProvider
import com.opoleyes.data.model.Mission
import com.opoleyes.data.model.MissionData
import com.opoleyes.data.model.MissionDifficulty
import com.opoleyes.data.model.SimulacroHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreferencesManagerIntroTest {

    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        val ctx = TestContextProvider.getContext()
        prefs = PreferencesManager(ctx)
        prefs.resetAll()
    }

    @Test
    fun isIntroShown_defaultsToFalse() {
        assertFalse("Intro should not be shown by default", prefs.isIntroShown("intro_survival_rank_0"))
        assertFalse("Timetrial intro should not be shown by default", prefs.isIntroShown("intro_timetrial"))
    }

    @Test
    fun setIntroShown_persistsFlag() {
        prefs.setIntroShown("intro_survival_rank_0")
        assertTrue("Intro flag should persist after set", prefs.isIntroShown("intro_survival_rank_0"))
    }

    @Test
    fun setIntroShown_isIndependentPerKey() {
        prefs.setIntroShown("intro_survival_rank_0")
        assertTrue(prefs.isIntroShown("intro_survival_rank_0"))
        assertFalse("Other keys should remain unseen", prefs.isIntroShown("intro_survival_rank_1"))
        assertFalse(prefs.isIntroShown("intro_timetrial"))
    }

    @Test
    fun setIntroShown_survivalRanksAreIndependent() {
        prefs.setIntroShown("intro_survival_rank_0")
        prefs.setIntroShown("intro_survival_rank_2")
        assertTrue(prefs.isIntroShown("intro_survival_rank_0"))
        assertFalse(prefs.isIntroShown("intro_survival_rank_1"))
        assertTrue(prefs.isIntroShown("intro_survival_rank_2"))
    }

    @Test
    fun resetAll_clearsIntroFlags() {
        prefs.setIntroShown("intro_survival_rank_0")
        prefs.setIntroShown("intro_timetrial")
        prefs.resetAll()
        assertFalse("Flags should clear after resetAll", prefs.isIntroShown("intro_survival_rank_0"))
        assertFalse(prefs.isIntroShown("intro_timetrial"))
    }

    // === setDebugMode ===

    @Test
    fun setDebugMode_true_setsMaxExamTo50() {
        prefs.setDebugMode(true)
        assertEquals("Debug mode should set max exam questions to 50", 50, prefs.getMaxExamQuestions())
    }

    @Test
    fun setDebugMode_true_unlocksSimulacro() {
        prefs.setDebugMode(true)
        assertTrue("Debug mode should unlock simulacro", prefs.isSimulacroUnlocked())
    }

    @Test
    fun setDebugMode_false_restoresSavedState() {
        // Set up initial state
        prefs.setMaxExamQuestions(20)
        prefs.setSimulacroUnlocked()
        // Enable debug mode (saves current state)
        prefs.setDebugMode(true)
        // Disable debug mode (should restore saved state)
        prefs.setDebugMode(false)
        assertFalse("Debug mode should be off", prefs.isDebugMode())
        assertEquals("Should restore saved max exam questions", 20, prefs.getMaxExamQuestions())
        assertTrue("Should restore saved simulacro unlocked", prefs.isSimulacroUnlocked())
    }

    @Test
    fun setDebugMode_false_withoutSnapshot_fallsBackToDefaults() {
        // After resetAll, no SAVED_* keys exist. Calling setDebugMode(false)
        // directly (without first calling setDebugMode(true)) should fall back
        // to the default values since there's no saved snapshot to restore.
        prefs.resetAll()
        prefs.setDebugMode(false)
        assertEquals("Should fall back to default max exam questions", 10, prefs.getMaxExamQuestions())
        assertFalse("Should fall back to default simulacro locked", prefs.isSimulacroUnlocked())
    }

    // === Achievements ===

    @Test
    fun getAchievements_emptyByDefault() {
        assertTrue("Achievements should be empty by default", prefs.getAchievements().isEmpty())
    }

    @Test
    fun saveAchievements_roundTrip() {
        val achievements = mapOf("first_correct" to 1000L, "combo5" to 2000L)
        prefs.saveAchievements(achievements)
        val retrieved = prefs.getAchievements()
        assertEquals(2, retrieved.size)
        assertEquals(1000L, retrieved["first_correct"])
        assertEquals(2000L, retrieved["combo5"])
    }

    // === Daily Missions ===

    @Test
    fun getDailyMissions_nullByDefault() {
        assertFalse("Daily missions should be null by default", prefs.getDailyMissions() != null)
    }

    @Test
    fun saveDailyMissions_roundTrip() {
        val mission = Mission(
            type = "streak", icon = "🔥", text = "Test mission",
            target = 5, current = 0, completed = false, reward = 50,
            key = "streak", testId = null, difficulty = MissionDifficulty.EASY
        )
        val data = MissionData(date = "2026-01-01", missions = listOf(mission))
        prefs.saveDailyMissions(data)
        val retrieved = prefs.getDailyMissions()
        assertTrue("Retrieved missions should not be null", retrieved != null)
        assertEquals("2026-01-01", retrieved!!.date)
        assertEquals(1, retrieved.missions.size)
        assertEquals("streak", retrieved.missions[0].type)
        assertEquals(5, retrieved.missions[0].target)
    }

    // === Simulacro History ===

    @Test
    fun getSimulacroHistory_emptyByDefault() {
        assertTrue("Simulacro history should be empty by default", prefs.getSimulacroHistory().isEmpty())
    }

    @Test
    fun addSimulacroHistory_addsEntries() {
        val entry1 = SimulacroHistoryEntry("2026-01-01", 40.0f, 60, 30, 10, true)
        prefs.addSimulacroHistory(entry1)
        val history = prefs.getSimulacroHistory()
        assertEquals(1, history.size)
        assertEquals(40.0f, history[0].points, 0.01f)
        assertEquals(60, history[0].correct)
    }

    @Test
    fun addSimulacroHistory_preservesOrder() {
        val entry1 = SimulacroHistoryEntry("2026-01-01", 40.0f, 60, 30, 10, true)
        val entry2 = SimulacroHistoryEntry("2026-01-02", 35.0f, 50, 40, 10, false)
        val entry3 = SimulacroHistoryEntry("2026-01-03", 50.0f, 80, 10, 10, true)
        prefs.addSimulacroHistory(entry1)
        prefs.addSimulacroHistory(entry2)
        prefs.addSimulacroHistory(entry3)
        val history = prefs.getSimulacroHistory()
        assertEquals(3, history.size)
        assertEquals("2026-01-01", history[0].date)
        assertEquals("2026-01-02", history[1].date)
        assertEquals("2026-01-03", history[2].date)
    }

    // === Records ===

    @Test
    fun setRecord_andGet_perMode() {
        assertEquals("Default record should be 0", 0, prefs.getRecord("survival"))
        prefs.setRecord("survival", 500)
        assertEquals(500, prefs.getRecord("survival"))
        // Other modes should be independent
        assertEquals("Timetrial record should still be 0", 0, prefs.getRecord("timetrial"))
        prefs.setRecord("timetrial", 300)
        assertEquals(300, prefs.getRecord("timetrial"))
        assertEquals("Survival record should still be 500", 500, prefs.getRecord("survival"))
    }

    @Test
    fun setRecordCombo_andGet_perMode() {
        assertEquals(0, prefs.getRecordCombo("survival"))
        prefs.setRecordCombo("survival", 15)
        assertEquals(15, prefs.getRecordCombo("survival"))
        assertEquals(0, prefs.getRecordCombo("quick"))
    }

    @Test
    fun setRecordAcc_andGet_perMode() {
        assertEquals(0, prefs.getRecordAcc("survival"))
        prefs.setRecordAcc("survival", 85)
        assertEquals(85, prefs.getRecordAcc("survival"))
        assertEquals(0, prefs.getRecordAcc("quick"))
    }

    // === Law Mastered ===

    @Test
    fun setLawMastered_andIsLawMastered_roundTrip() {
        assertFalse("Law should not be mastered by default", prefs.isLawMastered("test1"))
        prefs.setLawMastered("test1")
        assertTrue("Law should be mastered after set", prefs.isLawMastered("test1"))
        assertFalse("Other law should not be mastered", prefs.isLawMastered("test2"))
    }

    // === Max Exam Questions ===

    @Test
    fun getMaxExamQuestions_defaultIs10() {
        assertEquals("Default max exam questions should be 10", 10, prefs.getMaxExamQuestions())
    }

    @Test
    fun setMaxExamQuestions_changesValue() {
        prefs.setMaxExamQuestions(20)
        assertEquals(20, prefs.getMaxExamQuestions())
    }

    // === Simulacro Unlocked ===

    @Test
    fun isSimulacroUnlocked_falseByDefault() {
        assertFalse("Simulacro should be locked by default", prefs.isSimulacroUnlocked())
    }

    @Test
    fun setSimulacroUnlocked_unlocksSimulacro() {
        prefs.setSimulacroUnlocked()
        assertTrue("Simulacro should be unlocked after set", prefs.isSimulacroUnlocked())
    }

    // === Last Known Rank Index ===

    @Test
    fun getLastKnownRankIndex_defaultIs0() {
        assertEquals("Default last known rank index should be 0", 0, prefs.getLastKnownRankIndex())
    }

    @Test
    fun setLastKnownRankIndex_roundTrip() {
        prefs.setLastKnownRankIndex(3)
        assertEquals(3, prefs.getLastKnownRankIndex())
    }

    // === Games Played ===

    @Test
    fun getGamesPlayed_defaultIs0() {
        assertEquals(0, prefs.getGamesPlayed())
    }

    @Test
    fun incrementGamesPlayed_increments() {
        assertEquals(1, prefs.incrementGamesPlayed())
        assertEquals(2, prefs.incrementGamesPlayed())
        assertEquals(3, prefs.incrementGamesPlayed())
        assertEquals(3, prefs.getGamesPlayed())
    }

    // === XP ===

    @Test
    fun getXP_defaultIs0() {
        assertEquals(0, prefs.getXP())
    }

    @Test
    fun addXP_accumulates() {
        assertEquals(100, prefs.addXP(100))
        assertEquals(150, prefs.addXP(50))
        assertEquals(150, prefs.getXP())
    }

    // === Multiplier ===

    @Test
    fun getMultiplier_defaultIs1() {
        assertEquals(1, prefs.getMultiplier())
    }

    @Test
    fun setMultiplier_roundTrip() {
        prefs.setMultiplier(2)
        assertEquals(2, prefs.getMultiplier())
    }

    // === Stats ===

    @Test
    fun getStats_emptyByDefault() {
        assertTrue("Stats should be empty by default", prefs.getStats().isEmpty())
    }

    @Test
    fun saveStats_roundTrip() {
        val stats = mapOf("test1:1" to com.opoleyes.data.model.QuestionStat(5, 2))
        prefs.saveStats(stats)
        val retrieved = prefs.getStats()
        assertEquals(5, retrieved["test1:1"]?.correct)
        assertEquals(2, retrieved["test1:1"]?.wrong)
    }

    // === resetAll clears everything ===

    @Test
    fun resetAll_clearsXP() {
        prefs.addXP(500)
        prefs.resetAll()
        assertEquals("XP should be 0 after resetAll", 0, prefs.getXP())
    }

    @Test
    fun resetAll_clearsGamesPlayed() {
        prefs.incrementGamesPlayed()
        prefs.incrementGamesPlayed()
        prefs.resetAll()
        assertEquals("Games played should be 0 after resetAll", 0, prefs.getGamesPlayed())
    }

    @Test
    fun resetAll_clearsRecords() {
        prefs.setRecord("survival", 500)
        prefs.setRecordCombo("survival", 15)
        prefs.setRecordAcc("survival", 85)
        prefs.resetAll()
        assertEquals("Record should be 0 after resetAll", 0, prefs.getRecord("survival"))
        assertEquals("Combo record should be 0 after resetAll", 0, prefs.getRecordCombo("survival"))
        assertEquals("Acc record should be 0 after resetAll", 0, prefs.getRecordAcc("survival"))
    }

    @Test
    fun resetAll_clearsLawMastered() {
        prefs.setLawMastered("test1")
        prefs.resetAll()
        assertFalse("Law mastered should be cleared after resetAll", prefs.isLawMastered("test1"))
    }

    @Test
    fun resetAll_clearsAchievements() {
        prefs.saveAchievements(mapOf("first_correct" to 1000L))
        prefs.resetAll()
        assertTrue("Achievements should be empty after resetAll", prefs.getAchievements().isEmpty())
    }

    @Test
    fun resetAll_clearsSimulacroHistory() {
        prefs.addSimulacroHistory(SimulacroHistoryEntry("2026-01-01", 40f, 60, 30, 10, true))
        prefs.resetAll()
        assertTrue("Simulacro history should be empty after resetAll", prefs.getSimulacroHistory().isEmpty())
    }

    @Test
    fun resetAll_clearsSimulacroUnlocked() {
        prefs.setSimulacroUnlocked()
        prefs.resetAll()
        assertFalse("Simulacro should be locked after resetAll", prefs.isSimulacroUnlocked())
    }

    @Test
    fun resetAll_clearsMaxExamQuestions() {
        prefs.setMaxExamQuestions(50)
        prefs.resetAll()
        assertEquals("Max exam questions should be 10 after resetAll", 10, prefs.getMaxExamQuestions())
    }

    @Test
    fun resetAll_clearsStats() {
        prefs.saveStats(mapOf("test1:1" to com.opoleyes.data.model.QuestionStat(5, 2)))
        prefs.resetAll()
        assertTrue("Stats should be empty after resetAll", prefs.getStats().isEmpty())
    }

    @Test
    fun resetAll_clearsMultiplier() {
        prefs.setMultiplier(2)
        prefs.resetAll()
        assertEquals("Multiplier should be 1 after resetAll", 1, prefs.getMultiplier())
    }

    @Test
    fun resetAll_clearsLastKnownRankIndex() {
        prefs.setLastKnownRankIndex(5)
        prefs.resetAll()
        assertEquals("Last known rank index should be 0 after resetAll", 0, prefs.getLastKnownRankIndex())
    }

    @Test
    fun resetAll_clearsDailyMissions() {
        val data = MissionData(date = LocalDate.now().toString(), missions = emptyList())
        prefs.saveDailyMissions(data)
        prefs.resetAll()
        assertFalse("Daily missions should be null after resetAll", prefs.getDailyMissions() != null)
    }
}
