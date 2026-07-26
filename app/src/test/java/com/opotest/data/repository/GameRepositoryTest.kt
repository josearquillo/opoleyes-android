package com.opotest.data.repository

import com.opotest.TestContextProvider
import com.opotest.data.local.DataProvider
import com.opotest.data.local.PreferencesManager
import com.opotest.data.model.Question
import com.opotest.data.model.Answer
import com.opotest.data.model.TestData
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameRepositoryTest {

    private lateinit var repo: GameRepository
    private lateinit var prefs: PreferencesManager
    private lateinit var context: android.content.Context

    @Before
    fun setup() {
        val context = TestContextProvider.getContext()
        this.context = context
        prefs = PreferencesManager(context)
        prefs.resetAll()
        repo = GameRepository(context)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun fun_loadData_returnsNonEmptyList() {
        val data = DataProvider.loadData(context)
        assertTrue(data.isNotEmpty())
    }

    @Test
    fun fun_loadData_cachesResult() {
        val data1 = DataProvider.loadData(context)
        val data2 = DataProvider.loadData(context)
        assertEquals(data1.size, data2.size)
    }

    @Test
    fun fun_getTestDataMap_returnsMap() {
        val map = DataProvider.getTestDataMap(context)
        assertTrue(map.isNotEmpty())
    }

    @Test
    fun fun_getTests_returnsTests() {
        val tests = DataProvider.getTests(context)
        assertTrue(tests.isNotEmpty())
    }

    @Test
    fun fun_getTemaTests_filtersNullTemas() {
        val temaTests = DataProvider.getTemaTests(context)
        temaTests.forEach { assertNotNull(it.tema) }
    }

    @Test
    fun fun_startTemaGame_returnsQuestions() {
        val temaTests = DataProvider.getTemaTests(context)
        if (temaTests.isNotEmpty()) {
            val pool = repo.startTemaGame(temaTests[0].id)
            assertTrue(pool.isNotEmpty())
        }
    }

    @Test
    fun fun_startTemaGame_nonexistentReturnsEmpty() {
        val pool = repo.startTemaGame("nonexistent_id")
        assertTrue(pool.isEmpty())
    }

    @Test
    fun fun_startAllLawsGame_returnsQuestions() {
        val pool = repo.startAllLawsGame()
        assertTrue(pool.isNotEmpty())
    }

    @Test
    fun fun_startQuickGame_returnsQuestions() {
        val pool = repo.startQuickGame()
        assertTrue(pool.isNotEmpty())
    }

    @Test
    fun fun_startQuickGame_limitsTo20() {
        val pool = repo.startQuickGame()
        assertTrue(pool.size <= 20)
    }

    @Test
    fun fun_startTraining_returnsTestData() {
        val temaTests = DataProvider.getTemaTests(context)
        if (temaTests.isNotEmpty()) {
            val td = repo.startTraining(temaTests[0].id)
            assertTrue(td.questions.isNotEmpty())
        }
    }

    @Test
    fun fun_startTraining_nonexistentReturnsEmpty() {
        val td = repo.startTraining("nonexistent")
        assertTrue(td.questions.isEmpty())
    }

    @Test
    fun fun_startTrainingCustom_returnsQuestions() {
        val td = repo.startTrainingCustom("", 10)
        assertTrue(td.questions.isNotEmpty())
        assertTrue(td.questions.size <= 10)
    }

    @Test
    fun fun_startTrainingCustom_withCount() {
        val td = repo.startTrainingCustom("", 5)
        assertTrue(td.questions.size <= 5)
    }

    @Test
    fun fun_getFreePowerUps_emptyByDefault() {
        assertTrue(repo.getFreePowerUps().isEmpty())
    }

    @Test
    fun fun_addFreePowerUps() {
        repo.addFreePowerUps(listOf("shield", "fiftyFifty"))
        val powerUps = repo.getFreePowerUps()
        assertEquals(2, powerUps.size)
        assertTrue(powerUps.contains("shield"))
        assertTrue(powerUps.contains("fiftyFifty"))
    }

    @Test
    fun fun_clearFreePowerUps() {
        repo.addFreePowerUps(listOf("shield"))
        repo.clearFreePowerUps()
        assertTrue(repo.getFreePowerUps().isEmpty())
    }

    @Test
    fun fun_getMultiplier_defaultsTo1() {
        assertEquals(1, repo.getMultiplier())
    }

    @Test
    fun fun_setMultiplier() {
        repo.setMultiplier(3)
        assertEquals(3, repo.getMultiplier())
    }

    @Test
    fun fun_addFreePowerUps_appends() {
        repo.addFreePowerUps(listOf("shield"))
        repo.addFreePowerUps(listOf("fiftyFifty"))
        val powerUps = repo.getFreePowerUps()
        assertEquals(2, powerUps.size)
    }
}
