package com.opoleyes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import com.opoleyes.ui.components.GameButton
import com.opoleyes.ui.components.LoadingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamResultScreen(navController: NavController, gameViewModel: GameViewModel) {
    val result by gameViewModel.examResult.collectAsState()
    val simulacroResult by gameViewModel.simulacroResult.collectAsState()
    val isSimulacro by gameViewModel.isSimulacroMode.collectAsState()
    val xpGained by gameViewModel.xpGained.collectAsState()
    val isLoading by gameViewModel.isLoading.collectAsState()

    if (result == null && simulacroResult == null && !gameViewModel.isRetrying) {
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
                        stringResource(R.string.resultados),
                        color = TextLight, fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
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
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
        if (isSimulacro && sr != null) {
            SimulacroResultContent(sr, xpGained, allQuestions, showReview) { showReview = !showReview }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GameButton(
                    text = stringResource(R.string.retry_label),
                    modifier = Modifier.weight(1f).height(50.dp),
                    color1 = Success,
                    color2 = SuccessDark
                ) {
                    gameViewModel.startSimulacroAsync { }
                    navController.navigate(Routes.EXAM) { popUpTo(Routes.EXAM_RESULT) { inclusive = true } }
                }
                GameButton(
                    text = stringResource(R.string.menu),
                    modifier = Modifier.weight(1f).height(50.dp),
                    color1 = Primary,
                    color2 = PurpleDark
                ) {
                    navController.navigate(Routes.HOME) { popUpTo(0) }
                }
            }
        } else if (r != null) {
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GameButton(
                    text = stringResource(R.string.retry_label),
                    modifier = Modifier.weight(1f).height(50.dp),
                    color1 = Success,
                    color2 = SuccessDark
                ) {
                    gameViewModel.startExamAsync(gameViewModel.lastExamQuestionCount) { }
                    navController.navigate(Routes.EXAM) { popUpTo(Routes.EXAM_RESULT) { inclusive = true } }
                }
                GameButton(
                    text = stringResource(R.string.menu),
                    modifier = Modifier.weight(1f).height(50.dp),
                    color1 = Primary,
                    color2 = PurpleDark
                ) {
                    navController.navigate(Routes.HOME) { popUpTo(0) }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        }
    }

    if (isLoading) {
        LoadingOverlay()
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

    // Count-up animation
    val animatedPoints = remember { Animatable(0f) }
    LaunchedEffect(sr.points) {
        animatedPoints.animateTo(sr.points, animationSpec = tween(1200, easing = FastOutSlowInEasing))
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
                String.format("%.2f", animatedPoints.value),
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

    // Count-up for simulacro stats
    val animCorrect = remember { Animatable(0f) }
    val animWrong = remember { Animatable(0f) }
    val animUnanswered = remember { Animatable(0f) }
    LaunchedEffect(sr.correct, sr.wrong, sr.unanswered) {
        animCorrect.animateTo(sr.correct.toFloat(), animationSpec = tween(800, easing = FastOutSlowInEasing))
        animWrong.animateTo(sr.wrong.toFloat(), animationSpec = tween(800, easing = FastOutSlowInEasing))
        animUnanswered.animateTo(sr.unanswered.toFloat(), animationSpec = tween(800, easing = FastOutSlowInEasing))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(Icons.Default.Check, animCorrect.value.toInt().toString(), stringResource(R.string.correct_label), Success)
        StatItem(Icons.Default.Close, animWrong.value.toInt().toString(), stringResource(R.string.wrong_label), Danger)
        StatItem("—", animUnanswered.value.toInt().toString(), stringResource(R.string.unanswered_label), TextMuted)
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

    // Count-up animation
    val animatedScore = remember { Animatable(0f) }
    LaunchedEffect(r.score) {
        animatedScore.animateTo(r.score, animationSpec = tween(1200, easing = FastOutSlowInEasing))
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
                String.format("%.1f", animatedScore.value),
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
    // Count-up for stats
    val animatedCorrect = remember { Animatable(0f) }
    val animatedWrong = remember { Animatable(0f) }
    val animatedUnanswered = remember { Animatable(0f) }
    LaunchedEffect(r.correct, r.wrong, r.unanswered) {
        animatedCorrect.animateTo(r.correct.toFloat(), animationSpec = tween(800, easing = FastOutSlowInEasing))
        animatedWrong.animateTo(r.wrong.toFloat(), animationSpec = tween(800, easing = FastOutSlowInEasing))
        animatedUnanswered.animateTo(r.unanswered.toFloat(), animationSpec = tween(800, easing = FastOutSlowInEasing))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(Icons.Default.Check, animatedCorrect.value.toInt().toString(), stringResource(R.string.correct_label), Success)
        StatItem(Icons.Default.Close, animatedWrong.value.toInt().toString(), stringResource(R.string.wrong_label), Danger)
        StatItem("—", animatedUnanswered.value.toInt().toString(), stringResource(R.string.unanswered_label), TextMuted)
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
    // Animated bar fill
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(lr.correct, lr.total) {
        animatedProgress.animateTo(
            if (lr.total > 0) lr.correct.toFloat() / lr.total else 0f,
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        )
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
            progress = { animatedProgress.value },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = BgDark
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
                Text(stringResource(R.string.question_prefix, idx), color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
