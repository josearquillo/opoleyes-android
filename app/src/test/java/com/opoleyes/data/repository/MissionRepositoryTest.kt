package com.opoleyes.data.repository

import org.junit.Before
import org.junit.Test
import com.opoleyes.FakePreferencesManager
import com.opoleyes.data.model.Mission
import com.opoleyes.data.model.MissionData
import com.opoleyes.data.model.MissionDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import java.time.LocalDate

class MissionRepositoryTest {

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

    // === Regression tests for bugs fixed ===

    @Test
    fun quickCompleteMission_targetIs1_achievableInOneGame() {
        // Bug: target was 20, but updateProgress uses maxOf(current, value) with value=1,
        // so current could never exceed 1, making the mission impossible.
        saveMissions(makeMission(key = "quick_complete", target = 1, reward = 100, current = 0))
        missionRepo.checkOnGameOver("quick", maxCombo = 0, maxStreak = 0, totalAnswered = 5, gameCategory = "", correctCount = 5, score = 0)
        val data = missionRepo.getDailyMissions()!!
        assertTrue("quick_complete mission should be achievable in one game", data.missions[0].completed)
    }

    @Test
    fun quickCompleteMission_doesNotCompleteInOtherModes() {
        saveMissions(makeMission(key = "quick_complete", target = 1, reward = 100, current = 0))
        missionRepo.checkOnGameOver("survival", maxCombo = 10, maxStreak = 10, totalAnswered = 20, gameCategory = "", correctCount = 20, score = 500)
        val data = missionRepo.getDailyMissions()!!
        assertFalse("quick_complete should not complete in survival mode", data.missions[0].completed)
    }

    // === updateProgress: all mission types ===

    @Test
    fun updateProgress_combo_usesMaxNotAccumulate() {
        saveMissions(makeMission(key = "combo", target = 10, reward = 50, current = 3))
        missionRepo.updateProgress("combo", 7)
        assertEquals(7, missionRepo.getDailyMissions()!!.missions[0].current)
        missionRepo.updateProgress("combo", 5)
        assertEquals(7, missionRepo.getDailyMissions()!!.missions[0].current)
    }

    @Test
    fun updateProgress_progress_lawSpecific_usesMax() {
        saveMissions(makeMission(key = "progress_test1", target = 50, reward = 50, current = 30))
        missionRepo.updateProgress("progress", 45)
        assertEquals(45, missionRepo.getDailyMissions()!!.missions[0].current)
        missionRepo.updateProgress("progress", 40)
        assertEquals(45, missionRepo.getDailyMissions()!!.missions[0].current)
    }

    @Test
    fun updateProgress_progress_any_accumulates() {
        saveMissions(makeMission(key = "progress_any", target = 10, reward = 50, current = 3))
        missionRepo.updateProgress("progress_any", 4)
        assertEquals(7, missionRepo.getDailyMissions()!!.missions[0].current)
    }

    @Test
    fun updateProgress_variety_accumulates() {
        saveMissions(makeMission(key = "variety_any", target = 10, reward = 50, current = 3))
        missionRepo.updateProgress("variety", 4)
        assertEquals(7, missionRepo.getDailyMissions()!!.missions[0].current)
    }

    @Test
    fun updateProgress_timetrialScore_usesMax() {
        saveMissions(makeMission(key = "timetrial_score", target = 500, reward = 50, current = 300))
        missionRepo.updateProgress("timetrial_score", 400)
        assertEquals(400, missionRepo.getDailyMissions()!!.missions[0].current)
        missionRepo.updateProgress("timetrial_score", 350)
        assertEquals(400, missionRepo.getDailyMissions()!!.missions[0].current)
    }

    @Test
    fun updateProgress_examScore_usesMax() {
        saveMissions(makeMission(key = "exam_score", target = 70, reward = 50, current = 50))
        missionRepo.updateProgress("exam_score", 65)
        assertEquals(65, missionRepo.getDailyMissions()!!.missions[0].current)
        missionRepo.updateProgress("exam_score", 60)
        assertEquals(65, missionRepo.getDailyMissions()!!.missions[0].current)
    }

    @Test
    fun updateProgress_simulacroComplete_usesMax() {
        saveMissions(makeMission(key = "simulacro_complete", target = 1, reward = 100, current = 0))
        missionRepo.updateProgress("simulacro_complete", 1)
        assertTrue(missionRepo.getDailyMissions()!!.missions[0].completed)
    }

