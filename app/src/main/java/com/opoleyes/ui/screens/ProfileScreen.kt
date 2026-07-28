package com.opoleyes.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.data.Constants
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository
import com.opoleyes.ui.components.GameButton
import com.opoleyes.ui.components.ProgressBar
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = navController.context
    val progressRepo = ProgressRepository(context)
    val statsRepo = StatsRepository(context)

    val rank = remember { progressRepo.getRank() }
    val xpProgress = remember { progressRepo.getXPProgress() }
    val achievements = remember { progressRepo.getAchievements() }
    val gamesPlayed = remember { progressRepo.getGamesPlayed() }
    val totalCorrect = remember { statsRepo.getTotalCorrect() }
    val totalWrong = remember { statsRepo.getTotalWrong() }
    val globalProgress = remember { statsRepo.getGlobalProgress() }
    val temaTests = remember { DataProvider.getTemaTests(context) }
    val dominatedLaws = remember { temaTests.count { statsRepo.getLeyProgress(it.id) >= 100 } }

    val prefs = remember { PreferencesManager(context) }
    val powerUps = remember { prefs.getFreePowerUps() }
    val powerUpCounts = remember {
        mapOf(
            (Icons.Default.Lightbulb to "Pista") to powerUps.count { it == "hint" },
            (Icons.Default.Shield to "Escudo") to powerUps.count { it == "shield" },
            (Icons.Default.SwapHoriz to "50/50") to powerUps.count { it == "fiftyFifty" },
            (Icons.Default.AutoAwesome to "x2 pts") to powerUps.count { it == "doubleScore" }
        )
    }

    var showResetDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reiniciar progreso") },
            text = { Text("¿Estás seguro? Se borrará todo tu progreso, XP, logros y récords.") },
            confirmButton = {
                TextButton(onClick = {
                    progressRepo.resetAll()
                    PreferencesManager(context).initPowerUpsIfNeeded()
                    showResetDialog = false
                    navController.navigate(Routes.HOME) { popUpTo(0) }
                }) { Text("Reiniciar", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil", color = TextLight, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextLight)
                    }
                },
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
        Text(rank.icon, fontSize = 48.sp)
        Text(rank.name, color = TextLight, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        ProgressBar(progress = xpProgress.pct / 100f, color = Primary, height = 14)
        Spacer(Modifier.height(4.dp))
        Text("${xpProgress.intoRank} / ${xpProgress.rankSpan} XP", color = TextMuted, fontSize = 12.sp)

        Spacer(Modifier.height(24.dp))

        // Records
        Text("Récords", color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        val modes = listOf("survival" to "Supervivencia", "timetrial" to "Contrarreloj", "quick" to "Repaso Express", "challenge" to "Reto")
        modes.forEach { (mode, label) ->
            val record = progressRepo.getRecord(mode)
            val unlocked = progressRepo.isUnlocked(mode)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (unlocked) label else "$label (bloqueado)", color = if (unlocked) TextLight else TextDim, fontSize = 14.sp)
                Text(if (unlocked) "$record pts" else "—", color = if (unlocked) Warning else TextDim, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Power-ups
        Text("Ayudas disponibles", color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            powerUpCounts.forEach { (pair, count) ->
                val (icon, label) = pair
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgCard)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(icon, contentDescription = label, tint = TextLight, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        if (count > 0) "x$count" else "—",
                        color = if (count > 0) Warning else TextDim,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Achievements
        Text("Logros (${achievements.size}/${Constants.ACHIEVEMENTS.size})", color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        // Use a simple grid via Rows
        val chunked = Constants.ACHIEVEMENTS.chunked(5)
        chunked.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { ach ->
                    val isUnlocked = achievements.containsKey(ach.id)
                    Column(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BgCard.copy(alpha = if (isUnlocked) 1f else 0.3f))
                            .alpha(if (isUnlocked) 1f else 0.3f)
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(ach.icon, fontSize = 20.sp)
                        Text(ach.name, color = TextMuted, fontSize = 7.sp, maxLines = 2)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))

        // Stats
        Text("Estadísticas", color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        listOf(
            "Partidas jugadas" to "$gamesPlayed",
            "Aciertos totales" to "$totalCorrect",
            "Fallos totales" to "$totalWrong",
            "Progreso global" to "$globalProgress%",
            "Leyes dominadas" to "$dominatedLaws/${temaTests.size}"
        ).forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = TextMuted, fontSize = 14.sp)
                Text(value, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
        GameButton("Reiniciar progreso", color1 = Danger, color2 = DangerDark, modifier = Modifier.fillMaxWidth()) {
            showResetDialog = true
        }
        Spacer(Modifier.height(24.dp))
        }
    }
}
