package com.opoleyes.data.repository

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.opoleyes.TestContextProvider
import com.opoleyes.data.local.PreferencesManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressRepositoryTest {

    private lateinit var prefs: PreferencesManager
    private lateinit var repo: ProgressRepository

    @Before
    fun setup() {
        val ctx = TestContextProvider.getContext()
        prefs = PreferencesManager(ctx)
        prefs.resetAll()
        repo = ProgressRepository(ctx)
    }

    @Test
    fun xp_addsCorrectly() {
        assertEquals(0, repo.getXP())
        repo.addXP(100)
        assertEquals(100, repo.getXP())
        repo.addXP(50)
        assertEquals(150, repo.getXP())
    }

    @Test
    fun rankIndex_correctForXpThresholds() {
        assertEquals(0, repo.getRankIndex())
        repo.addXP(200)
        assertEquals(1, repo.getRankIndex())
        repo.addXP(600)
        assertEquals(2, repo.getRankIndex())
        repo.addXP(1700)
        assertEquals(3, repo.getRankIndex())
    }

    @Test
    fun rankIndex_clampsAtMax() {
        repo.addXP(100000)
        assertEquals(6, repo.getRankIndex())
    }

    @Test
    fun rankIndexForXP_matchesGetRankIndex() {
        repo.addXP(500)
        assertEquals(repo.getRankIndex(), repo.getRankIndexForXP(repo.getXP()))
    }

    @Test
    fun achievements_unlockOnceOnly() {
        val first = repo.unlockAchievement("first_correct")
        assertTrue("First unlock returns achievement", first != null)
        val second = repo.unlockAchievement("first_correct")
        assertTrue("Second unlock returns null", second == null)
    }

    @Test
    fun gamesPlayed_increments() {
        assertEquals(0, repo.getGamesPlayed())
        repo.incrementGamesPlayed()
        assertEquals(1, repo.getGamesPlayed())
        repo.incrementGamesPlayed()
        assertEquals(2, repo.getGamesPlayed())
    }

    @Test
    fun record_setAndGet() {
        assertEquals(0, repo.getRecord("survival"))
        repo.setRecord("survival", 500)
        assertEquals(500, repo.getRecord("survival"))
    }

    @Test
    fun powerUps_addAndClear() {
        prefs.setFreePowerUps(listOf("shield", "hint"))
        assertEquals(listOf("shield", "hint"), prefs.getFreePowerUps())
        prefs.clearFreePowerUps()
        assertTrue(prefs.getFreePowerUps().isEmpty())
    }
}
