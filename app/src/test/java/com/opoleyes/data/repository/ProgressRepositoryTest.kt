package com.opoleyes.data.repository

import com.opoleyes.TestContextProvider
import com.opoleyes.data.local.PreferencesManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressRepositoryTest {

    private lateinit var repo: ProgressRepository
    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        val context = TestContextProvider.getContext()
        prefs = PreferencesManager(context)
        prefs.resetAll()
        repo = ProgressRepository(context)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun fun_getXP_startsAt0() {
        assertEquals(0, repo.getXP())
    }

    @Test
    fun fun_addXP_increments() {
        repo.addXP(100)
        assertEquals(100, repo.getXP())
        repo.addXP(50)
        assertEquals(150, repo.getXP())
    }

    @Test
    fun fun_getRankIndex_startsAt0() {
        assertEquals(0, repo.getRankIndex())
    }

    @Test
    fun fun_getRankIndexForXP() {
        assertEquals(0, repo.getRankIndexForXP(0))
        assertEquals(0, repo.getRankIndexForXP(499))
        assertEquals(1, repo.getRankIndexForXP(500))
        assertEquals(1, repo.getRankIndexForXP(1499))
        assertEquals(2, repo.getRankIndexForXP(1500))
        assertEquals(11, repo.getRankIndexForXP(100000))
        assertEquals(11, repo.getRankIndexForXP(999999))
    }

    @Test
    fun fun_getRank_returnsCorrectRank() {
        assertEquals("Novato", repo.getRank().name)
        repo.addXP(500)
        assertEquals("Principiante", repo.getRank().name)
        repo.addXP(1000)
        assertEquals("Aprendiz", repo.getRank().name)
    }

    @Test
    fun fun_getXPProgress() {
        val progress = repo.getXPProgress()
        assertEquals(0, progress.pct)
        assertEquals(0, progress.intoRank)
    }

    @Test
    fun fun_getXPProgressFor_midRank() {
        repo.addXP(750)
        val progress = repo.getXPProgressFor(750)
        assertEquals(25, progress.pct)
        assertEquals(250, progress.intoRank)
        assertEquals(1000, progress.rankSpan)
    }

    @Test
    fun fun_getXPProgressFor_maxRank() {
        val progress = repo.getXPProgressFor(100000)
        assertEquals(100, progress.pct)
    }

    @Test
    fun fun_getUnlocks_atRank0() {
        val u = repo.getUnlocks()
        assertTrue(u.survival)
        assertFalse(u.timetrial)
        assertFalse(u.quick)
        assertFalse(u.challenge)
        assertFalse(u.exam)
        assertTrue(u.powerUps)
        assertTrue(u.hint)
        assertTrue(u.shield)
        assertTrue(u.fiftyFifty)
        assertTrue(u.doubleScore)
        assertFalse(u.lifeRecovery)
        assertEquals(1, u.dailyMissions)
    }

    @Test
    fun fun_getUnlocks_atRank1() {
        repo.addXP(500)
        val u = repo.getUnlocks()
        assertTrue(u.timetrial)
        assertTrue(u.shield)
        assertFalse(u.quick)
        assertFalse(u.exam)
    }

    @Test
    fun fun_getUnlocks_atRank2() {
        repo.addXP(1500)
        val u = repo.getUnlocks()
        assertTrue(u.quick)
        assertTrue(u.hint)
        assertFalse(u.exam)
        assertEquals(2, u.dailyMissions)
    }

    @Test
    fun fun_getUnlocks_atRank3() {
        repo.addXP(3500)
        val u = repo.getUnlocks()
        assertTrue(u.powerUps)
        assertTrue(u.fiftyFifty)
        assertTrue(u.exam)
    }

    @Test
    fun fun_getUnlocks_atRank4() {
        repo.addXP(7000)
        val u = repo.getUnlocks()
        assertTrue(u.challenge)
        assertTrue(u.lifeRecovery)
    }

    @Test
    fun fun_getUnlocks_atRank5() {
        repo.addXP(12000)
        val u = repo.getUnlocks()
        assertTrue(u.doubleScore)
    }

    @Test
    fun fun_getUnlocks_atRank8() {
        repo.addXP(45000)
        val u = repo.getUnlocks()
        assertEquals(3, u.dailyMissions)
    }

    @Test
    fun fun_isUnlocked() {
        assertTrue(repo.isUnlocked("survival"))
        assertFalse(repo.isUnlocked("timetrial"))
        assertTrue(repo.isUnlocked("shield"))
        assertTrue(repo.isUnlocked("hint"))
        assertTrue(repo.isUnlocked("fiftyFifty"))
        assertTrue(repo.isUnlocked("doubleScore"))
        assertFalse(repo.isUnlocked("exam"))
        assertFalse(repo.isUnlocked("nonexistent"))
    }

    @Test
    fun fun_getMissionCount_atRank0() {
        assertEquals(1, repo.getMissionCount())
    }

    @Test
    fun fun_getMissionCount_atRank2() {
        repo.addXP(1500)
        assertEquals(2, repo.getMissionCount())
    }

    @Test
    fun fun_unlockAchievement_firstTime() {
        val ach = repo.unlockAchievement("first_correct")
        assertNotNull(ach)
        assertEquals("first_correct", ach?.id)
    }

    @Test
    fun fun_unlockAchievement_secondTimeReturnsNull() {
        repo.unlockAchievement("first_correct")
        assertNull(repo.unlockAchievement("first_correct"))
    }

    @Test
    fun fun_getAchievements_emptyByDefault() {
        assertTrue(repo.getAchievements().isEmpty())
    }

    @Test
    fun fun_gamesPlayed_startsAt0() {
        assertEquals(0, repo.getGamesPlayed())
    }

    @Test
    fun fun_incrementGamesPlayed() {
        assertEquals(1, repo.incrementGamesPlayed())
        assertEquals(2, repo.incrementGamesPlayed())
        assertEquals(2, repo.getGamesPlayed())
    }

    @Test
    fun fun_record_setAndGet() {
        repo.setRecord("survival", 500)
        assertEquals(500, repo.getRecord("survival"))
    }

    @Test
    fun fun_recordCombo_setAndGet() {
        repo.setRecordCombo("survival", 15)
        assertEquals(15, repo.getRecordCombo("survival"))
    }

    @Test
    fun fun_recordAcc_setAndGet() {
        repo.setRecordAcc("survival", 90)
        assertEquals(90, repo.getRecordAcc("survival"))
    }

    @Test
    fun fun_getMaxComboRecord() {
        repo.setRecordCombo("survival", 10)
        repo.setRecordCombo("timetrial", 20)
        repo.setRecordCombo("quick", 5)
        repo.setRecordCombo("challenge", 15)
        assertEquals(20, repo.getMaxComboRecord())
    }

    @Test
    fun fun_resetAll() {
        repo.addXP(1000)
        repo.incrementGamesPlayed()
        repo.resetAll()
        assertEquals(0, repo.getXP())
        assertEquals(0, repo.getGamesPlayed())
    }

    @Test
    fun fun_addXP_withMultiplier() {
        prefs.setMultiplier(2)
        repo.addXP(100)
        assertEquals(200, repo.getXP())
    }

    @Test
    fun fun_addXP_multiplierResetsAfterUse() {
        prefs.setMultiplier(2)
        repo.addXP(100)
        assertEquals(1, prefs.getMultiplier())
    }
}
