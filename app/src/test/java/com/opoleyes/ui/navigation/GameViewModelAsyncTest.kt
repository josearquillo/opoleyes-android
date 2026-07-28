package com.opoleyes.ui.navigation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.PreferencesManager
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
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameViewModelAsyncTest {

    private lateinit var vm: GameViewModel
    private lateinit var prefs: PreferencesManager
    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        prefs = PreferencesManager(app)
        prefs.resetAll()
        vm = GameViewModel(app)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    private fun waitForCallback(latch: CountDownLatch, timeoutMs: Long = 10000) {
        val start = System.currentTimeMillis()
        while (latch.count > 0 && System.currentTimeMillis() - start < timeoutMs) {
            ShadowLooper.idleMainLooper()
            if (latch.await(100, TimeUnit.MILLISECONDS)) break
        }
    }

    @Test
    fun fun_startAllLawsGameAsync_callsOnDoneWithTrue() {
        val latch = CountDownLatch(1)
        var result: Boolean? = null
        vm.startAllLawsGameAsync {
            result = it
            latch.countDown()
        }
        waitForCallback(latch)
        assertTrue("Should call onDone with true", result == true)
        assertFalse("isLoading should be false after completion", vm.isLoading.value)
    }

    @Test
    fun fun_startQuickGameAsync_callsOnDone() {
        val latch = CountDownLatch(1)
        var result: Boolean? = null
        vm.startQuickGameAsync {
            result = it
            latch.countDown()
        }
        waitForCallback(latch)
        assertNotNull("onDone should be called", result)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun fun_startChallengeGameAsync_callsOnDone() {
        val latch = CountDownLatch(1)
        var result: Boolean? = null
        vm.startChallengeGameAsync {
            result = it
            latch.countDown()
        }
        waitForCallback(latch)
        assertNotNull("onDone should be called", result)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun fun_startTemaGameAsync_callsOnDone() {
        val latch = CountDownLatch(1)
        var result: Boolean? = null
        vm.startTemaGameAsync("test1") {
            result = it
            latch.countDown()
        }
        waitForCallback(latch)
        assertNotNull("onDone should be called", result)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun fun_startExamAsync_loadsQuestions_andSetsState() {
        val latch = CountDownLatch(1)
        var result: Boolean? = null
        vm.startExamAsync(10) {
            result = it
            latch.countDown()
        }
        waitForCallback(latch)
        assertTrue("Should call onDone with true", result == true)
        assertEquals(10, vm.examTotalQuestions.value)
        assertNotNull("Current question should be set", vm.examCurrentQuestion.value)
        assertEquals(0, vm.examQuestionNum.value)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun fun_startExamAsync_clearsPreviousResult() {
        // First exam
        val latch1 = CountDownLatch(1)
        vm.startExamAsync(10) { latch1.countDown() }
        waitForCallback(latch1)
        vm.examNavigate(0)
        vm.examAnswer("A")
        vm.finishExam()
        assertNotNull(vm.examResult.value)

        // Start new exam
        val latch2 = CountDownLatch(1)
        vm.startExamAsync(5) { latch2.countDown() }
        waitForCallback(latch2)
        assertEquals(null, vm.examResult.value)
        assertEquals(5, vm.examTotalQuestions.value)
    }

    @Test
    fun fun_startAllLawsGameAsync_setsLoadingDuringExecution() {
        val latch = CountDownLatch(1)
        vm.startAllLawsGameAsync { latch.countDown() }
        assertTrue("isLoading should be true during async operation", vm.isLoading.value)
        waitForCallback(latch)
        assertFalse("isLoading should be false after completion", vm.isLoading.value)
    }

    @Test
    fun fun_startExamAsync_setsLoadingDuringExecution() {
        val latch = CountDownLatch(1)
        vm.startExamAsync(10) { latch.countDown() }
        assertTrue("isLoading should be true during async", vm.isLoading.value)
        waitForCallback(latch)
        assertFalse("isLoading should be false after completion", vm.isLoading.value)
    }
}
