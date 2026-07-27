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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rank + XP bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RankBadge(rank, Modifier.clickable { navController.navigate(Routes.PROFILE) })
            Spacer(Modifier.weight(1f))
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
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(xpProgress.pct / 100f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Brush.horizontalGradient(listOf(Primary, Accent)))
                )
            }
        }
        Text("${xpProgress.intoRank} / ${xpProgress.rankSpan} XP", color = TextMuted, fontSize = 10.sp)

        Spacer(Modifier.height(24.dp))

        // Logo
        Text(
            "OPOLEYES",
            color = Accent,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.scale(logoScale)
        )

        Spacer(Modifier.height(24.dp))

        // Missions
        if (missions.missions.isNotEmpty()) {
            Text("📋 Misiones diarias", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            missions.missions.forEach { m ->
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
        } else {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("🎯", fontSize = 32.sp)
                Spacer(Modifier.height(8.dp))
                Text("Vuelve mañana para nuevas misiones", color = TextMuted, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Buttons
        GameButton(
            text = "JUGAR",
            icon = "🎮",
            modifier = Modifier.fillMaxWidth().height(70.dp),
            color1 = Success,
            color2 = SuccessDark
        ) { navController.navigate(Routes.MODE_SELECT) }
        Spacer(Modifier.height(14.dp))
        GameButton(
            text = "Entrenar",
            icon = "🎯",
            modifier = Modifier.fillMaxWidth().height(56.dp),
            color1 = Primary,
            color2 = PurpleDark
        ) { navController.navigate(Routes.TRAIN_SELECT) }
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
