package com.opoleyes.ui.screens

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
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.ui.components.GameButton
import com.opoleyes.ui.components.LoadingOverlay
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*

@Composable
fun ModeSelectScreen(navController: NavController, gameViewModel: GameViewModel) {
    val context = navController.context
    val progressRepo = ProgressRepository(context)
    val unlocks = remember { progressRepo.getUnlocks() }
    val rankIndex = remember { progressRepo.getRankIndex() }
    val isLoading by gameViewModel.isLoading.collectAsState()

    val modes = listOf(
        ModeInfo(GameMode.SURVIVAL, "❤️", "Supervivencia", "3 vidas, sin tiempo. Los combos recuperan vida.", true, 0),
        ModeInfo(GameMode.TIMETRIAL, "⏱️", "Contrarreloj", "180s. +15s acierto, -10s fallo.", unlocks.timetrial, 1),
        ModeInfo(GameMode.QUICK, "⚡", "Repaso Express", "20 preguntas enfocadas en fallos previos.", unlocks.quick, 2),
        ModeInfo(GameMode.EXAM, "📝", "Modo Examen", "Simula el examen oficial. Sin vidas, sin power-ups, corrección al final.", unlocks.exam, 3),
        ModeInfo(GameMode.CHALLENGE, "🏆", "Modo Reto", "120s, máxima dificultad, todas las leyes.", unlocks.challenge, 4),
    )

    var showExamDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                            gameViewModel.startQuickGameAsync { ok -> if (ok) navController.navigate(Routes.GAME) }
                        }
                        GameMode.CHALLENGE -> {
                            gameViewModel.startChallengeGameAsync { ok -> if (ok) navController.navigate(Routes.GAME) }
                        }
                        GameMode.EXAM -> {
                            showExamDialog = true
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

    if (isLoading) {
        LoadingOverlay()
    }

    if (showExamDialog) {
        ExamConfigDialog(
            onDismiss = { showExamDialog = false },
            onStart = { count ->
                showExamDialog = false
                gameViewModel.startExamAsync(count) { ok ->
                    if (ok) navController.navigate(Routes.EXAM)
                }
            }
        )
    }
}

@Composable
private fun ModeCard(mode: ModeInfo, rankIndex: Int, onClick: () -> Unit) {
    val locked = !mode.unlocked
    val colors = when (mode.mode) {
        GameMode.SURVIVAL -> listOf(Danger, DangerDark)
        GameMode.TIMETRIAL -> listOf(Cyan, Color(0xFF155e75))
        GameMode.QUICK -> listOf(Warning, Color(0xFF92400e))
        GameMode.EXAM -> listOf(Success, SuccessDark)
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

@Composable
private fun ExamConfigDialog(
    onDismiss: () -> Unit,
    onStart: (Int) -> Unit
) {
    var customCount by remember { mutableStateOf("25") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        titleContentColor = TextLight,
        title = { Text("📝 Configurar examen", color = TextLight, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Selecciona el número de preguntas:", color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExamPresetButton("Auxilio\n50 preg.", 50, onStart)
                    ExamPresetButton("Tramitación\n100 preg.", 100, onStart)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExamPresetButton("Gestión\n100 preg.", 100, onStart)
                    ExamPresetButton("Rápido\n25 preg.", 25, onStart)
                }
                Spacer(Modifier.height(16.dp))
                Text("Personalizado:", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customCount,
                        onValueChange = { customCount = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextLight),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BgCard,
                            unfocusedContainerColor = BgCard,
                            focusedIndicatorColor = Primary,
                            unfocusedIndicatorColor = SurfaceVariant
                        )
                    )
                    Button(
                        onClick = {
                            val count = customCount.toIntOrNull()?.coerceIn(5, 200) ?: 25
                            onStart(count)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("Empezar") }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) }
        }
    )
}

@Composable
private fun ExamPresetButton(label: String, count: Int, onStart: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.48f)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant)
            .clickable { onStart(count) }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
