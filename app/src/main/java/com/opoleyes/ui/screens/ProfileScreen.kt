package com.opoleyes.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.R
import com.opoleyes.data.Constants
import com.opoleyes.ui.components.GameButton
import com.opoleyes.ui.components.ProgressBar
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, gameViewModel: GameViewModel) {
    val data = remember {
        gameViewModel.preloadProfileData()
        gameViewModel.profileData
    } ?: return
    val rank = data.rank
    val xpProgress = data.xpProgress
    val achievements = data.achievements
    val gamesPlayed = data.gamesPlayed
    val totalCorrect = data.totalCorrect
    val totalWrong = data.totalWrong
    val globalProgress = data.globalProgress
    val temaTests = data.temaTests
    val dominatedLaws = data.dominatedLaws

    var showResetDialog by remember { mutableStateOf(false) }
    var selectedAchievement by remember { mutableStateOf<com.opoleyes.data.model.Achievement?>(null) }
    val scrollState = rememberScrollState()

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = BgCard,
            title = { Text(stringResource(R.string.reset_progress)) },
            text = { Text(stringResource(R.string.reset_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    gameViewModel.resetProgress()
                    showResetDialog = false
                    navController.navigate(Routes.HOME) { popUpTo(0) }
                }) { Text(stringResource(R.string.reset), color = Danger) }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    selectedAchievement?.let { ach ->
        val isUnlocked = achievements.containsKey(ach.id)
        AlertDialog(
            onDismissRequest = { selectedAchievement = null },
            containerColor = BgCard,
            titleContentColor = TextLight,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ach.icon, fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(ach.name, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text(
                        if (isUnlocked) stringResource(R.string.achievement_unlocked_status) else stringResource(R.string.achievement_locked_status),
                        color = if (isUnlocked) Success else TextDim,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(ach.desc, color = TextMuted, fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAchievement = null }) {
                    Text(stringResource(R.string.close), color = PrimaryLight)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile), color = TextLight, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = TextLight)
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
                .padding(16.dp)
                .adaptiveWidth(),
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
        Text(stringResource(R.string.records), color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        val modeLabels = linkedMapOf(
            "survival" to stringResource(R.string.mode_survival),
            "timetrial" to stringResource(R.string.mode_timetrial),
            "quick" to stringResource(R.string.mode_quick)
        )
        modeLabels.forEach { (mode, label) ->
            val record = data.records[mode] ?: 0
            val unlocked = data.unlockedModes[mode] ?: false
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (unlocked) label else "$label ${stringResource(R.string.blocked_suffix)}", color = if (unlocked) TextLight else TextDim, fontSize = 14.sp)
                Text(if (unlocked) "$record pts" else "—", color = if (unlocked) Warning else TextDim, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Achievements
        Text(stringResource(R.string.achievements, achievements.size, Constants.ACHIEVEMENTS.size), color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        // Use a simple grid via Rows — column count adapts to screen width
        val columns = integerResource(R.integer.achievements_columns)
        val chunked = Constants.ACHIEVEMENTS.chunked(columns)
        chunked.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { ach ->
                    val isUnlocked = achievements.containsKey(ach.id)
                    Column(
                        modifier = Modifier
                            .width(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BgCard.copy(alpha = if (isUnlocked) 1f else 0.3f))
                            .alpha(if (isUnlocked) 1f else 0.3f)
                            .clickable { selectedAchievement = ach }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(ach.icon, fontSize = 24.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            ach.name,
                            color = TextMuted,
                            fontSize = 10.sp,
                            maxLines = 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))

        // Stats
        Text(stringResource(R.string.statistics), color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        listOf(
            stringResource(R.string.games_played) to "$gamesPlayed",
            stringResource(R.string.total_correct) to "$totalCorrect",
            stringResource(R.string.total_wrong) to "$totalWrong",
            stringResource(R.string.global_progress) to "$globalProgress%",
            stringResource(R.string.dominated_laws) to "$dominatedLaws/${temaTests.size}"
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
        GameButton(stringResource(R.string.reset_progress), color1 = Danger, color2 = DangerDark, modifier = Modifier.fillMaxWidth()) {
            showResetDialog = true
        }
        Spacer(Modifier.height(24.dp))
        }
    }
}
