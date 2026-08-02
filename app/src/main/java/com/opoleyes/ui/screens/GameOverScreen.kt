package com.opoleyes.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.R
import com.opoleyes.data.model.ChestReward
import com.opoleyes.data.model.ChestType
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.model.RankUpOverlay
import com.opoleyes.ui.components.*
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.dotlottie.dlplayer.Mode
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
    val quickRewardEarned by gameViewModel.quickRewardEarned.collectAsState()
    val quickRewardPowerUp by gameViewModel.quickRewardPowerUp.collectAsState()

    var displayScore by remember { mutableStateOf(0) }
    var chestOpened by remember { mutableStateOf(false) }
    var chestVisible by remember { mutableStateOf(false) }
    var confettiTrigger by remember { mutableStateOf<Any?>(null) }
    var chestShake by remember { mutableStateOf(0) }

    val chestBlocking = chestReward != null && !chestOpened

    // Show chest popup after 2s delay
    LaunchedEffect(chestReward) {
        if (chestReward != null) {
            chestVisible = false
            chestOpened = false
            delay(2000)
            chestVisible = true
            delay(800)
            repeat(3) {
                chestShake++
                delay(300)
            }
        }
    }

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

    val anyRecord = newRecord || newComboRecord || newAccRecord
    val modeName = when (uiState.mode) {
        GameMode.SURVIVAL -> stringResource(R.string.mode_survival)
        GameMode.TIMETRIAL -> stringResource(R.string.mode_timetrial)
        GameMode.QUICK -> stringResource(R.string.mode_quick)
        GameMode.EXAM -> stringResource(R.string.mode_exam)
        GameMode.SIMULACRO -> stringResource(R.string.mode_simulacro)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
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
                if (anyRecord) Icons.Default.EmojiEvents else Icons.AutoMirrored.Filled.Assignment,
                contentDescription = null,
                tint = if (anyRecord) Warning else TextLight,
                modifier = Modifier.size(56.dp).scale(iconScale)
            )

            Text(stringResource(R.string.game_over), color = TextLight, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(modeName, color = TextMuted, fontSize = 14.sp)

            if (anyRecord) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.new_record), color = Warning, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (newRecord) Text(stringResource(R.string.score_label), color = Warning, fontSize = 10.sp)
                    if (newComboRecord) Text("Combo x${uiState.maxCombo}", color = Warning, fontSize = 10.sp)
                    if (newAccRecord) Text("$accuracy% ${stringResource(R.string.accuracy_label)}", color = Warning, fontSize = 10.sp)
                }
            } else if (medal.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(medal, fontSize = 20.sp)
            }

            Spacer(Modifier.height(20.dp))
            Text("$displayScore", color = Accent, fontSize = 56.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.points), color = TextMuted, fontSize = 16.sp)

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
                StatCardWithIcon(Icons.AutoMirrored.Filled.Assignment, "${uiState.totalAnswered}", stringResource(R.string.questions_label), PrimaryLight, Modifier.weight(1f))
                StatCardWithIcon(Icons.Default.LocalFireDepartment, "${uiState.maxCombo}", stringResource(R.string.max_combo_label), Orange, Modifier.weight(1f))
                StatCardWithIcon(Icons.Default.TrackChanges, "$accuracy%", stringResource(R.string.accuracy_label), Success, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Quick reward earned banner
            if (quickRewardEarned && uiState.mode == GameMode.QUICK) {
                val (emoji, label) = when (quickRewardPowerUp) {
                    "shield" -> "🛡️" to stringResource(R.string.shield)
                    "hint" -> "💡" to stringResource(R.string.hint)
                    "fiftyFifty" -> "✂️" to stringResource(R.string.fifty_fifty)
                    "doubleScore" -> "✨" to stringResource(R.string.double_points)
                    else -> "🎁" to "Power-up"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(Success, SuccessDark)))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.quick_reward_earned), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("+1 $label", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GameButton(
                    text = stringResource(R.string.play_again),
                    enabled = !chestBlocking,
                    modifier = Modifier.weight(1f).height(50.dp).alpha(if (chestBlocking) 0.4f else 1f),
                    color1 = Success,
                    color2 = SuccessDark
                ) {
                    gameViewModel.clearChest()
                    gameViewModel.clearRankUp()
                    gameViewModel.clearQuickReward()
                    when (uiState.mode) {
                        GameMode.QUICK -> gameViewModel.startQuickGame()
                        else -> {
                            val category = gameViewModel.getCategory()
                            if (category.isNotEmpty())
                                gameViewModel.startTemaGame(category)
                            else
                                gameViewModel.startAllLawsGame()
                        }
                    }
                    navController.navigate(Routes.GAME) { popUpTo(Routes.GAME) { inclusive = true } }
                }
                GameButton(
                    text = stringResource(R.string.menu),
                    enabled = !chestBlocking,
                    modifier = Modifier.weight(1f).height(50.dp).alpha(if (chestBlocking) 0.4f else 1f),
                    color1 = Primary,
                    color2 = PurpleDark
                ) {
                    gameViewModel.clearQuickReward()
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            }
            Spacer(Modifier.height(40.dp))
            }

        // Confetti overlay
        ConfettiBurst(
            trigger = confettiTrigger,
            modifier = Modifier.fillMaxSize()
        )

        // Chest overlay
        chestReward?.let { chest ->
            if (chestVisible) {
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
                        chestVisible = false
                    }
                )
            }
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
    val typeLabel = chest.type.label
    val typeColor = when (chest.type) { ChestType.BRONZE -> Warning; ChestType.SILVER -> Silver; ChestType.GOLD -> Accent }
    val lottieAsset = when (chest.type) { ChestType.BRONZE -> "gift_bronze.json"; ChestType.SILVER -> "gift_silver.json"; ChestType.GOLD -> "gift_gold.json" }

    // Shake animation for chest
    val chestShakeAnim = remember { Animatable(0f) }
    LaunchedEffect(shakeCount) {
        if (shakeCount > 0 && !opened) {
            chestShakeAnim.snapTo(0f)
            @Suppress("DEPRECATION")
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
            .background(Scrim)
            .clickable { if (opened) onDismiss() else onOpen() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow((openGlow * 20).dp, RoundedCornerShape(16.dp), clip = false, ambientColor = typeColor.copy(alpha = openGlow * 0.6f), spotColor = typeColor.copy(alpha = openGlow * 0.8f))
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(BgCard, BgDark)))
                .border(2.dp, typeColor, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!opened) {
                Box(modifier = Modifier.offset { IntOffset(chestShakeAnim.value.toInt(), 0) }) {
                    DotLottieAnimation(
                        source = DotLottieSource.Asset(lottieAsset),
                        autoplay = true,
                        loop = true,
                        speed = 1f,
                        useFrameInterpolation = false,
                        playMode = Mode.FORWARD,
                        modifier = Modifier.size(120.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.tap_to_open), color = Warning, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            } else {
                // Scale-in for opened content
                val contentScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "contentScale"
                )
                Box(modifier = Modifier.scale(contentScale)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DotLottieAnimation(
                            source = DotLottieSource.Asset(lottieAsset),
                            autoplay = true,
                            loop = false,
                            speed = 1f,
                            useFrameInterpolation = false,
                            playMode = Mode.FORWARD,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(typeLabel, color = typeColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("+${chest.xp} XP", color = AccentLight, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        if (chest.powerUps.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Row {
                                chest.powerUps.forEach { pu ->
                                    val puEmoji = when (pu) {
                                        "shield" -> "🛡️"; "fiftyFifty" -> "✂️"; "hint" -> "💡"
                                        "lifeRecovery" -> "❤️"; "doubleScore" -> "✨"
                                        else -> "🎁"
                                    }
                                    Text(puEmoji, fontSize = 24.sp)
                                    Spacer(Modifier.width(16.dp))
                                }
                            }
                        }
                        if (chest.multiplier) {
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.next_xp_double), color = Warning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.tap_to_continue), color = TextDim, fontSize = 13.sp)
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
            .background(ScrimHeavy)
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
                    .background(Brush.verticalGradient(listOf(BgCard, BgDark)))
                    .border(2.dp, Warning, RoundedCornerShape(20.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Celebration, contentDescription = null, tint = Warning, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text(overlay.newRank.icon, fontSize = 64.sp)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.rank_up_message, overlay.newRank.name), color = Warning, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("${overlay.oldRank.icon} ${overlay.oldRank.name} → ${overlay.newRank.icon} ${overlay.newRank.name}", color = AccentLight, fontSize = 15.sp)

                val unlockText = com.opoleyes.data.Constants.RANK_UNLOCKS[overlay.newRank.index]
                if (unlockText != null) {
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.unlocked_label), color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TrackColorDim
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

                if (overlay.powerUpRewards.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.powerups_received), color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    val powerUpIcons = mapOf(
                        "shield" to "🛡️ Escudo",
                        "hint" to "💡 Pista",
                        "fiftyFifty" to "🎯 50/50",
                        "doubleScore" to "✨ x2 pts"
                    )
                    val counts = overlay.powerUpRewards.groupingBy { it }.eachCount()
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        counts.forEach { (id, count) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TrackColorDim
                            ) {
                                Text(
                                    "${powerUpIcons[id] ?: id} ${if (count > 1) "x$count" else ""}",
                                    color = TextLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                GameButton(stringResource(R.string.continue_label), color1 = Primary, color2 = PurpleDark) { onDismiss() }
            }
        }
    }
}
