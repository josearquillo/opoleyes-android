package com.opoleyes.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.opoleyes.data.model.GameMode
import com.opoleyes.data.model.Mission
import com.opoleyes.data.repository.MissionRepository
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.ui.components.*
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(navController: NavController, gameViewModel: GameViewModel) {
    val context = navController.context
    val progressRepo = ProgressRepository(context)
    val missionRepo = MissionRepository(context)

    val rank = remember { progressRepo.getRank() }
    val xpProgress = remember { progressRepo.getXPProgress() }
    val missions = remember { missionRepo.generateDailyMissions() }

    val scrollState = rememberScrollState()
    val infiniteTransition = rememberInfiniteTransition(label = "home")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "logo"
    )
    val playPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "playPulse"
    )
    val playGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "playGlow"
    )

    // Animated XP bar
    val xpAnim = remember { Animatable(0f) }
    LaunchedEffect(xpProgress.pct) {
        xpAnim.animateTo(xpProgress.pct / 100f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    // Staggered appearance
    var visibleItems by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (i in 1..10) {
            delay(80)
            visibleItems = i
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile card with circular progress ring
        StaggeredAppearance(visibleItems, 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(BgCard, BgCardDark)))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressRing(
                    progress = xpProgress.pct / 100f,
                    size = 56,
                    strokeWidth = 5,
                    ringColor = Accent,
                    modifier = Modifier.clickable { navController.navigate(Routes.PROFILE) }
                ) {
                    Text(rank.icon, fontSize = 24.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(rank.name, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(xpAnim.value)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Brush.horizontalGradient(listOf(Primary, Accent)))
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text("${xpProgress.intoRank} / ${xpProgress.rankSpan} XP", color = TextMuted, fontSize = 10.sp)
                }
                // Help button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Warning, Color(0xFFd97706))))
                        .clickable { navController.navigate(Routes.HELP) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Logo
        StaggeredAppearance(visibleItems, 1) {
            Text(
                "OPOLEYES",
                color = Accent,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(logoScale)
            )
        }

        Spacer(Modifier.height(20.dp))

        // Missions
        if (missions.missions.isNotEmpty()) {
            StaggeredAppearance(visibleItems, 2) {
                Text("📋 Misiones diarias", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(12.dp))
            missions.missions.forEachIndexed { idx, m ->
                StaggeredAppearance(visibleItems, 3 + idx) {
                    MissionCard(m) {
                        if (!m.completed) {
                            gameViewModel.pendingMode = GameMode.SURVIVAL
                            when (m.type) {
                                "review" -> {
                                    gameViewModel.startQuickGame()
                                    navController.navigate(Routes.GAME)
                                }
                                "progress", "variety" -> {
                                    if (m.testId != null) {
                                        gameViewModel.startTemaGame(m.testId)
                                    } else {
                                        gameViewModel.startAllLawsGame()
                                    }
                                    navController.navigate(Routes.GAME)
                                }
                                "quality", "combo" -> {
                                    gameViewModel.startAllLawsGame()
                                    navController.navigate(Routes.GAME)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        } else {
            StaggeredAppearance(visibleItems, 3) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("🎯", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Vuelve mañana para nuevas misiones", color = TextMuted, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Big JUGAR button with pulse + glow
        StaggeredAppearance(visibleItems, 8) {
            GameButton(
                text = "JUGAR",
                icon = "🎮",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .scale(playPulse)
                    .shadow((playGlow * 16).dp, RoundedCornerShape(14.dp), clip = false, ambientColor = Success.copy(alpha = playGlow), spotColor = Success.copy(alpha = playGlow)),
                color1 = Success,
                color2 = SuccessDark
            ) { navController.navigate(Routes.MODE_SELECT) }
        }
    }
}

@Composable
fun StaggeredAppearance(visibleCount: Int, index: Int, content: @Composable () -> Unit) {
    val visible = visibleCount > index
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "stagger$index"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(300),
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
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (mission.completed) "✅" else mission.icon, fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                mission.text,
                color = if (mission.completed) Color(0xFF86efac) else Color(0xFFcbd5e1),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${mission.current}/${mission.target}",
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar(
            progress = (mission.current.toFloat() / mission.target).coerceIn(0f, 1f),
            color = accentColor,
            height = 6
        )
        Spacer(Modifier.height(4.dp))
        Text("+${mission.reward} XP", color = TextDim, fontSize = 10.sp)
    }
}
