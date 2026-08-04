package com.opoleyes.domain

import android.content.Context
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.model.QuestionEntry
import com.opoleyes.data.repository.StatsRepository

class ExamEngine(private val context: Context) {
    private val statsRepo = StatsRepository(context)

    data class ExamQuestion(
        val question: QuestionEntry,
        val userAnswer: String? = null
    )

    data class ExamResult(
        val total: Int,
        val correct: Int,
        val wrong: Int,
        val unanswered: Int,
        val score: Float,
        val perLaw: Map<String, LawResult>
    )

    data class LawResult(
        val total: Int,
        val correct: Int,
        val wrong: Int,
        val unanswered: Int
    )

    data class SimulacroResult(
        val total: Int,
        val correct: Int,
        val wrong: Int,
        val unanswered: Int,
        val points: Float,
        val maxPoints: Float,
        val passingScore: Float,
        val passed: Boolean,
        val perLaw: Map<String, LawResult>
    )

    private var questions: List<ExamQuestion> = emptyList()
    private var currentIndex: Int = 0
    private var testLawMap: Map<String, String> = emptyMap()

    companion object {
        const val SIMULACRO_QUESTIONS = 100
        const val SIMULACRO_TIME_SECONDS = 100 * 60
        const val SIMULACRO_CORRECT_POINTS = 0.60f
        const val SIMULACRO_WRONG_PENALTY = 0.15f
        const val SIMULACRO_MAX_POINTS = SIMULACRO_QUESTIONS * SIMULACRO_CORRECT_POINTS
        const val SIMULACRO_PASSING_SCORE = SIMULACRO_MAX_POINTS / 2f
    }

    fun getQuestionCount(): Int = questions.size
    fun getCurrentIndex(): Int = currentIndex
    fun getCurrentQuestion(): ExamQuestion? = questions.getOrNull(currentIndex)
    fun getQuestions(): List<ExamQuestion> = questions
    fun isFinished(): Boolean = currentIndex >= questions.size

    fun loadExam(questionCount: Int) {
        val lawWeights = mapOf(
            "LOPJ" to 28,
            "LEC" to 22,
            "LECrim" to 15,
            "Constitución/UE/Org" to 12,
            "Contencioso" to 8,
            "Social" to 5,
            "Registro Civil" to 5,
            "Concursal" to 3,
            "Otros" to 2
        )

        val allData = DataProvider.loadData(context).filter { it.test.tema != null }
        val stats = statsRepo.getStats()
        val poolsByLaw = mutableMapOf<String, MutableList<QuestionEntry>>()
        val testLaw = mutableMapOf<String, String>()

        for (d in allData) {
            val law = mapTestToLaw(d.test.name)
            testLaw[d.test.id] = law
            val am = d.answers.associate { it.id to it.correct }
            for (q in d.questions) {
                val correct = am[q.id] ?: continue
                val key = (q.test_id) + ":" + (q.orig_id)
                val s = stats[key]
                val attempted = if (s != null) s.correct + s.wrong else 0
                val weight = if (s != null && attempted >= 3)
                    maxOf((100 * (1.0 - s.correct.toDouble() / attempted)).toInt(), 5)
                else 50
                val entry = QuestionEntry(
                    enunciado = q.enunciado,
                    opciones = q.opciones,
                    correct = correct,
                    weight = weight,
                    testId = q.test_id,
                    origId = q.orig_id.toString(),
                    difficulty = q.difficulty
                )
                poolsByLaw.getOrPut(law) { mutableListOf() }.add(entry)
            }
        }
        testLawMap = testLaw

        for (pool in poolsByLaw.values) pool.shuffle()

        val selected = mutableListOf<QuestionEntry>()
        val totalWeight = lawWeights.values.sum()

        // Largest remainder method: floor each law's quota, then distribute
        // leftover slots to the laws with the largest fractional parts.
        // This guarantees the sum never exceeds questionCount (the old rounding
        // could overshoot and then randomly truncate questions via take()).
        val rawQuotas = lawWeights.mapValues { (_, weight) ->
            questionCount.toDouble() * weight / totalWeight
        }
        val counts = mutableMapOf<String, Int>()
        var assigned = 0
        for ((law, raw) in rawQuotas) {
            val pool = poolsByLaw[law] ?: continue
            val floor = minOf(raw.toInt(), pool.size)
            counts[law] = floor
            assigned += floor
        }

        var remaining = questionCount - assigned
        val sortedByRemainder = rawQuotas.entries
            .filter { poolsByLaw[it.key] != null }
            .sortedByDescending { it.value - it.value.toInt() }
        while (remaining > 0) {
            var gaveAny = false
            for (entry in sortedByRemainder) {
                if (remaining <= 0) break
                val law = entry.key
                val pool = poolsByLaw[law]!!
                val cur = counts[law] ?: 0
                if (cur < pool.size) {
                    counts[law] = cur + 1
                    remaining--
                    gaveAny = true
                }
            }
            if (!gaveAny) break // all pools exhausted
        }

        for ((law, count) in counts) {
            val pool = poolsByLaw[law] ?: continue
            if (count > 0) selected.addAll(pool.take(count))
        }

        // Fill any remaining slots from all pools (e.g. if some laws had tiny pools)
        while (selected.size < questionCount) {
            val remainingPool = poolsByLaw.values.flatten().filter { it !in selected }
            if (remainingPool.isEmpty()) break
            selected.add(remainingPool.random())
        }

        selected.shuffle()
        questions = selected.take(questionCount).map { eq -> ExamQuestion(eq) }
        currentIndex = 0
    }

