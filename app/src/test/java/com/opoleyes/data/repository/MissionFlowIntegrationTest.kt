package com.opoleyes.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.Mission
import com.opoleyes.data.model.MissionData
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
class MissionFlowIntegrationTest {

    private lateinit var missionRepo: MissionRepository
    private lateinit var prefs: PreferencesManager
    private lateinit var progressRepo: ProgressRepository
    private lateinit var statsRepo: StatsRepository

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        prefs = PreferencesManager(app)
        prefs.resetAll()
        missionRepo = MissionRepository(app)
        progressRepo = ProgressRepository(app)
        statsRepo = StatsRepository(app)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun fun_generateDailyMissions_returnsMissions() {
        val data = missionRepo.generateDailyMissions()
        assertTrue("Should generate at least 1 mission", data.missions.isNotEmpty())
    }

    @Test
    fun fun_generateDailyMissions_sameDay_returnsCached() {
        val data1 = missionRepo.generateDailyMissions()
        val data2 = missionRepo.generateDailyMissions()
        assertEquals(data1.date, data2.date)
        assertEquals(data1.missions.size, data2.missions.size)
    }

    @Test
    fun fun_generateDailyMissions_missionCountMatchesUnlock() {
        val data = missionRepo.generateDailyMissions()
        assertEquals(progressRepo.getMissionCount(), data.missions.size)
    }

    @Test
    fun fun_updateProgress_completesMission_grantsXP() {
        val mission = Mission("combo", "🔥", "Get combo x3", 3, 0, false, 50, "combo")
        val data = MissionData("2026-07-28", listOf(mission))
        missionRepo.saveDailyMissions(data)
        val xpBefore = progressRepo.getXP()
        missionRepo.updateProgress("combo", 3)
        assertTrue("XP should increase on mission completion", progressRepo.getXP() > xpBefore)
        val updated = missionRepo.getDailyMissions()!!
        assertTrue(updated.missions[0].completed)
    }

    @Test
    fun fun_updateProgress_allMissionsComplete_grantsBonusXP() {
        val m1 = Mission("combo", "🔥", "Get combo x3", 3, 0, false, 50, "combo")
        val m2 = Mission("quality", "🎯", "Streak 3", 3, 0, false, 50, "streak")
        val data = MissionData("2026-07-28", listOf(m1, m2))
        missionRepo.saveDailyMissions(data)
        val xpBefore = progressRepo.getXP()
        missionRepo.updateProgress("combo", 3)
        missionRepo.updateProgress("streak", 3)
        val xpAfter = progressRepo.getXP()
        // 50 (m1) + 50 (m2) + 200 (all complete bonus) = 300
        assertTrue("Should grant mission XP + completion bonus", xpAfter - xpBefore >= 300)
    }

    @Test
    fun fun_updateProgress_doesNotCompleteIfBelowTarget() {
        val mission = Mission("combo", "🔥", "Get combo x5", 5, 0, false, 50, "combo")
        val data = MissionData("2026-07-28", listOf(mission))
        missionRepo.saveDailyMissions(data)
        missionRepo.updateProgress("combo", 3)
        val updated = missionRepo.getDailyMissions()!!
        assertFalse("Mission should not be completed below target", updated.missions[0].completed)
    }

    @Test
    fun fun_updateProgress_alreadyCompleted_doesNotRegrantXP() {
        val mission = Mission("combo", "🔥", "Get combo x3", 3, 0, false, 50, "combo")
        val data = MissionData("2026-07-28", listOf(mission))
        missionRepo.saveDailyMissions(data)
        missionRepo.updateProgress("combo", 3)
        val xpAfterFirst = progressRepo.getXP()
        missionRepo.updateProgress("combo", 5)
        val xpAfterSecond = progressRepo.getXP()
        assertEquals("Should not re-grant XP for already completed mission", xpAfterFirst, xpAfterSecond)
    }

    @Test
    fun fun_checkOnGameOver_updatesStreakAndCombo() {
        val m1 = Mission("combo", "🔥", "Get combo x5", 5, 0, false, 50, "combo")
        val m2 = Mission("quality", "🎯", "Streak 5", 5, 0, false, 50, "streak")
        val data = MissionData("2026-07-28", listOf(m1, m2))
        missionRepo.saveDailyMissions(data)
        missionRepo.checkOnGameOver("survival", maxCombo = 7, totalAnswered = 15, gameCategory = "")
        val updated = missionRepo.getDailyMissions()!!
        assertTrue("Combo mission should be completed", updated.missions[0].completed)
        assertTrue("Streak mission should be completed", updated.missions[1].completed)
    }

    @Test
    fun fun_checkOnGameOver_quickMode_updatesQuickReview() {
        val mission = Mission("review", "🔄", "Answer 10 in quick", 10, 0, false, 50, "quick_review")
        val data = MissionData("2026-07-28", listOf(mission))
        missionRepo.saveDailyMissions(data)
        missionRepo.checkOnGameOver("quick", maxCombo = 3, totalAnswered = 15, gameCategory = "")
        val updated = missionRepo.getDailyMissions()!!
        assertTrue("Quick review mission should be completed", updated.missions[0].completed)
    }

    @Test
    fun fun_checkOnGameOver_noMissionsData_doesNotCrash() {
        missionRepo.checkOnGameOver("survival", maxCombo = 5, totalAnswered = 10, gameCategory = "")
    }

    @Test
    fun fun_updateProgress_progressType_updatesCorrectly() {
        val mission = Mission("progress", "📈", "Reach 50%", 50, 0, false, 50, "progress_test1")
        val data = MissionData("2026-07-28", listOf(mission))
        missionRepo.saveDailyMissions(data)
        missionRepo.updateProgress("progress", 30)
        assertEquals(30, missionRepo.getDailyMissions()!!.missions[0].current)
        missionRepo.updateProgress("progress", 50)
        assertTrue(missionRepo.getDailyMissions()!!.missions[0].completed)
    }

    @Test
    fun fun_updateProgress_varietyType_updatesCorrectly() {
        val mission = Mission("variety", "🌍", "Play any law", 1, 0, false, 50, "variety_any")
        val data = MissionData("2026-07-28", listOf(mission))
        missionRepo.saveDailyMissions(data)
        missionRepo.updateProgress("variety", 1)
        assertTrue(missionRepo.getDailyMissions()!!.missions[0].completed)
    }
}
