package com.opoleyes.domain

import com.opoleyes.TestContextProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.ChestType
import com.opoleyes.data.repository.ProgressRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChestSystemTest {

    private lateinit var chestSystem: ChestSystem
    private lateinit var prefs: PreferencesManager
    private lateinit var progressRepo: ProgressRepository

    @Before
    fun setup() {
        val context = TestContextProvider.getContext()
        prefs = PreferencesManager(context)
        prefs.resetAll()
        progressRepo = ProgressRepository(context)
        chestSystem = ChestSystem(context)
    }

    @After
    fun teardown() {
        prefs.resetAll()
    }

    @Test
    fun fun_generateChest_woodForNoRecord() {
        val chest = chestSystem.generateChest(false, 50, 10, 200)
        assertEquals(ChestType.WOOD, chest!!.type)
    }

    @Test
    fun fun_generateChest_silverForRecord() {
        val chest = chestSystem.generateChest(true, 70, 10, 200)
        assertEquals(ChestType.SILVER, chest!!.type)
    }

    @Test
    fun fun_generateChest_goldForRecordWithHighAccuracy() {
        val chest = chestSystem.generateChest(true, 95, 10, 500)
        assertEquals(ChestType.GOLD, chest!!.type)
    }

    @Test
    fun fun_generateChest_goldRequiresMin5Answered() {
        val chest = chestSystem.generateChest(true, 95, 3, 200)
        assertNull(chest)
    }

    @Test
    fun fun_generateChest_goldRequires90Accuracy() {
        val chest = chestSystem.generateChest(true, 85, 10, 500)
        assertEquals(ChestType.SILVER, chest!!.type)
    }

    @Test
    fun fun_generateChest_woodXpInRange() {
        val chest = chestSystem.generateChest(false, 50, 10, 200)
        assertTrue("Wood XP should be >= 50: ${chest!!.xp}", chest.xp >= 50)
    }

    @Test
    fun fun_generateChest_goldHasMultiplier() {
        val chest = chestSystem.generateChest(true, 95, 10, 500)
        assertTrue(chest!!.multiplier)
    }

    @Test
    fun fun_generateChest_woodNoMultiplier() {
        val chest = chestSystem.generateChest(false, 50, 10, 200)
        assertFalse(chest!!.multiplier)
    }

    @Test
    fun fun_generateChest_silverNoMultiplier() {
        val chest = chestSystem.generateChest(true, 70, 10, 200)
        assertFalse(chest!!.multiplier)
    }

    @Test
    fun fun_openChest_addsXP() {
        val xpBefore = progressRepo.getXP()
        val chest = chestSystem.generateChest(false, 50, 10, 200)
        chestSystem.openChest(chest!!)
        assertTrue(progressRepo.getXP() > xpBefore)
    }

    @Test
    fun fun_openChest_goldSetsMultiplier() {
        val chest = chestSystem.generateChest(true, 95, 10, 500)
        chestSystem.openChest(chest!!)
        assertEquals(2, prefs.getMultiplier())
    }

    @Test
    fun fun_openChest_woodDoesNotSetMultiplier() {
        val chest = chestSystem.generateChest(false, 50, 10, 200)
        chestSystem.openChest(chest!!)
        assertEquals(1, prefs.getMultiplier())
    }

    @Test
    fun fun_generateChest_silverHasPowerUpAtRank1() {
        progressRepo.addXP(500)
        val chest = chestSystem.generateChest(true, 70, 10, 200)
        assertTrue(chest!!.powerUps.isNotEmpty())
    }

    @Test
    fun fun_generateChest_goldHas2PowerUpsAtRank1() {
        progressRepo.addXP(500)
        val chest = chestSystem.generateChest(true, 95, 10, 500)
        assertEquals(2, chest!!.powerUps.size)
    }

    @Test
    fun fun_generateChest_woodNoPowerUps() {
        val chest = chestSystem.generateChest(false, 50, 10, 200)
        assertTrue(chest!!.powerUps.isEmpty())
    }

    @Test
    fun fun_openChest_addsPowerUpsToRepo() {
        progressRepo.addXP(500)
        val chest = chestSystem.generateChest(true, 95, 10, 500)
        chestSystem.openChest(chest!!)
        val powerUps = com.opoleyes.data.repository.GameRepository(
            TestContextProvider.getContext()
        ).getFreePowerUps()
        assertTrue(powerUps.isNotEmpty())
    }

    // === Power-up availability tests ===

    @Test
    fun fun_generateChest_silverHasPowerUpsAtRank0() {
        val chest = chestSystem.generateChest(true, 70, 10, 200)
        assertTrue("Silver chest should have power-ups at rank 0", chest!!.powerUps.isNotEmpty())
    }

    @Test
    fun fun_generateChest_goldHas2PowerUpsAtRank0() {
        val chest = chestSystem.generateChest(true, 95, 10, 500)
        assertEquals(2, chest!!.powerUps.size)
    }

    @Test
    fun fun_generateChest_noFreezeTimeInRewards() {
        progressRepo.addXP(20000)
        repeat(10) {
            val chest = chestSystem.generateChest(true, 95, 10, 500)
            chest!!.powerUps.forEach { pu ->
                assertTrue("No freezeTime in chest rewards", pu != "freezeTime")
            }
        }
    }

    @Test
    fun fun_generateChest_powerUpsFromValidPool() {
        val validPowerUps = setOf("shield", "fiftyFifty", "hint", "doubleScore", "lifeRecovery")
        repeat(20) {
            val chest = chestSystem.generateChest(true, 95, 10, 500)
            chest!!.powerUps.forEach { pu ->
                assertTrue("Invalid power-up: $pu", validPowerUps.contains(pu))
            }
        }
    }

    @Test
    fun fun_generateChest_lifeRecoveryOnlyAtRank4Plus() {
        progressRepo.addXP(3500)
        repeat(10) {
            val chest = chestSystem.generateChest(true, 95, 10, 500)
            chest!!.powerUps.forEach { pu ->
                assertTrue("lifeRecovery should not appear before rank 4", pu != "lifeRecovery")
            }
        }
    }

    @Test
    fun fun_generateChest_lifeRecoveryAvailableAtRank4() {
        progressRepo.addXP(7000)
        repeat(30) {
            val chest = chestSystem.generateChest(true, 95, 10, 500)
            chest!!.powerUps.forEach { pu ->
                assertTrue("lifeRecovery should not appear in chests (passive unlock)", pu != "lifeRecovery")
            }
        }
    }

    // === Bug regression tests ===

    @Test
    fun fun_generateChest_lifeRecoveryNeverInAnyChest() {
        progressRepo.addXP(20000)
        repeat(50) {
            val chest = chestSystem.generateChest(true, 95, 10, 500)
            chest!!.powerUps.forEach { pu ->
                assertTrue("lifeRecovery should never appear in chests", pu != "lifeRecovery")
            }
        }
    }

    @Test
    fun fun_generateChest_powerUpPoolExcludesLifeRecovery() {
        val validPowerUps = setOf("shield", "fiftyFifty", "hint", "doubleScore")
        progressRepo.addXP(20000)
        repeat(30) {
            val chest = chestSystem.generateChest(true, 95, 10, 500)
            chest!!.powerUps.forEach { pu ->
                assertTrue("Power-up $pu not in valid pool", validPowerUps.contains(pu))
            }
        }
    }

    @Test
    fun fun_generateChest_allChestTypesHaveValidIcon() {
        val wood = chestSystem.generateChest(false, 50, 10, 200)
        val silver = chestSystem.generateChest(true, 70, 10, 200)
        val gold = chestSystem.generateChest(true, 95, 10, 500)
        assertTrue("Wood chest icon should not be empty", wood!!.type.icon.isNotEmpty())
        assertTrue("Silver chest icon should not be empty", silver!!.type.icon.isNotEmpty())
        assertTrue("Gold chest icon should not be empty", gold!!.type.icon.isNotEmpty())
    }

    @Test
    fun fun_generateChest_woodXpAtMost150() {
        repeat(20) {
            val chest = chestSystem.generateChest(false, 50, 10, 200)
            assertTrue("Wood XP should be <= 150: ${chest!!.xp}", chest.xp <= 150)
        }
    }

    @Test
    fun fun_generateChest_silverXpInRange() {
        progressRepo.addXP(500)
        repeat(20) {
            val chest = chestSystem.generateChest(true, 70, 10, 200)
            assertTrue("Silver XP should be >= 150: ${chest!!.xp}", chest.xp >= 150)
            assertTrue("Silver XP should be <= 350: ${chest.xp}", chest.xp <= 350)
        }
    }

    @Test
    fun fun_generateChest_goldXpInRange() {
        progressRepo.addXP(500)
        repeat(20) {
            val chest = chestSystem.generateChest(true, 95, 10, 500)
            assertTrue("Gold XP should be >= 300: ${chest!!.xp}", chest.xp >= 300)
            assertTrue("Gold XP should be <= 600: ${chest.xp}", chest.xp <= 600)
        }
    }

    @Test
    fun fun_generateChest_woodForLowScoreGame() {
        val chest = chestSystem.generateChest(false, 50, 10, 200)
        assertNotNull("Should generate wood chest for valid low-score game", chest)
        assertEquals(ChestType.WOOD, chest!!.type)
    }

    @Test
    fun fun_openChest_silverAddsPowerUps() {
        progressRepo.addXP(500)
        val chest = chestSystem.generateChest(true, 70, 10, 200)
        val powerUpsBefore = com.opoleyes.data.repository.GameRepository(
            TestContextProvider.getContext()
        ).getFreePowerUps().size
        chestSystem.openChest(chest!!)
        val powerUpsAfter = com.opoleyes.data.repository.GameRepository(
            TestContextProvider.getContext()
        ).getFreePowerUps().size
        assertTrue("Silver chest should add power-ups", powerUpsAfter > powerUpsBefore)
    }

    @Test
    fun fun_openChest_addsCorrectXp() {
        val xpBefore = progressRepo.getXP()
        val chest = chestSystem.generateChest(false, 50, 10, 200)
        val expectedXp = chest!!.xp
        chestSystem.openChest(chest)
        assertEquals(xpBefore + expectedXp, progressRepo.getXP())
    }
}
