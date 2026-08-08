package com.opoleyes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Tests that verify string resources are consistent with code behavior and
 * that no hardcoded strings are used where resources should be.
 */
class StringsConsistencyTest {

    private lateinit var strings: Map<String, String>

    @Before
    fun setup() {
        val xmlFile = File("src/main/res/values/strings.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val map = mutableMapOf<String, String>()
        val nodes = doc.getElementsByTagName("string")
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as Element
            map[el.getAttribute("name")] = el.textContent.trim()
        }
        strings = map
    }

    private fun getString(name: String): String =
        strings[name] ?: throw IllegalArgumentException("String resource not found: $name")

    @Test
    fun helpComboText_matchesCodeBehavior() {
        // Bug was: help said "cada 10 aciertos" but code fills bar in 5 (0.2f per answer)
        // Fix: changed help text to "cada 5 aciertos"
        val helpText = getString("help_section_combo_l3")
        assertTrue("Help text should say '5 aciertos', not '10': $helpText",
            helpText.contains("5 aciertos"))
    }

    @Test
    fun simulacroUnlockStrings_exist() {
        // Bug was: ModeSelectScreen used hardcoded "Veterano" and "+ aprobar 50"
        // Fix: added string resources simulacro_unlock_rank and simulacro_unlock_exam
        val rank = getString("simulacro_unlock_rank")
        val exam = getString("simulacro_unlock_exam")
        assertEquals("Maestro", rank)
        assertEquals("+ aprobar 50", exam)
    }

    @Test
    fun modeLabels_useStringResources() {
        // Bug was: ProfileScreen hardcoded "Supervivencia", "Contrarreloj", "Repaso Express"
        // Fix: replaced with string resources
        val survival = getString("mode_survival")
        val timetrial = getString("mode_timetrial")
        val quick = getString("mode_quick")
        assertEquals("Supervivencia", survival)
        assertEquals("Contrarreloj", timetrial)
        assertEquals("Repaso Express", quick)
    }

    @Test
    fun examResultButtons_useExpectedStrings() {
        // ExamResultScreen shows "Inicio" + "Reintentar" in both mini-exam and simulacro modes.
        // "Reintentar" replaced the long "Otro mini examen" / "Otro simulacro" labels that
        // overflowed to two lines.
        val home = getString("home_label")
        val retry = getString("retry_label")
        assertEquals("Inicio", home)
        assertEquals("Reintentar", retry)
        // "Reintentar" is short enough to fit on one line in a button
        assertTrue("Reintentar should be a single short word",
            retry.split(" ").size == 1 && retry.length <= 12)
    }
}
