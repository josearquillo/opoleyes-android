package com.opoleyes.data.repository

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.opoleyes.TestContextProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.Mission
import com.opoleyes.data.model.MissionData
import com.opoleyes.data.model.MissionDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MissionRepositoryTest {

    private lateinit var prefs: PreferencesManager
    private lateinit var missionRepo: MissionRepository
    private lateinit var progressRepo: ProgressRepository

    @Before
    fun setup() {
        val ctx = TestContextProvider.getContext()
        prefs = PreferencesManager(ctx)
        prefs.resetAll()
        missionRepo = MissionRepository(ctx)
        progressRepo = ProgressRepository(ctx)
    }

    private fun makeMission(
        key: String = "streak",
        target: Int = 5,
        reward: Int = 50,
        current: Int = 0,
        completed: Boolean = false,
        testId: String? = null
    ): Mission = Mission(
        type = "streak",
        icon = "🔥",
        text = "Test mission",
        target = target,
        current = current,
        completed = completed,
        reward = reward,
        key = key,
        testId = testId,
        difficulty = MissionDifficulty.EASY
    )

    private fun saveMissions(vararg missions: Mission) {
        missionRepo.saveDailyMissions(MissionData(
            date = LocalDate.now().toString(),
            missions = missions.toList()
        ))
    }

    @Test
    fun updateProgress_completesMissionAndAwardsXp() {
        saveMissions(makeMission(key = "streak", target = 3, reward = 50, current = 0))
        val xpBefore = progressRepo.getXP()
        missionRepo.updateProgress("streak", 3)
        val data = missionRepo.getDailyMissions()!!
        assertTrue("Mission completed", data.missions[0].completed)
        assertEquals(xpBefore + 50, progressRepo.getXP())
    }

    @Test
    fun updateProgress_doesNotAwardBonusXpForAllCompleted() {
        saveMissions(
            makeMission(key = "streak", target = 1, reward = 30, current = 0),
            makeMission(key = "combo", target = 1, reward = 30, current = 0)
        )
        val xpBefore = progressRepo.getXP()
        missionRepo.updateProgress("streak", 1)
        missionRepo.updateProgress("combo", 1)
        val data = missionRepo.getDailyMissions()!!
        assertTrue("All missions completed", data.missions.all { it.completed })
        assertEquals(xpBefore + 60, progressRepo.getXP())
    }

    @Test
    fun updateProgress_doesNotRecompleteAlreadyCompleted() {
        saveMissions(makeMission(key = "streak", target = 1, reward = 50, current = 0))
        missionRepo.updateProgress("streak", 1)
        val xpAfterFirst = progressRepo.getXP()
        missionRepo.updateProgress("streak", 5)
        assertEquals("No double XP for already completed", xpAfterFirst, progressRepo.getXP())
    }

    @Test
    fun updateProgress_accumulatesQuickReview() {
        saveMissions(makeMission(key = "quick_review", target = 10, reward = 50, current = 3))
        missionRepo.updateProgress("quick_review", 4)
        val data = missionRepo.getDailyMissions()!!
        assertEquals(7, data.missions[0].current)
        assertFalse("Not completed yet", data.missions[0].completed)
    }

    @Test
    fun updateProgress_maxForStreakNotAccumulate() {
        saveMissions(makeMission(key = "streak", target = 10, reward = 50, current = 3))
        missionRepo.updateProgress("streak", 5)
        assertEquals(5, missionRepo.getDailyMissions()!!.missions[0].current)
        missionRepo.updateProgress("streak", 3)
        assertEquals(5, missionRepo.getDailyMissions()!!.missions[0].current)
    }

    @Test
    fun generateDailyMissions_returnsMissionsForToday() {
        val data = missionRepo.generateDailyMissions()
        assertEquals(LocalDate.now().toString(), data.date)
        assertTrue("At least 1 mission", data.missions.isNotEmpty())
    }

    @Test
    fun generateDailyMissions_sameDayReturnsSame() {
        val first = missionRepo.generateDailyMissions()
        val second = missionRepo.generateDailyMissions()
        assertEquals(first.date, second.date)
        assertEquals(first.missions.size, second.missions.size)
    }
}
