package com.opoleyes.ui.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.opoleyes.data.model.TestData
import com.opoleyes.data.repository.GameRepository
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository
import com.opoleyes.domain.AchievementChecker
import com.opoleyes.domain.AchievementContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TrainingViewModel(application: Application) : AndroidViewModel(application) {
    private val gameRepo = GameRepository(application)
    private val statsRepo = StatsRepository(application)
    private val progressRepo = ProgressRepository(application)
    private val achievementChecker = AchievementChecker(application)

    var currentTestData: TestData? = null
    var currentQuestionIndex: Int = 0
    var userAnswers: MutableMap<Int, String> = mutableMapOf()
    var flaggedQuestions: MutableSet<Int> = mutableSetOf()
    var flaggedList: MutableList<Int> = mutableListOf()
    var flaggedIndex: Int = 0
    var reviewWrongList: MutableList<Int> = mutableListOf()
    var reviewWrongIndex: Int = 0

    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    fun startTraining(testId: String): Boolean {
        currentTestData = gameRepo.startTraining(testId)
        if (currentTestData?.questions.isNullOrEmpty()) return false
        currentQuestionIndex = 0
        userAnswers.clear()
        flaggedQuestions.clear()
        flaggedList.clear()
        flaggedIndex = 0
        reviewWrongList.clear()
        reviewWrongIndex = 0
        updateUiState()
        return true
    }

    fun startTrainingCustom(category: String, count: Int): Boolean {
        currentTestData = gameRepo.startTrainingCustom(category, count)
        if (currentTestData?.questions.isNullOrEmpty()) return false
        currentQuestionIndex = 0
        userAnswers.clear()
        flaggedQuestions.clear()
        flaggedList.clear()
        flaggedIndex = 0
        reviewWrongList.clear()
        reviewWrongIndex = 0
        updateUiState()
        return true
    }

    fun answerQuestion(questionId: Int, option: String) {
        userAnswers[questionId] = option
        updateUiState()
    }

    fun toggleFlag(questionId: Int) {
        if (flaggedQuestions.contains(questionId)) flaggedQuestions.remove(questionId)
        else flaggedQuestions.add(questionId)
        updateUiState()
    }

    fun nextQuestion() {
        currentTestData?.let { td ->
            if (currentQuestionIndex < td.questions.size - 1) {
                currentQuestionIndex++
                updateUiState()
            }
        }
    }

    fun prevQuestion() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--
            updateUiState()
        }
    }

    fun goToQuestion(index: Int) {
        currentQuestionIndex = index
        updateUiState()
    }

    fun buildFlaggedList() {
        flaggedList = currentTestData?.questions?.mapIndexed { i, q ->
            if (flaggedQuestions.contains(q.id)) i else -1
        }?.filter { it >= 0 }?.toMutableList() ?: mutableListOf()
        flaggedIndex = 0
    }

    fun buildWrongList() {
        val td = currentTestData ?: return
        val am = td.answers.associate { it.id to it.correct }
        reviewWrongList = td.questions.mapIndexed { i, q ->
            val userAns = userAnswers[q.id]
            val correct = am[q.id]
            if (userAns != null && userAns != correct) i else -1
        }.filter { it >= 0 }.toMutableList()
        reviewWrongIndex = 0
    }

    fun submitResults(): SubmitResult {
        val td = currentTestData ?: return SubmitResult(0, 0, 0)
        val am = td.answers.associate { it.id to it.correct }
        var correct = 0
        var wrong = 0
        var unanswered = 0
        for (q in td.questions) {
            val userAns = userAnswers[q.id]
            val correctAns = am[q.id]
            if (userAns == null) unanswered++
            else if (userAns == correctAns) {
                correct++
                val key = "${q.test_id}:${q.orig_id}"
                statsRepo.updateStat(key, true)
            } else {
                wrong++
                val key = "${q.test_id}:${q.orig_id}"
                statsRepo.updateStat(key, false)
            }
        }
        progressRepo.addXP(correct * 5)
        progressRepo.incrementTrainingsDone()
        achievementChecker.check(AchievementContext())
        return SubmitResult(correct, wrong, unanswered)
    }

    fun getScorePercent(): Int {
        val td = currentTestData ?: return 0
        if (td.questions.isEmpty()) return 0
        val am = td.answers.associate { it.id to it.correct }
        val correct = td.questions.count { q -> userAnswers[q.id] == am[q.id] }
        return correct * 100 / td.questions.size
    }

    private fun updateUiState() {
        _uiState.value = TrainingUiState(
            currentIndex = currentQuestionIndex,
            totalQuestions = currentTestData?.questions?.size ?: 0,
            selectedOption = currentTestData?.questions?.getOrNull(currentQuestionIndex)?.let { userAnswers[it.id] },
            isFlagged = currentTestData?.questions?.getOrNull(currentQuestionIndex)?.let { flaggedQuestions.contains(it.id) } ?: false
        )
    }

    fun getProgressRepo() = progressRepo
    fun getStatsRepo() = statsRepo
}

data class TrainingUiState(
    val currentIndex: Int = 0,
    val totalQuestions: Int = 0,
    val selectedOption: String? = null,
    val isFlagged: Boolean = false
)

data class SubmitResult(
    val correct: Int,
    val wrong: Int,
    val unanswered: Int
)
