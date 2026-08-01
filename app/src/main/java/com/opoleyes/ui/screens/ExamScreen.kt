package com.opoleyes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.R
import com.opoleyes.ui.components.LoadingOverlay
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(navController: NavController, gameViewModel: GameViewModel) {
    val examQuestionNum by gameViewModel.examQuestionNum.collectAsState()
    val examAnswered by gameViewModel.examAnswered.collectAsState()
    val isLoading by gameViewModel.isLoading.collectAsState()
    val isSimulacro by gameViewModel.isSimulacroMode.collectAsState()
    val simulacroTimer by gameViewModel.simulacroTimer.collectAsState()

    val currentQ by gameViewModel.examCurrentQuestion.collectAsState()
    val totalQuestions by gameViewModel.examTotalQuestions.collectAsState()
    val allQuestions = remember { gameViewModel.getExamQuestions() }

    if (currentQ == null || totalQuestions == 0) {
        if (!isLoading) {
            var navigated by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                if (!navigated) {
                    navigated = true
                    navController.navigate(Routes.HOME) { popUpTo(0) }
                }
            }
        }
        if (isLoading) LoadingOverlay()
        return
    }

    val examQ = currentQ!!
    val question = examQ.question
    val userAnswer = examQ.userAnswer
    val isLast = examQuestionNum == totalQuestions - 1

    val allLetters = listOf("A", "B", "C", "D")
    val displayLetters = remember(question) { allLetters.filter { question.opciones[it] != null } }

    var showExitDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    BackHandler { showExitDialog = true }

    // Simulacro timer countdown
    if (isSimulacro) {
        LaunchedEffect(simulacroTimer) {
            if (simulacroTimer > 0) {
                kotlinx.coroutines.delay(1000L)
                val expired = gameViewModel.tickSimulacroTimer()
                if (expired) {
                    gameViewModel.finishExam()
                    navController.navigate(Routes.EXAM_RESULT)
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.exit_exam)) },
            text = { Text(stringResource(R.string.lose_exam_progress)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }) { Text(stringResource(R.string.exit)) }
            },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showFinishDialog) {
        val unansweredCount = totalQuestions - examAnswered
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text(stringResource(R.string.finish_exam)) },
            text = {
                if (unansweredCount > 0) {
                    Text(stringResource(R.string.unanswered_remaining, unansweredCount, if (unansweredCount > 1) "s" else ""))
                } else {
                    Text(stringResource(R.string.all_answered))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    gameViewModel.finishExam()
                    navController.navigate(Routes.EXAM_RESULT)
                }) { Text(stringResource(R.string.finish), color = Success) }
            },
            dismissButton = { TextButton(onClick = { showFinishDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (isSimulacro) stringResource(R.string.mode_simulacro) else stringResource(R.string.mode_exam),
                            color = TextLight, fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { showExitDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.exit), tint = TextLight)
                        }
                    },
                    actions = {
                        if (isSimulacro) {
                            val mins = simulacroTimer / 60
                            val secs = simulacroTimer % 60
                            val timerColor = if (simulacroTimer <= 60) Danger else TextLight
                            Text(
                                String.format("%02d:%02d", mins, secs),
                                color = timerColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        } else {
                            Text(
                                stringResource(R.string.answered_of, examAnswered, totalQuestions),
                                color = TextMuted,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(end = 16.dp)
                            )
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
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { (examQuestionNum + 1f) / totalQuestions },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = Primary,
                    trackColor = BgCard
                )

            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(R.string.question_of, examQuestionNum + 1, totalQuestions),
                color = TextMuted,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))

            QuestionNavigationGrid(
                total = totalQuestions,
                current = examQuestionNum,
                answered = allQuestions.map { it.userAnswer != null },
                onNavigate = { idx -> gameViewModel.examNavigate(idx) }
            )

            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = BgCard
            ) {
                Text(
                    question.enunciado,
                    color = TextLight,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            displayLetters.forEach { letter ->
                val optionText = question.opciones[letter] ?: return@forEach
                val isSelected = userAnswer == letter
                val optionColor = if (isSelected) Brush.verticalGradient(listOf(Primary, PurpleDark)) else Brush.verticalGradient(listOf(BgCard, BgDark))
                val borderColor = if (isSelected) PrimaryLight else Color.Transparent

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(optionColor)
                        .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable {
                            if (userAnswer == letter) {
                                gameViewModel.examClearAnswer()
                            } else {
                                gameViewModel.examAnswer(letter)
                            }
                        }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                letter,
                                color = if (isSelected) Primary else TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            optionText,
                            color = if (isSelected) Color.White else TextLight,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { gameViewModel.examPrev() },
                    enabled = examQuestionNum > 0,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight),
                    border = BorderStroke(1.dp, SurfaceVariant)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.previous))
                }

                if (isLast) {
                    Button(
                        onClick = { showFinishDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Success)
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.finish))
                    }
                } else {
                    Button(
                        onClick = { gameViewModel.examNext() },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text(stringResource(R.string.next))
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun QuestionNavigationGrid(
    total: Int,
    current: Int,
    answered: List<Boolean>,
    onNavigate: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(current) {
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        if (visibleItems.isNotEmpty()) {
            val firstVisible = visibleItems.first().index
            val lastVisible = visibleItems.last().index
            if (current < firstVisible || current > lastVisible) {
                listState.animateScrollToItem(current)
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(total) { idx ->
            val isCurrent = idx == current
            val isAnswered = answered.getOrNull(idx) ?: false
            val bgColor = when {
                isCurrent -> Primary
                isAnswered -> SuccessDark
                else -> BgCard
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable { onNavigate(idx) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${idx + 1}",
                    color = if (isCurrent || isAnswered) Color.White else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
