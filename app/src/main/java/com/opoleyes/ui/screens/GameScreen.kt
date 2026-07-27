package com.opoleyes.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.data.model.GameMode
import com.opoleyes.ui.components.*
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun GameScreen(navController: NavController, gameViewModel: GameViewModel) {
    val uiState by gameViewModel.uiState.collectAsState()
    val popups by gameViewModel.popups.collectAsState()
    val powerUpToast by gameViewModel.powerUpToast.collectAsState()
    val toasts by gameViewModel.toasts.collectAsState()

    val q = uiState.currentQ
    if (q == null) {
        LaunchedEffect(Unit) { navController.navigate(Routes.GAME_OVER) }
        return
    }

    var showExitDialog by remember { mutableStateOf(false) }
    var autoAdvanceTriggered by remember { mutableStateOf(false) }
    var questionKey by remember { mutableStateOf(0) }
    var particleTrigger by remember { mutableStateOf<Any?>(null) }
    var shakeTrigger by remember { mutableStateOf<Any?>(null) }
    var comboScale by remember { mutableStateOf(1f) }

    BackHandler { showExitDialog = true }

    // Question slide animation
    var questionVisible by remember { mutableStateOf(false) }
    LaunchedEffect(q) {
        questionVisible = false
        delay(50)
        questionVisible = true
        questionKey++
    }

    // Combo bounce animation
    LaunchedEffect(uiState.combo) {
        if (uiState.combo > 0) {
            comboScale = 1.3f
            delay(100)
            comboScale = 1f
        }
    }

    // Particle/shake on answer
    LaunchedEffect(uiState.answered, uiState.selectedOption) {
        if (uiState.answered && uiState.selectedOption != null) {
            if (uiState.selectedOption == q.correct) {
                particleTrigger = Any()
            } else {
                shakeTrigger = Any()
            }
        }
    }

    // Timer countdown
    LaunchedEffect(uiState.mode, uiState.timer) {
        if ((uiState.mode == GameMode.TIMETRIAL || uiState.mode == GameMode.CHALLENGE) && !uiState.answered && uiState.timer > 0) {
            if (!gameViewModel.engine.freezeActive) {
                delay(1000)
                gameViewModel.engine.timer = (gameViewModel.engine.timer - 1f).coerceAtLeast(0f)
                if (gameViewModel.engine.timer <= 0) {
                    gameViewModel.onGameOver()
                    navController.navigate(Routes.GAME_OVER) {
                        popUpTo(Routes.GAME) { inclusive = true }
                    }
                }
                gameViewModel.updateUiState()
            }
        }
    }

    // Auto-advance after answer
    LaunchedEffect(uiState.answered) {
        if (uiState.answered && !autoAdvanceTriggered) {
            autoAdvanceTriggered = true
            val delayMs = if (uiState.mode == GameMode.QUICK && uiState.selectedOption != q.correct) 3500L else 1500L
            delay(delayMs)
            if (gameViewModel.isGameOver()) {
                gameViewModel.onGameOver()
                navController.navigate(Routes.GAME_OVER) {
                    popUpTo(Routes.GAME) { inclusive = true }
                }
            } else {
                gameViewModel.nextQuestion()
                autoAdvanceTriggered = false
            }
        }
    }

    // Auto-dismiss power-up toast
    LaunchedEffect(powerUpToast) {
        if (powerUpToast != null) {
            delay(4000)
            gameViewModel.clearPowerUpToast()
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Volver al menú?") },
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

    ShakeBox(
        shakeTrigger = shakeTrigger,
        intensity = 10f,
        modifier = Modifier.fillMaxSize().background(BgDark)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Spacer(Modifier.height(4.dp))

                // Progress bar for questions (only for modes with fixed total)
                val totalQ = when (uiState.mode) {
                    GameMode.QUICK -> 20
                    GameMode.CHALLENGE -> 15
                    else -> 0
                }
                if (totalQ > 0) {
                    val progressFrac = uiState.questionNum.toFloat() / totalQ.toFloat()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFrac.coerceIn(0f, 1f))
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Brush.horizontalGradient(listOf(Primary, Accent)))
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }

                HudBar(
                    score = uiState.score,
                    combo = uiState.combo,
                    lives = uiState.lives,
                    timer = uiState.timer,
                    mode = uiState.mode,
                    questionNum = uiState.questionNum,
                    shieldCharges = uiState.shieldCharges,
                    freezeActive = uiState.freezeActive
                )

                // Combo with bounce effect
                if (uiState.combo > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val comboColor = when {
                            uiState.combo >= 20 -> Warning
                            uiState.combo >= 10 -> Danger
                            else -> Orange
                        }
                        Text(
                            "🔥 x${uiState.combo}",
                            color = comboColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.scale(comboScale)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    ComboBar(
                        fill = if (uiState.comboOverchargeActive) 1f else uiState.comboBarFill,
                        overchargeActive = uiState.comboOverchargeActive,
                        overchargeCharges = uiState.comboOverchargeCharges
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Question card with slide animation
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    Column {
                        AnimatedVisibility(
                            visible = questionVisible,
                            enter = slideInHorizontally(
                                animationSpec = tween(350, easing = FastOutSlowInEasing),
                                initialOffsetX = { it / 4 }
                            ) + fadeIn(tween(300))
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.verticalGradient(listOf(BgCard, BgCardDark)))
                                        .padding(20.dp)
                                ) {
                                    Text(q.enunciado, color = TextLight, fontSize = 17.sp)
                                }

                                Spacer(Modifier.height(12.dp))

                                // Power-up buttons
                                if (!uiState.answered) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        if (uiState.hintCharges > 0 && !uiState.hintActive && uiState.mode != GameMode.CHALLENGE && uiState.mode != GameMode.QUICK) {
                                            PowerUpButton("💡 Pista", "x${uiState.hintCharges}", Color(0xFFa16207)) { gameViewModel.useHint() }
                                        }
                                        if (uiState.fiftyFiftyCharges > 0 && !uiState.fiftyFiftyActive && uiState.mode != GameMode.CHALLENGE && uiState.mode != GameMode.QUICK) {
                                            PowerUpButton("🎯 50/50", "x${uiState.fiftyFiftyCharges}", Purple) { gameViewModel.activateFiftyFifty() }
                                        }
                                        if (uiState.freezeCharges > 0 && !uiState.freezeActive && uiState.mode == GameMode.TIMETRIAL) {
                                            PowerUpButton("🧊 Congelar", "x${uiState.freezeCharges}", Cyan) { gameViewModel.activateFreeze() }
                                        }
                                        if (uiState.doubleScoreCharges > 0 && !uiState.doubleScoreActive && uiState.mode != GameMode.CHALLENGE && uiState.mode != GameMode.QUICK) {
                                            PowerUpButton("✨ x2 pts", "x${uiState.doubleScoreCharges}", Warning) { gameViewModel.activateDoubleScore() }
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                }

                                // Options
                                val letters = listOf("A", "B", "C", "D")
                                letters.forEachIndexed { index, letter ->
                                    val text = q.opciones[letter] ?: return@forEachIndexed
                                    val isFiftyFiftyRemoved = uiState.fiftyFiftyActive && uiState.fiftyFiftyRemoved.contains(letter)
                                    if (isFiftyFiftyRemoved) return@forEachIndexed

                                    var visible by remember(q, letter) { mutableStateOf(false) }
                                    LaunchedEffect(q) {
                                        delay(index * 80L)
                                        visible = true
                                    }
                                    AnimatedVisibility(visible = visible) {
                                        OptionCard(
                                            letter = letter,
                                            text = text,
                                            modifier = Modifier.fillMaxWidth(),
                                            isCorrect = letter == q.correct,
                                            isSelected = uiState.selectedOption == letter,
                                            isWrong = uiState.answered && uiState.selectedOption == letter && letter != q.correct,
                                            isHintRemoved = uiState.hintActive && uiState.hintRemoved.contains(letter),
                                            answered = uiState.answered,
                                            enabled = !uiState.answered
                                        ) {
                                            gameViewModel.answer(letter)
                                        }
                                        Spacer(Modifier.height(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Particle burst overlay on correct answer
            ParticleBurst(
                trigger = particleTrigger,
                color = Success,
                particleCount = 24,
                modifier = Modifier.fillMaxSize()
            )

            // Floating popups overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    popups.forEach { popup ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(popup) {
                            delay((popup.delay * 1000).toLong())
                            visible = true
                        }
                        AnimatedVisibility(visible = visible) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    popup.text,
                                    color = popup.color,
                                    fontSize = popup.size.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Power-up toast
            if (powerUpToast != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        modifier = Modifier.padding(top = 60.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = SuccessDark
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(powerUpToast!!.icon, fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(powerUpToast!!.text, color = Success, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // Achievement toasts
            if (toasts.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                    ) {
                        toasts.takeLast(3).forEach { ach ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = BgCard
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ach.icon, fontSize = 24.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(ach.name, color = Warning, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(ach.desc, color = TextMuted, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        LaunchedEffect(toasts) {
                            delay(3000)
                            gameViewModel.clearToasts()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PowerUpButton(text: String, charges: String, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.6f))))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(charges, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
    }
}
