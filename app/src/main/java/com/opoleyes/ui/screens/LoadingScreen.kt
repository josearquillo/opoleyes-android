package com.opoleyes.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.repository.MissionRepository
import com.opoleyes.ui.components.GameButton
import com.opoleyes.ui.components.ShimmerBox
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(navController: NavController) {
    var error by remember { mutableStateOf<String?>(null) }
    var fadeOut by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val context = navController.context
            val tests = DataProvider.loadData(context)
            if (tests.isEmpty()) {
                error = "No se pudieron cargar los datos."
            } else {
                val missionRepo = MissionRepository(context)
                missionRepo.generateDailyMissions()
                delay(400)
                fadeOut = true
                delay(300)
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.LOADING) { inclusive = true }
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Error desconocido"
        }
    }

    val fadeAlpha by animateFloatAsState(
        targetValue = if (fadeOut) 0f else 1f,
        animationSpec = tween(300),
        label = "fade"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "logoScale"
    )
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shimmer"
    )

    Box(
        modifier = Modifier.fillMaxSize().alpha(fadeAlpha),
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
                Text(
                    "OPOLEYES",
                    color = Accent,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.scale(logoScale)
                )

                Spacer(Modifier.height(20.dp))

                CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)

                Spacer(Modifier.height(16.dp))

                ShimmerBox(
                    modifier = Modifier
                        .width(120.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgCard)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(BgCard))
                }

                Spacer(Modifier.height(6.dp))

                Text("Cargando...", color = TextMuted.copy(alpha = shimmerAlpha), fontSize = 14.sp)
            }
        }
    }
}