    fun answer(letter: String) {
        if (currentIndex >= questions.size) return
        questions = questions.toMutableList().also {
            it[currentIndex] = it[currentIndex].copy(userAnswer = letter)
        }
    }

    fun clearAnswer() {
        if (currentIndex >= questions.size) return
        questions = questions.toMutableList().also {
            it[currentIndex] = it[currentIndex].copy(userAnswer = null)
        }
    }

    fun navigateTo(index: Int) {
        if (questions.isEmpty()) return
        currentIndex = index.coerceIn(0, questions.size - 1)
    }

    fun next(): Boolean {
        return if (currentIndex < questions.size - 1) {
            currentIndex++
            true
        } else false
    }

    fun prev(): Boolean {
        return if (currentIndex > 0) {
            currentIndex--
            true
        } else false
    }

    fun getAnsweredCount(): Int = questions.count { it.userAnswer != null }

    fun grade(): ExamResult {
        var correct = 0
        var wrong = 0
        var unanswered = 0
        val perLaw = mutableMapOf<String, LawResult>()

        for (eq in questions) {
            val law = testLawMap[eq.question.testId] ?: "Otros"
            val lawResult = perLaw.getOrPut(law) { LawResult(0, 0, 0, 0) }
            perLaw[law] = lawResult.copy(
                total = lawResult.total + 1,
                correct = lawResult.correct + (if (eq.userAnswer == eq.question.correct) 1 else 0),
                wrong = lawResult.wrong + (if (eq.userAnswer != null && eq.userAnswer != eq.question.correct) 1 else 0),
                unanswered = lawResult.unanswered + (if (eq.userAnswer == null) 1 else 0)
            )

            when {
                eq.userAnswer == null -> unanswered++
                eq.userAnswer == eq.question.correct -> correct++
                else -> wrong++
            }
        }

        val total = questions.size
        val score = if (total > 0) correct.toFloat() / total * 10f else 0f

        return ExamResult(total, correct, wrong, unanswered, score, perLaw)
    }

    fun loadSimulacro() {
        loadExam(SIMULACRO_QUESTIONS)
    }

    fun gradeSimulacro(): SimulacroResult {
        var correct = 0
        var wrong = 0
        var unanswered = 0
        val perLaw = mutableMapOf<String, LawResult>()

        for (eq in questions) {
            val law = testLawMap[eq.question.testId] ?: "Otros"
            val lawResult = perLaw.getOrPut(law) { LawResult(0, 0, 0, 0) }
            perLaw[law] = lawResult.copy(
                total = lawResult.total + 1,
                correct = lawResult.correct + (if (eq.userAnswer == eq.question.correct) 1 else 0),
                wrong = lawResult.wrong + (if (eq.userAnswer != null && eq.userAnswer != eq.question.correct) 1 else 0),
                unanswered = lawResult.unanswered + (if (eq.userAnswer == null) 1 else 0)
            )

            when {
                eq.userAnswer == null -> unanswered++
                eq.userAnswer == eq.question.correct -> correct++
                else -> wrong++
            }
        }

        val total = questions.size
        val points = correct * SIMULACRO_CORRECT_POINTS - wrong * SIMULACRO_WRONG_PENALTY
        val passed = points >= SIMULACRO_PASSING_SCORE

        return SimulacroResult(
            total = total,
            correct = correct,
            wrong = wrong,
            unanswered = unanswered,
            points = points,
            maxPoints = SIMULACRO_MAX_POINTS,
            passingScore = SIMULACRO_PASSING_SCORE,
            passed = passed,
            perLaw = perLaw
        )
    }

    private fun mapTestToLaw(testName: String): String {
        val n = java.text.Normalizer.normalize(testName, java.text.Normalizer.Form.NFD)
            .replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
            .lowercase()
        return when {
            n.contains("concursal") -> "Concursal"
            n.contains("registro civil") -> "Registro Civil"
            n.contains("contencioso") -> "Contencioso"
            n.contains("social") -> "Social"
            n.contains("penal") || n.contains("sumario") || n.contains("abreviado") ||
                n.contains("jurado") || n.contains("delito") || n.contains("vsm") ||
                n.contains("menor") || n.contains("enjuiciamiento r") -> "LECrim"
            n.contains("constituc") || n.contains("tribunal constitucional") || n.contains("igualdad") ||
                n.contains("gobierno") || n.contains("organizacion territorial") || n.contains("union europea") ||
                n.contains("regimen local") -> "Constitución/UE/Org"
            n.contains("ordinario") || n.contains("verbal") || n.contains("monitorio") || n.contains("matrimonial") ||
                n.contains("voluntaria") || n.contains("recursos civil") || n.contains("ejecuci") ||
                n.contains("apremio") || n.contains("cautelar") || n.contains("costas") || n.contains("patrimonios") ||
                n.contains("diligencias") || n.contains("representac") || n.contains("jurisdicc") ||
                n.contains("actuaciones") || n.contains("archivo") || n.contains("cuestiones generales") -> "LEC"
            n.contains("poder judicial") || n.contains("cgpj") || n.contains("jueces") || n.contains("magistrados") ||
                n.contains("fiscal") || n.contains("organizac") || n.contains("tribunales") || n.contains("paz") ||
                n.contains("carta") || n.contains("oficina judicial") || n.contains("protecc") || n.contains("letrado") ||
                n.contains("cuerpos") || n.contains("libertad sindical") || n.contains("huelga") -> "LOPJ"
            else -> "Otros"
        }
    }
}
