package com.opoleyes.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.dotlottie.dlplayer.Mode
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.data.repository.MissionRepository
import com.opoleyes.data.repository.ProgressRepository
import com.opoleyes.data.repository.StatsRepository
import com.opoleyes.ui.components.GameButton
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun LoadingScreen(navController: NavController) {
    var error by remember { mutableStateOf<String?>(null) }
    var fadeOut by remember { mutableStateOf(false) }

    val minDisplayTime = 3500

    LaunchedEffect(Unit) {
        try {
            val context = navController.context
            val startTime = System.currentTimeMillis()

            // Do all real work in background
            val tests = withContext(Dispatchers.Default) {
                DataProvider.loadData(context)
            }
            if (tests.isEmpty()) {
                error = "No se pudieron cargar los datos."
                return@LaunchedEffect
            }
            withContext(Dispatchers.Default) {
                MissionRepository(context).generateDailyMissions()
                PreferencesManager(context).initPowerUpsIfNeeded()
                ProgressRepository(context).getXPProgress()
                StatsRepository(context).getStats()
                DataProvider.getTestDataMap(context)
            }

            // Ensure minimum display time
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < minDisplayTime) {
                delay((minDisplayTime - elapsed).toLong())
            }

            fadeOut = true
            delay(400)
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.LOADING) { inclusive = true }
            }
        } catch (e: Exception) {
            error = e.message ?: "Error desconocido"
        }
    }

    val fadeAlpha by animateFloatAsState(
        targetValue = if (fadeOut) 0f else 1f,
        animationSpec = tween(400),
        label = "fade"
    )

    Box(
        modifier = Modifier.fillMaxSize().alpha(fadeAlpha),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            if (error != null) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = Danger,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("No se pudieron cargar los datos", color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(error ?: "", color = TextMuted, fontSize = 14.sp)
                Spacer(Modifier.height(24.dp))
                GameButton("Reintentar", color1 = Primary, color2 = PurpleDark) {
                    error = null
                    navController.navigate(Routes.LOADING) { popUpTo(Routes.LOADING) { inclusive = true } }
                }
            } else {
                // Balanza animation
                DotLottieAnimation(
                    source = DotLottieSource.Asset("law_and_justice.json"),
                    autoplay = true,
                    loop = true,
                    speed = 1.25f,
                    useFrameInterpolation = false,
                    playMode = Mode.FORWARD,
                    modifier = Modifier.size(200.dp)
                )

                Spacer(Modifier.height(48.dp))

                // Status text
                Text(
                    "Cargando aplicación...",
                    color = TextMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
