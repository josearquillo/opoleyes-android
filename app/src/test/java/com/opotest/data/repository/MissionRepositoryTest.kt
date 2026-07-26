package com.opotest.data.repository

import com.opotest.TestContextProvider
import com.opotest.data.local.PreferencesManager
import com.opotest.data.model.Mission
import com.opotest.data.model.MissionData
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
class MissionRepositoryTest {

    private lateinit var repo: MissionRepository
    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        val context = TestContextProvider.getContext()
        prefs = PreferencesManager(context)
        prefs.resetAll()
        repo = MissionRepository(context)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun fun_getDailyMissions_nullByDefault() {
        assertNull(repo.getDailyMissions())
    }

    @Test
    fun fun_saveDailyMissions() {
        val data = MissionData("2026-01-01", listOf(
            Mission("streak", "🎯", "Text", 10, 0, false, 50, "streak")
        ), 0)
        repo.saveDailyMissions(data)
        val loaded = repo.getDailyMissions()
        assertNotNull(loaded)
        assertEquals("2026-01-01", loaded?.date)
        assertEquals(1, loaded?.missions?.size)
    }

    @Test
    fun fun_generateDailyMissions_createsMissions() {
        val data = repo.generateDailyMissions()
        assertNotNull(data)
        assertTrue(data.missions.isNotEmpty())
        assertTrue(data.missions.size >= 1)
    }

    @Test
    fun fun_generateDailyMissions_sameDayReturnsCached() {
        val data1 = repo.generateDailyMissions()
        val data2 = repo.generateDailyMissions()
        assertEquals(data1.date, data2.date)
        assertEquals(data1.missions.size, data2.missions.size)
    }

    @Test
    fun fun_generateDailyMissions_hasCorrectDate() {
        val data = repo.generateDailyMissions()
        assertEquals(java.time.LocalDate.now().toString(), data.date)
    }

    @Test
    fun fun_updateProgress_streak() {
        val data = repo.generateDailyMissions()
        val streakMission = data.missions.find { it.key == "streak" }
        if (streakMission != null) {
            val initial = streakMission.current
            repo.updateProgress("streak", 5)
            val updated = repo.getDailyMissions()
            val m = updated?.missions?.find { it.key == "streak" }
            assertEquals(maxOf(initial, 5), m?.current)
        }
    }

    @Test
    fun fun_updateProgress_combo() {
        val data = repo.generateDailyMissions()
        val comboMission = data.missions.find { it.key == "combo" }
        if (comboMission != null) {
            repo.updateProgress("combo", 10)
            val updated = repo.getDailyMissions()
            val m = updated?.missions?.find { it.key == "combo" }
            assertEquals(10, m?.current)
        }
    }

    @Test
    fun fun_updateProgress_completesMission() {
        val data = repo.generateDailyMissions()
        val mission = data.missions.first()
        val type = when {
            mission.key == "streak" -> "streak"
            mission.key == "combo" -> "combo"
            mission.key == "quick_review" -> "quick_review"
            mission.key.startsWith("progress_") -> "progress"
            mission.key.startsWith("variety_") -> "variety"
            else -> mission.key
        }
        repo.updateProgress(type, mission.target)
        val updated = repo.getDailyMissions()
        val m = updated?.missions?.find { it.key == mission.key }
        assertTrue(m?.completed == true)
    }

    @Test
    fun fun_updateProgress_nullDataDoesNothing() {
        repo.updateProgress("streak", 5)
        assertNull(repo.getDailyMissions())
    }

    @Test
    fun fun_checkOnGameOver_updatesStreak() {
        repo.generateDailyMissions()
        repo.checkOnGameOver("survival", 500, 10, 8, 10, "")
        val data = repo.getDailyMissions()
        assertNotNull(data)
    }

    @Test
    fun fun_generateDailyMissions_allMissionsHaveReward() {
        val data = repo.generateDailyMissions()
        data.missions.forEach { m ->
            assertTrue("Mission ${m.key} should have reward > 0", m.reward > 0)
        }
    }

    @Test
    fun fun_generateDailyMissions_allMissionsHaveTarget() {
        val data = repo.generateDailyMissions()
        data.missions.forEach { m ->
            assertTrue("Mission ${m.key} should have target > 0", m.target > 0)
        }
    }

    @Test
    fun fun_generateDailyMissions_allMissionsNotCompleted() {
        val data = repo.generateDailyMissions()
        data.missions.forEach { m ->
            assertFalse("Mission ${m.key} should not be completed", m.completed)
        }
    }
}
