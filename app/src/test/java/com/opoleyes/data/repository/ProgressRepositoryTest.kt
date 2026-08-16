package com.opoleyes.data.repository

import org.junit.Before
import org.junit.Test
import com.opoleyes.FakePreferencesManager
import com.opoleyes.data.model.SimulacroHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class ProgressRepositoryTest {

    private lateinit var prefs: FakePreferencesManager
    private lateinit var repo: ProgressRepository

    @Before
    fun setup() {
        prefs = FakePreferencesManager()
        prefs.resetAll()
        repo = ProgressRepository(prefs)
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
        repo.addXP(8000)
        assertEquals(1, repo.getRankIndex())
        repo.addXP(10000)
        assertEquals(2, repo.getRankIndex())
        repo.addXP(13000)
        assertEquals(3, repo.getRankIndex())
    }

    @Test
    fun rankIndex_clampsAtMax() {
        repo.addXP(200000)
        assertEquals(8, repo.getRankIndex())
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

    // === getRank ===

    @Test
    fun getRank_returnsCorrectRankForXp() {
        assertEquals("Novato", repo.getRank().name)
        repo.addXP(8000)
        assertEquals("Principiante", repo.getRank().name)
        repo.addXP(10000)
        assertEquals("Aprendiz", repo.getRank().name)
    }

    @Test
    fun getRank_maxRankForHighXp() {
        repo.addXP(200000)
        assertEquals("Leyenda", repo.getRank().name)
    }

    // === getXPProgress ===

    @Test
    fun getXPProgress_correctForRank0() {
        val progress = repo.getXPProgress()
        assertEquals(0, progress.intoRank)
        assertEquals(8000, progress.rankSpan)
        assertEquals(0, progress.pct)
        assertEquals(8000, progress.nextXp)
    }

    @Test
    fun getXPProgress_correctForMidRank() {
        repo.addXP(19000) // rank 2 (Aprendiz, 18000 XP), 1000 into rank
        val progress = repo.getXPProgress()
        assertEquals(1000, progress.intoRank)
        // rankSpan = 31000 - 18000 = 13000
        assertEquals(13000, progress.rankSpan)
        // pct = 1000 * 100 / 13000 = 7
        assertEquals(7, progress.pct)
        assertEquals(31000, progress.nextXp)
    }

    @Test
    fun getXPProgress_maxRankIs100Percent() {
        repo.addXP(200000) // rank 8 (Leyenda)
        val progress = repo.getXPProgress()
        assertEquals(100, progress.pct)
    }

    @Test
    fun getXPProgress_pctInRange0to100() {
        repo.addXP(500)
        val progress = repo.getXPProgress()
        assertTrue("Pct should be 0-100, got ${progress.pct}", progress.pct in 0..100)
    }

    // === getUnlocks ===

    @Test
    fun getUnlocks_rank0_onlySurvivalAndPowerUps() {
        val unlocks = repo.getUnlocks()
        assertTrue("Survival should be unlocked at rank 0", unlocks.survival)
        assertFalse("Timetrial should be locked at rank 0", unlocks.timetrial)
        assertFalse("Quick should be locked at rank 0", unlocks.quick)
        assertFalse("Exam should be locked at rank 0", unlocks.exam)
        assertFalse("Simulacro should be locked at rank 0", unlocks.simulacro)
        assertTrue("Power-ups should be unlocked at rank 0", unlocks.powerUps)
        assertFalse("Shield should be locked (removed)", unlocks.shield)
        assertTrue("FiftyFifty should be unlocked at rank 0", unlocks.fiftyFifty)
        assertFalse("Hint should be locked at rank 0", unlocks.hint)
        assertFalse("DoubleScore should be locked (removed)", unlocks.doubleScore)
        assertEquals("Daily missions should be 2 at rank 0", 2, unlocks.dailyMissions)
    }

    @Test
    fun getUnlocks_rank3_unlocksTimetrial() {
        repo.addXP(31000) // rank 3
        val unlocks = repo.getUnlocks()
        assertTrue("Timetrial should be unlocked at rank 3", unlocks.timetrial)
        assertFalse("Quick should still be locked at rank 3", unlocks.quick)
    }

    @Test
    fun getUnlocks_rank5_unlocksQuick() {
        repo.addXP(67000) // rank 5
        val unlocks = repo.getUnlocks()
        assertTrue("Quick should be unlocked at rank 5", unlocks.quick)
        assertFalse("Exam should still be locked at rank 5", unlocks.exam)
        assertEquals("Daily missions should be 3 at rank 5", 3, unlocks.dailyMissions)
    }

    @Test
    fun getUnlocks_rank6_unlocks3DailyMissions() {
        repo.addXP(92000) // rank 6
        val unlocks = repo.getUnlocks()
        assertEquals("Daily missions should be 3 at rank 6", 3, unlocks.dailyMissions)
    }

    @Test
    fun getUnlocks_rank7_unlocksExam() {
        repo.addXP(122000) // rank 7
        val unlocks = repo.getUnlocks()
        assertTrue("Exam should be unlocked at rank 7", unlocks.exam)
    }

    // === isSimulacroUnlocked / unlockSimulacro ===

    @Test
    fun isSimulacroUnlocked_falseByDefault() {
        assertFalse("Simulacro should be locked by default", repo.isSimulacroUnlocked())
    }

    @Test
    fun unlockSimulacro_unlocksIt() {
        repo.unlockSimulacro()
        assertTrue("Simulacro should be unlocked after unlockSimulacro", repo.isSimulacroUnlocked())
    }

    @Test
    fun unlockSimulacro_idempotent() {
        repo.unlockSimulacro()
        repo.unlockSimulacro() // should not crash
        assertTrue("Simulacro should still be unlocked", repo.isSimulacroUnlocked())
    }

    // === getSimulacroHistory / addSimulacroHistory ===

    @Test
    fun getSimulacroHistory_emptyByDefault() {
        assertTrue("Simulacro history should be empty by default", repo.getSimulacroHistory().isEmpty())
    }

    @Test
    fun addSimulacroHistory_addsEntry() {
        repo.addSimulacroHistory(SimulacroHistoryEntry("2026-01-01", 40f, 60, 30, 10, true))
        val history = repo.getSimulacroHistory()
        assertEquals(1, history.size)
        assertEquals(40f, history[0].points, 0.01f)
        assertTrue(history[0].passed)
    }

    @Test
    fun addSimulacroHistory_preservesOrder() {
        repo.addSimulacroHistory(SimulacroHistoryEntry("2026-01-01", 40f, 60, 30, 10, true))
        repo.addSimulacroHistory(SimulacroHistoryEntry("2026-01-02", 35f, 50, 40, 10, false))
        val history = repo.getSimulacroHistory()
        assertEquals(2, history.size)
        assertEquals("2026-01-01", history[0].date)
        assertEquals("2026-01-02", history[1].date)
    }

    // === getMaxExamQuestions / unlockNextExamQuestions ===

    @Test
    fun getMaxExamQuestions_defaultIs10() {
        assertEquals(10, repo.getMaxExamQuestions())
    }

    @Test
    fun unlockNextExamQuestions_incrementsPreset() {
        assertEquals(10, repo.getMaxExamQuestions())
        repo.unlockNextExamQuestions()
        assertEquals(20, repo.getMaxExamQuestions())
        repo.unlockNextExamQuestions()
        assertEquals(30, repo.getMaxExamQuestions())
    }

    @Test
    fun unlockNextExamQuestions_capsAtMaxPreset() {
        // Keep unlocking until we hit the max
        for (i in 0..10) repo.unlockNextExamQuestions()
        assertEquals("Should cap at 50 (last preset)", 50, repo.getMaxExamQuestions())
    }

    // === getLastKnownRankIndex / setLastKnownRankIndex ===

    @Test
    fun getLastKnownRankIndex_defaultIs0() {
        assertEquals(0, repo.getLastKnownRankIndex())
    }

    @Test
    fun setLastKnownRankIndex_roundTrip() {
        repo.setLastKnownRankIndex(4)
        assertEquals(4, repo.getLastKnownRankIndex())
    }

    // === getMissionCount ===

    @Test
    fun getMissionCount_rank0_returns2() {
        assertEquals("Rank 0 should have 2 daily missions", 2, repo.getMissionCount())
    }

    @Test
    fun getMissionCount_rank4_returns3() {
        repo.addXP(47000) // rank 4
        assertEquals("Rank 4 should have 3 daily missions", 3, repo.getMissionCount())
    }

    @Test
    fun getMissionCount_rank6_returns3() {
        repo.addXP(92000) // rank 6
        assertEquals("Rank 6 should have 3 daily missions", 3, repo.getMissionCount())
    }

    // === isUnlocked (feature flags) ===

    @Test
    fun isUnlocked_survival_alwaysTrue() {
        assertTrue(repo.isUnlocked("survival"))
    }

    @Test
    fun isUnlocked_timetrial_rankDependent() {
        assertFalse("Timetrial locked at rank 0", repo.isUnlocked("timetrial"))
        repo.addXP(31000) // rank 3
        assertTrue("Timetrial unlocked at rank 3", repo.isUnlocked("timetrial"))
    }

    @Test
    fun isUnlocked_unknownFeature_returnsFalse() {
        assertFalse("Unknown feature should return false", repo.isUnlocked("unknown_feature"))
    }

    // === Records via ProgressRepository ===

    @Test
    fun setRecord_viaRepo_roundTrip() {
        assertEquals(0, repo.getRecord("survival"))
        repo.setRecord("survival", 500)
        assertEquals(500, repo.getRecord("survival"))
    }

    @Test
    fun setRecordCombo_viaRepo_roundTrip() {
        assertEquals(0, repo.getRecordCombo("survival"))
        repo.setRecordCombo("survival", 15)
        assertEquals(15, repo.getRecordCombo("survival"))
    }

    @Test
    fun setRecordAcc_viaRepo_roundTrip() {
        assertEquals(0, repo.getRecordAcc("survival"))
        repo.setRecordAcc("survival", 85)
        assertEquals(85, repo.getRecordAcc("survival"))
    }

    @Test
    fun getMaxComboRecord_returnsMaxAcrossModes() {
        repo.setRecordCombo("survival", 10)
        repo.setRecordCombo("timetrial", 15)
        repo.setRecordCombo("quick", 8)
        assertEquals(15, repo.getMaxComboRecord())
    }

    // === resetAll ===

    @Test
    fun resetAll_clearsXP() {
        repo.addXP(500)
        repo.resetAll()
        assertEquals(0, repo.getXP())
    }

    @Test
    fun resetAll_clearsRankIndex() {
        repo.addXP(31000) // rank 3
        repo.resetAll()
        assertEquals(0, repo.getRankIndex())
    }

    @Test
    fun resetAll_clearsSimulacroUnlocked() {
        repo.unlockSimulacro()
        repo.resetAll()
        assertFalse(repo.isSimulacroUnlocked())
    }

    @Test
    fun resetAll_clearsSimulacroHistory() {
        repo.addSimulacroHistory(SimulacroHistoryEntry("2026-01-01", 40f, 60, 30, 10, true))
        repo.resetAll()
        assertTrue(repo.getSimulacroHistory().isEmpty())
    }

    @Test
    fun resetAll_clearsMaxExamQuestions() {
        repo.unlockNextExamQuestions()
        repo.resetAll()
        assertEquals(10, repo.getMaxExamQuestions())
    }

    @Test
    fun resetAll_clearsLastKnownRankIndex() {
        repo.setLastKnownRankIndex(5)
        repo.resetAll()
        assertEquals(0, repo.getLastKnownRankIndex())
    }

    @Test
    fun resetAll_clearsAchievements() {
        repo.unlockAchievement("first_correct")
        repo.resetAll()
        assertTrue(repo.getAchievements().isEmpty())
    }

    @Test
    fun resetAll_clearsGamesPlayed() {
        repo.incrementGamesPlayed()
        repo.incrementGamesPlayed()
        repo.resetAll()
        assertEquals(0, repo.getGamesPlayed())
    }

    // === Debug mode ===

    @Test
    fun debugMode_xpReturns100000() {
        prefs.setDebugMode(true)
        assertEquals("Debug mode should return 100000 XP", 100000, repo.getXP())
    }

    @Test
    fun debugMode_addXpReturns100000() {
        prefs.setDebugMode(true)
        assertEquals("Debug mode addXP should return 100000", 100000, repo.addXP(500))
    }

    @Test
    fun debugMode_maxExamQuestionsIs50() {
        prefs.setDebugMode(true)
        assertEquals("Debug mode should return 50 max exam questions", 50, repo.getMaxExamQuestions())
    }

    @Test
    fun debugMode_simulacroUnlocked() {
        prefs.setDebugMode(true)
        assertTrue("Debug mode should unlock simulacro", repo.isSimulacroUnlocked())
    }

    @Test
    fun debugMode_allModesUnlocked() {
        prefs.setDebugMode(true)
        val unlocks = repo.getUnlocks()
        assertTrue(unlocks.survival)
        assertTrue(unlocks.timetrial)
        assertTrue(unlocks.quick)
        assertTrue(unlocks.exam)
        assertTrue(unlocks.simulacro)
    }

    // === isUnlocked: all feature branches ===

    @Test
    fun isUnlocked_quick_rankDependent() {
        assertFalse("Quick locked at rank 0", repo.isUnlocked("quick"))
        repo.addXP(67000) // rank 5
        assertTrue("Quick unlocked at rank 5", repo.isUnlocked("quick"))
    }

    @Test
    fun isUnlocked_exam_rankDependent() {
        assertFalse("Exam locked at rank 0", repo.isUnlocked("exam"))
        repo.addXP(122000) // rank 7
        assertTrue("Exam unlocked at rank 7", repo.isUnlocked("exam"))
    }

    @Test
    fun isUnlocked_simulacro_rankDependent() {
        assertFalse("Simulacro locked by default", repo.isUnlocked("simulacro"))
        repo.unlockSimulacro()
        assertTrue("Simulacro unlocked after unlockSimulacro", repo.isUnlocked("simulacro"))
    }

    @Test
    fun isUnlocked_powerUps_alwaysTrue() {
        assertTrue("PowerUps always unlocked", repo.isUnlocked("powerUps"))
    }

    @Test
    fun isUnlocked_hint_rankDependent() {
        assertFalse("Hint locked at rank 0", repo.isUnlocked("hint"))
        repo.addXP(8000) // rank 1
        assertTrue("Hint unlocked at rank 1", repo.isUnlocked("hint"))
    }

    @Test
    fun isUnlocked_shield_alwaysFalse() {
        assertFalse("Shield always locked (removed)", repo.isUnlocked("shield"))
    }

    @Test
    fun isUnlocked_fiftyFifty_alwaysTrue() {
        assertTrue("FiftyFifty always unlocked", repo.isUnlocked("fiftyFifty"))
    }

    @Test
    fun isUnlocked_lifeRecovery_alwaysTrue() {
        assertTrue("LifeRecovery always unlocked", repo.isUnlocked("lifeRecovery"))
    }

    @Test
    fun isUnlocked_doubleScore_alwaysFalse() {
        assertFalse("DoubleScore always locked (removed)", repo.isUnlocked("doubleScore"))
    }

    @Test
    fun getXPProgressFor_specificXp() {
        val progress = repo.getXPProgressFor(13000)
        assertEquals(5000, progress.intoRank) // 13000 - 8000 = 5000
        assertEquals(10000, progress.rankSpan) // 18000 - 8000 = 10000
        assertEquals(50, progress.pct) // 5000 * 100 / 10000 = 50
    }

    @Test
    fun getXPProgressFor_maxRank() {
        val progress = repo.getXPProgressFor(200000)
        assertEquals(100, progress.pct)
    }
}
