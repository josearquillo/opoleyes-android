package com.opoleyes.domain

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.opoleyes.TestContextProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AchievementCheckerTest {

    private lateinit var prefs: PreferencesManager
    private lateinit var checker: AchievementChecker

    @Before
    fun setup() {
        val ctx = TestContextProvider.getContext()
        prefs = PreferencesManager(ctx)
        prefs.resetAll()
        checker = AchievementChecker(ctx)
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
        val repo = ProgressRepository(TestContextProvider.getContext())
        repeat(10) { repo.incrementGamesPlayed() }
        val result = checker.checkGameOver(AchievementContext(gameOver = true))
        assertTrue("dedicated unlocked at 10 games", result.any { it.id == "dedicated" })
    }

    @Test
    fun gameOver_recordSurvival_unlocksOnNewRecord() {
        val result = checker.checkGameOver(AchievementContext(gameOver = true, newRecord = true, gameMode = "survival"))
        assertTrue("record_survival unlocked", result.any { it.id == "record_survival" })
    }
}
