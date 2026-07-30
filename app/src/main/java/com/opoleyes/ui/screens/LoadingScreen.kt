package com.opoleyes.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
    var progress by remember { mutableStateOf(0f) }
    var statusIndex by remember { mutableStateOf(0) }
    var dotCount by remember { mutableStateOf(0) }

    // Real preload stages — messages shown as the bar animates
    val stages = listOf(
        "Cargando preguntas",
        "Generando misiones diarias",
        "Preparando power-ups",
        "Inicializando progreso",
        "Listo"
    )
    val minDisplayTime = 3000

    LaunchedEffect(Unit) {
        try {
            val context = navController.context
            val startTime = System.currentTimeMillis()

            // Do all real work in background (fast, but warms caches)
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
                // Preload stats cache so game modes start instantly
                StatsRepository(context).getStats()
                DataProvider.getTestDataMap(context)
            }

            // Animate progress bar smoothly over minDisplayTime
            // regardless of how fast the real work finished
            val steps = 100
            val interval = minDisplayTime.toLong() / steps
            for (i in 1..steps) {
                progress = i / 100f
                statusIndex = ((i / 100f) * stages.size).toInt()
                    .coerceAtMost(stages.size - 1)
                delay(interval)
            }

            // Ensure minimum display time
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < minDisplayTime) {
                delay(minDisplayTime - elapsed)
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

    // Animated dots for the current status
    LaunchedEffect(statusIndex, error) {
        dotCount = 0
        while (error == null && statusIndex < 4) {
            delay(400)
            dotCount = (dotCount + 1) % 4
        }
    }

    val fadeAlpha by animateFloatAsState(
        targetValue = if (fadeOut) 0f else 1f,
        animationSpec = tween(400),
        label = "fade"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val dots = if (statusIndex < 4) ".".repeat(dotCount) else ""

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
                DotLottieAnimation(
                    source = DotLottieSource.Asset("law_and_justice.json"),
                    autoplay = true,
                    loop = true,
                    speed = 1.25f,
                    useFrameInterpolation = false,
                    playMode = Mode.FORWARD,
                    modifier = Modifier.size(200.dp)
                )

                Spacer(Modifier.height(32.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SurfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Primary, Accent))
                            )
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Status text with animated dots
                Text(
                    "${stages[statusIndex]}$dots",
                    color = TextLight,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(6.dp))

                // Percentage
                Text(
                    "${(animatedProgress * 100).toInt()}%",
                    color = TextDim,
                    fontSize = 13.sp
                )
            }
        }
    }
}
