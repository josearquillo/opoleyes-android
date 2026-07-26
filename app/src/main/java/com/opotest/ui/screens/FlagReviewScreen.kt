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
fun FlagReviewScreen(navController: NavController, trainingViewModel: TrainingViewModel) {
    val td = trainingViewModel.currentTestData
    if (td == null || trainingViewModel.flaggedList.isEmpty()) {
        LaunchedEffect(Unit) {
            trainingViewModel.submitResults()
            navController.navigate(Routes.RESULTS)
        }
        return
    }

    val idx = trainingViewModel.flaggedList.getOrNull(trainingViewModel.flaggedIndex)
    if (idx == null) {
        LaunchedEffect(Unit) {
            trainingViewModel.submitResults()
            navController.navigate(Routes.RESULTS)
        }
        return
    }

    val currentQ = td.questions[idx]
    val letters = listOf("A", "B", "C", "D")
    val isLast = trainingViewModel.flaggedIndex == trainingViewModel.flaggedList.size - 1

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("🚩 Repaso de dudosas", color = TextLight, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Dudosa ${trainingViewModel.flaggedIndex + 1} de ${trainingViewModel.flaggedList.size}", color = TextMuted, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        ProgressBar(
            progress = (trainingViewModel.flaggedIndex + 1f) / trainingViewModel.flaggedList.size,
            color = Warning,
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
                    val selected = trainingViewModel.userAnswers[currentQ.id] == letter
                    OptionCard(
                        letter = letter,
                        text = text,
                        modifier = Modifier.fillMaxWidth(),
                        isSelected = selected,
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
                if (trainingViewModel.flaggedIndex > 0) trainingViewModel.flaggedIndex--
            }
            GameButton("Quitar 🚩", modifier = Modifier.weight(1f).height(44.dp), color1 = Warning, color2 = Color(0xFF92400e)) {
                trainingViewModel.flaggedQuestions.remove(currentQ.id)
                trainingViewModel.buildFlaggedList()
                if (trainingViewModel.flaggedList.isEmpty()) {
                    trainingViewModel.submitResults()
                    navController.navigate(Routes.RESULTS)
                }
            }
            GameButton(
                text = if (isLast) "Entregar ✓" else "Siguiente →",
                modifier = Modifier.weight(1f).height(44.dp),
                color1 = Primary,
                color2 = PurpleDark,
                enabled = trainingViewModel.userAnswers[currentQ.id] != null
            ) {
                if (isLast) {
                    trainingViewModel.submitResults()
                    navController.navigate(Routes.RESULTS)
                } else {
                    trainingViewModel.flaggedIndex++
                }
            }
        }
    }
}
