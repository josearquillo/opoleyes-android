package com.opoleyes.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.R
import com.opoleyes.data.Constants
import com.opoleyes.data.model.GameMode
import com.opoleyes.ui.components.GameButton
import com.opoleyes.ui.components.LoadingOverlay
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectScreen(navController: NavController, gameViewModel: GameViewModel) {
    val unlocks = remember { gameViewModel.getUnlocks() }
    val isLoading by gameViewModel.isLoading.collectAsState()

    val modes = listOf(
        ModeInfo(GameMode.SURVIVAL, Icons.Default.Favorite, stringResource(R.string.mode_survival), stringResource(R.string.mode_survival_desc), true, 0),
        ModeInfo(GameMode.TIMETRIAL, Icons.Default.Timer, stringResource(R.string.mode_timetrial), stringResource(R.string.mode_timetrial_desc), unlocks.timetrial, 1),
        ModeInfo(GameMode.QUICK, Icons.Default.Bolt, stringResource(R.string.mode_quick), stringResource(R.string.mode_quick_desc), unlocks.quick, 2),
        ModeInfo(GameMode.EXAM, Icons.AutoMirrored.Filled.Assignment, stringResource(R.string.mode_exam), stringResource(R.string.mode_exam_desc), unlocks.exam, 3),
        ModeInfo(GameMode.CHALLENGE, Icons.Default.EmojiEvents, stringResource(R.string.mode_challenge), stringResource(R.string.mode_challenge_desc), unlocks.challenge, 4),
    )

    var showExamDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.select_mode), color = TextLight, fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            modes.forEachIndexed { index, mode ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(index * 100L)
                    visible = true
                }
                AnimatedVisibility(visible = visible) {
                    ModeCard(mode, enabled = !isLoading) {
                        when (mode.mode) {
                            GameMode.QUICK -> {
                                gameViewModel.startQuickGameAsync { ok -> if (ok) navController.navigate(Routes.GAME) }
                            }
                            GameMode.CHALLENGE -> {
                                gameViewModel.startChallengeGameAsync { ok -> if (ok) navController.navigate(Routes.GAME) }
                            }
                            GameMode.EXAM -> {
                                showExamDialog = true
                            }
                            else -> {
                                gameViewModel.pendingMode = mode.mode
                                navController.navigate(Routes.TEMA_SELECT)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (isLoading) {
        LoadingOverlay()
    }

    if (showExamDialog) {
        ExamConfigDialog(
            onDismiss = { showExamDialog = false },
            onStart = { count ->
                showExamDialog = false
                gameViewModel.startExamAsync(count) { ok ->
                    if (ok) navController.navigate(Routes.EXAM)
                }
            }
        )
    }
}

@Composable
private fun ModeCard(mode: ModeInfo, enabled: Boolean = true, onClick: () -> Unit) {
    val locked = !mode.unlocked
    val colors = when (mode.mode) {
        GameMode.SURVIVAL -> listOf(Danger, DangerDark)
        GameMode.TIMETRIAL -> listOf(Primary, PurpleDark)
        GameMode.QUICK -> listOf(Warning, WarningDark)
        GameMode.EXAM -> listOf(Success, SuccessDark)
        GameMode.CHALLENGE -> listOf(Accent, AccentLight)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(if (locked) listOf(BgCard, BgCardDark) else colors))
            .clickable(enabled = !locked && enabled) { onClick() }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                mode.icon,
                contentDescription = null,
                tint = if (locked) TextDim else Color.White,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(mode.name, color = if (locked) TextDim else Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(mode.desc, color = if (locked) TextDim else Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
            if (locked) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.locked), tint = TextDim)
                    Text(Constants.getRankByIndex(mode.requiredRank).name, color = TextDim, fontSize = 10.sp)
                }
            }
        }
    }
}

private data class ModeInfo(
    val mode: GameMode,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val name: String,
    val desc: String,
    val unlocked: Boolean,
    val requiredRank: Int
)

@Composable
private fun ExamConfigDialog(
    onDismiss: () -> Unit,
    onStart: (Int) -> Unit
) {
    val presets = listOf(
        ExamPreset(stringResource(R.string.exam_preset_fast), 25, Primary, PurpleDark),
        ExamPreset(stringResource(R.string.exam_preset_standard), 50, Danger, DangerDark),
        ExamPreset(stringResource(R.string.exam_preset_full), 100, Warning, WarningDark)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        titleContentColor = TextLight,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = Accent, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.configure_exam), color = TextLight, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(stringResource(R.string.question_count), color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))

                presets.forEach { preset ->
                    ExamPresetCard(
                        preset = preset,
                        onClick = { onStart(preset.count) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = TextMuted) }
        }
    )
}

private data class ExamPreset(
    val name: String,
    val count: Int,
    val color1: Color,
    val color2: Color
)

@Composable
private fun ExamPresetCard(preset: ExamPreset, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(listOf(preset.color1, preset.color2)))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                preset.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.questions_count, preset.count),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
    }
}
