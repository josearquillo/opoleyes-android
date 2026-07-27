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
class StatsRepositoryTest {

    private lateinit var repo: StatsRepository
    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        val context = TestContextProvider.getContext()
        prefs = PreferencesManager(context)
        prefs.resetAll()
        repo = StatsRepository(context)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun fun_getStats_emptyByDefault() {
        assertTrue(repo.getStats().isEmpty())
    }

    @Test
    fun fun_updateStat_correct() {
        repo.updateStat("key1", true)
        val stats = repo.getStats()
        assertEquals(1, stats["key1"]?.correct)
        assertEquals(0, stats["key1"]?.wrong)
    }

    @Test
    fun fun_updateStat_wrong() {
        repo.updateStat("key1", false)
        val stats = repo.getStats()
        assertEquals(0, stats["key1"]?.correct)
        assertEquals(1, stats["key1"]?.wrong)
    }

    @Test
    fun fun_updateStat_multiple() {
        repo.updateStat("key1", true)
        repo.updateStat("key1", true)
        repo.updateStat("key1", false)
        val stats = repo.getStats()
        assertEquals(2, stats["key1"]?.correct)
        assertEquals(1, stats["key1"]?.wrong)
    }

    @Test
    fun fun_getWeight_noStats_returns50() {
        assertEquals(50, repo.getWeight("nonexistent"))
    }

    @Test
    fun fun_getWeight_lessThan3Attempts_returns50() {
        repo.updateStat("key1", true)
        repo.updateStat("key1", false)
        assertEquals(50, repo.getWeight("key1"))
    }

    @Test
    fun fun_getWeight_allCorrect_returns5() {
        repeat(5) { repo.updateStat("key1", true) }
        assertEquals(5, repo.getWeight("key1"))
    }

    @Test
    fun fun_getWeight_allWrong_returnsHighWeight() {
        repeat(5) { repo.updateStat("key1", false) }
        val w = repo.getWeight("key1")
        assertTrue("Weight should be high for all wrong: $w", w >= 90)
    }

    @Test
    fun fun_getWeight_mixed() {
        repeat(3) { repo.updateStat("key1", true) }
        repeat(2) { repo.updateStat("key1", false) }
        val w = repo.getWeight("key1")
        assertTrue("Weight should reflect 40% wrong: $w", w in 30..50)
    }

    @Test
    fun fun_getTotalCorrect() {
        repo.updateStat("k1", true)
        repo.updateStat("k2", true)
        repo.updateStat("k3", false)
        assertEquals(2, repo.getTotalCorrect())
    }

    @Test
    fun fun_getTotalWrong() {
        repo.updateStat("k1", true)
        repo.updateStat("k2", false)
        repo.updateStat("k3", false)
        assertEquals(2, repo.getTotalWrong())
    }

    @Test
    fun fun_getTotalCorrect_empty() {
        assertEquals(0, repo.getTotalCorrect())
    }

    @Test
    fun fun_saveStats() {
        val stats = mapOf("k1" to QuestionStat(correct = 5, wrong = 3))
        repo.saveStats(stats)
        assertEquals(5, repo.getStats()["k1"]?.correct)
        assertEquals(3, repo.getStats()["k1"]?.wrong)
    }

    @Test
    fun fun_getLeyProgress_nonexistentTest_returns0() {
        assertEquals(0, repo.getLeyProgress("nonexistent"))
    }
}
