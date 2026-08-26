package com.opoleyes.data.repository

import com.opoleyes.FakePreferencesManager
import com.opoleyes.data.model.MissionDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests that daily mission generation produces missions with distinct keys
 * across difficulty levels (no duplicate mission types).
 */
class MissionRepositoryDedupTest {

    private lateinit var prefs: FakePreferencesManager
    private lateinit var missionRepo: MissionRepository
    private lateinit var progressRepo: ProgressRepository

    @Before
    fun setup() {
        prefs = FakePreferencesManager()
        prefs.resetAll()
        missionRepo = MissionRepository(prefs)
        progressRepo = ProgressRepository(prefs)
    }

    @Test
    fun generatedMissions_haveDistinctKeys() {
        val data = missionRepo.generateDailyMissions()
        assertTrue("Should generate missions", data.missions.isNotEmpty())

        val keys = data.missions.map { it.key }
        val uniqueKeys = keys.toSet()
        assertEquals(
            "Mission keys should be unique: $keys",
            keys.size,
            uniqueKeys.size
        )
    }

    @Test
    fun generatedMissions_haveCorrectDifficultyLevels() {
        val data = missionRepo.generateDailyMissions()
        val difficulties = data.missions.map { it.difficulty }

        if (data.missions.size >= 1) {
            assertTrue("First mission should be EASY", difficulties[0] == MissionDifficulty.EASY)
        }
        if (data.missions.size >= 2) {
            assertTrue("Second mission should be MEDIUM", difficulties[1] == MissionDifficulty.MEDIUM)
        }
        if (data.missions.size >= 3) {
            assertTrue("Third mission should be HARD", difficulties[2] == MissionDifficulty.HARD)
        }
    }

    @Test
    fun generatedMissions_allThreeDifficultiesPresent() {
        val data = missionRepo.generateDailyMissions()
        assertTrue("Should have at least 1 mission", data.missions.size >= 1)

        val hasEasy = data.missions.any { it.difficulty == MissionDifficulty.EASY }
        val hasMedium = data.missions.any { it.difficulty == MissionDifficulty.MEDIUM }
        val hasHard = data.missions.any { it.difficulty == MissionDifficulty.HARD }

        assertTrue("Should have at least one EASY mission", hasEasy)
        if (data.missions.size >= 2) {
            assertTrue("Should have at least one MEDIUM mission", hasMedium)
        }
        if (data.missions.size >= 3) {
            assertTrue("Should have at least one HARD mission", hasHard)
        }
    }

    @Test
    fun easyAndMediumMissions_neverShareSameKey() {
        // generateDailyMissions caches by date, so we can only call it once
        // per day. Verify the single generated set.
        val data = missionRepo.generateDailyMissions()
        if (data.missions.size >= 2) {
            val easyKey = data.missions[0].key
            val mediumKey = data.missions[1].key
            assertTrue(
                "Easy ($easyKey) and Medium ($mediumKey) should have different keys",
                easyKey != mediumKey
            )
        }
    }

    @Test
    fun allSelectedMissions_neverShareSameKey() {
        val data = missionRepo.generateDailyMissions()
        val keys = data.missions.map { it.key }
        val uniqueKeys = keys.toSet()
        assertEquals(
            "All mission keys should be unique: $keys",
            keys.size,
            uniqueKeys.size
        )
    }
}
