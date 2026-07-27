package com.opoleyes.domain

import android.content.Context
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.ChestReward
import com.opoleyes.data.model.ChestType
import com.opoleyes.data.repository.GameRepository
import com.opoleyes.data.repository.ProgressRepository

class ChestSystem(private val context: Context) {
    private val progressRepo = ProgressRepository(context)
    private val gameRepo = GameRepository(context)

    fun generateChest(newRecord: Boolean, accuracy: Int, totalAnswered: Int, score: Int): ChestReward? {
        if (totalAnswered < 5 || score < 100) return null
        val type = when {
            newRecord && accuracy >= 90 && totalAnswered >= 10 -> ChestType.GOLD
            newRecord && accuracy >= 70 -> ChestType.SILVER
            accuracy >= 80 && totalAnswered >= 10 -> ChestType.SILVER
            else -> ChestType.WOOD
        }
        val rankIdx = progressRepo.getRankIndex()
        val hasPowerUps = progressRepo.isUnlocked("shield") || progressRepo.isUnlocked("fiftyFifty") ||
                progressRepo.isUnlocked("doubleScore")
        val xpBonus = if (hasPowerUps) 1 else 2

        val (xpMin, xpMax) = when (type) {
            ChestType.WOOD -> 50 to 150
            ChestType.SILVER -> 150 to 350
            ChestType.GOLD -> 300 to 600
        }
        val lootXP = (xpMin..xpMax).random()
        val actualXP = lootXP * xpBonus

        val powerUps = mutableListOf<String>()
        if (hasPowerUps) {
            when (type) {
                ChestType.SILVER -> powerUps.add(pickRandomPowerUp(rankIdx))
                ChestType.GOLD -> {
                    powerUps.add(pickRandomPowerUp(rankIdx))
                    powerUps.add(pickRandomPowerUp(rankIdx))
                }
                else -> {}
            }
        }
        val multiplier = type == ChestType.GOLD

        return ChestReward(type, actualXP, powerUps, multiplier)
    }

    fun openChest(reward: ChestReward) {
        progressRepo.addXP(reward.xp)
        if (reward.powerUps.isNotEmpty()) gameRepo.addFreePowerUps(reward.powerUps)
        if (reward.multiplier) gameRepo.setMultiplier(2)
    }

    private fun pickRandomPowerUp(rankIdx: Int): String {
        val available = mutableListOf("shield", "fiftyFifty", "hint", "doubleScore")
        return available.random()
    }
}
