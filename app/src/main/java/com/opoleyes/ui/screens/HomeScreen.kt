package com.opoleyes.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.model.Mission
import com.opoleyes.data.repository.MissionRepository
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.ui.components.*
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, gameViewModel: GameViewModel) {
    val context = navController.context
    val progressRepo = ProgressRepository(context)
    val missionRepo = MissionRepository(context)
    val isLoading by gameViewModel.isLoading.collectAsState()

    val rank = remember { progressRepo.getRank() }
    val xpProgress = remember { progressRepo.getXPProgress() }
    val missions = remember { missionRepo.generateDailyMissions() }

    val scrollState = rememberScrollState()

    // Animated XP bar
    val xpAnim = remember { Animatable(0f) }
    LaunchedEffect(xpProgress.pct) {
        xpAnim.animateTo(xpProgress.pct / 100f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    // Staggered appearance
    var visibleItems by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (i in 1..6) {
            delay(60)
            visibleItems = i
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "OPOLEYES",
                        color = Accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.PROFILE) }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Perfil",
                            tint = TextLight
                        )
                    }
                    IconButton(onClick = { navController.navigate(Routes.HELP) }) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = "Ayuda",
                            tint = TextLight
                        )
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
            // Rank + XP card
            StaggeredAppearance(visibleItems, 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(BgCard, BgCardDark)))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(rank.icon, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rank.name, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(xpAnim.value)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Brush.horizontalGradient(listOf(Primary, Accent)))
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text("${xpProgress.intoRank} / ${xpProgress.rankSpan} XP", color = TextMuted, fontSize = 10.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Missions
            if (missions.missions.isNotEmpty()) {
                StaggeredAppearance(visibleItems, 1) {
                    Text("Misiones diarias", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(8.dp))
                missions.missions.forEachIndexed { idx, m ->
                    StaggeredAppearance(visibleItems, 2 + idx) {
                        MissionCard(m) {
                            if (!m.completed) {
                                gameViewModel.pendingMode = GameMode.SURVIVAL
                                when (m.type) {
                                    "review" -> {
                                        gameViewModel.startQuickGameAsync { ok -> if (ok) navController.navigate(Routes.GAME) }
                                    }
                                    "progress", "variety" -> {
                                        if (m.testId != null) {
                                            gameViewModel.startTemaGameAsync(m.testId) { ok -> if (ok) navController.navigate(Routes.GAME) }
                                        } else {
                                            gameViewModel.startAllLawsGameAsync { ok -> if (ok) navController.navigate(Routes.GAME) }
                                        }
                                    }
                                    "quality", "combo" -> {
                                        gameViewModel.startAllLawsGameAsync { ok -> if (ok) navController.navigate(Routes.GAME) }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            } else {
                StaggeredAppearance(visibleItems, 2) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text("Vuelve mañana para nuevas misiones", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // JUGAR button
            StaggeredAppearance(visibleItems, 5) {
                GameButton(
                    text = "JUGAR",
                    icon = "▶",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    color1 = Success,
                    color2 = SuccessDark,
                    textFontSize = 24,
                    iconFontSize = 24
                ) { navController.navigate(Routes.MODE_SELECT) }
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
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 16f,
        animationSpec = tween(250),
        label = "offset$index"
    )
    Box(modifier = Modifier.alpha(alpha).offset { IntOffset(0, offsetY.toInt()) }) {
        content()
    }
}

@Composable
fun MissionCard(mission: Mission, onClick: () -> Unit) {
    val accentColor = if (mission.completed) Success else PrimaryLight
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (mission.completed) Brush.verticalGradient(listOf(SuccessDark, Color(0xFF052e16)))
                else Brush.verticalGradient(listOf(BgCard, BgCardDark))
            )
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (mission.completed) "✓" else "•", fontSize = 14.sp, color = if (mission.completed) Success else TextMuted)
            Spacer(Modifier.width(8.dp))
            Text(
                mission.text,
                color = if (mission.completed) Color(0xFF86efac) else Color(0xFFcbd5e1),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${mission.current}/${mission.target}",
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        ProgressBar(
            progress = (mission.current.toFloat() / mission.target).coerceIn(0f, 1f),
            color = accentColor,
            height = 4
        )
    }
}
