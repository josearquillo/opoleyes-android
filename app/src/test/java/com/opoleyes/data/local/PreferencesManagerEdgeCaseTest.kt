package com.opoleyes.data.local

import com.opoleyes.TestContextProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreferencesManagerEdgeCaseTest {

    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        prefs = PreferencesManager(TestContextProvider.getContext())
        prefs.resetAll()
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    // === Edge cases: XP ===

    @Test
    fun fun_addXP_negativeAmount_canGoNegative() {
        // addXP does not clamp to 0; it simply adds
        prefs.addXP(-100)
        assertEquals(-100, prefs.getXP())
    }

    @Test
    fun fun_addXP_largeAmount_storesCorrectly() {
        prefs.addXP(999999)
        assertEquals(999999, prefs.getXP())
    }

    // === Edge cases: power-ups ===

    @Test
    fun fun_setFreePowerUps_emptyList() {
        prefs.setFreePowerUps(emptyList())
        assertTrue(prefs.getFreePowerUps().isEmpty())
    }

    @Test
    fun fun_clearFreePowerUps_emptiesList() {
        prefs.setFreePowerUps(listOf("shield", "hint", "fiftyFifty"))
        prefs.clearFreePowerUps()
        assertTrue(prefs.getFreePowerUps().isEmpty())
    }

    @Test
    fun fun_setFreePowerUps_duplicatesAllowed() {
        prefs.setFreePowerUps(listOf("shield", "shield", "shield"))
        assertEquals(3, prefs.getFreePowerUps().size)
    }

    // === Edge cases: stats ===

    @Test
    fun fun_saveStats_emptyMap() {
        prefs.saveStats(emptyMap())
        assertTrue(prefs.getStats().isEmpty())
    }

    @Test
    fun fun_saveStats_overwritesPrevious() {
        val stats1 = mapOf("key1" to com.opoleyes.data.model.QuestionStat(correct = 5, wrong = 2))
        prefs.saveStats(stats1)
        val stats2 = mapOf("key1" to com.opoleyes.data.model.QuestionStat(correct = 10, wrong = 0))
        prefs.saveStats(stats2)
        assertEquals(10, prefs.getStats()["key1"]!!.correct)
        assertEquals(0, prefs.getStats()["key1"]!!.wrong)
    }

    // === Edge cases: achievements ===

    @Test
    fun fun_saveAchievements_emptyMap() {
        prefs.saveAchievements(emptyMap())
        assertTrue(prefs.getAchievements().isEmpty())
    }

    @Test
    fun fun_saveAchievements_overwritesPrevious() {
        prefs.saveAchievements(mapOf("ach1" to 1000L))
        prefs.saveAchievements(mapOf("ach2" to 2000L))
        val ach = prefs.getAchievements()
        assertFalse(ach.containsKey("ach1"))
        assertTrue(ach.containsKey("ach2"))
    }

    // === Edge cases: records ===

    @Test
    fun fun_getRecord_nonexistentMode_returns0() {
        assertEquals(0, prefs.getRecord("nonexistent_mode"))
    }

    @Test
    fun fun_getRecordCombo_nonexistentMode_returns0() {
        assertEquals(0, prefs.getRecordCombo("nonexistent_mode"))
    }

    @Test
    fun fun_getRecordAcc_nonexistentMode_returns0() {
        assertEquals(0, prefs.getRecordAcc("nonexistent_mode"))
    }

    // === Edge cases: debug mode ===

    @Test
    fun fun_isDebugMode_defaultFalse() {
        assertFalse(prefs.isDebugMode())
    }

    @Test
    fun fun_setDebugMode_true_persists() {
        prefs.setDebugMode(true)
        assertTrue(prefs.isDebugMode())
    }

    @Test
    fun fun_setDebugMode_false_afterTrue() {
        prefs.setDebugMode(true)
        prefs.setDebugMode(false)
        assertFalse(prefs.isDebugMode())
    }

    // === Edge cases: law mastered ===

    @Test
    fun fun_isLawMastered_defaultFalse() {
        assertFalse(prefs.isLawMastered("test1"))
    }

    @Test
    fun fun_setLawMastered_persists() {
        prefs.setLawMastered("test1")
        assertTrue(prefs.isLawMastered("test1"))
    }

    // === Edge cases: multiplier ===

    @Test
    fun fun_getMultiplier_default1() {
        assertEquals(1, prefs.getMultiplier())
    }

    @Test
    fun fun_setMultiplier_persists() {
        prefs.setMultiplier(2)
        assertEquals(2, prefs.getMultiplier())
    }

    // === Edge cases: games played ===

    @Test
    fun fun_getGamesPlayed_default0() {
        assertEquals(0, prefs.getGamesPlayed())
    }

    @Test
    fun fun_incrementGamesPlayed_returnsNewValue() {
        val result = prefs.incrementGamesPlayed()
        assertEquals(1, result)
        assertEquals(1, prefs.getGamesPlayed())
    }

    // === Edge cases: daily missions ===

    @Test
    fun fun_getDailyMissions_defaultNull() {
        assertEquals(null, prefs.getDailyMissions())
    }

    @Test
    fun fun_saveDailyMissions_persists() {
        val data = com.opoleyes.data.model.MissionData(
            "2026-07-28",
            listOf(com.opoleyes.data.model.Mission("test", "🎯", "Test mission", 1, 0, false, 50, "test_key"))
        )
        prefs.saveDailyMissions(data)
        val loaded = prefs.getDailyMissions()
        assertEquals("2026-07-28", loaded!!.date)
        assertEquals(1, loaded.missions.size)
    }

    // === Edge cases: reset ===

    @Test
    fun fun_resetAll_clearsAllData() {
        prefs.addXP(500)
        prefs.setRecord("survival", 1000)
        prefs.incrementGamesPlayed()
        prefs.setDebugMode(true)
        prefs.setFreePowerUps(listOf("shield"))
        prefs.resetAll()
        assertEquals(0, prefs.getXP())
        assertEquals(0, prefs.getRecord("survival"))
        assertEquals(0, prefs.getGamesPlayed())
        assertFalse(prefs.isDebugMode())
        assertTrue(prefs.getFreePowerUps().isEmpty())
    }
}
