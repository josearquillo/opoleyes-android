package com.opotest.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import com.opotest.data.model.GameMode
import com.opotest.data.repository.ProgressRepository
import com.opotest.ui.components.GameButton
import com.opotest.ui.navigation.GameViewModel
import com.opotest.ui.navigation.Routes
import com.opotest.ui.theme.*

@Composable
fun ModeSelectScreen(navController: NavController, gameViewModel: GameViewModel) {
    val context = navController.context
    val progressRepo = ProgressRepository(context)
    val unlocks = remember { progressRepo.getUnlocks() }
    val rankIndex = remember { progressRepo.getRankIndex() }

    val modes = listOf(
        ModeInfo(GameMode.SURVIVAL, "❤️", "Supervivencia", "3 vidas, sin tiempo. Los combos recuperan vida.", true, 0),
        ModeInfo(GameMode.TIMETRIAL, "⏱️", "Contrarreloj", "180s. +15s acierto, -10s fallo.", unlocks.timetrial, 1),
        ModeInfo(GameMode.QUICK, "⚡", "Repaso Express", "20 preguntas enfocadas en fallos previos.", unlocks.quick, 2),
        ModeInfo(GameMode.CHALLENGE, "🏆", "Modo Reto", "120s, máxima dificultad, todas las leyes.", unlocks.challenge, 4),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Selecciona modo", color = TextLight, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        modes.forEachIndexed { index, mode ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(index * 100L)
                visible = true
            }
            AnimatedVisibility(visible = visible) {
                ModeCard(mode, rankIndex) {
                    when (mode.mode) {
                        GameMode.QUICK -> {
                            if (gameViewModel.startQuickGame()) navController.navigate(Routes.GAME)
                        }
                        GameMode.CHALLENGE -> {
                            if (gameViewModel.startChallengeGame()) navController.navigate(Routes.GAME)
                        }
                        else -> {
                            gameViewModel.pendingMode = mode.mode
                            navController.navigate(Routes.TEMA_SELECT)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ModeCard(mode: ModeInfo, rankIndex: Int, onClick: () -> Unit) {
    val locked = !mode.unlocked
    val colors = when (mode.mode) {
        GameMode.SURVIVAL -> listOf(Danger, DangerDark)
        GameMode.TIMETRIAL -> listOf(Cyan, Color(0xFF155e75))
        GameMode.QUICK -> listOf(Warning, Color(0xFF92400e))
        GameMode.CHALLENGE -> listOf(Accent, PurpleDark)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(if (locked) listOf(BgCard, BgCardDark) else colors))
            .clickable(enabled = !locked) { onClick() }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(mode.icon, fontSize = 40.sp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(mode.name, color = if (locked) TextDim else Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(mode.desc, color = if (locked) TextDim else Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
            if (locked) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = TextDim)
                    Text("Rango ${mode.requiredRank + 1}", color = TextDim, fontSize = 10.sp)
                }
            }
        }
    }
}

private data class ModeInfo(
    val mode: GameMode,
    val icon: String,
    val name: String,
    val desc: String,
    val unlocked: Boolean,
    val requiredRank: Int
)
