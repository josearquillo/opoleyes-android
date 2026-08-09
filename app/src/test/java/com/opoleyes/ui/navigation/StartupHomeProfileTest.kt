package com.opoleyes.ui.navigation

import com.opoleyes.FakeGameRepository
import com.opoleyes.FakePreferencesManager
import com.opoleyes.TestFakes
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.repository.MissionRepository
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StartupHomeProfileTest {

    private lateinit var vm: GameViewModel
    private lateinit var prefs: FakePreferencesManager
    private lateinit var progressRepo: ProgressRepository
    private lateinit var statsRepo: StatsRepository
    private lateinit var missionRepo: MissionRepository

    companion object {
        private val testDispatcher = StandardTestDispatcher()

        @JvmStatic
        @BeforeClass
        fun setUpClass() { Dispatchers.setMain(testDispatcher) }

        @JvmStatic
        @AfterClass
        fun tearDownClass() { Dispatchers.resetMain() }
    }

    @Before
    fun setup() {
        prefs = FakePreferencesManager()
        prefs.resetAll()
        progressRepo = ProgressRepository(prefs)
        statsRepo = StatsRepository(prefs)
        missionRepo = MissionRepository(prefs)
        val engine = GameEngine.createForTest(FakeGameRepository(), statsRepo, progressRepo, prefs)
        val examEngine = ExamEngine.createForTest(statsRepo, TestFakes.makePool(100))
        vm = GameViewModel.createForTest(
            progressRepo, statsRepo, missionRepo,
            AchievementChecker(prefs), ChestSystem(prefs),
            prefs, engine, examEngine
        )
    }

    @After
    fun teardown() { prefs.resetAll() }

    // ========================
    // Startup: preloadHomeData
    // ========================

    @Test
    fun startup_preloadHomeData_isNotNull() {
        vm.preloadHomeData()
        assertNotNull("homePreload should be populated", vm.homePreload)
    }

    @Test
    fun startup_preloadHomeData_hasRank() {
        vm.preloadHomeData()
        assertNotNull(vm.homePreload?.rank)
    }

    @Test
    fun startup_preloadHomeData_hasXpProgress() {
        vm.preloadHomeData()
        assertNotNull(vm.homePreload?.xpProgress)
    }

    @Test
    fun startup_preloadHomeData_hasMissions() {
        vm.preloadHomeData()
        assertNotNull(vm.homePreload?.missions)
    }

    @Test
    fun startup_preloadHomeData_isIdempotent() {
        vm.preloadHomeData()
        val first = vm.homePreload
        vm.preloadHomeData()
        assertEquals("Second preload should return same data", first, vm.homePreload)
    }

    @Test
    fun startup_preloadHomeData_reflectsStatsAfterGame() {
        vm.preloadHomeData()
        val initialCorrect = vm.homePreload!!.totalCorrect
        assertEquals(0, initialCorrect)

        // Play a quick game
        vm.startQuickGame()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.nextQuestion()
        vm.answer(vm.uiState.value.currentQ!!.correct)
        vm.onGameOver()

        // Reset and reload
        vm.resetProgress()
        vm.preloadHomeData()
        // After reset, stats should be 0
        assertEquals(0, vm.homePreload!!.totalCorrect)
    }

    // ========================
    // Profile: preloadProfileData
    // ========================

    @Test
    fun profile_preloadProfileData_isNotNull() {
        vm.preloadProfileData()
        assertNotNull("profileData should be populated", vm.profileData)
    }

    @Test
    fun profile_preloadProfileData_hasRank() {
        vm.preloadProfileData()
        assertNotNull(vm.profileData?.rank)
    }

    @Test
    fun profile_preloadProfileData_hasRecords() {
        vm.preloadProfileData()
        val records = vm.profileData?.records
        assertNotNull(records)
        assertTrue("Records should contain survival", records!!.containsKey("survival"))
        assertTrue("Records should contain timetrial", records.containsKey("timetrial"))
        assertTrue("Records should contain quick", records.containsKey("quick"))
    }

    @Test
    fun profile_preloadProfileData_hasUnlockedModes() {
        vm.preloadProfileData()
        val unlocked = vm.profileData?.unlockedModes
        assertNotNull(unlocked)
        assertTrue("UnlockedModes should contain survival", unlocked!!.containsKey("survival"))
        assertTrue("UnlockedModes should contain timetrial", unlocked.containsKey("timetrial"))
        assertTrue("UnlockedModes should contain quick", unlocked.containsKey("quick"))
    }

    @Test
    fun profile_preloadProfileData_isIdempotent() {
        vm.preloadProfileData()
        val first = vm.profileData
        vm.preloadProfileData()
        assertEquals("Second preload should return same data", first, vm.profileData)
    }

    // ========================
    // Profile: resetProgress
    // ========================

    @Test
    fun profile_resetProgress_clearsHomePreload() {
        vm.preloadHomeData()
        assertNotNull(vm.homePreload)
        vm.resetProgress()
        assertNull("Home preload should be null after reset", vm.homePreload)
    }

    @Test
    fun profile_resetProgress_clearsProfileData() {
        vm.preloadProfileData()
        assertNotNull(vm.profileData)
        vm.resetProgress()
        assertNull("Profile data should be null after reset", vm.profileData)
    }

    @Test
    fun profile_resetProgress_clearsRankUpOverlay() {
        // Play a game to potentially trigger rank up
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        vm.resetProgress()
        assertNull("Rank up overlay should be null after reset", vm.rankUpOverlay.value)
    }

    @Test
    fun profile_resetProgress_clearsChestReward() {
        vm.startQuickGame()
        for (i in 0 until 5) {
            vm.answer(vm.uiState.value.currentQ!!.correct)
            if (i < 4) vm.nextQuestion()
        }
        vm.onGameOver()
        // Chest may or may not be generated, but reset should clear it
        vm.resetProgress()
        assertNull("Chest reward should be null after reset", vm.chestReward.value)
    }

    @Test
    fun profile_resetProgress_xpGoesToZero() {
        progressRepo.addXP(500)
        assertEquals(500, progressRepo.getXP())
        vm.resetProgress()
        assertEquals(0, progressRepo.getXP())
    }

    // ========================
    // Debug mode
    // ========================

    @Test
    fun debugMode_defaultIsFalse() {
        assertFalse(vm.isDebugMode())
    }

    @Test
    fun debugMode_enableSetsFlag() {
        vm.setDebugMode(true)
        assertTrue(vm.isDebugMode())
    }

    @Test
    fun debugMode_enableInvalidatesHomePreload() {
        vm.preloadHomeData()
        assertNotNull(vm.homePreload)
        vm.setDebugMode(true)
        assertNull("Home preload should be invalidated on debug mode change", vm.homePreload)
    }

    @Test
    fun debugMode_enableInvalidatesProfileData() {
        vm.preloadProfileData()
        assertNotNull(vm.profileData)
        vm.setDebugMode(true)
        assertNull("Profile data should be invalidated on debug mode change", vm.profileData)
    }

    // ========================
    // Getters for UI
    // ========================

    @Test
    fun getTemaTests_returnsEmptyByDefault() {
        assertEquals(0, vm.getTemaTests().size)
    }

    @Test
    fun getUnlocks_returnsValidObject() {
        val unlocks = vm.getUnlocks()
        assertNotNull(unlocks)
    }

    @Test
    fun getMaxExamQuestions_defaultIs10() {
        assertEquals(10, vm.getMaxExamQuestions())
    }

    @Test
    fun getExamQuestionsPresets_isNotEmpty() {
        assertTrue("Exam question presets should not be empty", vm.examQuestionPresets.isNotEmpty())
    }

    @Test
    fun getSimulacroHistory_defaultIsEmpty() {
        assertEquals(0, vm.getSimulacroHistory().size)
    }
}
