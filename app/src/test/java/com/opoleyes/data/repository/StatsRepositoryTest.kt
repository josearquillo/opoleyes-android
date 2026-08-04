package com.opoleyes.data.repository

import androidx.test.core.app.ApplicationProvider
import com.opoleyes.TestContextProvider
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.QuestionStat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StatsRepositoryTest {

    private lateinit var prefs: PreferencesManager
    private lateinit var statsRepo: StatsRepository

    @Before
    fun setup() {
        val ctx = TestContextProvider.getContext()
        prefs = PreferencesManager(ctx)
        prefs.resetAll()
        statsRepo = StatsRepository(ctx)
    }

    // === updateStat ===

    @Test
    fun updateStat_correct_incrementsCorrectCounter() {
        statsRepo.updateStat("test1:1", isCorrect = true)
        val stats = statsRepo.getStats()
        assertEquals(1, stats["test1:1"]?.correct)
        assertEquals(0, stats["test1:1"]?.wrong)
    }

    @Test
    fun updateStat_wrong_incrementsWrongCounter() {
        statsRepo.updateStat("test1:1", isCorrect = false)
        val stats = statsRepo.getStats()
        assertEquals(0, stats["test1:1"]?.correct)
        assertEquals(1, stats["test1:1"]?.wrong)
    }

    @Test
    fun updateStat_multipleCalls_accumulate() {
        statsRepo.updateStat("test1:1", isCorrect = true)
        statsRepo.updateStat("test1:1", isCorrect = true)
        statsRepo.updateStat("test1:1", isCorrect = false)
        statsRepo.updateStat("test1:2", isCorrect = true)
        val stats = statsRepo.getStats()
        assertEquals(2, stats["test1:1"]?.correct)
        assertEquals(1, stats["test1:1"]?.wrong)
        assertEquals(1, stats["test1:2"]?.correct)
    }

    // === getStats ===

    @Test
    fun getStats_emptyByDefault() {
        val stats = statsRepo.getStats()
        assertTrue("Stats should be empty by default", stats.isEmpty())
    }

    @Test
    fun getStats_returnsAccumulatedMap() {
        statsRepo.updateStat("a:1", true)
        statsRepo.updateStat("b:2", false)
        statsRepo.updateStat("c:3", true)
        val stats = statsRepo.getStats()
        assertEquals(3, stats.size)
        assertEquals(1, stats["a:1"]?.correct)
        assertEquals(1, stats["b:2"]?.wrong)
        assertEquals(1, stats["c:3"]?.correct)
    }

    @Test
    fun getStats_cachesResult() {
        statsRepo.updateStat("test1:1", true)
        val first = statsRepo.getStats()
        // Modify prefs directly (bypassing repo cache)
        prefs.saveStats(mapOf("test1:1" to QuestionStat(99, 0)))
        val cached = statsRepo.getStats()
        // Should still return cached value, not the directly-written one
        assertEquals("Cache should prevent reload", first["test1:1"]?.correct, cached["test1:1"]?.correct)
    }

    // === getWeight ===

    @Test
    fun getWeight_zeroAttempts_returns50() {
        assertEquals("Weight for unseen question should be 50", 50, statsRepo.getWeight("test1:1"))
    }

    @Test
    fun getWeight_fewAttempts_returns50() {
        // < 3 attempts → 50
        statsRepo.updateStat("test1:1", true)
        statsRepo.updateStat("test1:1", false)
        assertEquals("Weight for < 3 attempts should be 50", 50, statsRepo.getWeight("test1:1"))
    }

    @Test
    fun getWeight_lowAccuracy_returnsHighWeight() {
        // 3 attempts, 0 correct → weight = 100 * (1 - 0/3) = 100
        statsRepo.updateStat("test1:1", false)
        statsRepo.updateStat("test1:1", false)
        statsRepo.updateStat("test1:1", false)
        val weight = statsRepo.getWeight("test1:1")
        assertEquals("Weight for 0% accuracy should be 100", 100, weight)
    }

    @Test
    fun getWeight_highAccuracy_returnsLowWeight() {
        // 3 attempts, 3 correct → weight = 100 * (1 - 3/3) = 0, but maxOf(w, 5) = 5
        statsRepo.updateStat("test1:1", true)
        statsRepo.updateStat("test1:1", true)
        statsRepo.updateStat("test1:1", true)
        val weight = statsRepo.getWeight("test1:1")
        assertEquals("Weight for 100% accuracy should be 5 (min)", 5, weight)
    }

    @Test
    fun getWeight_mediumAccuracy_returnsMediumWeight() {
        // 4 attempts, 2 correct → weight = 100 * (1 - 2/4) = 50
        statsRepo.updateStat("test1:1", true)
        statsRepo.updateStat("test1:1", true)
        statsRepo.updateStat("test1:1", false)
        statsRepo.updateStat("test1:1", false)
        val weight = statsRepo.getWeight("test1:1")
        assertEquals("Weight for 50% accuracy should be 50", 50, weight)
    }

    // === getLeyProgress ===

    @Test
    fun getLeyProgress_validTestId_returnsPercentageInRange() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        val temaTests = DataProvider.getTemaTests(ctx)
        assertTrue("Should have tema tests", temaTests.isNotEmpty())
        val testId = temaTests.first().id
        val progress = statsRepo.getLeyProgress(testId)
        assertTrue("Ley progress should be 0-100, got $progress", progress in 0..100)
    }

    @Test
    fun getLeyProgress_invalidTestId_returns0() {
        assertEquals("Progress for invalid testId should be 0", 0, statsRepo.getLeyProgress("nonexistent_test"))
    }

    @Test
    fun getLeyProgress_emptyStats_returns0() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        val temaTests = DataProvider.getTemaTests(ctx)
        val testId = temaTests.first().id
        assertEquals("Progress with no stats should be 0", 0, statsRepo.getLeyProgress(testId))
    }

    // === getGlobalProgress ===

    @Test
    fun getGlobalProgress_emptyStats_returns0() {
        assertEquals("Global progress with no stats should be 0", 0, statsRepo.getGlobalProgress())
    }

    @Test
    fun getGlobalProgress_returnsPercentageInRange() {
        // Add some stats
        statsRepo.updateStat("test1:1", true)
        val progress = statsRepo.getGlobalProgress()
        assertTrue("Global progress should be 0-100, got $progress", progress in 0..100)
    }

    // === getTotalCorrect / getTotalWrong ===

    @Test
    fun getTotalCorrect_emptyStats_returns0() {
        assertEquals(0, statsRepo.getTotalCorrect())
    }

    @Test
    fun getTotalWrong_emptyStats_returns0() {
        assertEquals(0, statsRepo.getTotalWrong())
    }

    @Test
    fun getTotalCorrect_sumsAllCorrect() {
        statsRepo.updateStat("a:1", true)
        statsRepo.updateStat("a:2", true)
        statsRepo.updateStat("b:1", false)
        statsRepo.updateStat("c:1", true)
        assertEquals(3, statsRepo.getTotalCorrect())
    }

    @Test
    fun getTotalWrong_sumsAllWrong() {
        statsRepo.updateStat("a:1", true)
        statsRepo.updateStat("a:2", false)
        statsRepo.updateStat("b:1", false)
        statsRepo.updateStat("c:1", true)
        assertEquals(2, statsRepo.getTotalWrong())
    }

    // === invalidateCache ===

    @Test
    fun invalidateCache_forcesReloadOnNextGetStats() {
        statsRepo.updateStat("test1:1", true)
        val before = statsRepo.getStats()
        assertEquals(1, before["test1:1"]?.correct)
        // Write directly to prefs, bypassing repo
        prefs.saveStats(mapOf("test1:1" to QuestionStat(99, 0)))
        // Without invalidation, cache returns stale data
        val cached = statsRepo.getStats()
        assertEquals(1, cached["test1:1"]?.correct)
        // After invalidation, getStats reloads from prefs
        statsRepo.invalidateCache()
        val reloaded = statsRepo.getStats()
        assertEquals(99, reloaded["test1:1"]?.correct)
    }

    @Test
    fun invalidateCache_thenUpdateStat_worksCorrectly() {
        statsRepo.updateStat("test1:1", true)
        statsRepo.invalidateCache()
        statsRepo.updateStat("test1:1", true)
        val stats = statsRepo.getStats()
        assertEquals(2, stats["test1:1"]?.correct)
    }

    // === saveStats ===

    @Test
    fun saveStats_storesAndCachesMap() {
        val statsMap = mapOf(
            "test1:1" to QuestionStat(5, 2),
            "test1:2" to QuestionStat(3, 1)
        )
        statsRepo.saveStats(statsMap)
        val retrieved = statsRepo.getStats()
        assertEquals(5, retrieved["test1:1"]?.correct)
        assertEquals(2, retrieved["test1:1"]?.wrong)
        assertEquals(3, retrieved["test1:2"]?.correct)
    }
}
