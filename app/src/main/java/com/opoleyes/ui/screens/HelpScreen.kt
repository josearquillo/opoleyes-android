package com.opoleyes.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.BuildConfig
import com.opoleyes.R
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HelpScreen(navController: NavController, gameViewModel: GameViewModel) {
    val sections = listOf(
        HelpSectionData("🎯", stringResource(R.string.help_section_objective_title), listOf(
            stringResource(R.string.help_section_objective_l1),
            stringResource(R.string.help_section_objective_l2),
            stringResource(R.string.help_section_objective_l3)
        )),
        HelpSectionData("🎮", stringResource(R.string.help_section_modes_title), listOf(
            stringResource(R.string.help_section_modes_l1),
            stringResource(R.string.help_section_modes_l2),
            stringResource(R.string.help_section_modes_l3),
            stringResource(R.string.help_section_modes_l4),
            stringResource(R.string.help_section_modes_l5)
        )),
        HelpSectionData("✨", stringResource(R.string.help_section_powerups_title), listOf(
            stringResource(R.string.help_section_powerups_l1),
            stringResource(R.string.help_section_powerups_l2),
            stringResource(R.string.help_section_powerups_l3),
            stringResource(R.string.help_section_powerups_l4),
            stringResource(R.string.help_section_powerups_l5),
            stringResource(R.string.help_section_powerups_l6),
            stringResource(R.string.help_section_powerups_l7)
        )),
        HelpSectionData("🔥", stringResource(R.string.help_section_combo_title), listOf(
            stringResource(R.string.help_section_combo_l1),
            stringResource(R.string.help_section_combo_l2),
            stringResource(R.string.help_section_combo_l3),
            stringResource(R.string.help_section_combo_l4)
        )),
        HelpSectionData("🌱", stringResource(R.string.help_section_ranks_title), listOf(
            stringResource(R.string.help_section_ranks_l1),
            stringResource(R.string.help_section_ranks_l2),
            stringResource(R.string.help_section_ranks_l3)
        )),
        HelpSectionData("🎁", stringResource(R.string.help_section_bonus_title), listOf(
            stringResource(R.string.help_section_bonus_l1),
            stringResource(R.string.help_section_bonus_l2),
            stringResource(R.string.help_section_bonus_l3)
        )),
        HelpSectionData("📋", stringResource(R.string.help_section_missions_title), listOf(
            stringResource(R.string.help_section_missions_l1),
            stringResource(R.string.help_section_missions_l2),
            stringResource(R.string.help_section_missions_l3),
            stringResource(R.string.help_section_missions_l4),
            stringResource(R.string.help_section_missions_l5),
            stringResource(R.string.help_section_missions_l6)
        )),
        HelpSectionData("ℹ️", stringResource(R.string.help_section_info_title), listOf(
            stringResource(R.string.help_section_info_l1),
            stringResource(R.string.help_section_info_l2),
            stringResource(R.string.help_section_info_l3)
        ))
    )

    var debugMode by remember { mutableStateOf(gameViewModel.isDebugMode()) }
    var debugToast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(debugToast) {
        if (debugToast != null) {
            kotlinx.coroutines.delay(2000)
            debugToast = null
        }
    }

    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help), color = TextLight, fontWeight = FontWeight.Bold) },
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
        ) {
            val debugOnText = stringResource(R.string.debug_on)
            val debugOffText = stringResource(R.string.debug_off)
            Text(
                stringResource(R.string.help),
                color = TextLight,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = {
                        if (BuildConfig.DEBUG) {
                            debugMode = !debugMode
                            gameViewModel.setDebugMode(debugMode)
                            debugToast = if (debugMode) debugOnText else debugOffText
                        }
                    }
                )
            )
            if (debugToast != null) {
                Spacer(Modifier.height(4.dp))
                Text(debugToast!!, color = if (debugMode) Success else TextMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(20.dp))
            sections.forEach { section ->
                HelpSection(section)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HelpSection(data: HelpSectionData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(BgCard, BgDark)))
            .padding(16.dp)
    ) {
        Text("${data.icon} ${data.title}", color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        data.lines.forEach { line ->
            Text(line, color = TextMuted, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
        }
    }
}

private data class HelpSectionData(val icon: String, val title: String, val lines: List<String>)
