package com.opoleyes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.R
import com.opoleyes.domain.ExamEngine
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamResultScreen(navController: NavController, gameViewModel: GameViewModel) {
    val result by gameViewModel.examResult.collectAsState()
    val simulacroResult by gameViewModel.simulacroResult.collectAsState()
    val isSimulacro by gameViewModel.isSimulacroMode.collectAsState()
    val xpGained by gameViewModel.xpGained.collectAsState()

    if (result == null && simulacroResult == null) {
        var navigated by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!navigated) {
                navigated = true
                navController.navigate(Routes.HOME) { popUpTo(0) }
            }
        }
        return
    }

    val r = result
    val sr = simulacroResult
    var showReview by remember { mutableStateOf(false) }
    val allQuestions = remember { gameViewModel.getExamQuestions() }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isSimulacro) stringResource(R.string.simulacro_result) else stringResource(R.string.exam_result),
                        color = TextLight, fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        gameViewModel.clearExamResult()
                        navController.navigate(Routes.HOME) { popUpTo(0) }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = TextLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark,
                    titleContentColor = TextLight
                )
            )
        },
        containerColor = BgDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
        if (isSimulacro && sr != null) {
            SimulacroResultContent(sr, xpGained, allQuestions, showReview) { showReview = !showReview }
            SimulacroActions(
                onHome = {
                    gameViewModel.clearExamResult()
                    navController.navigate(Routes.HOME) { popUpTo(0) }
                },
                onAnother = {
                    gameViewModel.clearExamResult()
                    navController.navigate(Routes.MODE_SELECT) { popUpTo(Routes.HOME) }
                }
            )
        } else if (r != null) {
            Text(stringResource(R.string.exam_result), color = TextLight, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))

            ScoreCard(r)
            Spacer(Modifier.height(16.dp))

            StatsRow(r)
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.per_law), color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            r.perLaw.forEach { (law, lr) ->
                LawBreakdownRow(law, lr)
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.xp_earned, xpGained), color = AccentLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { showReview = !showReview },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BgCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVariant)
            ) {
                Text(if (showReview) stringResource(R.string.hide_review) else stringResource(R.string.review_answers), color = TextLight)
            }

            AnimatedVisibility(showReview) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    allQuestions.forEachIndexed { idx, eq ->
                        QuestionReviewCard(idx + 1, eq)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        gameViewModel.clearExamResult()
                        navController.navigate(Routes.HOME) { popUpTo(0) }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVariant)
                ) {
                    Text(stringResource(R.string.home_label))
                }
                Button(
                    onClick = {
                        gameViewModel.clearExamResult()
                        navController.navigate(Routes.MODE_SELECT) { popUpTo(Routes.HOME) }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(stringResource(R.string.another_exam))
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SimulacroResultContent(
    sr: ExamEngine.SimulacroResult,
    xpGained: Int,
    allQuestions: List<ExamEngine.ExamQuestion>,
    showReview: Boolean,
    onToggleReview: () -> Unit
) {
    val scoreColor = if (sr.passed) Success else Danger
    val gradeText = if (sr.passed) stringResource(R.string.grade_pass) else stringResource(R.string.grade_fail)

    Text(stringResource(R.string.simulacro_result), color = TextLight, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(24.dp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BgCard
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                String.format("%.2f", sr.points),
                color = scoreColor,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.out_of_fifteen, sr.maxPoints.toInt()),
                color = TextMuted, fontSize = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(gradeText, color = scoreColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.passing_score, sr.passingScore.toInt()),
                color = TextMuted, fontSize = 12.sp
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(Icons.Default.Check, sr.correct.toString(), stringResource(R.string.correct_label), Success)
        StatItem(Icons.Default.Close, sr.wrong.toString(), stringResource(R.string.wrong_label), Danger)
        StatItem("—", sr.unanswered.toString(), stringResource(R.string.unanswered_label), TextMuted)
    }

    Spacer(Modifier.height(16.dp))

    Text(stringResource(R.string.per_law), color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    sr.perLaw.forEach { (law, lr) ->
        LawBreakdownRow(law, lr)
        Spacer(Modifier.height(6.dp))
    }

    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.xp_earned, xpGained), color = AccentLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = onToggleReview,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVariant)
    ) {
        Text(
            if (showReview) stringResource(R.string.hide_review) else stringResource(R.string.review_answers),
            color = TextLight
        )
    }

    AnimatedVisibility(showReview) {
        Column {
            Spacer(Modifier.height(16.dp))
            allQuestions.forEachIndexed { idx, eq ->
                QuestionReviewCard(idx + 1, eq)
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    Spacer(Modifier.height(24.dp))
}

@Composable
private fun SimulacroActions(onHome: () -> Unit, onAnother: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onHome,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVariant)
        ) {
            Text(stringResource(R.string.home_label))
        }
        Button(
            onClick = onAnother,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Text(stringResource(R.string.another_simulacro))
        }
    }
}

