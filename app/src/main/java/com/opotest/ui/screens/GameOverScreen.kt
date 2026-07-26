package com.opotest.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.opotest.data.model.ChestReward
import com.opotest.data.model.ChestType
import com.opotest.data.model.GameMode
import com.opotest.data.model.RankUpOverlay
import com.opotest.ui.components.GameButton
import com.opotest.ui.components.ProgressBar
import com.opotest.ui.components.StatCard
import com.opotest.ui.navigation.GameViewModel
import com.opotest.ui.navigation.Routes
import com.opotest.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun GameOverScreen(navController: NavController, gameViewModel: GameViewModel) {
    val uiState by gameViewModel.uiState.collectAsState()
    val newRecord by gameViewModel.newRecord.collectAsState()
    val newComboRecord by gameViewModel.newComboRecord.collectAsState()
    val newAccRecord by gameViewModel.newAccRecord.collectAsState()
    val xpGained by gameViewModel.xpGained.collectAsState()
    val medal by gameViewModel.medal.collectAsState()
    val accuracy by gameViewModel.accuracy.collectAsState()
    val chestReward by gameViewModel.chestReward.collectAsState()
    val rankUpOverlay by gameViewModel.rankUpOverlay.collectAsState()

    var displayScore by remember { mutableStateOf(0) }
    var chestOpened by remember { mutableStateOf(false) }

    // Score count-up animation
    LaunchedEffect(uiState.score) {
        val target = uiState.score
        val duration = 1500
        val steps = 60
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val eased = 1 - (1 - t) * (1 - t) * (1 - t)
            displayScore = (target * eased).toInt()
            delay((duration / steps).toLong())
        }
        displayScore = target
    }

    val anyRecord = newRecord || newComboRecord || newAccRecord
    val icon = if (anyRecord) "🏆" else medal.ifEmpty { "🎮" }
    val modeName = when (uiState.mode) {
        GameMode.SURVIVAL -> "❤️ Supervivencia"
        GameMode.TIMETRIAL -> "⏱️ Contrarreloj"
        GameMode.QUICK -> "⚡ Repaso Express"
        GameMode.CHALLENGE -> "🏆 Modo Reto"
    }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            Text(icon, fontSize = 56.sp)
            Text("Game Over", color = TextLight, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(modeName, color = TextMuted, fontSize = 14.sp)

            if (anyRecord) {
                Spacer(Modifier.height(12.dp))
                Text("¡NUEVO RÉCORD!", color = Warning, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (newRecord) Text("🏆 Puntuación", color = Warning, fontSize = 10.sp)
                    if (newComboRecord) Text("🔥 Combo x${uiState.maxCombo}", color = Warning, fontSize = 10.sp)
                    if (newAccRecord) Text("🎯 $accuracy% precisión", color = Warning, fontSize = 10.sp)
                }
            } else if (medal.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(medal, fontSize = 20.sp)
            }

            Spacer(Modifier.height(20.dp))
            Text("$displayScore", color = PrimaryLight, fontSize = 56.sp, fontWeight = FontWeight.Bold)
            Text("puntos", color = TextMuted, fontSize = 16.sp)

            if (xpGained > 0) {
                Spacer(Modifier.height(12.dp))
                Text("+$xpGained XP", color = AccentLight, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))

            // Stat cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("${uiState.totalAnswered}", "Preguntas", Modifier.weight(1f))
                StatCard("${uiState.maxCombo}", "Combo máx", Modifier.weight(1f))
                StatCard("$accuracy%", "Precisión", Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GameButton(
                    text = "🔄 Jugar",
                    modifier = Modifier.weight(1f).height(50.dp),
                    color1 = Success,
                    color2 = SuccessDark
                ) {
                    when (uiState.mode) {
                        GameMode.QUICK -> gameViewModel.startQuickGame()
                        GameMode.CHALLENGE -> gameViewModel.startChallengeGame()
                        else -> {
                            if (gameViewModel.engine.category.isNotEmpty())
                                gameViewModel.startTemaGame(gameViewModel.engine.category)
                            else
                                gameViewModel.startAllLawsGame()
                        }
                    }
                    navController.navigate(Routes.GAME) { popUpTo(Routes.GAME) { inclusive = true } }
                }
                GameButton(
                    text = "🏠 Menú",
                    modifier = Modifier.weight(1f).height(50.dp),
                    color1 = Primary,
                    color2 = PurpleDark
                ) {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            }
            Spacer(Modifier.height(40.dp))
        }

        // Chest overlay
        chestReward?.let { chest ->
            ChestOverlay(
                chest = chest,
                opened = chestOpened,
                onOpen = {
                    chestOpened = true
                    gameViewModel.openChest()
                },
                onDismiss = {
                    gameViewModel.clearChest()
                    chestOpened = false
                }
            )
        }

        // Rank-up overlay
        rankUpOverlay?.let { overlay ->
            RankUpOverlayView(overlay) {
                gameViewModel.clearRankUp()
            }
        }
    }
}