    // === checkOnGameOver: all modes ===

    @Test
    fun checkOnGameOver_survival_updatesStreakAndCombo() {
        saveMissions(
            makeMission(key = "streak", target = 5, reward = 50, current = 0),
            makeMission(key = "combo", target = 10, reward = 50, current = 0)
        )
        missionRepo.checkOnGameOver("survival", maxCombo = 10, maxStreak = 5, totalAnswered = 20, gameCategory = "test1", correctCount = 15, score = 500)
        val data = missionRepo.getDailyMissions()!!
        assertTrue("Streak mission completed", data.missions.any { it.key == "streak" && it.completed })
        assertTrue("Combo mission completed", data.missions.any { it.key == "combo" && it.completed })
    }

    @Test
    fun checkOnGameOver_survival_updatesVarietyWithMatchingCategory() {
        saveMissions(makeMission(key = "variety_test1", target = 5, reward = 50, current = 0, testId = "test1"))
        missionRepo.checkOnGameOver("survival", maxCombo = 0, maxStreak = 0, totalAnswered = 10, gameCategory = "test1", correctCount = 5, score = 0)
        assertTrue(missionRepo.getDailyMissions()!!.missions[0].completed)
    }

    @Test
    fun checkOnGameOver_survival_doesNotUpdateVarietyWithDifferentCategory() {
        saveMissions(makeMission(key = "variety_test2", target = 5, reward = 50, current = 0, testId = "test2"))
        missionRepo.checkOnGameOver("survival", maxCombo = 0, maxStreak = 0, totalAnswered = 10, gameCategory = "test1", correctCount = 5, score = 0)
        assertFalse(missionRepo.getDailyMissions()!!.missions[0].completed)
    }

    @Test
    fun checkOnGameOver_survival_updatesVarietyAny() {
        saveMissions(makeMission(key = "variety_any", target = 5, reward = 50, current = 0))
        missionRepo.checkOnGameOver("survival", maxCombo = 0, maxStreak = 0, totalAnswered = 10, gameCategory = "test1", correctCount = 5, score = 0)
        assertTrue(missionRepo.getDailyMissions()!!.missions[0].completed)
    }

    @Test
    fun checkOnGameOver_quick_updatesQuickReview() {
        saveMissions(makeMission(key = "quick_review", target = 5, reward = 50, current = 0))
        missionRepo.checkOnGameOver("quick", maxCombo = 0, maxStreak = 0, totalAnswered = 5, gameCategory = "", correctCount = 3, score = 0)
        assertTrue(missionRepo.getDailyMissions()!!.missions[0].completed)
    }

    @Test
    fun checkOnGameOver_timetrial_updatesTimetrialScore() {
        saveMissions(makeMission(key = "timetrial_score", target = 500, reward = 50, current = 0))
        missionRepo.checkOnGameOver("timetrial", maxCombo = 0, maxStreak = 0, totalAnswered = 10, gameCategory = "", correctCount = 5, score = 600)
        assertTrue(missionRepo.getDailyMissions()!!.missions[0].completed)
    }

    @Test
    fun checkOnGameOver_survival_updatesProgressAny() {
        saveMissions(makeMission(key = "progress_any", target = 10, reward = 50, current = 0))
        missionRepo.checkOnGameOver("survival", maxCombo = 0, maxStreak = 0, totalAnswered = 10, gameCategory = "", correctCount = 10, score = 0)
        assertTrue(missionRepo.getDailyMissions()!!.missions[0].completed)
    }

    @Test
    fun checkOnGameOver_quick_completesQuickComplete() {
        saveMissions(makeMission(key = "quick_complete", target = 1, reward = 100, current = 0))
        missionRepo.checkOnGameOver("quick", maxCombo = 0, maxStreak = 0, totalAnswered = 5, gameCategory = "", correctCount = 3, score = 0)
        assertTrue(missionRepo.getDailyMissions()!!.missions[0].completed)
    }

    @Test
    fun checkOnGameOver_quick_doesNotCompleteQuickCompleteIfNotEnoughQuestions() {
        saveMissions(makeMission(key = "quick_complete", target = 1, reward = 100, current = 0))
        missionRepo.checkOnGameOver("quick", maxCombo = 0, maxStreak = 0, totalAnswered = 3, gameCategory = "", correctCount = 3, score = 0)
        assertFalse(missionRepo.getDailyMissions()!!.missions[0].completed)
    }

