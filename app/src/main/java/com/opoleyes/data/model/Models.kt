package com.opoleyes.data.model

import androidx.compose.ui.graphics.Color

data class Test(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val questions_file: String = "",
    val title: String = "",
    val tema: Int? = null
)

data class Question(
    val id: Int = 0,
    val test_id: String = "",
    val orig_id: Int = 0,
    val enunciado: String = "",
    val opciones: Map<String, String> = emptyMap()
)

data class Answer(
    val id: Int = 0,
    val correct: String = ""
)

data class TestData(
    val test: Test = Test(),
    val questions: List<Question> = emptyList(),
    val answers: List<Answer> = emptyList()
)

data class QuestionEntry(
    val enunciado: String,
    val opciones: Map<String, String>,
    val correct: String,
    val weight: Int,
    val testId: String,
    val origId: String
)

data class QuestionStat(
    val correct: Int = 0,
    val wrong: Int = 0
)

enum class GameMode(val displayName: String, val icon: String) {
    SURVIVAL("Supervivencia", "❤️"),
    TIMETRIAL("Contrarreloj", "⏱️"),
    QUICK("Repaso Express", "⚡"),
    CHALLENGE("Modo Reto", "🏆"),
    EXAM("Modo Examen", "📝")
}

enum class ChestType(val icon: String, val label: String) {
    BRONZE("🥉", "Bonus Bronce"),
    SILVER("🥈", "Bonus Plata"),
    GOLD("🥇", "Bonus Oro")
}

data class ChestReward(
    val type: ChestType,
    val xp: Int,
    val powerUps: List<String>,
    val multiplier: Boolean
)

data class Rank(
    val name: String,
    val icon: String,
    val xp: Int,
    val index: Int
)

data class Achievement(
    val id: String,
    val icon: String,
    val name: String,
    val desc: String
)

enum class MissionDifficulty(val label: String, val icon: String) {
    EASY("Fácil", "🟢"),
    MEDIUM("Media", "🟡"),
    HARD("Difícil", "🔴")
}

data class Mission(
    val type: String,
    val icon: String,
    val text: String,
    val target: Int,
    var current: Int,
    var completed: Boolean,
    val reward: Int,
    val key: String,
    val testId: String? = null,
    val difficulty: MissionDifficulty = MissionDifficulty.MEDIUM
)

data class MissionData(
    val date: String,
    val missions: List<Mission>
)

data class XPProgress(
    val pct: Int,
    val intoRank: Int,
    val rankSpan: Int,
    val nextXp: Int
)

data class FloatingPopup(
    val text: String,
    val color: Color,
    val size: Int,
    val delay: Float,
    val icon: String = ""
)

data class PowerUpToast(
    val text: String,
    val icon: String
)

data class RankUpOverlay(
    val oldRank: Rank,
    val newRank: Rank
)
