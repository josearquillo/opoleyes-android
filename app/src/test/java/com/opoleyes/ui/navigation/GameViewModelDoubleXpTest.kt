package com.opoleyes.ui.navigation

import com.opoleyes.FakeGameRepository
import com.opoleyes.FakePreferencesManager
import com.opoleyes.TestFakes
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository
import com.opoleyes.data.repository.MissionRepository
import com.opoleyes.domain.AchievementChecker
import com.opoleyes.domain.ChestSystem
import com.opoleyes.domain.ExamEngine
import com.opoleyes.domain.GameEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * Tests for the rewarded-ad doubleXp feature in GameViewModel.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GameViewModelDoubleXpTest {

    companion object {
        private val testDispatcher = StandardTestDispatcher()

        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            Dispatchers.setMain(testDispatcher)
        }

        @JvmStatic
        @AfterClass
        fun tearDownClass() {
            Dispatchers.resetMain()
        }
    }

    private lateinit var prefs: FakePreferencesManager
    private lateinit var progressRepo: ProgressRepository
    private lateinit var vm: GameViewModel

    @Before
    fun setup() {
        prefs = FakePreferencesManager()
        prefs.resetAll()
        progressRepo = ProgressRepository(prefs)
        val statsRepo = StatsRepository(prefs)
        val missionRepo = MissionRepository(prefs)
        val gameRepo = FakeGameRepository()
        val engine = GameEngine.createForTest(gameRepo, statsRepo, progressRepo, prefs)
        val examEngine = ExamEngine.createForTest(statsRepo, TestFakes.makePool(100))
        vm = GameViewModel.createForTest(
            progressRepo, statsRepo, missionRepo,
            AchievementChecker(prefs), ChestSystem(prefs), prefs, engine, examEngine
        )
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun doubleXp_doublesCurrentXpGained() {
        // Simulate XP gained by playing a game
        vm.engine.mode = com.opoleyes.data.model.GameMode.SURVIVAL
        vm.engine.startAllLawsGame()
        vm.engine.nextQuestion()
        vm.engine.answer(vm.engine.currentQ!!.correct)

        // Trigger game over to populate xpGained
        vm.onGameOver()

        val xpBefore = vm.xpGained.value
        assertTrue("Should have XP gained > 0", xpBefore > 0)

        vm.doubleXp()

        val xpAfter = vm.xpGained.value
        assertEquals("XP should be doubled", xpBefore * 2, xpAfter)
    }

    @Test
    fun doubleXp_onlyAppliesOnce() {
        vm.engine.mode = com.opoleyes.data.model.GameMode.SURVIVAL
        vm.engine.startAllLawsGame()
        vm.engine.nextQuestion()
        vm.engine.answer(vm.engine.currentQ!!.correct)

        vm.onGameOver()

        val xpOriginal = vm.xpGained.value
        assertTrue("Should have XP gained > 0", xpOriginal > 0)

        vm.doubleXp()
        val xpAfterFirst = vm.xpGained.value
        assertEquals(xpOriginal * 2, xpAfterFirst)

        // Second call should be a no-op
        vm.doubleXp()
        val xpAfterSecond = vm.xpGained.value
        assertEquals("Double XP should only apply once", xpAfterFirst, xpAfterSecond)
    }

    @Test
    fun doubleXp_doesNothingWhenNoXpGained() {
        // No game played, xpGained should be 0
        assertEquals(0, vm.xpGained.value)
        assertFalse("Should not be doubled initially", vm.isXpDoubled())

        vm.doubleXp()

        assertEquals("XP should remain 0", 0, vm.xpGained.value)
        assertFalse("Should not be marked as doubled", vm.isXpDoubled())
    }

    @Test
    fun isXpDoubled_reflectsState() {
        assertFalse("Initially not doubled", vm.isXpDoubled())

        vm.engine.mode = com.opoleyes.data.model.GameMode.SURVIVAL
        vm.engine.startAllLawsGame()
        vm.engine.nextQuestion()
        vm.engine.answer(vm.engine.currentQ!!.correct)
        vm.onGameOver()

        vm.doubleXp()
        assertTrue("Should be doubled after calling doubleXp", vm.isXpDoubled())
    }
}
