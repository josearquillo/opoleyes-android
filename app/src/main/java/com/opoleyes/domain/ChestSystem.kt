package com.opoleyes.domain

import android.content.Context
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.ChestReward
import com.opoleyes.data.model.ChestType
import com.opoleyes.data.repository.ProgressRepository

class ChestSystem(
    private val progressRepo: ProgressRepository,
    private val prefs: com.opoleyes.data.IPreferencesManager
) {

    constructor(context: Context) : this(
        ProgressRepository(context),
        PreferencesManager(context)
    )

    constructor(prefs: com.opoleyes.data.IPreferencesManager) : this(
        ProgressRepository(prefs),
        prefs
    )

    fun generateChest(newRecord: Boolean, accuracy: Int, totalAnswered: Int): ChestReward? {
        if (totalAnswered < 3) return null
        val type = when {
            newRecord && accuracy >= 90 && totalAnswered >= 10 -> ChestType.GOLD
            newRecord && accuracy >= 70 -> ChestType.SILVER
            accuracy >= 80 && totalAnswered >= 5 -> ChestType.SILVER
            accuracy >= 60 -> ChestType.BRONZE
            else -> return null
        }

        val (xpMin, xpMax) = when (type) {
            ChestType.BRONZE -> 50 to 150
            ChestType.SILVER -> 150 to 350
            ChestType.GOLD -> 300 to 600
        }
        val lootXP = (xpMin..xpMax).random()
        val multiplier = type == ChestType.GOLD

        return ChestReward(type, lootXP, emptyList(), multiplier)
    }

    fun openChest(reward: ChestReward) {
        progressRepo.addXP(reward.xp)
        if (reward.multiplier) prefs.setMultiplier(2)
    }
}
