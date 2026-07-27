package com.opoleyes.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LoadingScreen(navController: NavController) {
    var loading by remember { mutableStateOf(true) }
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
    val balanceAngle by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "balance"
    )
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
                // Animated justice scales
                Canvas(modifier = Modifier.size(80.dp)) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val armLength = size.width * 0.35f
                    val rad = Math.toRadians(balanceAngle.toDouble())

                    // Post
                    drawLine(
                        color = Accent,
                        start = Offset(cx, cy - armLength),
                        end = Offset(cx, cy + armLength * 0.8f),
                        strokeWidth = 4f
                    )
                    // Base
                    drawLine(
                        color = Accent,
                        start = Offset(cx - 20f, cy + armLength * 0.8f),
                        end = Offset(cx + 20f, cy + armLength * 0.8f),
                        strokeWidth = 4f
                    )
                    // Crossbar (rotated)
                    val x1 = cx - cos(rad) * armLength
                    val y1 = cy - armLength + sin(rad) * armLength * 0.3f
                    val x2 = cx + cos(rad) * armLength
                    val y2 = cy - armLength - sin(rad) * armLength * 0.3f
                    drawLine(
                        color = Accent,
                        start = Offset(x1.toFloat(), y1.toFloat()),
                        end = Offset(x2.toFloat(), y2.toFloat()),
                        strokeWidth = 3f
                    )
                    // Left pan strings
                    drawLine(color = TextMuted, start = Offset(x1.toFloat(), y1.toFloat()), end = Offset(x1.toFloat(), y1.toFloat() + 25f), strokeWidth = 1.5f)
                    // Left pan
                    drawArc(
                        color = Accent.copy(alpha = 0.6f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(x1.toFloat() - 15f, y1.toFloat() + 20f),
                        size = Size(30f, 15f),
                        style = Stroke(width = 2f)
                    )
                    // Right pan strings
                    drawLine(color = TextMuted, start = Offset(x2.toFloat(), y2.toFloat()), end = Offset(x2.toFloat(), y2.toFloat() + 25f), strokeWidth = 1.5f)
                    // Right pan
                    drawArc(
                        color = Accent.copy(alpha = 0.6f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(x2.toFloat() - 15f, y2.toFloat() + 20f),
                        size = Size(30f, 15f),
                        style = Stroke(width = 2f)
                    )
                }

                Spacer(Modifier.height(20.dp))

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
