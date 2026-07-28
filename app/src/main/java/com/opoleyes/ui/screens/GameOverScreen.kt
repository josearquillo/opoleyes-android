package com.opoleyes.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.data.model.ChestReward
import com.opoleyes.data.model.ChestType
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.model.RankUpOverlay
import com.opoleyes.ui.components.*
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
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
    var confettiTrigger by remember { mutableStateOf<Any?>(null) }
    var chestShake by remember { mutableStateOf(0) }

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
        // Trigger confetti on new record or rank up
        if (newRecord || newComboRecord || newAccRecord) {
            delay(200)
            confettiTrigger = Any()
        }
    }

    // Chest shake animation before opening
    LaunchedEffect(chestReward) {
        if (chestReward != null && !chestOpened) {
            delay(2500)
            repeat(3) {
                chestShake++
                delay(300)
            }
        }
    }

    val anyRecord = newRecord || newComboRecord || newAccRecord
    val modeName = when (uiState.mode) {
        GameMode.SURVIVAL -> "Supervivencia"
        GameMode.TIMETRIAL -> "Contrarreloj"
        GameMode.QUICK -> "Repaso Express"
        GameMode.CHALLENGE -> "Modo Reto"
        GameMode.EXAM -> "Modo Examen"
    }

    // Staggered appearance
    var visibleItems by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (i in 1..8) {
            delay(100)
            visibleItems = i
        }
    }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Fin de partida", color = TextLight, fontWeight = FontWeight.Bold) },
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(Modifier.height(20.dp))

            // Icon with scale-in animation
            val iconScale by animateFloatAsState(
                targetValue = if (visibleItems > 0) 1f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "iconScale"
            )
            Icon(
                if (anyRecord) Icons.Default.EmojiEvents else Icons.Default.Assignment,
                contentDescription = null,
                tint = if (anyRecord) Warning else TextLight,
                modifier = Modifier.size(56.dp).scale(iconScale)
            )

            Text("Fin de partida", color = TextLight, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(modeName, color = TextMuted, fontSize = 14.sp)

            if (anyRecord) {
                Spacer(Modifier.height(12.dp))
                Text("¡NUEVO RÉCORD!", color = Warning, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (newRecord) Text("Puntuación", color = Warning, fontSize = 10.sp)
                    if (newComboRecord) Text("Combo x${uiState.maxCombo}", color = Warning, fontSize = 10.sp)
                    if (newAccRecord) Text("$accuracy% precisión", color = Warning, fontSize = 10.sp)
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

            // Stat cards with icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCardWithIcon(Icons.Default.Assignment, "${uiState.totalAnswered}", "Preguntas", PrimaryLight, Modifier.weight(1f))
                StatCardWithIcon(Icons.Default.LocalFireDepartment, "${uiState.maxCombo}", "Combo máx", Orange, Modifier.weight(1f))
                StatCardWithIcon(Icons.Default.TrackChanges, "$accuracy%", "Precisión", Success, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GameButton(
                    text = "Jugar",
                    modifier = Modifier.weight(1f).height(50.dp),
                    color1 = Success,
                    color2 = SuccessDark
                ) {
                    gameViewModel.clearChest()
                    gameViewModel.clearRankUp()
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
                    gameViewModel.nextQuestion()
                    navController.navigate(Routes.GAME) { popUpTo(Routes.GAME) { inclusive = true } }
                }
                GameButton(
                    text = "Menú",
                    modifier = Modifier.weight(1f).height(50.dp),
                    color1 = Primary,
                    color2 = PurpleDark
                ) {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            }
            Spacer(Modifier.height(40.dp))
            }
        }

        // Confetti overlay
        ConfettiBurst(
            trigger = confettiTrigger,
            modifier = Modifier.fillMaxSize()
        )

        // Chest overlay
        chestReward?.let { chest ->
            ChestOverlay(
                chest = chest,
                opened = chestOpened,
                shakeCount = chestShake,
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
fun ChestOverlay(chest: ChestReward, opened: Boolean, shakeCount: Int, onOpen: () -> Unit, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(2500); visible = true }

    if (!visible) return

    val typeIcon = when (chest.type) { ChestType.WOOD -> Icons.Default.Inventory2; ChestType.SILVER -> Icons.Default.Redeem; ChestType.GOLD -> Icons.Default.Redeem }
    val typeLabel = chest.type.label
    val typeColor = when (chest.type) { ChestType.WOOD -> TextMuted; ChestType.SILVER -> Color(0xFFcbd5e1); ChestType.GOLD -> Warning }

    // Shake animation for chest
    val chestShakeAnim = remember { Animatable(0f) }
    LaunchedEffect(shakeCount) {
        if (shakeCount > 0 && !opened) {
            chestShakeAnim.snapTo(0f)
            chestShakeAnim.animateTo(1f, animationSpec = keyframes {
                durationMillis = 300
                0f at 0 with LinearEasing
                8f at 50 with LinearEasing
                -8f at 100 with LinearEasing
                6f at 150 with LinearEasing
                -6f at 200 with LinearEasing
                0f at 300 with LinearEasing
            })
        }
    }

    // Glow on open
    val openGlow by animateFloatAsState(
        targetValue = if (opened) 1f else 0f,
        animationSpec = tween(400),
        label = "openGlow"
    )

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
                .shadow((openGlow * 20).dp, RoundedCornerShape(16.dp), clip = false, ambientColor = typeColor.copy(alpha = openGlow * 0.6f), spotColor = typeColor.copy(alpha = openGlow * 0.8f))
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(BgCard, BgCardDark)))
                .border(2.dp, typeColor, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!opened) {
                Box(modifier = Modifier.offset { IntOffset(chestShakeAnim.value.toInt(), 0) }) {
                    Icon(typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(64.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("¡Toca para abrir!", color = Warning, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            } else {
                // Scale-in for opened content
                val contentScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "contentScale"
                )
                Box(modifier = Modifier.scale(contentScale)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(typeLabel, color = typeColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("+${chest.xp} XP", color = AccentLight, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        if (chest.powerUps.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Row {
                                chest.powerUps.forEach { pu ->
                                    val puIcon = when (pu) {
                                        "shield" -> Icons.Default.Shield; "fiftyFifty" -> Icons.Default.SwapHoriz; "hint" -> Icons.Default.Lightbulb
                                        "lifeRecovery" -> Icons.Default.Favorite; "doubleScore" -> Icons.Default.AutoAwesome
                                        else -> Icons.Default.Redeem
                                    }
                                    Icon(puIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(28.dp))
                                    Spacer(Modifier.width(16.dp))
                                }
                            }
                        }
                        if (chest.multiplier) {
                            Spacer(Modifier.height(12.dp))
                            Text("x2 XP en la próxima partida", color = Warning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
    var rankConfetti by remember { mutableStateOf<Any?>(null) }
    LaunchedEffect(Unit) {
        delay(500)
        visible = true
        delay(300)
        rankConfetti = Any()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        ConfettiBurst(trigger = rankConfetti, modifier = Modifier.fillMaxSize(), durationMs = 2500)

        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(animationSpec = tween(600, easing = EaseOutBack)) + fadeIn(tween(400))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .shadow(16.dp, RoundedCornerShape(20.dp), clip = false, ambientColor = Warning.copy(alpha = 0.4f), spotColor = Warning.copy(alpha = 0.6f))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(BgCard, BgCardDark)))
                    .border(2.dp, Warning, RoundedCornerShape(20.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Celebration, contentDescription = null, tint = Warning, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text(overlay.newRank.icon, fontSize = 64.sp)
                Spacer(Modifier.height(12.dp))
                Text("¡Has subido a ${overlay.newRank.name}!", color = Warning, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("${overlay.oldRank.icon} ${overlay.oldRank.name} → ${overlay.newRank.icon} ${overlay.newRank.name}", color = Color(0xFFfcd34d), fontSize = 15.sp)

                val unlockText = com.opoleyes.data.Constants.RANK_UNLOCKS[overlay.newRank.index]
                if (unlockText != null) {
                    Spacer(Modifier.height(20.dp))
                    Text("Has desbloqueado:", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
