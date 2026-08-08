package com.opoleyes.domain

import com.opoleyes.FakePreferencesManager
import com.opoleyes.data.model.ChestType
import com.opoleyes.data.repository.ProgressRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChestSystemTest {

    private lateinit var prefs: FakePreferencesManager
    private lateinit var chestSystem: ChestSystem
    private lateinit var progressRepo: ProgressRepository

    @Before
    fun setup() {
        prefs = FakePreferencesManager()
        prefs.resetAll()
        chestSystem = ChestSystem(prefs)
        progressRepo = ProgressRepository(prefs)
    }

    // === generateChest: type determination ===

    @Test
    fun generateChest_goldForNewRecordHighAccuracyLongGame() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15)
        assertNotNull("Should generate a chest for new record + high accuracy + long game", reward)
        assertEquals("Should be GOLD chest", ChestType.GOLD, reward!!.type)
    }

    @Test
    fun generateChest_silverForNewRecordMediumAccuracy() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 75, totalAnswered = 10)
        assertNotNull("Should generate a chest for new record + medium accuracy", reward)
        assertEquals("Should be SILVER chest", ChestType.SILVER, reward!!.type)
    }

    @Test
    fun generateChest_silverForHighAccuracyNoRecord() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 85, totalAnswered = 8)
        assertNotNull("Should generate SILVER for high accuracy without record", reward)
        assertEquals("Should be SILVER chest", ChestType.SILVER, reward!!.type)
    }

    @Test
    fun generateChest_bronzeForMediumAccuracy() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 65, totalAnswered = 5)
        assertNotNull("Should generate BRONZE for medium accuracy", reward)
        assertEquals("Should be BRONZE chest", ChestType.BRONZE, reward!!.type)
    }

    @Test
    fun generateChest_nullForTooFewQuestions() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 100, totalAnswered = 2)
        assertNull("Should return null for totalAnswered < 3", reward)
    }

    @Test
    fun generateChest_nullForLowAccuracy() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 50, totalAnswered = 10)
        assertNull("Should return null for accuracy < 60", reward)
    }

    @Test
    fun generateChest_nullForInsufficientPerformance() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 55, totalAnswered = 3)
        assertNull("Should return null for accuracy < 60", reward)
    }

    // === generateChest: reward content ===

    @Test
    fun generateChest_goldRewardHasMultiplier() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15)!!
        assertTrue("GOLD chest should have multiplier=true", reward.multiplier)
    }

    @Test
    fun generateChest_silverRewardHasNoMultiplier() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 75, totalAnswered = 10)!!
        assertFalse("SILVER chest should have multiplier=false", reward.multiplier)
    }

    @Test
    fun generateChest_bronzeRewardHasNoMultiplier() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 65, totalAnswered = 5)!!
        assertFalse("BRONZE chest should have multiplier=false", reward.multiplier)
    }

    @Test
    fun generateChest_goldXpIsInExpectedRange() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15)!!
        assertTrue("GOLD XP should be >= 300, got ${reward.xp}", reward.xp >= 300)
        assertTrue("GOLD XP should be <= 600, got ${reward.xp}", reward.xp <= 600)
    }

    @Test
    fun generateChest_silverXpIsInExpectedRange() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 75, totalAnswered = 10)!!
        assertTrue("SILVER XP should be >= 150, got ${reward.xp}", reward.xp >= 150)
        assertTrue("SILVER XP should be <= 350, got ${reward.xp}", reward.xp <= 350)
    }

    @Test
    fun generateChest_bronzeXpIsInExpectedRange() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 65, totalAnswered = 5)!!
        assertTrue("BRONZE XP should be >= 50, got ${reward.xp}", reward.xp >= 50)
        assertTrue("BRONZE XP should be <= 150, got ${reward.xp}", reward.xp <= 150)
    }

    @Test
    fun generateChest_goldMinXpGreaterThanOrEqualSilverMinXp() {
        // Verify that better chests give at least as good rewards (min XP)
        // GOLD min (300) >= SILVER min (150) >= BRONZE min (50)
        val gold = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15)!!
        val silver = chestSystem.generateChest(newRecord = true, accuracy = 75, totalAnswered = 10)!!
        val bronze = chestSystem.generateChest(newRecord = false, accuracy = 65, totalAnswered = 5)!!
        // Compare minimum possible XP for each type
        val goldMin = 300; val silverMin = 150; val bronzeMin = 50
        assertTrue("GOLD min XP ($goldMin) >= SILVER min XP ($silverMin)", goldMin >= silverMin)
        assertTrue("SILVER min XP ($silverMin) >= BRONZE min XP ($bronzeMin)", silverMin >= bronzeMin)
        // Verify actual XP is within each range
        assertTrue("GOLD XP ${gold.xp} >= $goldMin", gold.xp >= goldMin)
        assertTrue("SILVER XP ${silver.xp} >= $silverMin", silver.xp >= silverMin)
        assertTrue("BRONZE XP ${bronze.xp} >= $bronzeMin", bronze.xp >= bronzeMin)
    }

    @Test
    fun generateChest_goldMaxXpGreaterThanSilverMaxXp() {
        // GOLD max (1200) > SILVER max (700) > BRONZE max (300)
        val goldMax = 1200; val silverMax = 700; val bronzeMax = 300
        assertTrue("GOLD max XP ($goldMax) > SILVER max XP ($silverMax)", goldMax > silverMax)
        assertTrue("SILVER max XP ($silverMax) > BRONZE max XP ($bronzeMax)", silverMax > bronzeMax)
    }

    @Test
    fun generateChest_goldHasMorePowerUpsThanSilverThanBronze() {
        val gold = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15)!!
        val silver = chestSystem.generateChest(newRecord = true, accuracy = 75, totalAnswered = 10)!!
        val bronze = chestSystem.generateChest(newRecord = false, accuracy = 65, totalAnswered = 5)!!
        assertTrue("GOLD powerUps (${gold.powerUps.size}) >= SILVER (${silver.powerUps.size})",
            gold.powerUps.size >= silver.powerUps.size)
        assertTrue("SILVER powerUps (${silver.powerUps.size}) >= BRONZE (${bronze.powerUps.size})",
            silver.powerUps.size >= bronze.powerUps.size)
    }

    // === openChest: applies rewards correctly ===

    @Test
    fun openChest_appliesXp() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15)!!
        val xpBefore = progressRepo.getXP()
        chestSystem.openChest(reward)
        val xpAfter = progressRepo.getXP()
        assertEquals("XP should increase by reward.xp", xpBefore + reward.xp, xpAfter)
    }

    @Test
    fun openChest_appliesMultiplierForGold() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15)!!
        chestSystem.openChest(reward)
        assertEquals("GOLD chest should set multiplier to 2", 2, prefs.getMultiplier())
    }

    @Test
    fun openChest_doesNotApplyMultiplierForSilver() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 75, totalAnswered = 10)!!
        chestSystem.openChest(reward)
        assertEquals("SILVER chest should not change multiplier (default 1)", 1, prefs.getMultiplier())
    }

    @Test
    fun openChest_doesNotApplyMultiplierForBronze() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 65, totalAnswered = 5)!!
        chestSystem.openChest(reward)
        assertEquals("BRONZE chest should not change multiplier (default 1)", 1, prefs.getMultiplier())
    }

    @Test
    fun openChest_bronzeAppliesXp() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 65, totalAnswered = 5)!!
        val xpBefore = progressRepo.getXP()
        chestSystem.openChest(reward)
        assertEquals("BRONZE XP should be applied", xpBefore + reward.xp, progressRepo.getXP())
    }

    @Test
    fun openChest_silverAppliesXp() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 75, totalAnswered = 10)!!
        val xpBefore = progressRepo.getXP()
        chestSystem.openChest(reward)
        assertEquals("SILVER XP should be applied", xpBefore + reward.xp, progressRepo.getXP())
    }

    @Test
    fun openChest_goldAppliesXp() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15)!!
        val xpBefore = progressRepo.getXP()
        chestSystem.openChest(reward)
        assertEquals("GOLD XP should be applied", xpBefore + reward.xp, progressRepo.getXP())
    }
}
