package com.opoleyes.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StatsRepositoryDataTest {

    private lateinit var prefs: PreferencesManager
    private lateinit var statsRepo: StatsRepository

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        prefs = PreferencesManager(ctx)
        prefs.resetAll()
        statsRepo = StatsRepository(ctx)
    }

    @Test
    fun getLeyProgress_validTestId_returnsPercentageInRange() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val temaTests = DataProvider.getTemaTests(ctx)
        assertTrue("Should have tema tests", temaTests.isNotEmpty())
        val testId = temaTests.first().id
        val progress = statsRepo.getLeyProgress(testId)
        assertTrue("Ley progress should be 0-100, got $progress", progress in 0..100)
    }

    @Test
    fun getLeyProgress_emptyStats_returns0() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val temaTests = DataProvider.getTemaTests(ctx)
        val testId = temaTests.first().id
        assertEquals("Progress with no stats should be 0", 0, statsRepo.getLeyProgress(testId))
    }

    @Test
    fun getGlobalProgress_returnsPercentageInRange() {
        statsRepo.updateStat("test1:1", true)
        val progress = statsRepo.getGlobalProgress()
        assertTrue("Global progress should be 0-100, got $progress", progress in 0..100)
    }
}
