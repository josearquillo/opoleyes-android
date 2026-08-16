package com.opoleyes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.opoleyes.ui.rememberHaptics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.opoleyes.R
import com.opoleyes.data.Constants
import com.opoleyes.data.model.GameMode
import com.opoleyes.ui.components.*
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameScreen(navController: NavController, gameViewModel: GameViewModel) {
    val uiState by gameViewModel.uiState.collectAsState()
    val popups by gameViewModel.popups.collectAsState()
    val powerUpToast by gameViewModel.powerUpToast.collectAsState()
    val toasts by gameViewModel.toasts.collectAsState()

    val q = uiState.currentQ
    if (q == null) {
        // Guard against duplicate navigation on recomposition
        var navigated by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!navigated) {
                navigated = true
                navController.navigate(Routes.GAME_OVER)
            }
        }
        return
    }

    var showExitDialog by remember { mutableStateOf(false) }
    var autoAdvanceTriggered by remember { mutableStateOf(false) }
    var particleTrigger by remember { mutableStateOf<Any?>(null) }
    var shakeTrigger by remember { mutableStateOf<Any?>(null) }

    BackHandler { showExitDialog = true }

    val haptics = rememberHaptics()

    // Question slide animation
    var questionVisible by remember { mutableStateOf(false) }
    LaunchedEffect(q) {
        questionVisible = false
        delay(50)
        questionVisible = true
    }

    // Particle/shake on answer
    LaunchedEffect(uiState.answered, uiState.selectedOption) {
        if (uiState.answered && uiState.selectedOption != null) {
            if (uiState.selectedOption == q.correct) {
                particleTrigger = Any()
                haptics.correct()
            } else {
                shakeTrigger = Any()
                haptics.wrong()
            }
        }
    }

    // Track background time for timer adjustment
    var pausedAtMs by remember { mutableStateOf(0L) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    pausedAtMs = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (pausedAtMs > 0L) {
                        val elapsed = (System.currentTimeMillis() - pausedAtMs) / 1000f
                        if (gameViewModel.isTimedMode()) {
                            if (gameViewModel.applyPausedElapsed(elapsed)) {
                                gameViewModel.onGameOver()
                                navController.navigate(Routes.GAME_OVER) {
                                    popUpTo(Routes.GAME) { inclusive = true }
                                }
                            }
                        }
                        pausedAtMs = 0L
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Timer countdown (pauses while exit dialog is open)
    LaunchedEffect(uiState.mode, uiState.timer, uiState.answered, showExitDialog) {
        if (uiState.mode == GameMode.TIMETRIAL && !uiState.answered && uiState.timer > 0 && !showExitDialog) {
            delay(1000)
            if (gameViewModel.tickTimer()) {
                gameViewModel.onGameOver()
                navController.navigate(Routes.GAME_OVER) {
                    popUpTo(Routes.GAME) { inclusive = true }
                }
            }
        }
    }

    // Auto-advance after answer
    LaunchedEffect(uiState.answered) {
        if (uiState.answered && !autoAdvanceTriggered) {
            autoAdvanceTriggered = true
            val delayMs = if (uiState.mode == GameMode.QUICK && uiState.selectedOption != q.correct) 3500L else 2000L
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
            containerColor = BgCard,
            title = { Text(stringResource(R.string.back_to_menu)) },
            text = { Text(stringResource(R.string.lose_progress)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    gameViewModel.exitGame()
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }) { Text(stringResource(R.string.exit)) }
            },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text(stringResource(R.string.cancel)) } }
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
                    GameMode.QUICK -> Constants.QUICK_MODE_QUESTIONS
                    else -> 0
                }
                if (totalQ > 0) {
                    val progressFrac = uiState.questionNum.toFloat() / totalQ.toFloat()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(TrackColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFrac.coerceIn(0f, 1f))
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)))
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }

                AnimatedHudBar(
                    score = uiState.score,
                    lives = uiState.lives,
                    timer = uiState.timer,
                    mode = uiState.mode,
                    maxLives = gameViewModel.engine.maxLives
                )

                // Combo bar (bottom only, no fire icon up top)
                if (uiState.combo > 0) {
                    Spacer(Modifier.height(4.dp))
                    ComboBar(
                        fill = if (uiState.comboOverchargeActive) 1f else uiState.comboBarFill,
                        overchargeActive = uiState.comboOverchargeActive,
                        overchargeCharges = uiState.comboOverchargeCharges,
                        combo = uiState.combo,
                        streak = uiState.streak,
                        streakThreshold = Constants.STREAK_RECOVERY_THRESHOLD_BY_RANK[gameViewModel.getEngineRankIndex()] ?: 5
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
                                        .background(Brush.verticalGradient(listOf(BgCard, BgDark)))
                                        .padding(16.dp)
                                ) {
                                    Text(q.enunciado, color = TextLight, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(Modifier.height(8.dp))

                                // Power-up buttons (always visible to prevent layout shift on answer)
                                val availablePowerUps = gameViewModel.engine.availablePowerUps
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if ("hint" in availablePowerUps && uiState.mode != GameMode.QUICK) {
                                        PowerUpButton(stringResource(R.string.hint), "💡", 0, WarningDark, enabled = !uiState.answered && !uiState.hintActive && !uiState.powerUpUsedThisQuestion) { gameViewModel.useHint() }
                                    }
                                    if ("fiftyFifty" in availablePowerUps && uiState.mode != GameMode.QUICK) {
                                        PowerUpButton(stringResource(R.string.fifty_fifty), "✂️", 0, Primary, enabled = !uiState.answered && !uiState.fiftyFiftyActive && !uiState.powerUpUsedThisQuestion) { gameViewModel.activateFiftyFifty() }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))

                                // Options in original A-B-C-D order (never shuffled
                                // to preserve referential answers like "Todas las
                                // anteriores"). Always show all 4 options.
                                val allLetters = listOf("A", "B", "C", "D")
                                val presentLetters = remember(q) { allLetters.filter { q.opciones[it] != null } }
                                val removedByFiftyFifty = uiState.fiftyFiftyActive && uiState.fiftyFiftyRemoved.isNotEmpty()
                                presentLetters.forEachIndexed { index, letter ->
                                    key(letter) {
                                        val text = q.opciones[letter]!!
                                        val isFiftyFiftyRemoved =
                                            removedByFiftyFifty && uiState.fiftyFiftyRemoved.contains(letter) && letter != q.correct

                                        var enterVisible by remember(q, letter) { mutableStateOf(false) }
                                        LaunchedEffect(q) {
                                            delay(index * 80L)
                                            enterVisible = true
                                        }
                                        AnimatedVisibility(
                                            visible = enterVisible && !isFiftyFiftyRemoved,
                                            enter = fadeIn(tween(200)) + slideInVertically(
                                                animationSpec = tween(250, easing = FastOutSlowInEasing),
                                                initialOffsetY = { it / 4 }
                                            ),
                                            exit = fadeOut(tween(200)) + slideOutHorizontally(
                                                animationSpec = tween(200),
                                                targetOffsetX = { -it / 2 }
                                            )
                                        ) {
                                            Column {
                                                OptionCard(
                                                    letter = letter,
                                                    text = text,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    isCorrect = letter == q.correct,
                                                    isSelected = uiState.selectedOption == letter,
                                                    isWrong = uiState.answered && uiState.selectedOption == letter && letter != q.correct,
                                                    isHintRemoved = uiState.hintActive && uiState.hintRemoved.contains(letter),
                                                    answered = uiState.answered,
                                                    userWasCorrect = uiState.answered && uiState.selectedOption == q.correct,
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
                        AnimatedVisibility(
                            visible = visible,
                            enter = slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                initialOffsetY = { it / 2 }
                            ) + fadeIn(tween(200)),
                            exit = fadeOut(tween(400)) + slideOutVertically(
                                animationSpec = tween(400, easing = FastOutSlowInEasing),
                                targetOffsetY = { -it / 3 }
                            )
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ScrimStrong,
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                popup.color.copy(alpha = 0.3f),
                                                ScrimStrong,
                                                popup.color.copy(alpha = 0.3f)
                                            )
                                        )
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (popup.icon.isNotEmpty()) {
                                        Text(popup.icon, fontSize = (popup.size * 0.7f).sp)
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(
                                        popup.text,
                                        color = popup.color,
                                        fontSize = popup.size.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Power-up toast
            if (powerUpToast != null) {
                var toastVisible by remember { mutableStateOf(false) }
                LaunchedEffect(powerUpToast) {
                    toastVisible = powerUpToast != null
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    AnimatedVisibility(
                        visible = toastVisible,
                        enter = slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            initialOffsetY = { -it }
                        ) + fadeIn(tween(200)),
                        exit = fadeOut(tween(400)) + slideOutVertically(
                            animationSpec = tween(400),
                            targetOffsetY = { -it }
                        )
                    ) {
                        Surface(
                            modifier = Modifier.padding(top = 60.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = ScrimStrong,
                            shadowElevation = 8.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(SuccessDark, Success.copy(alpha = 0.3f), SuccessDark)
                                        )
                                    )
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(powerUpToast!!.icon, fontSize = 22.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(powerUpToast!!.text, color = Success, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Achievement toasts — shown at the top, overlaying the HUD area
            // (less important during gameplay than the answers at the bottom).
            if (toasts.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                    ) {
                        toasts.takeLast(3).forEach { ach ->
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(ach) {
                                delay(50)
                                visible = true
                            }
                            AnimatedVisibility(
                                visible = visible,
                                enter = slideInVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    initialOffsetY = { -it / 2 }
                                ) + fadeIn(tween(200)),
                                exit = fadeOut(tween(400)) + slideOutVertically(
                                    animationSpec = tween(400, easing = FastOutSlowInEasing),
                                    targetOffsetY = { -it / 3 }
                                )
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = BgCard,
                                    shadowElevation = 8.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(ach.icon, fontSize = 22.sp)
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(stringResource(R.string.achievement_unlocked), color = Accent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            Text(ach.name, color = Warning, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(ach.desc, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                        LaunchedEffect(toasts) {
                            delay(4000)
                            gameViewModel.clearToasts()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PowerUpButton(text: String, icon: String, charges: Int, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pressScale"
    )
    // Reset pressed state after the press animation so the button doesn't stay shrunk
    LaunchedEffect(pressed) {
        if (pressed) {
            delay(150)
            pressed = false
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val alpha = if (enabled) 1f else 0.3f

    Box(
        modifier = Modifier
            .scale(scale)
            .width(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(color.copy(alpha = alpha * 0.5f), color.copy(alpha = alpha * 0.25f))))
            .border(1.5.dp, if (enabled) color.copy(alpha = pulseAlpha) else color.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) {
                pressed = true
                onClick()
            }
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        // Glow shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = alpha * 0.15f))
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 22.sp, color = Color.White.copy(alpha = alpha))
            Spacer(Modifier.height(2.dp))
            Text(text, color = Color.White.copy(alpha = alpha), fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
        // Circular badge for charges (hidden when 0 = unlimited)
        if (charges > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha))
                    .border(1.dp, color.copy(alpha = alpha), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$charges", color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    LaunchedEffect(pressed) {
        if (pressed) {
            delay(150)
            pressed = false
        }
    }
}
