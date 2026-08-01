package com.opoleyes.domain

import android.content.Context
import com.opoleyes.data.model.Achievement
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository

data class AchievementContext(
    val firstCorrect: Boolean = false,
    val maxCombo: Int = 0,
    val gameOver: Boolean = false,
    val score: Int = 0,
    val gameMode: String = "",
    val newRecord: Boolean = false,
    val perfectGame: Boolean = false,
    val sharpshooter: Boolean = false,
    val fiftyFiftyUsed: Boolean = false,
    val lifeRecovered: Boolean = false
)

class AchievementChecker(private val context: Context) {
    private val progressRepo = ProgressRepository(context)
    private val statsRepo = StatsRepository(context)

    fun checkPerQuestion(ctx: AchievementContext): List<Achievement> {
        val unlocked = mutableListOf<Achievement>()
        if (ctx.firstCorrect) unlock("first_correct", unlocked)
        if (ctx.maxCombo >= 5) unlock("combo5", unlocked)
        if (ctx.maxCombo >= 10) unlock("combo10", unlocked)
        if (ctx.maxCombo >= 15) unlock("combo15", unlocked)
        if (ctx.maxCombo >= 20) unlock("combo20", unlocked)
        if (ctx.maxCombo >= 25) unlock("combo25", unlocked)
        if (ctx.fiftyFiftyUsed) unlock("strategist", unlocked)
        if (ctx.lifeRecovered) unlock("resurrection", unlocked)
        return unlocked
    }

    fun checkGameOver(ctx: AchievementContext): List<Achievement> {
        val unlocked = mutableListOf<Achievement>()
        val totalCorrect = statsRepo.getTotalCorrect()
        val gamesPlayed = progressRepo.getGamesPlayed()
        val rankIdx = progressRepo.getRankIndex()

        if (ctx.gameOver) unlock("first_record", unlocked)
        if (totalCorrect >= 100) unlock("100correct", unlocked)
        if (totalCorrect >= 500) unlock("500correct", unlocked)
        if (totalCorrect >= 1000) unlock("1000correct", unlocked)
        if (ctx.score >= 300) unlock("medal_bronze", unlocked)
        if (ctx.score >= 600) unlock("medal_silver", unlocked)
        if (ctx.score >= 1000) unlock("medal_gold", unlocked)
        if (ctx.newRecord && ctx.gameMode == "survival") unlock("record_survival", unlocked)
        if (ctx.newRecord && ctx.gameMode == "timetrial") unlock("record_timetrial", unlocked)
        if (ctx.newRecord && ctx.gameMode == "quick") unlock("record_quick", unlocked)
        if (gamesPlayed >= 10) unlock("dedicated", unlocked)
        if (gamesPlayed >= 25) unlock("habitual", unlocked)
        if (gamesPlayed >= 50) unlock("addicted", unlocked)
        if (rankIdx >= 4) unlock("expert", unlocked)
        if (rankIdx >= 6) unlock("master", unlocked)
        if (ctx.perfectGame) unlock("perfect_game", unlocked)
        if (ctx.sharpshooter) unlock("sharpshooter", unlocked)

        val temaTests = com.opoleyes.data.local.DataProvider.getTemaTests(context)
        var dominatedLaws = 0
        for (t in temaTests) { if (statsRepo.getLeyProgress(t.id) >= 100) dominatedLaws++ }
        if (dominatedLaws >= 1) unlock("first_law", unlocked)
        if (dominatedLaws >= 5) unlock("five_laws", unlocked)
        if (dominatedLaws >= 10) unlock("ten_laws", unlocked)
        if (dominatedLaws >= temaTests.size) unlock("all_laws", unlocked)

        return unlocked
    }

    private fun unlock(id: String, unlocked: MutableList<Achievement>) {
        progressRepo.unlockAchievement(id)?.let { unlocked.add(it) }
    }
}
