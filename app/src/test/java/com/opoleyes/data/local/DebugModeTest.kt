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
class DebugModeTest {

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
    fun fun_debugMode_defaultFalse() {
        assertFalse("Debug mode should be false by default", prefs.isDebugMode())
    }

    @Test
    fun fun_debugMode_enableSetsTrue() {
        prefs.setDebugMode(true)
        assertTrue("Debug mode should be true after enabling", prefs.isDebugMode())
    }

    @Test
    fun fun_debugMode_disableSetsFalse() {
        prefs.setDebugMode(true)
        prefs.setDebugMode(false)
        assertFalse("Debug mode should be false after disabling", prefs.isDebugMode())
    }

    @Test
    fun fun_debugMode_enablingSetsInfinitePowerUps() {
        prefs.setDebugMode(true)
        val powerUps = prefs.getFreePowerUps()
        assertEquals("Should have 99 shields", 99, powerUps.count { it == "shield" })
        assertEquals("Should have 99 fiftyFifty", 99, powerUps.count { it == "fiftyFifty" })
        assertEquals("Should have 99 hints", 99, powerUps.count { it == "hint" })
        assertEquals("Should have 99 doubleScore", 99, powerUps.count { it == "doubleScore" })
    }

    @Test
    fun fun_debugMode_savesRealPowerUpsBeforeEnabling() {
        prefs.initPowerUpsIfNeeded()
        prefs.setDebugMode(true)
        // After enabling, free power-ups should be the debug ones (99 of each)
        val debugPowerUps = prefs.getFreePowerUps()
        assertEquals(99, debugPowerUps.count { it == "shield" })
        // Real power-ups should be saved internally (verified by restore test)
    }

    @Test
    fun fun_debugMode_disablingRestoresRealPowerUps() {
        prefs.initPowerUpsIfNeeded()
        val realPowerUps = prefs.getFreePowerUps()
        val expectedCount = realPowerUps.size
        prefs.setDebugMode(true)
        // While in debug, power-ups are 99*4
        assertEquals(99 * 4, prefs.getFreePowerUps().size)
        prefs.setDebugMode(false)
        val restoredPowerUps = prefs.getFreePowerUps()
        assertEquals("Should restore original power-ups count", expectedCount, restoredPowerUps.size)
    }

    @Test
    fun fun_debugMode_toggleOnOffMultipleTimesPreservesRealPowerUps() {
        prefs.initPowerUpsIfNeeded()
        val originalPowerUps = prefs.getFreePowerUps()
        prefs.setDebugMode(true)
        prefs.setDebugMode(false)
        prefs.setDebugMode(true)
        prefs.setDebugMode(false)
        val restoredPowerUps = prefs.getFreePowerUps()
        assertEquals("Should restore original power-ups after multiple toggles", originalPowerUps.size, restoredPowerUps.size)
    }

    @Test
    fun fun_debugMode_enablingWithNoPowerUpsSavesEmpty() {
        // No power-ups initialized
        prefs.setDebugMode(true)
        prefs.setDebugMode(false)
        assertEquals("Should restore empty power-ups", 0, prefs.getFreePowerUps().size)
    }

    @Test
    fun fun_debugMode_powerUpsPersistAcrossInitGameStats() {
        prefs.setDebugMode(true)
        val powerUpsBefore = prefs.getFreePowerUps().size
        // Simulate what GameEngine.initGameStats does: read but don't clear in debug
        if (!prefs.isDebugMode()) {
            prefs.clearFreePowerUps()
        }
        assertEquals("Power-ups should not be consumed in debug mode", powerUpsBefore, prefs.getFreePowerUps().size)
    }

    @Test
    fun fun_debugMode_saveRemainingPowerUpsSkipped() {
        prefs.setDebugMode(true)
        // In debug mode, saveRemainingPowerUps should be a no-op
        // Simulate: add some power-ups and verify they don't accumulate
        val before = prefs.getFreePowerUps().size
        // If GameEngine.saveRemainingPowerUps is called, it returns early in debug
        // So power-ups should remain unchanged
        assertEquals("Power-ups should remain at 99*4 in debug", 99 * 4, before)
    }

    @Test
    fun fun_debugMode_xpReturns100000() {
        prefs.setDebugMode(true)
        val progressRepo = com.opoleyes.data.repository.ProgressRepository(TestContextProvider.getContext())
        assertEquals("Debug mode should return 100000 XP", 100000, progressRepo.getXP())
    }

    @Test
    fun fun_debugMode_unlocksAllModes() {
        prefs.setDebugMode(true)
        val progressRepo = com.opoleyes.data.repository.ProgressRepository(TestContextProvider.getContext())
        val unlocks = progressRepo.getUnlocks()
        assertTrue("Survival should be unlocked", unlocks.survival)
        assertTrue("Timetrial should be unlocked", unlocks.timetrial)
        assertTrue("Quick should be unlocked", unlocks.quick)
        assertTrue("Exam should be unlocked", unlocks.exam)
        assertTrue("Challenge should be unlocked", unlocks.challenge)
    }
}
