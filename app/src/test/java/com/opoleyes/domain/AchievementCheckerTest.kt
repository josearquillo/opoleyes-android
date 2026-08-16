package com.opoleyes.domain

import org.junit.Before
import org.junit.Test
import com.opoleyes.FakePreferencesManager
import com.opoleyes.data.repository.ProgressRepository
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals

class AchievementCheckerTest {

    private lateinit var prefs: FakePreferencesManager
    private lateinit var checker: AchievementChecker
    private lateinit var progressRepo: ProgressRepository

    @Before
    fun setup() {
        prefs = FakePreferencesManager()
        prefs.resetAll()
        progressRepo = ProgressRepository(prefs)
        checker = AchievementChecker(prefs)
    }

    @Test
    fun perQuestion_firstCorrect_unlocks() {
        val result = checker.checkPerQuestion(AchievementContext(firstCorrect = true, maxCombo = 0))
        assertTrue("first_correct unlocked", result.any { it.id == "first_correct" })
    }

    @Test
    fun perQuestion_combo5_unlocks() {
        val result = checker.checkPerQuestion(AchievementContext(maxCombo = 5))
        assertTrue("combo5 unlocked", result.any { it.id == "combo5" })
    }

    @Test
    fun perQuestion_combo10_unlocks() {
        val result = checker.checkPerQuestion(AchievementContext(maxCombo = 10))
        assertTrue("combo10 unlocked", result.any { it.id == "combo10" })
    }

    @Test
    fun perQuestion_lowCombo_doesNotUnlockCombo5() {
        val result = checker.checkPerQuestion(AchievementContext(maxCombo = 4))
        assertFalse("combo5 not unlocked with combo 4", result.any { it.id == "combo5" })
    }

    @Test
    fun perQuestion_strategist_unlocks() {
        val result = checker.checkPerQuestion(AchievementContext(fiftyFiftyUsed = true))
        assertTrue("strategist unlocked", result.any { it.id == "strategist" })
    }

    @Test
    fun perQuestion_resurrection_unlocks() {
        val result = checker.checkPerQuestion(AchievementContext(lifeRecovered = true))
        assertTrue("resurrection unlocked", result.any { it.id == "resurrection" })
    }

    @Test
    fun perQuestion_doesNotUnlockMilestoneAchievements() {
        val result = checker.checkPerQuestion(AchievementContext(firstCorrect = true, maxCombo = 5, score = 500))
        assertFalse("No medal_bronze from per-question", result.any { it.id == "medal_bronze" })
        assertFalse("No first_record from per-question", result.any { it.id == "first_record" })
        assertFalse("No 100correct from per-question", result.any { it.id == "100correct" })
    }

    @Test
    fun gameOver_firstRecord_unlocks() {
        val result = checker.checkGameOver(AchievementContext(gameOver = true))
        assertTrue("first_record unlocked", result.any { it.id == "first_record" })
    }

    @Test
    fun gameOver_medalBronze_unlocks() {
        val result = checker.checkGameOver(AchievementContext(score = 300))
        assertTrue("medal_bronze unlocked", result.any { it.id == "medal_bronze" })
    }

    @Test
    fun gameOver_medalGold_unlocks() {
        val result = checker.checkGameOver(AchievementContext(score = 1000))
        assertTrue("medal_gold unlocked", result.any { it.id == "medal_gold" })
    }

    @Test
    fun gameOver_doesNotUnlockComboAchievements() {
        val result = checker.checkGameOver(AchievementContext(maxCombo = 10, gameOver = true))
        assertFalse("No combo10 from game-over", result.any { it.id == "combo10" })
    }

    @Test
    fun achievements_unlockOnceOnly() {
        checker.checkPerQuestion(AchievementContext(firstCorrect = true))
        val result = checker.checkPerQuestion(AchievementContext(firstCorrect = true))
        assertTrue("first_correct not re-unlocked", result.none { it.id == "first_correct" })
    }

