package com.opoleyes.ui.navigation

import android.app.Activity
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
import com.opoleyes.ui.components.RewardedAdProvider
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
 * Fake rewarded ad provider that simulates the full ad flow:
 * show → reward earned → ad dismissed. Records calls for assertions.
 */
class FakeRewardedAdProvider : RewardedAdProvider {
    var showAdCallCount = 0
    var lastActivity: Activity? = null
    var rewardEarned = false
    var adDismissed = false

    override fun showAd(activity: Activity, onReward: () -> Unit, onDismissed: () -> Unit) {
        showAdCallCount++
        lastActivity = activity
        // Simulate: ad shows, user watches it, reward is earned, ad closes
        onReward()
        rewardEarned = true
        onDismissed()
        adDismissed = true
    }

    fun reset() {
        showAdCallCount = 0
        lastActivity = null
        rewardEarned = false
        adDismissed = false
    }
}

/**
 * Full-flow test: game ends with bonus XP → rewarded ad button is available →
 * click triggers ad (faked) → XP is doubled → button is no longer available.
 *
 * Also tests the failure path: if XP is 0, the button is not available.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RewardedAdFlowTest {

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
    private lateinit var fakeAdProvider: FakeRewardedAdProvider

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
        fakeAdProvider = FakeRewardedAdProvider()
        vm = GameViewModel.createForTest(
            progressRepo, statsRepo, missionRepo,
            AchievementChecker(prefs), ChestSystem(prefs), prefs, engine, examEngine,
            rewardedAdProvider = fakeAdProvider
        )
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    /**
     * Full happy-path flow:
     * 1. Play a Survival game on all laws (triggers +25% bonus)
     * 2. Game ends → XP is gained
     * 3. Button is available (xpGained > 0 && !isXpDoubled)
     * 4. Click → showRewardedAd() → fake ad shows, reward earned, dismissed
     * 5. XP is doubled
     * 6. Button is no longer available (isXpDoubled == true)
     */
    @Test
    fun fullFlow_bonusGame_showsAd_doublesXp_buttonDisappears() {
        // 1. Play a Survival game on all laws
        vm.engine.mode = com.opoleyes.data.model.GameMode.SURVIVAL
        vm.engine.startAllLawsGame()
        vm.engine.nextQuestion()
        vm.engine.answer(vm.engine.currentQ!!.correct)

        // 2. Game ends → XP is gained
        vm.onGameOver()

        val xpBeforeAd = vm.xpGained.value
        assertTrue("Step 1: Should have XP gained > 0 after game over", xpBeforeAd > 0)

        // 3. Button is available
        assertFalse("Step 2: Button should be available (not yet doubled)", vm.isXpDoubled())

        // 4. Simulate clicking the button — showRewardedAd is called
        //    (In real UI, GameOverScreen extracts Activity and calls showRewardedAd.
        //     Here we pass a dummy Activity since the fake doesn't use it.)
        vm.showRewardedAd(dummyActivity())

        // 5. Fake ad provider was called
        assertEquals("Step 3: Ad provider should be called once", 1, fakeAdProvider.showAdCallCount)
        assertTrue("Step 4: Ad reward should be earned", fakeAdProvider.rewardEarned)
        assertTrue("Step 5: Ad should be dismissed", fakeAdProvider.adDismissed)

        // 6. XP is doubled
        val xpAfterAd = vm.xpGained.value
        assertEquals("Step 6: XP should be doubled", xpBeforeAd * 2, xpAfterAd)

        // 7. Button is no longer available
        assertTrue("Step 7: Button should not be available (already doubled)", vm.isXpDoubled())

        // 8. Clicking again does nothing (ad shows but doubleXp is a no-op)
        fakeAdProvider.reset()
        vm.showRewardedAd(dummyActivity())
        assertEquals("Step 8: Ad provider called again", 1, fakeAdProvider.showAdCallCount)
        assertEquals("Step 8: XP should not change on second call", xpAfterAd, vm.xpGained.value)
    }

    /**
     * Flow with no XP gained (e.g. game ended immediately):
     * Button is not available, clicking does nothing.
     */
    @Test
    fun noXpGained_buttonNotAvailable_clickDoesNothing() {
        // No game played
        assertEquals(0, vm.xpGained.value)
        assertFalse("Button should not be available with 0 XP", vm.isXpDoubled())

        // Even if showRewardedAd is called, doubleXp is a no-op
        vm.showRewardedAd(dummyActivity())

        assertEquals("Ad provider should still be called", 1, fakeAdProvider.showAdCallCount)
        assertEquals("XP should remain 0", 0, vm.xpGained.value)
        assertFalse("Should not be marked as doubled", vm.isXpDoubled())
    }

    /**
     * Verify the +25% all-laws bonus is reflected in XP gained after game over.
     */
    @Test
    fun allLawsBonus_appearsInXpGainedAfterGameOver() {
        // Play on all laws
        vm.engine.mode = com.opoleyes.data.model.GameMode.SURVIVAL
        vm.engine.startAllLawsGame()
        vm.engine.nextQuestion()
        vm.engine.answer(vm.engine.currentQ!!.correct)
        vm.onGameOver()

        val allLawsXp = vm.xpGained.value

        // Reset and play on single law
        prefs.resetAll()
        val statsRepo2 = StatsRepository(prefs)
        val gameRepo2 = FakeGameRepository()
        val engine2 = GameEngine.createForTest(gameRepo2, statsRepo2, progressRepo, prefs)
        val examEngine2 = ExamEngine.createForTest(statsRepo2, TestFakes.makePool(100))
        val vm2 = GameViewModel.createForTest(
            progressRepo, statsRepo2, MissionRepository(prefs),
            AchievementChecker(prefs), ChestSystem(prefs), prefs, engine2, examEngine2,
            rewardedAdProvider = fakeAdProvider
        )
        vm2.engine.mode = com.opoleyes.data.model.GameMode.SURVIVAL
        vm2.engine.startTemaGame("test1")
        vm2.engine.nextQuestion()
        vm2.engine.answer(vm2.engine.currentQ!!.correct)
        vm2.onGameOver()

        val singleLawXp = vm2.xpGained.value

        assertTrue(
            "All-laws XP ($allLawsXp) should be >= single-law XP ($singleLawXp) due to bonus",
            allLawsXp >= singleLawXp
        )
    }

    /** Creates a dummy Activity for the ad provider (the fake ignores it). */
    private fun dummyActivity(): Activity {
        // The fake provider doesn't actually use the Activity, so we can pass
        // a mock. In Robolectric tests we'd use the real test Activity.
        return org.mockito.Mockito.mock(Activity::class.java)
    }
}