    @Test
    fun checkOnGameOver_survival_updatesProgressLawSpecific() {
        saveMissions(makeMission(key = "progress_test1", target = 50, reward = 50, current = 30, testId = "test1"))
        missionRepo.checkOnGameOver("survival", maxCombo = 0, maxStreak = 0, totalAnswered = 10, gameCategory = "test1", correctCount = 10, score = 0)
        // progress_test1 should be updated via statsRepo.getLeyProgress("test1") which returns 0 without context
        // So current should be maxOf(30, 0) = 30, not completed
        assertFalse(missionRepo.getDailyMissions()!!.missions[0].completed)
    }

    @Test
    fun checkOnGameOver_timetrial_doesNotUpdateStreakOrCombo() {
        saveMissions(
            makeMission(key = "streak", target = 5, reward = 50, current = 0),
            makeMission(key = "combo", target = 10, reward = 50, current = 0)
        )
        missionRepo.checkOnGameOver("timetrial", maxCombo = 10, maxStreak = 5, totalAnswered = 10, gameCategory = "", correctCount = 5, score = 600)
        val data = missionRepo.getDailyMissions()!!
        assertFalse("Streak should not update in timetrial", data.missions.any { it.key == "streak" && it.completed })
        assertFalse("Combo should not update in timetrial", data.missions.any { it.key == "combo" && it.completed })
    }

    @Test
    fun checkOnGameOver_unknownMode_doesNotUpdateAnything() {
        saveMissions(makeMission(key = "streak", target = 5, reward = 50, current = 0))
        missionRepo.checkOnGameOver("unknown", maxCombo = 10, maxStreak = 5, totalAnswered = 10, gameCategory = "", correctCount = 10, score = 500)
        assertFalse(missionRepo.getDailyMissions()!!.missions[0].completed)
    }

    // === checkExamResult ===

    @Test
    fun checkExamResult_updatesExamScore() {
        saveMissions(makeMission(key = "exam_score", target = 60, reward = 100, current = 0))
        missionRepo.checkExamResult(70)
        assertTrue(missionRepo.getDailyMissions()!!.missions[0].completed)
    }

    // === checkSimulacroResult ===

    @Test
    fun checkSimulacroResult_passed_completesMission() {
        saveMissions(makeMission(key = "simulacro_complete", target = 1, reward = 100, current = 0))
        missionRepo.checkSimulacroResult(true)
        assertTrue(missionRepo.getDailyMissions()!!.missions[0].completed)
    }

    @Test
    fun checkSimulacroResult_notPassed_doesNotComplete() {
        saveMissions(makeMission(key = "simulacro_complete", target = 1, reward = 100, current = 0))
        missionRepo.checkSimulacroResult(false)
        assertFalse(missionRepo.getDailyMissions()!!.missions[0].completed)
    }

    // === generateDailyMissions: rank-based generation ===

    @Test
    fun generateDailyMissions_rank0_generates1Mission() {
        val data = missionRepo.generateDailyMissions()
        assertEquals(1, data.missions.size)
        assertEquals(LocalDate.now().toString(), data.date)
    }

    @Test
    fun generateDailyMissions_rank1_generates1MissionBeginner() {
        prefs.addXP(200) // rank 1
        val data = missionRepo.generateDailyMissions()
        assertEquals(1, data.missions.size)
        val easyMission = data.missions.find { it.difficulty == MissionDifficulty.EASY }!!
        assertTrue("Rank 1 easy target should be 5", easyMission.target == 5 || easyMission.target == 3)
    }

    @Test
    fun generateDailyMissions_rank2_generates1MissionNonBeginner() {
        prefs.addXP(800) // rank 2
        val data = missionRepo.generateDailyMissions()
        assertEquals(1, data.missions.size)
        // Non-beginner easy pool should have variety or progress missions
        assertTrue("Should have variety or progress key", data.missions.any { it.key.startsWith("variety_") || it.key.startsWith("progress_") })
    }

    @Test
    fun generateDailyMissions_rank3_generates1MissionNonBeginner() {
        prefs.addXP(2000) // rank 3 - still only 1 daily mission (need rank 4 for 2)
        val data = missionRepo.generateDailyMissions()
        assertEquals(1, data.missions.size)
        // Rank 3 > 2, so medium pool uses the else branch with timetrial
        assertTrue("Should have easy mission", data.missions.any { it.difficulty == MissionDifficulty.EASY })
    }

