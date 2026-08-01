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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.R
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulacroIntroScreen(navController: NavController, gameViewModel: GameViewModel) {
    val isLoading by gameViewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mode_simulacro), color = TextLight, fontWeight = FontWeight.Bold) },
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
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.simulacro_intro_title),
                color = Accent,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            InfoCard(
                icon = "📝",
                title = stringResource(R.string.simulacro_info_questions_title),
                desc = stringResource(R.string.simulacro_info_questions_desc)
            )
            Spacer(Modifier.height(12.dp))

            InfoCard(
                icon = "⏱️",
                title = stringResource(R.string.simulacro_info_time_title),
                desc = stringResource(R.string.simulacro_info_time_desc)
            )
            Spacer(Modifier.height(12.dp))

            InfoCard(
                icon = "⚖️",
                title = stringResource(R.string.simulacro_info_weights_title),
                desc = stringResource(R.string.simulacro_info_weights_desc)
            )
            Spacer(Modifier.height(12.dp))

            InfoCard(
                icon = "🎯",
                title = stringResource(R.string.simulacro_info_scoring_title),
                desc = stringResource(R.string.simulacro_info_scoring_desc)
            )

            Spacer(Modifier.weight(1f))

            if (isLoading) {
                CircularProgressIndicator(color = Accent, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    gameViewModel.startSimulacroAsync { ok ->
                        if (ok) navController.navigate(Routes.EXAM)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(stringResource(R.string.simulacro_start), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoCard(icon: String, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(BgCard, BgDark)))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 28.sp)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(desc, color = TextMuted, fontSize = 13.sp)
        }
    }
}
