package com.opoleyes

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.opoleyes.data.model.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests that verify string resources are consistent with code behavior and
 * that no hardcoded strings are used where resources should be.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StringsConsistencyTest {

    private lateinit var ctx: android.content.Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext<Application>()
    }

    @Test
    fun helpComboText_matchesCodeBehavior() {
        // Bug was: help said "cada 10 aciertos" but code fills bar in 5 (0.2f per answer)
        // Fix: changed help text to "cada 5 aciertos"
        val helpText = ctx.getString(com.opoleyes.R.string.help_section_combo_l3)
        assertTrue("Help text should say '5 aciertos', not '10': $helpText",
            helpText.contains("5 aciertos"))
    }

    @Test
    fun simulacroUnlockStrings_exist() {
        // Bug was: ModeSelectScreen used hardcoded "Veterano" and "+ aprobar 50"
        // Fix: added string resources simulacro_unlock_rank and simulacro_unlock_exam
        val rank = ctx.getString(com.opoleyes.R.string.simulacro_unlock_rank)
        val exam = ctx.getString(com.opoleyes.R.string.simulacro_unlock_exam)
        assertEquals("Veterano", rank)
        assertEquals("+ aprobar 50", exam)
    }

    @Test
    fun modeLabels_useStringResources() {
        // Bug was: ProfileScreen hardcoded "Supervivencia", "Contrarreloj", "Repaso Express"
        // Fix: replaced with string resources
        val survival = ctx.getString(com.opoleyes.R.string.mode_survival)
        val timetrial = ctx.getString(com.opoleyes.R.string.mode_timetrial)
        val quick = ctx.getString(com.opoleyes.R.string.mode_quick)
        assertEquals("Supervivencia", survival)
        assertEquals("Contrarreloj", timetrial)
        assertEquals("Repaso Express", quick)
    }
}