    @Test
    fun gameOver_dedicated_unlocksAt10Games() {
        repeat(10) { progressRepo.incrementGamesPlayed() }
        val result = checker.checkGameOver(AchievementContext(gameOver = true))
        assertTrue("dedicated unlocked at 10 games", result.any { it.id == "dedicated" })
    }

    @Test
    fun gameOver_recordSurvival_unlocksOnNewRecord() {
        val result = checker.checkGameOver(AchievementContext(gameOver = true, newRecord = true, gameMode = "survival"))
        assertTrue("record_survival unlocked", result.any { it.id == "record_survival" })
    }

    @Test
    fun gameOver_examPassed_unlocks() {
        val result = checker.checkGameOver(AchievementContext(gameOver = true, examPassed = true))
        assertTrue("exam_pass unlocked", result.any { it.id == "exam_pass" })
    }

    @Test
    fun gameOver_examPerfect_unlocks() {
        val result = checker.checkGameOver(AchievementContext(gameOver = true, examPerfect = true))
        assertTrue("exam_perfect unlocked", result.any { it.id == "exam_perfect" })
    }

    @Test
    fun gameOver_examNotPassed_doesNotUnlock() {
        val result = checker.checkGameOver(AchievementContext(gameOver = true, examPassed = false))
        assertFalse("exam_pass not unlocked when not passed", result.any { it.id == "exam_pass" })
    }

    @Test
    fun gameOver_simulacroPassed_unlocks() {
        val result = checker.checkGameOver(AchievementContext(gameOver = true, simulacroPassed = true))
        assertTrue("simulacro_pass unlocked", result.any { it.id == "simulacro_pass" })
    }

    @Test
    fun gameOver_simulacroPerfect_unlocks() {
        val result = checker.checkGameOver(AchievementContext(gameOver = true, simulacroPerfect = true))
        assertTrue("simulacro_perfect unlocked", result.any { it.id == "simulacro_perfect" })
    }

    @Test
    fun gameOver_simulacroNotPassed_doesNotUnlock() {
        val result = checker.checkGameOver(AchievementContext(gameOver = true, simulacroPassed = false))
        assertFalse("simulacro_pass not unlocked when not passed", result.any { it.id == "simulacro_pass" })
    }

    @Test
    fun perQuestion_combo15_unlocks() {
        val result = checker.checkPerQuestion(AchievementContext(maxCombo = 15))
        assertTrue("combo15 unlocked", result.any { it.id == "combo15" })
    }

    @Test
    fun perQuestion_combo20_unlocks() {
        val result = checker.checkPerQuestion(AchievementContext(maxCombo = 20))
        assertTrue("combo20 unlocked", result.any { it.id == "combo20" })
    }

    @Test
    fun perQuestion_combo25_unlocks() {
        val result = checker.checkPerQuestion(AchievementContext(maxCombo = 25))
        assertTrue("combo25 unlocked", result.any { it.id == "combo25" })
    }

    @Test
    fun perQuestion_comboBlockedByLowMaxOptions() {
        val result = checker.checkPerQuestion(AchievementContext(maxCombo = 10, maxOptions = 3))
        assertFalse("combo5 not unlocked with maxOptions < 4", result.any { it.id == "combo5" })
        assertFalse("combo10 not unlocked with maxOptions < 4", result.any { it.id == "combo10" })
    }

    @Test
    fun gameOver_100correct_unlocks() {
        val stats = com.opoleyes.data.repository.StatsRepository(prefs)
        repeat(100) { stats.updateStat("key$it", true) }
        val result = checker.checkGameOver(AchievementContext(gameOver = true))
        assertTrue("100correct unlocked", result.any { it.id == "100correct" })
    }

    @Test
    fun gameOver_500correct_unlocks() {
        val stats = com.opoleyes.data.repository.StatsRepository(prefs)
        repeat(500) { stats.updateStat("key$it", true) }
        val result = checker.checkGameOver(AchievementContext(gameOver = true))
        assertTrue("500correct unlocked", result.any { it.id == "500correct" })
    }

