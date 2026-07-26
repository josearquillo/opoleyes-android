package com.opotest.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.opotest.ui.components.GameButton
import com.opotest.ui.components.OptionCard
import com.opotest.ui.components.ProgressBar
import com.opotest.ui.navigation.TrainingViewModel
import com.opotest.ui.navigation.Routes
import com.opotest.ui.theme.*

@Composable
fun TestBrowserScreen(navController: NavController, trainingViewModel: TrainingViewModel) {
    val uiState by trainingViewModel.uiState.collectAsState()
    val td = trainingViewModel.currentTestData
    if (td == null || td.questions.isEmpty()) {
        LaunchedEffect(Unit) { navController.navigate(Routes.ERROR) }
        return
    }

    val currentQ = td.questions.getOrNull(uiState.currentIndex)
    if (currentQ == null) {
        LaunchedEffect(Unit) { navController.navigate(Routes.ERROR) }
        return
    }

    val am = td.answers.associate { it.id to it.correct }
    val correctAnswer = am[currentQ.id]
    val isLast = uiState.currentIndex == td.questions.size - 1
    val letters = listOf("A", "B", "C", "D")

    var showExitDialog by remember { mutableStateOf(false) }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Salir del entrenamiento?") },
            text = { Text("Perderás tu progreso.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }) { Text("Salir") }
            },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Cancelar") } }
        )
    }

    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameButton("← Salir", modifier = Modifier.height(36.dp), color1 = SurfaceVariant, color2 = BgCard) {
                showExitDialog = true
            }
            Spacer(Modifier.weight(1f))
            Text("Pregunta ${uiState.currentIndex + 1} de ${uiState.totalQuestions} 🚩", color = TextLight, fontSize = 14.sp)
        }
        Spacer(Modifier.height(12.dp))
        ProgressBar(progress = (uiState.currentIndex + 1f) / uiState.totalQuestions, color = Primary, height = 6)
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(BgCard, BgCardDark)))
                        .padding(20.dp)
                ) {
                    Text(currentQ.enunciado, color = TextLight, fontSize = 16.sp)
                }
                Spacer(Modifier.height(16.dp))
                letters.forEach { letter ->
                    val text = currentQ.opciones[letter] ?: return@forEach
                    OptionCard(
                        letter = letter,
                        text = text,
                        modifier = Modifier.fillMaxWidth(),
                        isSelected = uiState.selectedOption == letter,
                        enabled = true,
                        answered = false
                    ) {
                        trainingViewModel.answerQuestion(currentQ.id, letter)
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GameButton("← Anterior", modifier = Modifier.weight(1f).height(44.dp), color1 = SurfaceVariant, color2 = BgCard) {
                trainingViewModel.prevQuestion()
            }
            GameButton(
                text = if (uiState.isFlagged) "Quitar 🚩" else "🚩 Dudosa",
                modifier = Modifier.weight(1f).height(44.dp),
                color1 = Warning,
                color2 = Color(0xFF92400e)
            ) { trainingViewModel.toggleFlag(currentQ.id) }
            GameButton(
                text = if (isLast) "Finalizar ✓" else "Siguiente →",
                modifier = Modifier.weight(1f).height(44.dp),
                color1 = Primary,
                color2 = PurpleDark
            ) {
                if (isLast) {
                    if (trainingViewModel.flaggedQuestions.isNotEmpty()) {
                        trainingViewModel.buildFlaggedList()
                        navController.navigate(Routes.FLAG_REVIEW)
                    } else {
                        trainingViewModel.submitResults()
                        navController.navigate(Routes.RESULTS)
                    }
                } else {
                    trainingViewModel.nextQuestion()
                }
            }
        }
    }
}
