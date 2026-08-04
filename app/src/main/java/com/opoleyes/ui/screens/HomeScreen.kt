package com.opoleyes.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.R
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.model.Mission
import com.opoleyes.data.model.MissionDifficulty
import com.opoleyes.ui.components.*
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, gameViewModel: GameViewModel) {
    val isLoading by gameViewModel.isLoading.collectAsState()
    val isDebugMode = gameViewModel.isDebugMode()

    // Data is precomputed off the main thread during the loading screen.
    // Fallback: compute synchronously (idempotent) if not yet available.
    val preload = gameViewModel.homePreload ?: remember {
        gameViewModel.preloadHomeData()
        gameViewModel.homePreload
    } ?: return
    val rank = preload.rank
    val xpProgress = preload.xpProgress
    val missions = preload.missions
    val totalCorrect = preload.totalCorrect
    val totalWrong = preload.totalWrong
    val maxCombo = preload.maxCombo
    val hasStats = totalCorrect + totalWrong > 0
    val accuracy = if (hasStats) totalCorrect * 100 / (totalCorrect + totalWrong) else 0

    val scrollState = rememberScrollState()

    // Animated XP bar
    val xpAnim = remember { Animatable(0f) }
    LaunchedEffect(xpProgress.pct) {
        xpAnim.animateTo(xpProgress.pct / 100f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    // Animated stats counters
    val accuracyAnim = remember { Animatable(0f) }
    val correctAnim = remember { Animatable(0f) }
    val comboAnim = remember { Animatable(0f) }
    LaunchedEffect(hasStats) {
        if (hasStats) {
            delay(400)
            accuracyAnim.animateTo(accuracy.toFloat(), animationSpec = tween(600, easing = FastOutSlowInEasing))
            correctAnim.animateTo(totalCorrect.toFloat(), animationSpec = tween(600, easing = FastOutSlowInEasing))
            comboAnim.animateTo(maxCombo.toFloat(), animationSpec = tween(600, easing = FastOutSlowInEasing))
        }
    }

    // Staggered appearance
    var visibleItems by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (i in 1..8) {
            delay(60)
            visibleItems = i
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.app_name), color = Accent, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        if (isDebugMode) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.debug_badge),
                                color = BgDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Warning)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.HELP) }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = stringResource(R.string.help), tint = TextLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark,
                    titleContentColor = Accent
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero rank card — tap to open profile
            StaggeredAppearance(visibleItems, 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(BgCard, BgDark)))
                        .border(1.dp, Accent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable { navController.navigate(Routes.PROFILE) }
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Rank badge circle
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Brush.horizontalGradient(listOf(Primary, PurpleDark)))
                                .border(2.dp, AccentLight.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(rank.icon, fontSize = 24.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rank.name, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(6.dp))
                            // XP bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TrackColorDim)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(xpAnim.value)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Brush.horizontalGradient(listOf(Accent, AccentLight)))
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("${xpProgress.intoRank} / ${xpProgress.rankSpan} XP", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Stats row
            if (hasStats) {
                Spacer(Modifier.height(12.dp))
                StaggeredAppearance(visibleItems, 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMiniCard(stringResource(R.string.stat_accuracy), "${accuracyAnim.value.toInt()}%", "🎯", Modifier.weight(1f))
                        StatMiniCard(stringResource(R.string.stat_correct), "${correctAnim.value.toInt()}", "✅", Modifier.weight(1f))
                        StatMiniCard(stringResource(R.string.stat_best_combo), "${comboAnim.value.toInt()}", "🔥", Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Missions
            if (missions.missions.isNotEmpty()) {
                StaggeredAppearance(visibleItems, 2) {
                    Text(stringResource(R.string.daily_missions), color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
                missions.missions.forEachIndexed { idx, m ->
                    StaggeredAppearance(visibleItems, 3 + idx) {
                        MissionCard(m) {
                            if (!m.completed) {
                                when (m.type) {
                                    "review" -> {
                                        gameViewModel.startQuickGameAsync { ok ->
                                            if (ok) {
                                                if (gameViewModel.shouldShowModeIntro(GameMode.QUICK)) {
                                                    navController.navigate(Routes.MODE_INTRO)
                                                } else {
                                                    navController.navigate(Routes.GAME)
                                                }
                                            }
                                        }
                                    }
                                    "progress", "variety" -> {
                                        gameViewModel.pendingMode = GameMode.SURVIVAL
                                        if (m.testId != null) {
                                            gameViewModel.startTemaGameAsync(m.testId) { ok ->
                                                if (ok) {
                                                    if (gameViewModel.shouldShowModeIntro(GameMode.SURVIVAL)) {
                                                        navController.navigate(Routes.MODE_INTRO)
                                                    } else {
                                                        navController.navigate(Routes.GAME)
                                                    }
                                                }
                                            }
                                        } else {
                                            gameViewModel.startAllLawsGameAsync { ok ->
                                                if (ok) {
                                                    if (gameViewModel.shouldShowModeIntro(GameMode.SURVIVAL)) {
                                                        navController.navigate(Routes.MODE_INTRO)
                                                    } else {
                                                        navController.navigate(Routes.GAME)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    "quality", "combo" -> {
                                        gameViewModel.pendingMode = GameMode.SURVIVAL
                                        gameViewModel.startAllLawsGameAsync { ok ->
                                            if (ok) {
                                                if (gameViewModel.shouldShowModeIntro(GameMode.SURVIVAL)) {
                                                    navController.navigate(Routes.MODE_INTRO)
                                                } else {
                                                    navController.navigate(Routes.GAME)
                                                }
                                            }
                                        }
                                    }
                                    "timetrial" -> {
                                        gameViewModel.pendingMode = GameMode.TIMETRIAL
                                        gameViewModel.startAllLawsGameAsync { ok ->
                                            if (ok) {
                                                if (gameViewModel.shouldShowModeIntro(GameMode.TIMETRIAL)) {
                                                    navController.navigate(Routes.MODE_INTRO)
                                                } else {
                                                    navController.navigate(Routes.GAME)
                                                }
                                            }
                                        }
                                    }
                                    "exam" -> {
                                        navController.navigate(Routes.MODE_SELECT)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            } else {
                StaggeredAppearance(visibleItems, 3) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.missions_empty), color = TextMuted, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // JUGAR button
            StaggeredAppearance(visibleItems, 7) {
                GameButton(
                    text = stringResource(R.string.play),
                    icon = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    color1 = Success,
                    color2 = SuccessDark,
                    textFontSize = 26,
                    iconSize = 26
                ) {
                    navController.navigate(Routes.MODE_SELECT)
                }
            }
        }
    }

    if (isLoading) {
        LoadingOverlay()
    }
}

@Composable
fun StaggeredAppearance(visibleCount: Int, index: Int, content: @Composable () -> Unit) {
    val visible = visibleCount > index
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(250),
        label = "stagger$index"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "scale$index"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 16f,
        animationSpec = tween(250),
        label = "offset$index"
    )
    Box(modifier = Modifier.alpha(alpha).scale(scale).offset { IntOffset(0, offsetY.toInt()) }) {
        content()
    }
}

@Composable
fun StatMiniCard(label: String, value: String, icon: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(BgCard, BgDark)))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
fun MissionCard(mission: Mission, onClick: () -> Unit) {
    val diffColor = when (mission.difficulty) {
        MissionDifficulty.EASY -> Success
        MissionDifficulty.MEDIUM -> Warning
        MissionDifficulty.HARD -> Danger
    }
    val accentColor = if (mission.completed) Success else diffColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (mission.completed) Modifier.background(SuccessDark) else Modifier.background(Brush.verticalGradient(listOf(BgCard, BgDark))))
            .border(width = 2.dp, color = if (mission.completed) Success else diffColor.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left color bar (difficulty indicator)
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (mission.completed) Success else diffColor)
        )
        Spacer(Modifier.width(10.dp))
        // Mission icon
        Text(mission.icon, fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                mission.text,
                color = if (mission.completed) SuccessLight else TextOption,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(6.dp))
            ProgressBar(
                progress = if (mission.target > 0) (mission.current.toFloat() / mission.target).coerceIn(0f, 1f) else 1f,
                color = accentColor,
                height = 4
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (mission.completed) "✓" else "${mission.current}/${mission.target}",
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "+${mission.reward} XP",
                color = if (mission.completed) SuccessLight else diffColor.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