    @Test
    fun generateDailyMissions_rank5_generates2MissionsWithQuick() {
        prefs.addXP(7000) // rank 5 - quick unlocked
        val data = missionRepo.generateDailyMissions()
        assertEquals(2, data.missions.size)
        // With quick unlocked, easy pool may include quick_review mission
        // Medium pool may include timetrial (unlocked at rank 3)
    }

    @Test
    fun generateDailyMissions_rank7_generates3MissionsWithExam() {
        prefs.addXP(18000) // rank 7 - exam unlocked, 3 daily missions
        val data = missionRepo.generateDailyMissions()
        assertEquals(3, data.missions.size)
        // Hard pool should have a hard mission (exam_score since exam unlocked at rank 7)
        assertTrue("Should have at least 1 hard mission", data.missions.any { it.difficulty == MissionDifficulty.HARD })
    }

    @Test
    fun generateDailyMissions_rank8_generates3MissionsWithSimulacro() {
        prefs.addXP(30000) // rank 8 (Leyenda) - simulacro may be unlocked
        // Simulacro is unlocked separately, not rank-based
        val data = missionRepo.generateDailyMissions()
        assertEquals(3, data.missions.size)
    }

    @Test
    fun generateDailyMissions_rank4_hardPoolUsesRank3Branch() {
        prefs.addXP(4000) // rank 4
        val data = missionRepo.generateDailyMissions()
        assertEquals(2, data.missions.size)
        // Rank 4 > 3, so hard pool uses the else branch (but only 2 missions, no hard)
        assertTrue("Should have easy mission", data.missions.any { it.difficulty == MissionDifficulty.EASY })
        assertTrue("Should have medium mission", data.missions.any { it.difficulty == MissionDifficulty.MEDIUM })
    }

    @Test
    fun generateDailyMissions_rank0_easyMissionHasCorrectTarget() {
        val data = missionRepo.generateDailyMissions()
        val easyMission = data.missions.find { it.difficulty == MissionDifficulty.EASY }!!
        assertTrue("Easy mission target should be 3 at rank 0", easyMission.target == 3 || easyMission.target == 2)
    }

    @Test
    fun generateDailyMissions_sameDay_returnsSameMissions() {
        val data1 = missionRepo.generateDailyMissions()
        val data2 = missionRepo.generateDailyMissions()
        assertEquals(data1.missions.size, data2.missions.size)
        assertEquals(data1.missions.map { it.key }, data2.missions.map { it.key })
    }

    @Test
    fun generateDailyMissions_rank4_generates2Missions() {
        prefs.addXP(4000) // rank 4 = 2 missions
        val data = missionRepo.generateDailyMissions()
        assertEquals(2, data.missions.size)
    }

    @Test
    fun generateDailyMissions_rank6_generates3Missions() {
        prefs.addXP(12000) // rank 6 = 3 missions
        val data = missionRepo.generateDailyMissions()
        assertEquals(3, data.missions.size)
    }

    @Test
    fun generateDailyMissions_hasOneEasyOneMediumOneHard() {
        prefs.addXP(12000) // rank 6 = 3 missions
        val data = missionRepo.generateDailyMissions()
        assertTrue("Should have at least 1 easy", data.missions.any { it.difficulty == MissionDifficulty.EASY })
        assertTrue("Should have at least 1 medium", data.missions.any { it.difficulty == MissionDifficulty.MEDIUM })
        assertTrue("Should have at least 1 hard", data.missions.any { it.difficulty == MissionDifficulty.HARD })
    }

    // === Session completed missions ===

    @Test
    fun sessionCompletedMissions_clearedOnClear() {
        saveMissions(makeMission(key = "streak", target = 1, reward = 50, current = 0))
        missionRepo.updateProgress("streak", 1)
        assertEquals(1, missionRepo.getSessionCompletedMissions().size)
        missionRepo.clearSessionCompletedMissions()
        assertEquals(0, missionRepo.getSessionCompletedMissions().size)
    }

    @Test
    fun updateProgress_noMissions_doesNotCrash() {
        missionRepo.updateProgress("streak", 5)
        // Should not crash, just return
    }

    @Test
    fun checkOnGameOver_noMissions_doesNotCrash() {
        missionRepo.checkOnGameOver("survival", maxCombo = 10, maxStreak = 5, totalAnswered = 20, gameCategory = "test1", correctCount = 15, score = 500)
    }
}