@Composable
fun ChestOverlay(chest: ChestReward, opened: Boolean, onOpen: () -> Unit, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(2500); visible = true }

    if (!visible) return

    val typeIcon = when (chest.type) { ChestType.WOOD -> "📦"; ChestType.SILVER -> "🗃️"; ChestType.GOLD -> "�" }
    val typeLabel = chest.type.label
    val typeColor = when (chest.type) { ChestType.WOOD -> TextMuted; ChestType.SILVER -> Color(0xFFcbd5e1); ChestType.GOLD -> Warning }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { if (opened) onDismiss() else onOpen() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(BgCard, BgCardDark)))
                .border(2.dp, typeColor, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!opened) {
                Text(typeIcon, fontSize = 64.sp)
                Spacer(Modifier.height(16.dp))
                Text("¡Toca para abrir!", color = Warning, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            } else {
                Text(typeIcon, fontSize = 32.sp)
                Spacer(Modifier.height(8.dp))
                Text(typeLabel, color = typeColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text("+${chest.xp} XP", color = AccentLight, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                if (chest.powerUps.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row {
                        chest.powerUps.forEach { pu ->
                            val puIcon = when (pu) {
                                "shield" -> "🛡️"; "fiftyFifty" -> "🎯"; "hint" -> "💡"
                                "lifeRecovery" -> "❤️"; "doubleScore" -> "✨"; "freezeTime" -> "🧊"
                                else -> "🎁"
                            }
                            Text(puIcon, fontSize = 28.sp)
                            Spacer(Modifier.width(16.dp))
                        }
                    }
                }
                if (chest.multiplier) {
                    Spacer(Modifier.height(12.dp))
                    Text("⚡ x2 XP en la próxima partida", color = Warning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                Text("Toca para continuar", color = TextDim, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun RankUpOverlayView(overlay: RankUpOverlay, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(500)
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(animationSpec = tween(600, easing = androidx.compose.animation.core.EaseOutBack)) + fadeIn(tween(400))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(BgCard, BgCardDark)))
                    .border(2.dp, Warning, RoundedCornerShape(20.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 56.sp)
                Spacer(Modifier.height(12.dp))
                Text(overlay.newRank.icon, fontSize = 64.sp)
                Spacer(Modifier.height(12.dp))
                Text("¡Has subido a ${overlay.newRank.name}!", color = Warning, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("${overlay.oldRank.icon} ${overlay.oldRank.name} → ${overlay.newRank.icon} ${overlay.newRank.name}", color = Color(0xFFfcd34d), fontSize = 15.sp)

                val unlockText = com.opotest.data.Constants.RANK_UNLOCKS[overlay.newRank.index]
                if (unlockText != null) {
                    Spacer(Modifier.height(20.dp))
                    Text("🔓 Has desbloqueado:", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            unlockText,
                            color = AccentLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                GameButton("Continuar", color1 = Primary, color2 = PurpleDark) { onDismiss() }
            }
        }
    }
}
