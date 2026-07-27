package com.opoleyes.data

import com.opoleyes.data.model.Rank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstantsTest {

    @Test
    fun ranks_has12Entries() {
        assertEquals(12, Constants.RANKS.size)
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
        assertEquals(100000, Constants.RANKS.last().xp)
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
    fun quickModeQuestions_is20() {
        assertEquals(20, Constants.QUICK_MODE_QUESTIONS)
    }

    @Test
    fun rankUnlocks_containsExpectedEntries() {
        assertTrue(Constants.RANK_UNLOCKS.containsKey(1))
        assertTrue(Constants.RANK_UNLOCKS.containsKey(2))
        assertTrue(Constants.RANK_UNLOCKS.containsKey(3))
        assertTrue(Constants.RANK_UNLOCKS.containsKey(4))
        assertTrue(Constants.RANK_UNLOCKS.containsKey(8))
    }

    @Test
    fun rankUnlocks_examAtRank3() {
        assertEquals("📝 Modo Examen", Constants.RANK_UNLOCKS[3])
    }

    @Test
    fun rankUnlocks_noFreezeTimeEntry() {
        Constants.RANK_UNLOCKS.values.forEach { unlock ->
            assertTrue("No entry should mention Freeze Time: $unlock",
                !unlock.contains("Freeze", ignoreCase = true))
        }
    }

    @Test
    fun rankPowerupRewards_hasEntries() {
        assertTrue(Constants.RANK_POWERUP_REWARDS.isNotEmpty())
    }

    @Test
    fun rankPowerupRewards_allContainValidPowerUps() {
        val validPowerUps = setOf("shield", "fiftyFifty", "hint", "doubleScore")
        Constants.RANK_POWERUP_REWARDS.forEach { (rank, rewards) ->
            assertTrue("Rank $rank rewards should not be empty", rewards.isNotEmpty())
            rewards.forEach { pu ->
                assertTrue("Rank $rank has invalid power-up: $pu", validPowerUps.contains(pu))
            }
        }
    }

    @Test
    fun rankPowerupRewards_noFreezeTime() {
        Constants.RANK_POWERUP_REWARDS.values.forEach { rewards ->
            assertTrue("No reward should contain freezeTime",
                !rewards.contains("freezeTime"))
        }
    }

    @Test
    fun rankPowerupRewards_higherRanksGiveMore() {
        val rank1Rewards = Constants.RANK_POWERUP_REWARDS[1]?.size ?: 0
        val rank8Rewards = Constants.RANK_POWERUP_REWARDS[8]?.size ?: 0
        assertTrue("Higher ranks should give more power-ups", rank8Rewards >= rank1Rewards)
    }

    @Test
    fun getRankByIndex_returnsCorrectRank() {
        assertEquals("Novato", Constants.getRankByIndex(0).name)
        assertEquals("Leyenda", Constants.getRankByIndex(11).name)
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
}
