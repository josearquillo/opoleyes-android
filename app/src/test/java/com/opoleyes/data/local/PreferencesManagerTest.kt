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
class PreferencesManagerTest {

    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        val context = TestContextProvider.getContext()
        prefs = PreferencesManager(context)
        prefs.resetAll()
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun fun_initPowerUpsIfNeeded_setsInitialPowerUps() {
        prefs.initPowerUpsIfNeeded()
        val powerUps = prefs.getFreePowerUps()
        assertEquals(4, powerUps.size)
        assertTrue(powerUps.contains("shield"))
        assertTrue(powerUps.contains("fiftyFifty"))
        assertTrue(powerUps.contains("hint"))
        assertTrue(powerUps.contains("doubleScore"))
    }

    @Test
    fun fun_initPowerUpsIfNeeded_doesNotDoubleInit() {
        prefs.initPowerUpsIfNeeded()
        val firstCount = prefs.getFreePowerUps().size
        prefs.initPowerUpsIfNeeded()
        val secondCount = prefs.getFreePowerUps().size
        assertEquals("Should not double-init power-ups", firstCount, secondCount)
    }

    @Test
    fun fun_resetAll_clearsEverything() {
        prefs.addXP(500)
        prefs.setRecord("survival", 1000)
        prefs.setDebugMode(true)
        prefs.initPowerUpsIfNeeded()
        prefs.resetAll()
        assertEquals(0, prefs.getXP())
        assertEquals(0, prefs.getRecord("survival"))
        assertFalse(prefs.isDebugMode())
        assertTrue(prefs.getFreePowerUps().isEmpty())
    }

    @Test
    fun fun_resetAll_clearsPowerUpsInitialized() {
        prefs.initPowerUpsIfNeeded()
        prefs.resetAll()
        // After reset, init should work again
        prefs.initPowerUpsIfNeeded()
        assertEquals(4, prefs.getFreePowerUps().size)
    }

    @Test
    fun fun_addXP_appliesMultiplier() {
        prefs.setMultiplier(2)
        val newXp = prefs.addXP(100)
        assertEquals(200, newXp)
        assertEquals(200, prefs.getXP())
    }

    @Test
    fun fun_addXP_resetsMultiplierAfterUse() {
        prefs.setMultiplier(2)
        prefs.addXP(100)
        assertEquals(1, prefs.getMultiplier())
    }

    @Test
    fun fun_addXP_noMultiplierByDefault() {
        val newXp = prefs.addXP(100)
        assertEquals(100, newXp)
        assertEquals(1, prefs.getMultiplier())
    }

    @Test
    fun fun_setFreePowerUps_overwrites() {
        prefs.setFreePowerUps(listOf("shield", "shield"))
        assertEquals(2, prefs.getFreePowerUps().size)
        prefs.setFreePowerUps(listOf("hint"))
        assertEquals(1, prefs.getFreePowerUps().size)
    }

    @Test
    fun fun_clearFreePowerUps_emptiesList() {
        prefs.setFreePowerUps(listOf("shield", "hint"))
        prefs.clearFreePowerUps()
        assertTrue(prefs.getFreePowerUps().isEmpty())
    }

    @Test
    fun fun_getFreePowerUps_emptyByDefault() {
        assertTrue(prefs.getFreePowerUps().isEmpty())
    }

    @Test
    fun fun_getXP_zeroByDefault() {
        assertEquals(0, prefs.getXP())
    }

    @Test
    fun fun_getMultiplier_oneByDefault() {
        assertEquals(1, prefs.getMultiplier())
    }

    @Test
    fun fun_getGamesPlayed_zeroByDefault() {
        assertEquals(0, prefs.getGamesPlayed())
    }

    @Test
    fun fun_incrementGamesPlayed() {
        assertEquals(1, prefs.incrementGamesPlayed())
        assertEquals(2, prefs.incrementGamesPlayed())
        assertEquals(2, prefs.getGamesPlayed())
    }

    @Test
    fun fun_setRecord_andGetRecord() {
        prefs.setRecord("survival", 500)
        assertEquals(500, prefs.getRecord("survival"))
        prefs.setRecord("timetrial", 300)
        assertEquals(300, prefs.getRecord("timetrial"))
        assertEquals(500, prefs.getRecord("survival"))
    }

    @Test
    fun fun_setRecordCombo_andGetRecordCombo() {
        prefs.setRecordCombo("survival", 15)
        assertEquals(15, prefs.getRecordCombo("survival"))
    }

    @Test
    fun fun_setRecordAcc_andGetRecordAcc() {
        prefs.setRecordAcc("challenge", 90)
        assertEquals(90, prefs.getRecordAcc("challenge"))
    }

    @Test
    fun fun_isDebugMode_falseByDefault() {
        assertFalse(prefs.isDebugMode())
    }

    @Test
    fun fun_setDebugMode_trueAndFalse() {
        prefs.setDebugMode(true)
        assertTrue(prefs.isDebugMode())
        prefs.setDebugMode(false)
        assertFalse(prefs.isDebugMode())
    }

    @Test
    fun fun_isLawMastered_falseByDefault() {
        assertFalse(prefs.isLawMastered("test1"))
    }

    @Test
    fun fun_setLawMastered_andCheck() {
        prefs.setLawMastered("test1")
        assertTrue(prefs.isLawMastered("test1"))
        assertFalse(prefs.isLawMastered("test2"))
    }

    @Test
    fun fun_getAchievements_emptyByDefault() {
        assertTrue(prefs.getAchievements().isEmpty())
    }

    @Test
    fun fun_saveAchievements_andGet() {
        val achievements = mapOf("ach1" to 1000L, "ach2" to 2000L)
        prefs.saveAchievements(achievements)
        val retrieved = prefs.getAchievements()
        assertEquals(2, retrieved.size)
        assertEquals(1000L, retrieved["ach1"])
        assertEquals(2000L, retrieved["ach2"])
    }

    @Test
    fun fun_getDailyMissions_nullByDefault() {
        assertEquals(null, prefs.getDailyMissions())
    }
}
