package com.opoleyes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
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

    val currentQ = gameViewModel.examEngine.getCurrentQuestion()
    val totalQuestions = gameViewModel.examEngine.getQuestionCount()
    val allQuestions = gameViewModel.examEngine.getQuestions()

    if (currentQ == null || totalQuestions == 0) {
        if (!isLoading) {
            LaunchedEffect(Unit) {
                navController.navigate(Routes.HOME) { popUpTo(0) }
            }
        }
        if (isLoading) LoadingOverlay()
        return
    }

    val question = currentQ.question
    val userAnswer = currentQ.userAnswer
    val isLast = examQuestionNum == totalQuestions - 1

    val allLetters = listOf("A", "B", "C", "D")
    val displayLetters = remember(question) { allLetters.filter { question.opciones[it] != null } }

    var showExitDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Salir del examen?") },
            text = { Text("Perderás tu progreso del examen.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }) { Text("Salir") }
            },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showFinishDialog) {
        val unansweredCount = totalQuestions - examAnswered
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("¿Finalizar examen?") },
            text = {
                if (unansweredCount > 0) {
                    Text("Te quedan $unansweredCount pregunta${if (unansweredCount > 1) "s" else ""} sin responder.")
                } else {
                    Text("Has respondido todas las preguntas.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    gameViewModel.finishExam()
                    navController.navigate(Routes.EXAM_RESULT)
                }) { Text("Finalizar", color = Success) }
            },
            dismissButton = { TextButton(onClick = { showFinishDialog = false }) { Text("Cancelar") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Modo Examen", color = TextLight, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { showExitDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Salir", tint = TextLight)
                        }
                    },
                    actions = {
                        Text(
                            "${examAnswered}/$totalQuestions",
                            color = TextMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 16.dp)
                        )
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
                "Pregunta ${examQuestionNum + 1} de $totalQuestions",
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
                val optionColor = if (isSelected) Brush.verticalGradient(listOf(Primary, PurpleDark)) else Brush.verticalGradient(listOf(BgCard, BgCardDark))
                val borderColor = if (isSelected) PrimaryLight else Color.Transparent

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(optionColor)
                        .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable { gameViewModel.examAnswer(letter) }
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
                    Text("Anterior")
                }

                if (isLast) {
                    Button(
                        onClick = { showFinishDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Success)
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Finalizar")
                    }
                } else {
                    Button(
                        onClick = { gameViewModel.examNext() },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Siguiente")
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
    LazyRow(
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
