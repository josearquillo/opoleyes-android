package com.opoleyes.data.repository

import com.opoleyes.TestContextProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.QuestionStat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StatsRepositoryEdgeCaseTest {

    private lateinit var repo: StatsRepository
    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        val ctx = TestContextProvider.getContext()
        prefs = PreferencesManager(ctx)
        prefs.resetAll()
        repo = StatsRepository(ctx)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    // === Edge cases: weight calculation ===

    @Test
    fun fun_getWeight_noStats_returns50() {
        assertEquals(50, repo.getWeight("nonexistent"))
    }

    @Test
    fun fun_getWeight_under3Attempts_returns50() {
        repo.updateStat("key1", true)
        repo.updateStat("key1", false)
        assertEquals(50, repo.getWeight("key1"))
    }

    @Test
    fun fun_getWeight_allWrong_returnsHighWeight() {
        for (i in 0 until 5) repo.updateStat("key2", false)
        val w = repo.getWeight("key2")
        assertTrue("All wrong should have high weight", w >= 90)
    }

    @Test
    fun fun_getWeight_allCorrect_returnsLowWeight() {
        for (i in 0 until 5) repo.updateStat("key3", true)
        val w = repo.getWeight("key3")
        assertEquals(5, w)
    }

    @Test
    fun fun_getWeight_neverBelow5() {
        for (i in 0 until 100) repo.updateStat("key4", true)
        assertTrue(repo.getWeight("key4") >= 5)
    }

    // === Edge cases: empty stats ===

    @Test
    fun fun_getTotalCorrect_emptyStats_returns0() {
        assertEquals(0, repo.getTotalCorrect())
    }

    @Test
    fun fun_getTotalWrong_emptyStats_returns0() {
        assertEquals(0, repo.getTotalWrong())
    }

    @Test
    fun fun_getGlobalProgress_emptyStats_returns0() {
        assertEquals(0, repo.getGlobalProgress())
    }

    // === Edge cases: update stat accumulation ===

    @Test
    fun fun_updateStat_accumulatesCorrectly() {
        repo.updateStat("key", true)
        repo.updateStat("key", true)
        repo.updateStat("key", false)
        val s = repo.getStats()["key"]!!
        assertEquals(2, s.correct)
        assertEquals(1, s.wrong)
    }

    @Test
    fun fun_updateStat_multipleKeys_independent() {
        repo.updateStat("keyA", true)
        repo.updateStat("keyB", false)
        val stats = repo.getStats()
        assertEquals(1, stats["keyA"]!!.correct)
        assertEquals(0, stats["keyA"]!!.wrong)
        assertEquals(0, stats["keyB"]!!.correct)
        assertEquals(1, stats["keyB"]!!.wrong)
    }

    // === Edge cases: ley progress ===

    @Test
    fun fun_getLeyProgress_nonexistentTest_returns0() {
        assertEquals(0, repo.getLeyProgress("nonexistent_test"))
    }

    // === Edge cases: totals ===

    @Test
    fun fun_getTotalCorrect_sumsAllKeys() {
        repo.updateStat("a", true)
        repo.updateStat("b", true)
        repo.updateStat("c", true)
        assertEquals(3, repo.getTotalCorrect())
    }

    @Test
    fun fun_getTotalWrong_sumsAllKeys() {
        repo.updateStat("a", false)
        repo.updateStat("b", false)
        assertEquals(2, repo.getTotalWrong())
    }
}
