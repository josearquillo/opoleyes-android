package com.opotest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opotest.data.local.DataProvider
import com.opotest.data.repository.MissionRepository
import com.opotest.data.repository.ProgressRepository
import com.opotest.ui.components.GameButton
import com.opotest.ui.navigation.Routes
import com.opotest.ui.theme.*

@Composable
fun LoadingScreen(navController: NavController) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val context = navController.context
            val tests = DataProvider.loadData(context)
            if (tests.isEmpty()) {
                error = "No se pudieron cargar los datos."
            } else {
                val missionRepo = MissionRepository(context)
                missionRepo.generateDailyMissions()
                loading = false
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.LOADING) { inclusive = true }
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Error desconocido"
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (error != null) {
                Text(error!!, color = Danger, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                GameButton("Volver", color1 = Danger, color2 = DangerDark) {
                    navController.navigate(Routes.ERROR)
                }
            } else {
                Text("OPOTEST", color = Accent, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator(color = Primary)
                Spacer(Modifier.height(16.dp))
                Text("Cargando...", color = TextMuted, fontSize = 16.sp)
            }
        }
    }
}
