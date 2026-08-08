package com.opoleyes.ui.navigation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.GameMode
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserFlowTemaTest {

    private lateinit var vm: GameViewModel
    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        prefs = PreferencesManager(app)
        prefs.resetAll()
        vm = GameViewModel(app)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun flow_temaGame_poolOnlyFromThatTema() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val temaTests = DataProvider.getTemaTests(app)
        assertTrue("Should have tema tests", temaTests.isNotEmpty())
        val firstTema = temaTests.first()
        val testData = DataProvider.getTestDataMap(app)[firstTema.id]!!
        val sourceTestIds = testData.questions.map { it.test_id }.toSet()

        vm.pendingMode = GameMode.SURVIVAL
        val ok = vm.startTemaGame(firstTema.id)
        assertTrue("Tema game should start", ok)

        for (q in vm.engine.pool) {
            assertTrue("Pool question testId '${q.testId}' should be from tema ${firstTema.id}",
                q.testId in sourceTestIds)
        }

        val currentQ = vm.engine.currentQ
        assertNotNull("Current question should not be null", currentQ)
        assertTrue("Current question should be from tema",
            currentQ!!.testId in sourceTestIds)
    }
}
