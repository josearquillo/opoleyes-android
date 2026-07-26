package com.opotest.domain

import com.opotest.TestContextProvider
import com.opotest.data.local.PreferencesManager
import com.opotest.data.model.ChestType
import com.opotest.data.repository.ProgressRepository
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
        val chest = chestSystem.generateChest(false, 50, 10)
        assertEquals(ChestType.WOOD, chest.type)
    }

    @Test
    fun fun_generateChest_silverForRecord() {
        val chest = chestSystem.generateChest(true, 50, 10)
        assertEquals(ChestType.SILVER, chest.type)
    }

    @Test
    fun fun_generateChest_goldForRecordWithHighAccuracy() {
        val chest = chestSystem.generateChest(true, 95, 10)
        assertEquals(ChestType.GOLD, chest.type)
    }

    @Test
    fun fun_generateChest_goldRequiresMin5Answered() {
        val chest = chestSystem.generateChest(true, 95, 3)
        assertEquals(ChestType.SILVER, chest.type)
    }

    @Test
    fun fun_generateChest_goldRequires90Accuracy() {
        val chest = chestSystem.generateChest(true, 85, 10)
        assertEquals(ChestType.SILVER, chest.type)
    }

    @Test
    fun fun_generateChest_woodXpInRange() {
        val chest = chestSystem.generateChest(false, 50, 10)
        assertTrue("Wood XP should be >= 50: ${chest.xp}", chest.xp >= 50)
    }

    @Test
    fun fun_generateChest_goldHasMultiplier() {
        val chest = chestSystem.generateChest(true, 95, 10)
        assertTrue(chest.multiplier)
    }

    @Test
    fun fun_generateChest_woodNoMultiplier() {
        val chest = chestSystem.generateChest(false, 50, 10)
        assertFalse(chest.multiplier)
    }

    @Test
    fun fun_generateChest_silverNoMultiplier() {
        val chest = chestSystem.generateChest(true, 50, 10)
        assertFalse(chest.multiplier)
    }

    @Test
    fun fun_openChest_addsXP() {
        val xpBefore = progressRepo.getXP()
        val chest = chestSystem.generateChest(false, 50, 10)
        chestSystem.openChest(chest)
        assertTrue(progressRepo.getXP() > xpBefore)
    }

    @Test
    fun fun_openChest_goldSetsMultiplier() {
        val chest = chestSystem.generateChest(true, 95, 10)
        chestSystem.openChest(chest)
        assertEquals(2, prefs.getMultiplier())
    }

    @Test
    fun fun_openChest_woodDoesNotSetMultiplier() {
        val chest = chestSystem.generateChest(false, 50, 10)
        chestSystem.openChest(chest)
        assertEquals(1, prefs.getMultiplier())
    }

    @Test
    fun fun_generateChest_silverHasPowerUpAtRank1() {
        progressRepo.addXP(500)
        val chest = chestSystem.generateChest(true, 50, 10)
        assertTrue(chest.powerUps.isNotEmpty())
    }

    @Test
    fun fun_generateChest_goldHas2PowerUpsAtRank1() {
        progressRepo.addXP(500)
        val chest = chestSystem.generateChest(true, 95, 10)
        assertEquals(2, chest.powerUps.size)
    }

    @Test
    fun fun_generateChest_woodNoPowerUps() {
        val chest = chestSystem.generateChest(false, 50, 10)
        assertTrue(chest.powerUps.isEmpty())
    }

    @Test
    fun fun_openChest_addsPowerUpsToRepo() {
        progressRepo.addXP(500)
        val chest = chestSystem.generateChest(true, 95, 10)
        chestSystem.openChest(chest)
        val powerUps = com.opotest.data.repository.GameRepository(
            TestContextProvider.getContext()
        ).getFreePowerUps()
        assertTrue(powerUps.isNotEmpty())
    }
}
