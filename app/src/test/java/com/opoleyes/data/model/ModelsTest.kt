package com.opoleyes.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test as JTest

class ModelsTest {

    @JTest
    fun test_defaultTest_hasEmptyDefaults() {
        val t = Test()
        assertEquals("", t.id)
        assertEquals("", t.name)
        assertEquals("", t.category)
        assertNull(t.tema)
    }

    @JTest
    fun test_withTema() {
        val t = Test(id = "t1", name = "Test 1", tema = 5)
        assertEquals("t1", t.id)
        assertEquals(5, t.tema)
    }

    @JTest
    fun fun_question_defaultIdIs0() {
        val q = Question()
        assertEquals(0, q.id)
        assertEquals("", q.enunciado)
        assertTrue(q.opciones.isEmpty())
    }

    @JTest
    fun fun_answer_defaults() {
        val a = Answer()
        assertEquals(0, a.id)
        assertEquals("", a.correct)
    }

    @JTest
    fun fun_testData_defaults() {
        val td = TestData()
        assertEquals(Test(), td.test)
        assertTrue(td.questions.isEmpty())
        assertTrue(td.answers.isEmpty())
    }

    @JTest
    fun fun_questionEntry_creation() {
        val qe = QuestionEntry(
            enunciado = "Pregunta 1",
            opciones = mapOf("A" to "Opción A"),
            correct = "A",
            weight = 50,
            testId = "t1",
            origId = "1"
        )
        assertEquals("Pregunta 1", qe.enunciado)
        assertEquals("A", qe.correct)
        assertEquals(50, qe.weight)
    }

    @JTest
    fun fun_questionStat_defaults() {
        val s = QuestionStat()
        assertEquals(0, s.correct)
        assertEquals(0, s.wrong)
    }

    @JTest
    fun fun_gameMode_has5Modes() {
        assertEquals(5, GameMode.values().size)
    }

    @JTest
    fun fun_gameMode_displayNames() {
        assertEquals("Supervivencia", GameMode.SURVIVAL.displayName)
        assertEquals("Contrarreloj", GameMode.TIMETRIAL.displayName)
        assertEquals("Repaso Express", GameMode.QUICK.displayName)
        assertEquals("Modo Reto", GameMode.CHALLENGE.displayName)
        assertEquals("Modo Examen", GameMode.EXAM.displayName)
    }

    @JTest
    fun fun_gameMode_icons() {
        assertTrue(GameMode.SURVIVAL.icon.isNotEmpty())
        assertTrue(GameMode.TIMETRIAL.icon.isNotEmpty())
        assertTrue(GameMode.QUICK.icon.isNotEmpty())
        assertTrue(GameMode.CHALLENGE.icon.isNotEmpty())
        assertTrue(GameMode.EXAM.icon.isNotEmpty())
    }

    @JTest
    fun fun_chestType_has3Types() {
        assertEquals(3, ChestType.values().size)
    }

    @JTest
    fun fun_chestType_labels() {
        assertEquals("Cofre de Madera", ChestType.WOOD.label)
        assertEquals("Cofre de Plata", ChestType.SILVER.label)
        assertEquals("Cofre de Oro", ChestType.GOLD.label)
    }

    @JTest
    fun fun_chestReward_creation() {
        val cr = ChestReward(ChestType.GOLD, 500, listOf("shield"), true)
        assertEquals(ChestType.GOLD, cr.type)
        assertEquals(500, cr.xp)
        assertTrue(cr.multiplier)
        assertEquals(1, cr.powerUps.size)
    }

    @JTest
    fun fun_rank_creation() {
        val r = Rank("Test", "🏆", 1000, 3)
        assertEquals("Test", r.name)
        assertEquals("🏆", r.icon)
        assertEquals(1000, r.xp)
        assertEquals(3, r.index)
    }

    @JTest
    fun fun_achievement_creation() {
        val a = Achievement("id1", "🎯", "Name", "Desc")
        assertEquals("id1", a.id)
        assertEquals("Name", a.name)
    }

    @JTest
    fun fun_mission_creation() {
        val m = Mission("quality", "🎯", "Text", 10, 0, false, 50, "streak")
        assertEquals("quality", m.type)
        assertEquals(10, m.target)
        assertFalse(m.completed)
        assertEquals(50, m.reward)
        assertNull(m.testId)
    }

    @JTest
    fun fun_mission_withTestId() {
        val m = Mission("progress", "📈", "Text", 10, 0, false, 50, "progress_t1", "t1")
        assertEquals("t1", m.testId)
    }

    @JTest
    fun fun_missionData_creation() {
        val md = MissionData("2026-01-01", emptyList())
        assertEquals("2026-01-01", md.date)
        assertTrue(md.missions.isEmpty())
    }

    @JTest
    fun fun_xpProgress_creation() {
        val xp = XPProgress(50, 250, 500, 1000)
        assertEquals(50, xp.pct)
        assertEquals(250, xp.intoRank)
        assertEquals(500, xp.rankSpan)
        assertEquals(1000, xp.nextXp)
    }

    @JTest
    fun fun_floatingPopup_creation() {
        val fp = FloatingPopup("+10", androidx.compose.ui.graphics.Color.White, 24, 0f)
        assertEquals("+10", fp.text)
        assertEquals(24, fp.size)
    }

    @JTest
    fun fun_powerUpToast_creation() {
        val pt = PowerUpToast("Text", "🧊")
        assertEquals("Text", pt.text)
        assertEquals("🧊", pt.icon)
    }

    @JTest
    fun fun_rankUpOverlay_creation() {
        val old = Rank("Novato", "🌱", 0, 0)
        val new = Rank("Principiante", "🌿", 500, 1)
        val ruo = RankUpOverlay(old, new)
        assertEquals(old, ruo.oldRank)
        assertEquals(new, ruo.newRank)
    }

    @JTest
    fun fun_questionStat_copy() {
        val s = QuestionStat(correct = 3, wrong = 2)
        val s2 = s.copy(correct = 4)
        assertEquals(4, s2.correct)
        assertEquals(2, s2.wrong)
        assertEquals(3, s.correct)
    }
}