    @Test
    fun gameOver_1000correct_unlocks() {
        val stats = com.opoleyes.data.repository.StatsRepository(prefs)
        repeat(1000) { stats.updateStat("key$it", true) }
        val result = checker.checkGameOver(AchievementContext(gameOver = true))
        assertTrue("1000correct unlocked", result.any { it.id == "1000correct" })
    }

    @Test
    fun gameOver_medalSilver_unlocks() {
        val result = checker.checkGameOver(AchievementContext(score = 600))
        assertTrue("medal_silver unlocked", result.any { it.id == "medal_silver" })
    }

    @Test
    fun gameOver_recordTimetrial_unlocksOnNewRecord() {
        val result = checker.checkGameOver(AchievementContext(gameOver = true, newRecord = true, gameMode = "timetrial"))
        assertTrue("record_timetrial unlocked", result.any { it.id == "record_timetrial" })
    }

    @Test
    fun gameOver_recordQuick_unlocksOnNewRecord() {
        val result = checker.checkGameOver(AchievementContext(gameOver = true, newRecord = true, gameMode = "quick"))
        assertTrue("record_quick unlocked", result.any { it.id == "record_quick" })
    }

    @Test
    fun gameOver_habitual_unlocksAt25Games() {
        repeat(25) { progressRepo.incrementGamesPlayed() }
        val result = checker.checkGameOver(AchievementContext(gameOver = true))
        assertTrue("habitual unlocked at 25 games", result.any { it.id == "habitual" })
    }

    @Test
    fun gameOver_addicted_unlocksAt50Games() {
        repeat(50) { progressRepo.incrementGamesPlayed() }
        val result = checker.checkGameOver(AchievementContext(gameOver = true))
        assertTrue("addicted unlocked at 50 games", result.any { it.id == "addicted" })
    }

    @Test
    fun gameOver_expert_unlocksAtRank5() {
        prefs.addXP(67000)
        val result = checker.checkGameOver(AchievementContext(gameOver = true))
        assertTrue("expert unlocked at rank 5", result.any { it.id == "expert" })
    }

    @Test
    fun gameOver_master_unlocksAtRank7() {
        prefs.addXP(122000)
        val result = checker.checkGameOver(AchievementContext(gameOver = true))
        assertTrue("master unlocked at rank 7", result.any { it.id == "master" })
    }

    @Test
    fun gameOver_perfectGame_unlocks() {
        val result = checker.checkGameOver(AchievementContext(gameOver = true, perfectGame = true, maxOptions = 4))
        assertTrue("perfect_game unlocked", result.any { it.id == "perfect_game" })
    }

    @Test
    fun gameOver_perfectGame_blockedByLowMaxOptions() {
        val result = checker.checkGameOver(AchievementContext(gameOver = true, perfectGame = true, maxOptions = 3))
        assertFalse("perfect_game not unlocked with maxOptions < 4", result.any { it.id == "perfect_game" })
    }

    @Test
    fun gameOver_sharpshooter_unlocks() {
        val result = checker.checkGameOver(AchievementContext(gameOver = true, sharpshooter = true, maxOptions = 4))
        assertTrue("sharpshooter unlocked", result.any { it.id == "sharpshooter" })
    }

    @Test
    fun gameOver_sharpshooter_blockedByLowMaxOptions() {
        val result = checker.checkGameOver(AchievementContext(gameOver = true, sharpshooter = true, maxOptions = 3))
        assertFalse("sharpshooter not unlocked with maxOptions < 4", result.any { it.id == "sharpshooter" })
    }

    @Test
    fun gameOver_firstLaw_doesNotCrashWithNullContext() {
        // AchievementChecker with null context (via IPreferencesManager constructor)
        // should handle law achievement checks gracefully (empty temaTests)
        val result = checker.checkGameOver(AchievementContext(gameOver = true))
        assertTrue("Should not crash with null context", result.isNotEmpty())
    }
}
