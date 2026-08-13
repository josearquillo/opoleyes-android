package com.opoleyes.data

import com.opoleyes.data.model.MissionDifficulty
import com.opoleyes.data.model.SimulacroHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstantsCoverageTest {

    @Test
    fun leyGroups_isNotEmpty() {
        assertTrue(Constants.LEY_GROUPS.isNotEmpty())
        assertEquals("Constitución Española", Constants.LEY_GROUPS[0].first)
    }

    @Test
    fun getLeyForTest_validNumber_returnsGroupName() {
        val result = Constants.getLeyForTest("Tema N1")
        assertEquals("Constitución Española", result)
    }

    @Test
    fun getLeyForTest_numberInRange_returnsCorrectGroup() {
        val result = Constants.getLeyForTest("Tema N5")
        assertEquals("Tribunal Constitucional", result)
    }

    @Test
    fun getLeyForTest_numberOutOfRange_returnsOriginalName() {
        val result = Constants.getLeyForTest("Tema N999")
        assertEquals("Tema N999", result)
    }

    @Test
    fun getLeyForTest_nonNumericName_returnsOriginalName() {
        val result = Constants.getLeyForTest("Some Random Name")
        assertEquals("Some Random Name", result)
    }

    @Test
    fun getLeyForTest_allValidTemas() {
        for (i in 1..101) {
            val result = Constants.getLeyForTest("Tema N$i")
            assertNotNull(result)
            assertTrue(result.isNotEmpty())
        }
    }

    @Test
    fun missionDifficulty_label_returnsNonEmpty() {
        for (diff in MissionDifficulty.entries) {
            assertTrue(diff.label.isNotEmpty())
        }
    }

    @Test
    fun missionDifficulty_icon_returnsNonEmpty() {
        for (diff in MissionDifficulty.entries) {
            assertTrue(diff.icon.isNotEmpty())
        }
    }

    @Test
    fun simulacroHistoryEntry_wrongAndUnanswered() {
        val entry = SimulacroHistoryEntry(
            date = "2024-01-01",
            points = 50f,
            correct = 10,
            wrong = 5,
            unanswered = 3,
            passed = true
        )
        assertEquals(5, entry.wrong)
        assertEquals(3, entry.unanswered)
    }
}
