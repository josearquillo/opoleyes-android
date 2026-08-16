package com.opoleyes.data

import com.opoleyes.data.model.Rank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstantsTest {

    @Test
    fun ranks_has9Entries() {
        assertEquals(9, Constants.RANKS.size)
    }

    @Test
    fun ranks_areInAscendingXpOrder() {
        for (i in 1 until Constants.RANKS.size) {
            assertTrue("Rank ${i-1} xp should be < rank ${i} xp",
                Constants.RANKS[i - 1].xp < Constants.RANKS[i].xp)
        }
    }

    @Test
    fun ranks_firstIsNovato() {
        assertEquals("Novato", Constants.RANKS[0].name)
        assertEquals(0, Constants.RANKS[0].xp)
    }

    @Test
    fun ranks_lastIsLeyenda() {
        assertEquals("Leyenda", Constants.RANKS.last().name)
        assertEquals(50000, Constants.RANKS.last().xp)
    }

    @Test
    fun ranks_allHaveUniqueNames() {
        val names = Constants.RANKS.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun ranks_allHaveIcons() {
        Constants.RANKS.forEach { rank ->
            assertTrue("Rank ${rank.name} should have icon", rank.icon.isNotEmpty())
        }
    }

    @Test
    fun achievements_hasEntries() {
        assertTrue(Constants.ACHIEVEMENTS.isNotEmpty())
    }

    @Test
    fun achievements_allHaveUniqueIds() {
        val ids = Constants.ACHIEVEMENTS.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun achievements_allHaveIconsAndNames() {
        Constants.ACHIEVEMENTS.forEach { ach ->
            assertTrue("Achievement ${ach.id} should have icon", ach.icon.isNotEmpty())
            assertTrue("Achievement ${ach.id} should have name", ach.name.isNotEmpty())
            assertTrue("Achievement ${ach.id} should have desc", ach.desc.isNotEmpty())
        }
    }

    @Test
    fun quickModeQuestions_is5() {
        assertEquals(5, Constants.QUICK_MODE_QUESTIONS)
    }

    @Test
    fun rankUnlocks_containsExpectedEntries() {
        assertTrue(Constants.RANK_UNLOCKS.containsKey(1))
        assertTrue(Constants.RANK_UNLOCKS.containsKey(2))
        assertTrue(Constants.RANK_UNLOCKS.containsKey(3))
        assertTrue(Constants.RANK_UNLOCKS.containsKey(5))
        assertTrue(Constants.RANK_UNLOCKS.containsKey(7))
        assertTrue(Constants.RANK_UNLOCKS.containsKey(8))
    }

    @Test
    fun rankUnlocks_examAtRank7() {
        assertEquals("📝 Mini Examen", Constants.RANK_UNLOCKS[7])
    }

    @Test
    fun rankUnlocks_noFreezeTimeEntry() {
        Constants.RANK_UNLOCKS.values.forEach { unlock ->
            assertTrue("No entry should mention Freeze Time: $unlock",
                !unlock.contains("Freeze", ignoreCase = true))
        }
    }

    @Test
    fun getRankByIndex_returnsCorrectRank() {
        assertEquals("Novato", Constants.getRankByIndex(0).name)
        assertEquals("Veterano", Constants.getRankByIndex(6).name)
        assertEquals("Maestro", Constants.getRankByIndex(7).name)
    }

    @Test
    fun getRankByIndex_outOfBoundsReturnsLast() {
        val rank = Constants.getRankByIndex(999)
        assertEquals("Leyenda", rank.name)
    }

    @Test
    fun getRankByIndex_negativeReturnsLast() {
        val rank = Constants.getRankByIndex(-1)
        assertEquals("Leyenda", rank.name)
    }

    @Test
    fun ranks_allHaveIndices() {
        Constants.RANKS.forEachIndexed { index, rank ->
            assertEquals(index, rank.index)
        }
    }

    @Test
    fun ranks_brechasAreMonotonicallyIncreasing() {
        // The gap between consecutive ranks must grow (or stay equal) as rank increases.
        // This ensures the late-game feels progressively harder.
        var prevGap = 0
        for (i in 1 until Constants.RANKS.size) {
            val gap = Constants.RANKS[i].xp - Constants.RANKS[i - 1].xp
            assertTrue("Gap at rank $i ($gap) should be >= previous gap ($prevGap)",
                gap >= prevGap)
            prevGap = gap
        }
    }
}
