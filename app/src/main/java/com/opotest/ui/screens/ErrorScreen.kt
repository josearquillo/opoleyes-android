package com.opotest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opotest.ui.components.GameButton
import com.opotest.ui.navigation.Routes
import com.opotest.ui.theme.*

@Composable
fun ErrorScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("❌", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text("Error", color = Danger, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            GameButton("Volver", color1 = Danger, color2 = DangerDark) {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = true }
                }
            }
        }
    }
}