@Composable
private fun ScoreCard(r: ExamEngine.ExamResult) {
    val scoreColor = when {
        r.score >= 7 -> Success
        r.score >= 5 -> Warning
        else -> Danger
    }
    val grade = when {
        r.score >= 9 -> stringResource(R.string.grade_outstanding)
        r.score >= 7 -> stringResource(R.string.grade_notable)
        r.score >= 6 -> stringResource(R.string.grade_good)
        r.score >= 5 -> stringResource(R.string.grade_pass)
        else -> stringResource(R.string.grade_fail)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BgCard
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                String.format("%.1f", r.score),
                color = scoreColor,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold
            )
            Text(stringResource(R.string.out_of_ten), color = TextMuted, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text(grade, color = scoreColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatsRow(r: ExamEngine.ExamResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(Icons.Default.Check, r.correct.toString(), stringResource(R.string.correct_label), Success)
        StatItem(Icons.Default.Close, r.wrong.toString(), stringResource(R.string.wrong_label), Danger)
        StatItem("—", r.unanswered.toString(), stringResource(R.string.unanswered_label), TextMuted)
    }
}

@Composable
private fun StatItem(icon: Any, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon is androidx.compose.ui.graphics.vector.ImageVector) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        } else {
            Text(icon as String, fontSize = 24.sp)
        }
        Text(value, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun LawBreakdownRow(law: String, lr: ExamEngine.LawResult) {
    val pct = if (lr.total > 0) lr.correct * 100 / lr.total else 0
    val barColor = when {
        pct >= 70 -> Success
        pct >= 50 -> Warning
        else -> Danger
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(law, color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("${lr.correct}/${lr.total}", color = TextMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (lr.total > 0) lr.correct.toFloat() / lr.total else 0f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = BgCardDark
        )
    }
}

@Composable
private fun QuestionReviewCard(idx: Int, eq: ExamEngine.ExamQuestion) {
    val isCorrect = eq.userAnswer == eq.question.correct
    val isUnanswered = eq.userAnswer == null
    val cardColor = when {
        isCorrect -> SuccessDark.copy(alpha = 0.3f)
        isUnanswered -> BgCard
        else -> DangerDark.copy(alpha = 0.3f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = cardColor
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("P$idx", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                if (isCorrect) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Success, modifier = Modifier.size(16.dp))
                } else if (!isUnanswered) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Danger, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isUnanswered) stringResource(R.string.unanswered_label) else if (isCorrect) stringResource(R.string.correct) else stringResource(R.string.incorrect),
                    color = if (isCorrect) Success else if (isUnanswered) TextMuted else Danger,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(eq.question.enunciado, color = TextLight, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            if (!isUnanswered && !isCorrect) {
                Text(stringResource(R.string.your_answer, eq.question.opciones[eq.userAnswer] ?: eq.userAnswer ?: ""),
                    color = Danger, fontSize = 12.sp)
            }
            Text(stringResource(R.string.correct_answer, eq.question.opciones[eq.question.correct] ?: eq.question.correct),
                color = Success, fontSize = 12.sp)
        }
    }
}
