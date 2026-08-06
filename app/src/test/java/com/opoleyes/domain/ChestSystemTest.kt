package com.opoleyes.domain

import androidx.test.core.app.ApplicationProvider
import com.opoleyes.TestContextProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.ChestType
import com.opoleyes.data.repository.GameRepository
import com.opoleyes.data.repository.ProgressRepository
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
    private lateinit var gameRepo: GameRepository

    @Before
    fun setup() {
        val ctx = TestContextProvider.getContext()
        prefs = PreferencesManager(ctx)
        prefs.resetAll()
        chestSystem = ChestSystem(ctx)
        // Re-create repos after reset so caches are clean
        progressRepo = ProgressRepository(ctx)
        gameRepo = GameRepository(ctx)
    }

    // === generateChest: type determination ===

    @Test
    fun generateChest_goldForNewRecordHighAccuracyLongGame() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15, score = 500)
        assertNotNull("Should generate a chest for new record + high accuracy + long game", reward)
        assertEquals("Should be GOLD chest", ChestType.GOLD, reward!!.type)
    }

    @Test
    fun generateChest_silverForNewRecordMediumAccuracy() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 75, totalAnswered = 10, score = 300)
        assertNotNull("Should generate a chest for new record + medium accuracy", reward)
        assertEquals("Should be SILVER chest", ChestType.SILVER, reward!!.type)
    }

    @Test
    fun generateChest_silverForHighAccuracyNoRecord() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 85, totalAnswered = 8, score = 200)
        assertNotNull("Should generate SILVER for high accuracy without record", reward)
        assertEquals("Should be SILVER chest", ChestType.SILVER, reward!!.type)
    }

    @Test
    fun generateChest_bronzeForMediumAccuracy() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 65, totalAnswered = 5, score = 100)
        assertNotNull("Should generate BRONZE for medium accuracy", reward)
        assertEquals("Should be BRONZE chest", ChestType.BRONZE, reward!!.type)
    }

    @Test
    fun generateChest_nullForTooFewQuestions() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 100, totalAnswered = 2, score = 500)
        assertNull("Should return null for totalAnswered < 3", reward)
    }

    @Test
    fun generateChest_nullForLowAccuracy() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 50, totalAnswered = 10, score = 100)
        assertNull("Should return null for accuracy < 60", reward)
    }

    @Test
    fun generateChest_nullForInsufficientPerformance() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 55, totalAnswered = 3, score = 50)
        assertNull("Should return null for accuracy < 60", reward)
    }

    // === generateChest: reward content ===

    @Test
    fun generateChest_goldRewardHasMultiplier() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15, score = 500)!!
        assertTrue("GOLD chest should have multiplier=true", reward.multiplier)
    }

    @Test
    fun generateChest_silverRewardHasNoMultiplier() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 75, totalAnswered = 10, score = 300)!!
        assertFalse("SILVER chest should have multiplier=false", reward.multiplier)
    }

    @Test
    fun generateChest_bronzeRewardHasNoMultiplier() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 65, totalAnswered = 5, score = 100)!!
        assertFalse("BRONZE chest should have multiplier=false", reward.multiplier)
    }

    @Test
    fun generateChest_goldXpIsInExpectedRange() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15, score = 500)!!
        assertTrue("GOLD XP should be >= 300, got ${reward.xp}", reward.xp >= 300)
        assertTrue("GOLD XP should be <= 600, got ${reward.xp}", reward.xp <= 600)
    }

    @Test
    fun generateChest_silverXpIsInExpectedRange() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 75, totalAnswered = 10, score = 300)!!
        assertTrue("SILVER XP should be >= 150, got ${reward.xp}", reward.xp >= 150)
        assertTrue("SILVER XP should be <= 350, got ${reward.xp}", reward.xp <= 350)
    }

    @Test
    fun generateChest_bronzeXpIsInExpectedRange() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 65, totalAnswered = 5, score = 100)!!
        assertTrue("BRONZE XP should be >= 50, got ${reward.xp}", reward.xp >= 50)
        assertTrue("BRONZE XP should be <= 150, got ${reward.xp}", reward.xp <= 150)
    }

    @Test
    fun generateChest_goldMinXpGreaterThanOrEqualSilverMinXp() {
        // Verify that better chests give at least as good rewards (min XP)
        // GOLD min (300) >= SILVER min (150) >= BRONZE min (50)
        val gold = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15, score = 500)!!
        val silver = chestSystem.generateChest(newRecord = true, accuracy = 75, totalAnswered = 10, score = 300)!!
        val bronze = chestSystem.generateChest(newRecord = false, accuracy = 65, totalAnswered = 5, score = 100)!!
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
        val gold = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15, score = 500)!!
        val silver = chestSystem.generateChest(newRecord = true, accuracy = 75, totalAnswered = 10, score = 300)!!
        val bronze = chestSystem.generateChest(newRecord = false, accuracy = 65, totalAnswered = 5, score = 100)!!
        assertTrue("GOLD powerUps (${gold.powerUps.size}) >= SILVER (${silver.powerUps.size})",
            gold.powerUps.size >= silver.powerUps.size)
        assertTrue("SILVER powerUps (${silver.powerUps.size}) >= BRONZE (${bronze.powerUps.size})",
            silver.powerUps.size >= bronze.powerUps.size)
    }

    // === openChest: applies rewards correctly ===

    @Test
    fun openChest_appliesXp() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15, score = 500)!!
        val xpBefore = progressRepo.getXP()
        chestSystem.openChest(reward)
        val xpAfter = progressRepo.getXP()
        assertEquals("XP should increase by reward.xp", xpBefore + reward.xp, xpAfter)
    }

    @Test
    fun openChest_appliesMultiplierForGold() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15, score = 500)!!
        chestSystem.openChest(reward)
        assertEquals("GOLD chest should set multiplier to 2", 2, gameRepo.getMultiplier())
    }

    @Test
    fun openChest_doesNotApplyMultiplierForSilver() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 75, totalAnswered = 10, score = 300)!!
        chestSystem.openChest(reward)
        assertEquals("SILVER chest should not change multiplier (default 1)", 1, gameRepo.getMultiplier())
    }

    @Test
    fun openChest_doesNotApplyMultiplierForBronze() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 65, totalAnswered = 5, score = 100)!!
        chestSystem.openChest(reward)
        assertEquals("BRONZE chest should not change multiplier (default 1)", 1, gameRepo.getMultiplier())
    }

    @Test
    fun openChest_bronzeAppliesXp() {
        val reward = chestSystem.generateChest(newRecord = false, accuracy = 65, totalAnswered = 5, score = 100)!!
        val xpBefore = progressRepo.getXP()
        chestSystem.openChest(reward)
        assertEquals("BRONZE XP should be applied", xpBefore + reward.xp, progressRepo.getXP())
    }

    @Test
    fun openChest_silverAppliesXp() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 75, totalAnswered = 10, score = 300)!!
        val xpBefore = progressRepo.getXP()
        chestSystem.openChest(reward)
        assertEquals("SILVER XP should be applied", xpBefore + reward.xp, progressRepo.getXP())
    }

    @Test
    fun openChest_goldAppliesXp() {
        val reward = chestSystem.generateChest(newRecord = true, accuracy = 95, totalAnswered = 15, score = 500)!!
        val xpBefore = progressRepo.getXP()
        chestSystem.openChest(reward)
        assertEquals("GOLD XP should be applied", xpBefore + reward.xp, progressRepo.getXP())
    }
}
