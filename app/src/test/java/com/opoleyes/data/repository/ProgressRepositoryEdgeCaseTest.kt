package com.opoleyes.data.repository

import com.opoleyes.TestContextProvider
import com.opoleyes.data.local.PreferencesManager
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
class ProgressRepositoryEdgeCaseTest {

    private lateinit var repo: ProgressRepository
    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        val ctx = TestContextProvider.getContext()
        prefs = PreferencesManager(ctx)
        prefs.resetAll()
        repo = ProgressRepository(ctx)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    // === Edge cases: XP ===

    @Test
    fun fun_getXP_freshStart_returns0() {
        assertEquals(0, repo.getXP())
    }

    @Test
    fun fun_addXP_accumulates() {
        repo.addXP(100)
        repo.addXP(200)
        assertEquals(300, repo.getXP())
    }

    @Test
    fun fun_addXP_negative_canGoNegative() {
        repo.addXP(50)
        repo.addXP(-100)
        // addXP does not clamp to 0
        assertEquals(-50, repo.getXP())
    }

    // === Edge cases: rank ===

    @Test
    fun fun_getRankIndex_freshStart_returns0() {
        assertEquals(0, repo.getRankIndex())
    }

    @Test
    fun fun_getRankIndex_highXP_returnsLastRank() {
        prefs.setDebugMode(true)
        val idx = repo.getRankIndex()
        assertTrue("Debug mode should give high rank", idx >= 10)
    }

    @Test
    fun fun_getXPProgress_atMaxRank_returns100() {
        prefs.setDebugMode(true)
        val progress = repo.getXPProgress()
        assertEquals(100, progress.pct)
    }

    // === Edge cases: unlocks ===

    @Test
    fun fun_getUnlocks_freshStart_onlySurvival() {
        val u = repo.getUnlocks()
        assertTrue(u.survival)
        assertFalse(u.timetrial)
        assertFalse(u.quick)
        assertFalse(u.challenge)
        assertFalse(u.exam)
    }

    @Test
    fun fun_getUnlocks_debugMode_allUnlocked() {
        prefs.setDebugMode(true)
        val u = repo.getUnlocks()
        assertTrue(u.survival)
        assertTrue(u.timetrial)
        assertTrue(u.quick)
        assertTrue(u.challenge)
        assertTrue(u.exam)
    }

    @Test
    fun fun_isUnlocked_unknownFeature_returnsFalse() {
        assertFalse(repo.isUnlocked("nonexistent"))
    }

    // === Edge cases: achievements ===

    @Test
    fun fun_unlockAchievement_alreadyUnlocked_returnsNull() {
        repo.unlockAchievement("first_correct")
        val second = repo.unlockAchievement("first_correct")
        assertEquals(null, second)
    }

    @Test
    fun fun_unlockAchievement_unknownId_returnsNull() {
        val result = repo.unlockAchievement("nonexistent_achievement")
        assertEquals(null, result)
    }

    // === Edge cases: records ===

    @Test
    fun fun_getRecord_freshStart_returns0() {
        assertEquals(0, repo.getRecord("survival"))
    }

    @Test
    fun fun_setRecord_updatesValue() {
        repo.setRecord("survival", 500)
        assertEquals(500, repo.getRecord("survival"))
    }

    @Test
    fun fun_getMaxComboRecord_returnsMaxAcrossModes() {
        repo.setRecordCombo("survival", 10)
        repo.setRecordCombo("timetrial", 15)
        repo.setRecordCombo("quick", 8)
        repo.setRecordCombo("challenge", 20)
        assertEquals(20, repo.getMaxComboRecord())
    }

    // === Edge cases: games played ===

    @Test
    fun fun_getGamesPlayed_freshStart_returns0() {
        assertEquals(0, repo.getGamesPlayed())
    }

    @Test
    fun fun_incrementGamesPlayed_accumulates() {
        repo.incrementGamesPlayed()
        repo.incrementGamesPlayed()
        repo.incrementGamesPlayed()
        assertEquals(3, repo.getGamesPlayed())
    }

    // === Edge cases: reset ===

    @Test
    fun fun_resetAll_clearsEverything() {
        repo.addXP(1000)
        repo.setRecord("survival", 500)
        repo.incrementGamesPlayed()
        repo.unlockAchievement("first_correct")
        repo.resetAll()
        assertEquals(0, repo.getXP())
        assertEquals(0, repo.getRecord("survival"))
        assertEquals(0, repo.getGamesPlayed())
        assertTrue(repo.getAchievements().isEmpty())
    }

    // === Edge cases: mission count ===

    @Test
    fun fun_getMissionCount_freshStart_returns1() {
        assertEquals(1, repo.getMissionCount())
    }

    @Test
    fun fun_getMissionCount_debugMode_returns3() {
        prefs.setDebugMode(true)
        assertEquals(3, repo.getMissionCount())
    }
}
