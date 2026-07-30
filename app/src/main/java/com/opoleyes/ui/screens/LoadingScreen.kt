package com.opoleyes.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.dotlottie.dlplayer.Mode
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.repository.MissionRepository
import com.opoleyes.ui.components.GameButton
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(navController: NavController) {
    var error by remember { mutableStateOf<String?>(null) }
    var fadeOut by remember { mutableStateOf(false) }
    var logoVisible by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }
    var spinnerVisible by remember { mutableStateOf(false) }
    var phraseIndex by remember { mutableStateOf(0) }

    val phrases = listOf(
        "Estudia leyes mientras juegas",
        "Supervivencia, contrarreloj, examen...",
        "Sube de rango y desbloquea modos",
        "Misiones diarias, cofres y logros",
        "Prepara tu oposición de justicia"
    )

    LaunchedEffect(Unit) {
        delay(100)
        logoVisible = true
        delay(300)
        taglineVisible = true
        delay(200)
        spinnerVisible = true
        try {
            val context = navController.context
            val tests = DataProvider.loadData(context)
            if (tests.isEmpty()) {
                error = "No se pudieron cargar los datos."
            } else {
                val missionRepo = MissionRepository(context)
                missionRepo.generateDailyMissions()
                delay(3500)
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

    LaunchedEffect(spinnerVisible) {
        if (spinnerVisible && error == null) {
            while (true) {
                delay(700)
                phraseIndex = (phraseIndex + 1) % phrases.size
            }
        }
    }

    val fadeAlpha by animateFloatAsState(
        targetValue = if (fadeOut) 0f else 1f,
        animationSpec = tween(300),
        label = "fade"
    )

    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "logoAlpha"
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (taglineVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "taglineAlpha"
    )
    val taglineOffset by animateFloatAsState(
        targetValue = if (taglineVisible) 0f else 20f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "taglineOffset"
    )
    val spinnerAlpha by animateFloatAsState(
        targetValue = if (spinnerVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "spinnerAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = Modifier.fillMaxSize().alpha(fadeAlpha),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    logoVisible = false
                    taglineVisible = false
                    spinnerVisible = false
                    navController.navigate(Routes.LOADING) { popUpTo(Routes.LOADING) { inclusive = true } }
                }
            } else {
                // Glow background behind logo
                val glowScale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
                    label = "glowScale"
                )
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.15f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
                    label = "glowAlpha"
                )
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(200.dp * glowScale)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Brush.radialGradient(listOf(Primary.copy(alpha = glowAlpha), Color.Transparent)))
                    )
                    DotLottieAnimation(
                        source = DotLottieSource.Asset("law_and_justice.json"),
                        autoplay = true,
                        loop = true,
                        speed = 1.25f,
                        useFrameInterpolation = false,
                        playMode = Mode.FORWARD,
                        modifier = Modifier
                            .size(160.dp)
                            .scale(logoScale)
                            .alpha(logoAlpha)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "Opoleyes",
                    color = Accent,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(logoAlpha)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "Oposiciones de Justicia",
                    color = TextMuted,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .alpha(taglineAlpha)
                        .padding(start = taglineOffset.dp)
                )

                Spacer(Modifier.height(32.dp))

                CircularProgressIndicator(
                    color = Primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .size(32.dp)
                        .alpha(spinnerAlpha)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    phrases[phraseIndex],
                    color = TextMuted.copy(alpha = pulseAlpha * spinnerAlpha),
                    fontSize = 14.sp,
                    modifier = Modifier.alpha(spinnerAlpha)
                )
            }
        }
    }
}
