package com.opoleyes.ui.screens

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
import com.opoleyes.ui.components.GameButton
import com.opoleyes.ui.components.ProgressBar
import com.opoleyes.ui.components.StatCard
import com.opoleyes.ui.navigation.TrainingViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*

@Composable
fun ResultsScreen(navController: NavController, trainingViewModel: TrainingViewModel) {
    val td = trainingViewModel.currentTestData
    val scorePercent = remember { trainingViewModel.getScorePercent() }
    val total = td?.questions?.size ?: 0
    val am = td?.answers?.associate { it.id to it.correct } ?: emptyMap()
    val correct = td?.questions?.count { q -> trainingViewModel.userAnswers[q.id] == am[q.id] } ?: 0
    val wrong = td?.questions?.count { q ->
        val ua = trainingViewModel.userAnswers[q.id]
        ua != null && ua != am[q.id]
    } ?: 0
    val unanswered = total - correct - wrong
    val hasWrong = wrong > 0

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        val icon = when {
            scorePercent == 100 -> "🎯"
            scorePercent >= 80 -> "🥇"
            scorePercent >= 60 -> "🥈"
            scorePercent >= 40 -> "🥉"
            else -> "📚"
        }
        Text(icon, fontSize = 56.sp)
        Text("Resultados", color = TextLight, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("$scorePercent%", color = PrimaryLight, fontSize = 56.sp, fontWeight = FontWeight.Bold)
        Text("aciertos", color = TextMuted, fontSize = 16.sp)

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("$correct", "Aciertos", Modifier.weight(1f))
            StatCard("$wrong", "Fallos", Modifier.weight(1f))
            StatCard("$unanswered", "Sin resp.", Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
        ProgressBar(progress = scorePercent / 100f, color = if (scorePercent >= 60) Success else Warning, height = 12)

        Spacer(Modifier.height(32.dp))
        if (hasWrong) {
            GameButton(
                text = "❌ Repasar fallos",
                modifier = Modifier.fillMaxWidth().height(50.dp),
                color1 = Danger,
                color2 = DangerDark
            ) {
                trainingViewModel.buildWrongList()
                navController.navigate(Routes.WRONG_REVIEW)
            }
            Spacer(Modifier.height(12.dp))
        }
        GameButton(
            text = "🏠 Menú",
            modifier = Modifier.fillMaxWidth().height(50.dp),
            color1 = Primary,
            color2 = PurpleDark
        ) {
            navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
        }
    }
}
