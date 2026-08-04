package com.opoleyes.data.local

import androidx.test.core.app.ApplicationProvider
import com.opoleyes.TestContextProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreferencesManagerIntroTest {

    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        val ctx = TestContextProvider.getContext()
        prefs = PreferencesManager(ctx)
        prefs.resetAll()
    }

    @Test
    fun isIntroShown_defaultsToFalse() {
        assertFalse("Intro should not be shown by default", prefs.isIntroShown("intro_survival_rank_0"))
        assertFalse("Timetrial intro should not be shown by default", prefs.isIntroShown("intro_timetrial"))
    }

    @Test
    fun setIntroShown_persistsFlag() {
        prefs.setIntroShown("intro_survival_rank_0")
        assertTrue("Intro flag should persist after set", prefs.isIntroShown("intro_survival_rank_0"))
    }

    @Test
    fun setIntroShown_isIndependentPerKey() {
        prefs.setIntroShown("intro_survival_rank_0")
        assertTrue(prefs.isIntroShown("intro_survival_rank_0"))
        assertFalse("Other keys should remain unseen", prefs.isIntroShown("intro_survival_rank_1"))
        assertFalse(prefs.isIntroShown("intro_timetrial"))
    }

    @Test
    fun setIntroShown_survivalRanksAreIndependent() {
        prefs.setIntroShown("intro_survival_rank_0")
        prefs.setIntroShown("intro_survival_rank_2")
        assertTrue(prefs.isIntroShown("intro_survival_rank_0"))
        assertFalse(prefs.isIntroShown("intro_survival_rank_1"))
        assertTrue(prefs.isIntroShown("intro_survival_rank_2"))
    }

    @Test
    fun resetAll_clearsIntroFlags() {
        prefs.setIntroShown("intro_survival_rank_0")
        prefs.setIntroShown("intro_timetrial")
        prefs.resetAll()
        assertFalse("Flags should clear after resetAll", prefs.isIntroShown("intro_survival_rank_0"))
        assertFalse(prefs.isIntroShown("intro_timetrial"))
    }
}
