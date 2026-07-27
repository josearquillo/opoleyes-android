package com.opoleyes.domain

import com.opoleyes.TestContextProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AchievementCheckerTest {

    private lateinit var checker: AchievementChecker
    private lateinit var prefs: PreferencesManager
    private lateinit var progressRepo: ProgressRepository
    private lateinit var statsRepo: StatsRepository

    @Before
    fun setup() {
        val context = TestContextProvider.getContext()
        prefs = PreferencesManager(context)
        prefs.resetAll()
        progressRepo = ProgressRepository(context)
        statsRepo = StatsRepository(context)
        checker = AchievementChecker(context)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun fun_check_firstCorrect() {
        val unlocked = checker.check(AchievementContext(firstCorrect = true))
        val ids = unlocked.map { it.id }
        assertTrue(ids.contains("first_correct"))
    }

    @Test
    fun fun_check_combo5() {
        val unlocked = checker.check(AchievementContext(maxCombo = 5))
        assertTrue(unlocked.map { it.id }.contains("combo5"))
    }

    @Test
    fun fun_check_combo10() {
        val unlocked = checker.check(AchievementContext(maxCombo = 10))
        assertTrue(unlocked.map { it.id }.contains("combo10"))
    }

    @Test
    fun fun_check_combo15() {
        val unlocked = checker.check(AchievementContext(maxCombo = 15))
        assertTrue(unlocked.map { it.id }.contains("combo15"))
    }

    @Test
    fun fun_check_combo20() {
        val unlocked = checker.check(AchievementContext(maxCombo = 20))
        assertTrue(unlocked.map { it.id }.contains("combo20"))
    }

    @Test
    fun fun_check_combo25() {
        val unlocked = checker.check(AchievementContext(maxCombo = 25))
        assertTrue(unlocked.map { it.id }.contains("combo25"))
    }

    @Test
    fun fun_check_gameOverUnlocksFirstRecord() {
        val unlocked = checker.check(AchievementContext(gameOver = true))
        assertTrue(unlocked.map { it.id }.contains("first_record"))
    }

    @Test
    fun fun_check_medalBronze() {
        val unlocked = checker.check(AchievementContext(score = 300, gameOver = true))
        assertTrue(unlocked.map { it.id }.contains("medal_bronze"))
    }

    @Test
    fun fun_check_medalSilver() {
        val unlocked = checker.check(AchievementContext(score = 600, gameOver = true))
        assertTrue(unlocked.map { it.id }.contains("medal_silver"))
    }

    @Test
    fun fun_check_medalGold() {
        val unlocked = checker.check(AchievementContext(score = 1000, gameOver = true))
        assertTrue(unlocked.map { it.id }.contains("medal_gold"))
    }

    @Test
    fun fun_check_recordSurvival() {
        val unlocked = checker.check(AchievementContext(newRecord = true, gameMode = "survival", gameOver = true))
        assertTrue(unlocked.map { it.id }.contains("record_survival"))
    }

    @Test
    fun fun_check_recordTimetrial() {
        val unlocked = checker.check(AchievementContext(newRecord = true, gameMode = "timetrial", gameOver = true))
        assertTrue(unlocked.map { it.id }.contains("record_timetrial"))
    }

    @Test
    fun fun_check_recordQuick() {
        val unlocked = checker.check(AchievementContext(newRecord = true, gameMode = "quick", gameOver = true))
        assertTrue(unlocked.map { it.id }.contains("record_quick"))
    }

    @Test
    fun fun_check_dedicated10Games() {
        repeat(10) { progressRepo.incrementGamesPlayed() }
        val unlocked = checker.check(AchievementContext())
        assertTrue(unlocked.map { it.id }.contains("dedicated"))
    }

    @Test
    fun fun_check_habitual25Games() {
        repeat(25) { progressRepo.incrementGamesPlayed() }
        val unlocked = checker.check(AchievementContext())
        assertTrue(unlocked.map { it.id }.contains("habitual"))
    }

    @Test
    fun fun_check_addicted50Games() {
        repeat(50) { progressRepo.incrementGamesPlayed() }
        val unlocked = checker.check(AchievementContext())
        assertTrue(unlocked.map { it.id }.contains("addicted"))
    }

    @Test
    fun fun_check_studious5Trainings() {
        repeat(5) { progressRepo.incrementTrainingsDone() }
        val unlocked = checker.check(AchievementContext())
        assertTrue(unlocked.map { it.id }.contains("studious"))
    }

    @Test
    fun fun_check_student10Trainings() {
        repeat(10) { progressRepo.incrementTrainingsDone() }
        val unlocked = checker.check(AchievementContext())
        assertTrue(unlocked.map { it.id }.contains("student"))
    }

    @Test
    fun fun_check_professor25Trainings() {
        repeat(25) { progressRepo.incrementTrainingsDone() }
        val unlocked = checker.check(AchievementContext())
        assertTrue(unlocked.map { it.id }.contains("professor"))
    }

    @Test
    fun fun_check_expertRank() {
        progressRepo.addXP(7000)
        val unlocked = checker.check(AchievementContext())
        assertTrue(unlocked.map { it.id }.contains("expert"))
    }

    @Test
    fun fun_check_masterRank() {
        progressRepo.addXP(20000)
        val unlocked = checker.check(AchievementContext())
        assertTrue(unlocked.map { it.id }.contains("master"))
    }

    @Test
    fun fun_check_perfectGame() {
        val unlocked = checker.check(AchievementContext(perfectGame = true))
        assertTrue(unlocked.map { it.id }.contains("perfect_game"))
    }

    @Test
    fun fun_check_sharpshooter() {
        val unlocked = checker.check(AchievementContext(sharpshooter = true))
        assertTrue(unlocked.map { it.id }.contains("sharpshooter"))
    }

    @Test
    fun fun_check_strategist() {
        val unlocked = checker.check(AchievementContext(fiftyFiftyUsed = true))
        assertTrue(unlocked.map { it.id }.contains("strategist"))
    }

    @Test
    fun fun_check_resurrection() {
        val unlocked = checker.check(AchievementContext(lifeRecovered = true))
        assertTrue(unlocked.map { it.id }.contains("resurrection"))
    }

    @Test
    fun fun_check_doesNotReunlock() {
        checker.check(AchievementContext(firstCorrect = true))
        val unlocked = checker.check(AchievementContext(firstCorrect = true))
        assertTrue(unlocked.none { it.id == "first_correct" })
    }

    @Test
    fun fun_check_emptyContextUnlocksNothing() {
        val unlocked = checker.check(AchievementContext())
        assertTrue(unlocked.isEmpty())
    }

    @Test
    fun fun_check_100correct() {
        repeat(100) { statsRepo.updateStat("key$it", true) }
        val unlocked = checker.check(AchievementContext())
        assertTrue(unlocked.map { it.id }.contains("100correct"))
    }
}
