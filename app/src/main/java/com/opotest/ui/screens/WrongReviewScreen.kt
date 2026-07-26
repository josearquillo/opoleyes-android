package com.opotest.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
fun WrongReviewScreen(navController: NavController, trainingViewModel: TrainingViewModel) {
    val td = trainingViewModel.currentTestData
    if (td == null || trainingViewModel.reviewWrongList.isEmpty()) {
        LaunchedEffect(Unit) { navController.navigate(Routes.RESULTS) }
        return
    }

    val idx = trainingViewModel.reviewWrongList.getOrNull(trainingViewModel.reviewWrongIndex)
    if (idx == null) {
        LaunchedEffect(Unit) { navController.navigate(Routes.RESULTS) }
        return
    }

    val currentQ = td.questions[idx]
    val am = td.answers.associate { it.id to it.correct }
    val correctAnswer = am[currentQ.id]
    val userAnswer = trainingViewModel.userAnswers[currentQ.id]
    val letters = listOf("A", "B", "C", "D")
    val isLast = trainingViewModel.reviewWrongIndex == trainingViewModel.reviewWrongList.size - 1

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("❌ Repaso de fallos", color = TextLight, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Fallo ${trainingViewModel.reviewWrongIndex + 1} de ${trainingViewModel.reviewWrongList.size}", color = TextMuted, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        ProgressBar(
            progress = (trainingViewModel.reviewWrongIndex + 1f) / trainingViewModel.reviewWrongList.size,
            color = Danger,
            height = 6
        )
        Spacer(Modifier.height(16.dp))

        val scrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState)) {
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
                        isCorrect = letter == correctAnswer,
                        isSelected = userAnswer == letter,
                        isWrong = userAnswer == letter && letter != correctAnswer,
                        answered = true,
                        enabled = false
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GameButton("←", modifier = Modifier.weight(1f).height(44.dp), color1 = SurfaceVariant, color2 = BgCard) {
                if (trainingViewModel.reviewWrongIndex > 0) trainingViewModel.reviewWrongIndex--
            }
            GameButton(
                text = if (isLast) "✓" else "→",
                modifier = Modifier.weight(1f).height(44.dp),
                color1 = Primary,
                color2 = PurpleDark
            ) {
                if (isLast) navController.navigate(Routes.RESULTS)
                else trainingViewModel.reviewWrongIndex++
            }
        }
    }
}
