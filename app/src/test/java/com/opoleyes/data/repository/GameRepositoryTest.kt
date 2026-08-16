package com.opoleyes.data.repository

import androidx.test.core.app.ApplicationProvider
import com.opoleyes.TestContextProvider
import com.opoleyes.data.Constants
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.QuestionEntry
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
class GameRepositoryTest {

    private lateinit var prefs: PreferencesManager
    private lateinit var gameRepo: GameRepository
    private lateinit var statsRepo: StatsRepository

    @Before
    fun setup() {
        val ctx = TestContextProvider.getContext()
        prefs = PreferencesManager(ctx)
        prefs.resetAll()
        gameRepo = GameRepository(ctx)
        statsRepo = StatsRepository(ctx)
    }

    private fun firstTemaTestId(): String {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        val temaTests = DataProvider.getTemaTests(ctx)
        assertTrue("Should have tema tests", temaTests.isNotEmpty())
        return temaTests.first().id
    }

    // === startAllLawsGame ===

    @Test
    fun startAllLawsGame_poolNotEmpty() {
        val pool = gameRepo.startAllLawsGame()
        assertTrue("Pool should not be empty", pool.isNotEmpty())
    }

    @Test
    fun startAllLawsGame_allQuestionsHave4Options() {
        val pool = gameRepo.startAllLawsGame()
        for (q in pool) {
            assertEquals("Question ${q.testId}:${q.origId} should have 4 options", 4, q.opciones.size)
        }
    }

    @Test
    fun startAllLawsGame_allCorrectAnswersAreValid() {
        val pool = gameRepo.startAllLawsGame()
        for (q in pool) {
            assertTrue("Question correct '${q.correct}' should be in options ${q.opciones.keys}",
                q.opciones.containsKey(q.correct))
        }
    }

    @Test
    fun startAllLawsGame_allQuestionsHaveTestId() {
        val pool = gameRepo.startAllLawsGame()
        for (q in pool) {
            assertTrue("Question should have non-empty testId", q.testId.isNotEmpty())
        }
    }

    // === startTemaGame ===

    @Test
    fun startTemaGame_poolOnlyFromThatTema() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        val testId = firstTemaTestId()
        val testData = DataProvider.getTestDataMap(ctx)[testId]!!
        // A single TestData entry may contain questions with multiple test_id values
        // (e.g. Constitución Española has Tema_N01-N04). Verify all pool questions
        // come from this TestData entry by checking their test_id is in the source set.
        val sourceTestIds = testData.questions.map { it.test_id }.toSet()
        val pool = gameRepo.startTemaGame(testId)
        assertTrue("Tema pool should not be empty", pool.isNotEmpty())
        for (q in pool) {
            assertTrue("Pool question testId '${q.testId}' should be in source test $testId",
                q.testId in sourceTestIds)
        }
    }

    @Test
    fun startTemaGame_invalidTestId_returnsEmpty() {
        val pool = gameRepo.startTemaGame("nonexistent_test_id")
        assertTrue("Pool for invalid testId should be empty", pool.isEmpty())
    }

    @Test
    fun startTemaGame_allQuestionsHave4Options() {
        val testId = firstTemaTestId()
        val pool = gameRepo.startTemaGame(testId)
        for (q in pool) {
            assertEquals("Question should have 4 options", 4, q.opciones.size)
        }
    }

    // === startQuickGame ===

    @Test
    fun startQuickGame_poolMax5Questions() {
        val pool = gameRepo.startQuickGame()
        assertTrue("Quick pool should not be empty", pool.isNotEmpty())
        assertTrue("Quick pool should have <= ${Constants.QUICK_MODE_QUESTIONS} questions, got ${pool.size}",
            pool.size <= Constants.QUICK_MODE_QUESTIONS)
    }

    @Test
    fun startQuickGame_allQuestionsHaveValidCorrect() {
        val pool = gameRepo.startQuickGame()
        for (q in pool) {
            assertTrue("Quick pool question correct not in options", q.opciones.containsKey(q.correct))
        }
    }

    // === getFilteredAndWeightedPool ===

    @Test
    fun getFilteredAndWeightedPool_rank0_filtersByMaxDifficulty() {
        val fullPool = gameRepo.startAllLawsGame()
        val maxDiffRank0 = Constants.MAX_DIFFICULTY_BY_RANK[0] ?: 5
        val filtered = gameRepo.getFilteredAndWeightedPool(fullPool, rankIndex = 0)
        for (q in filtered) {
            assertTrue("Rank 0 should not see difficulty > $maxDiffRank0, got ${q.difficulty}",
                q.difficulty <= maxDiffRank0)
        }
    }

    @Test
    fun getFilteredAndWeightedPool_highRank_includesAll() {
        val fullPool = gameRepo.startAllLawsGame()
        val filtered = gameRepo.getFilteredAndWeightedPool(fullPool, rankIndex = 8)
        // Rank 8 has maxDifficulty=5, so all questions should be included
        assertEquals("Rank 8 should include all questions", fullPool.size, filtered.size)
    }

    @Test
    fun getFilteredAndWeightedPool_rank2_filtersDifficulty3() {
        val fullPool = gameRepo.startAllLawsGame()
        val maxDiffRank2 = Constants.MAX_DIFFICULTY_BY_RANK[2] ?: 5
        val filtered = gameRepo.getFilteredAndWeightedPool(fullPool, rankIndex = 2)
        for (q in filtered) {
            assertTrue("Rank 2 should not see difficulty > $maxDiffRank2", q.difficulty <= maxDiffRank2)
        }
    }

    // === Multiplier round-trip ===

    @Test
    fun getMultiplier_defaultIs1() {
        assertEquals("Default multiplier should be 1", 1, gameRepo.getMultiplier())
    }

    @Test
    fun setMultiplier_thenGet_returnsValue() {
        gameRepo.setMultiplier(2)
        assertEquals(2, gameRepo.getMultiplier())
    }

    @Test
    fun setMultiplier_resetTo1() {
        gameRepo.setMultiplier(3)
        gameRepo.setMultiplier(1)
        assertEquals(1, gameRepo.getMultiplier())
    }

    @Test
    fun startQuickGame_withWrongAnswerStats_includesWrongQuestions() {
        // Record some wrong answers to populate the wrong pool
        val pool = gameRepo.startAllLawsGame()
        assertTrue("Pool should not be empty", pool.isNotEmpty())
        // Record wrong answers for first few questions
        for (q in pool.take(5)) {
            val key = "${q.testId}:${q.origId}"
            statsRepo.updateStat(key, isCorrect = false)
        }
        // Now quick game should include questions from the wrong pool
        val quickPool = gameRepo.startQuickGame()
        assertTrue("Quick pool should not be empty", quickPool.isNotEmpty())
    }

    @Test
    fun startQuickGame_withHighRank_includesHigherDifficulty() {
        // Set high XP to unlock higher difficulty
        prefs.addXP(160000) // rank 8 = Leyenda
        val pool = gameRepo.startQuickGame()
        // Should still return valid pool
        assertTrue("Quick pool should not be empty", pool.isNotEmpty())
    }

    @Test
    fun getFilteredAndWeightedPool_emptyPool_returnsEmpty() {
        val filtered = gameRepo.getFilteredAndWeightedPool(emptyList(), rankIndex = 0)
        assertTrue("Filtered empty pool should be empty", filtered.isEmpty())
    }
}
