package com.opoleyes.data.repository

import android.content.Context
import com.opoleyes.data.Constants
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.model.QuestionEntry
import com.opoleyes.data.model.TestData

class GameRepository(private val context: Context) {
    private val statsRepo = StatsRepository(context)
    private val prefs = PreferencesManager(context)

    private fun buildPoolFromTestData(td: TestData): List<QuestionEntry> {
        val am = td.answers.associate { it.id to it.correct }
        return td.questions.mapNotNull { q ->
            val correct = am[q.id] ?: return@mapNotNull null
            val key = (q.test_id) + ":" + (q.orig_id)
            QuestionEntry(
                enunciado = q.enunciado,
                opciones = q.opciones,
                correct = correct,
                weight = statsRepo.getWeight(key),
                testId = q.test_id,
                origId = q.orig_id.toString()
            )
        }
    }

    fun startTemaGame(testId: String): List<QuestionEntry> {
        val td = DataProvider.getTestDataMap(context)[testId] ?: return emptyList()
        return buildPoolFromTestData(td)
    }

    fun startAllLawsGame(): List<QuestionEntry> {
        val pool = mutableListOf<QuestionEntry>()
        for (d in DataProvider.loadData(context)) {
            if (d.test.tema == null) continue
            pool.addAll(buildPoolFromTestData(d))
        }
        return pool
    }

    fun startQuickGame(): List<QuestionEntry> {
        val stats = statsRepo.getStats()
        val wrongPool = mutableListOf<QuestionEntry>()
        val unansweredPool = mutableListOf<QuestionEntry>()
        val correctPool = mutableListOf<QuestionEntry>()

        for (d in DataProvider.loadData(context)) {
            if (d.test.tema == null) continue
            val am = d.answers.associate { it.id to it.correct }
            for (q in d.questions) {
                val correct = am[q.id] ?: continue
                val key = (q.test_id) + ":" + (q.orig_id)
                val s = stats[key]
                val entry = QuestionEntry(
                    enunciado = q.enunciado,
                    opciones = q.opciones,
                    correct = correct,
                    weight = statsRepo.getWeight(key),
                    testId = q.test_id,
                    origId = q.orig_id.toString()
                )
                when {
                    s != null && s.wrong > 0 -> wrongPool.add(entry)
                    s == null -> unansweredPool.add(entry)
                    else -> correctPool.add(entry)
                }
            }
        }
        var pool = (wrongPool + unansweredPool).toMutableList()
        if (pool.size < Constants.QUICK_MODE_QUESTIONS) pool.addAll(correctPool)
        pool.shuffle()
        return pool.take(Constants.QUICK_MODE_QUESTIONS)
    }

    fun startTraining(testId: String): TestData {
        return DataProvider.getTestDataMap(context)[testId] ?: TestData()
    }

    fun startTrainingCustom(category: String, count: Int): TestData {
        var pool = mutableListOf<QuestionEntry>()
        for (d in DataProvider.loadData(context)) {
            if (category.isNotEmpty() && d.test.category != category) continue
            val am = d.answers.associate { it.id to it.correct }
            for (q in d.questions) {
                val correct = am[q.id] ?: continue
                val key = (q.test_id) + ":" + (q.orig_id)
                pool.add(QuestionEntry(
                    enunciado = q.enunciado,
                    opciones = q.opciones,
                    correct = correct,
                    weight = statsRepo.getWeight(key),
                    testId = q.test_id,
                    origId = q.orig_id.toString()
                ))
            }
        }
        pool = pool.filter { it.weight > 0 }.toMutableList()
        pool.shuffle()
        val selected = pool.take(count)
        val questions = selected.mapIndexed { i, wq ->
            com.opoleyes.data.model.Question(
                id = i + 1, test_id = wq.testId, orig_id = wq.origId.toIntOrNull() ?: 0,
                enunciado = wq.enunciado, opciones = wq.opciones
            )
        }
        val answers = selected.mapIndexed { i, wq ->
            com.opoleyes.data.model.Answer(id = i + 1, correct = wq.correct)
        }
        return TestData(
            test = com.opoleyes.data.model.Test(id = "training", name = "Entrenamiento", category = category),
            questions = questions, answers = answers
        )
    }

    fun getFreePowerUps(): List<String> = prefs.getFreePowerUps()
    fun clearFreePowerUps() = prefs.clearFreePowerUps()
    fun addFreePowerUps(list: List<String>) {
        val current = prefs.getFreePowerUps().toMutableList()
        current.addAll(list)
        prefs.setFreePowerUps(current)
    }

    fun getMultiplier(): Int = prefs.getMultiplier()
    fun setMultiplier(value: Int) = prefs.setMultiplier(value)
}
