package com.opoleyes.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.dotlottie.dlplayer.Mode
import com.opoleyes.R
import com.opoleyes.data.model.XpBreakdown
import com.opoleyes.data.model.XpLine
import com.opoleyes.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class HeartParticle(
    val angle: Float,
    val speed: Float,
    val sizeDp: Float,
    val color: Color,
    val gravity: Float
)

@Composable
fun GameButton(
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    color1: Color = Primary,
    color2: Color = PurpleDark,
    enabled: Boolean = true,
    textFontSize: Int = 18,
    iconSize: Int = 20,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else 8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonElevation"
    )
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .scale(scale)
            .shadow(elevation.dp, shape, clip = false, ambientColor = color1.copy(alpha = 0.4f), spotColor = color1.copy(alpha = 0.6f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(color1, color2)))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { role = Role.Button }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = textFontSize.sp)
        }
    }
}

@Composable
fun OptionCard(
    text: String,
    modifier: Modifier = Modifier,
    isCorrect: Boolean = false,
    isSelected: Boolean = false,
    isWrong: Boolean = false,
    isHintRemoved: Boolean = false,
    answered: Boolean = false,
    userWasCorrect: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val bgColor = when {
        answered && isCorrect && userWasCorrect -> Brush.verticalGradient(listOf(SuccessDark, Success))
        answered && isCorrect && !userWasCorrect -> Brush.verticalGradient(listOf(BgCard, BgDark))
        answered && isWrong -> Brush.verticalGradient(listOf(DangerDark, Danger))
        !answered && isSelected -> Brush.verticalGradient(listOf(Primary, PurpleDark))
        !answered && isHintRemoved -> Brush.verticalGradient(listOf(HintRemoved, HintRemovedDark))
        else -> Brush.verticalGradient(listOf(BgCard, BgDark))
    }
    val textColor = when {
        isHintRemoved && !answered -> TextDim
        isSelected || (isCorrect && answered && userWasCorrect) -> Color.White
        isCorrect && answered && !userWasCorrect -> SuccessLight
        else -> TextOption
    }
    val borderColor = when {
        answered && isCorrect && userWasCorrect -> Success
        answered && isCorrect && !userWasCorrect -> Success.copy(alpha = 0.4f)
        answered && isWrong -> Danger
        else -> Color.Transparent
    }
    val borderWidth = when {
        answered && isCorrect && userWasCorrect -> 2.dp
        answered && isCorrect && !userWasCorrect -> 1.5.dp
        answered && isWrong -> 2.dp
        else -> 0.dp
    }
    val shape = MaterialTheme.shapes.medium

    val showEffect = answered && isCorrect && userWasCorrect
    val pulseScale by animateFloatAsState(
        targetValue = if (showEffect) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "correctPulse"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "correctGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    val flashAnim = remember { Animatable(0f) }
    LaunchedEffect(showEffect) {
        if (showEffect) {
            flashAnim.snapTo(0.6f)
            flashAnim.animateTo(0f, animationSpec = tween(600, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = modifier
            .scale(if (showEffect) pulseScale else 1f)
            .clip(shape)
            .background(bgColor)
            .then(
                if (flashAnim.value > 0f) Modifier.background(Warning.copy(alpha = flashAnim.value))
                else Modifier
            )
            .border(borderWidth, borderColor, shape)
            .then(
                if (showEffect) Modifier.border(3.dp, Warning.copy(alpha = glowPulse), shape)
                else Modifier
            )
            .clickable(
                enabled = enabled && !answered && !isHintRemoved,
                role = Role.Button,
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                color = textColor,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AnimatedHudBar(
    score: Int,
    combo: Int,
    lives: Int,
    timer: Float,
    mode: com.opoleyes.data.model.GameMode,
    questionNum: Int,
    streak: Int = 0,
    maxLives: Int = 3,
    modifier: Modifier = Modifier
) {
    // --- Score animation: roll-up counter + golden glow flash ---
    var displayedScore by remember { mutableStateOf(score) }
    var scoreFlash by remember { mutableStateOf(0f) }
    var scoreGlow by remember { mutableStateOf(0f) }
    val animatedScore by animateIntAsState(
        targetValue = displayedScore,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "scoreRoll"
    )
    LaunchedEffect(score) {
        if (score != displayedScore) {
            scoreFlash = 1f
            scoreGlow = 1f
            displayedScore = score
            kotlinx.coroutines.delay(50)
            scoreFlash = 0f
            kotlinx.coroutines.delay(500)
            scoreGlow = 0f
        }
    }
    val scoreScale by animateFloatAsState(
        targetValue = if (scoreGlow > 0f) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scoreScale"
    )
    val scoreColor by animateColorAsState(
        targetValue = if (scoreGlow > 0f) Warning else PrimaryLight,
        animationSpec = tween(300),
        label = "scoreColor"
    )

    // --- Lives animation: track previous, shatter on loss, pop-in on gain ---
    var prevLives by remember { mutableStateOf(lives) }
    var heartBurst by remember { mutableStateOf(0) }
    var heartPop by remember { mutableStateOf(0) }
    var shatteredIndex by remember { mutableStateOf(-1) }
    var poppedIndex by remember { mutableStateOf(-1) }
    var lifeLostFlash by remember { mutableStateOf(0f) }
    LaunchedEffect(lives) {
        if (lives < prevLives) {
            shatteredIndex = prevLives - 1
            heartBurst++
            lifeLostFlash = 0.4f
            kotlinx.coroutines.delay(600)
            lifeLostFlash = 0f
        } else if (lives > prevLives) {
            poppedIndex = lives - 1
            heartPop++
        }
        prevLives = lives
    }
    val animatedLifeFlash by animateFloatAsState(
        targetValue = lifeLostFlash,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "lifeFlash"
    )
    val showLives = mode == com.opoleyes.data.model.GameMode.SURVIVAL || mode == com.opoleyes.data.model.GameMode.QUICK

    // --- Timer animation: flash only on big jumps (>3s), not natural decrement ---
    var prevTimer by remember { mutableStateOf(timer) }
    var timerFlash by remember { mutableStateOf(0f) }
    LaunchedEffect(timer) {
        if (timer != prevTimer) {
            val diff = timer - prevTimer
            if (diff > 3f) {
                timerFlash = 1f // green flash (time gained)
            } else if (diff < -3f) {
                timerFlash = -1f // red flash (time lost)
            }
            prevTimer = timer
            if (timerFlash != 0f) {
                kotlinx.coroutines.delay(400)
                timerFlash = 0f
            }
        }
    }
    val timerScale by animateFloatAsState(
        targetValue = if (timerFlash != 0f) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "timerScale"
    )
    // Pulsing alpha for critical timer (<10s)
    val timerPulseTransition = rememberInfiniteTransition(label = "timerPulse")
    val timerPulseAlpha by timerPulseTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "timerPulseAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Scrim)
            .then(
                if (animatedLifeFlash > 0f) Modifier.background(Danger.copy(alpha = animatedLifeFlash))
                else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hearts with shatter and pop-in effects
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showLives) {
                for (i in 0 until maxLives) {
                    val isAlive = i < lives
                    HeartIcon(
                        isAlive = isAlive,
                        burstTrigger = if (i == shatteredIndex) heartBurst else 0,
                        popTrigger = if (i == poppedIndex) heartPop else 0
                    )
                    if (i < maxLives - 1) Spacer(Modifier.width(0.dp))
                }
            }
        }

        // Score with roll-up, scale punch and golden glow
        Box(contentAlignment = Alignment.Center) {
            if (scoreGlow > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Warning.copy(alpha = scoreGlow * 0.3f))
                )
            }
            Text(
                "$animatedScore pts",
                color = scoreColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                modifier = Modifier
                    .scale(scoreScale)
                    .then(
                        if (scoreFlash > 0f) Modifier.shadow(12.dp, RoundedCornerShape(8.dp), ambientColor = Warning, spotColor = Warning)
                        else Modifier
                    )
            )
        }

        // Timer with flash effect and critical pulsing
        if (mode == com.opoleyes.data.model.GameMode.TIMETRIAL) {
            val isCritical = timer < 10
            val isLow = timer in 10f..30f
            val timerColor = when {
                timerFlash > 0f -> Success
                timerFlash < 0f -> Danger
                isCritical -> Danger.copy(alpha = timerPulseAlpha)
                isLow -> Orange
                else -> TextLight
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.scale(timerScale)
            ) {
                Icon(Icons.Default.Timer, contentDescription = "Tiempo", tint = timerColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                Text("${timer.toInt()}s", color = timerColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
            }
        }

        Text("Pregunta $questionNum", color = TextDim, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun HeartIcon(
    isAlive: Boolean,
    burstTrigger: Int,
    popTrigger: Int
) {
    // Shatter phase: 0=idle, 1=tremble, 2=grow, 3=particles, 4=done
    var shatterPhase by remember { mutableStateOf(0) }
    var shatterScale by remember { mutableStateOf(1f) }
    var shatterAlpha by remember { mutableStateOf(1f) }
    var shatterRotation by remember { mutableStateOf(0f) }
    var trembleOffset by remember { mutableStateOf(0f) }
    var flashAlpha by remember { mutableStateOf(0f) }
    var shockwaveScale by remember { mutableStateOf(0f) }
    var shockwaveAlpha by remember { mutableStateOf(0f) }
    var shockwave2Scale by remember { mutableStateOf(0f) }
    var shockwave2Alpha by remember { mutableStateOf(0f) }
    var fragmentProgress by remember { mutableStateOf(0f) }
    var glowAlpha by remember { mutableStateOf(0f) }
    var particles by remember { mutableStateOf<List<HeartParticle>>(emptyList()) }

    // Pop-in animation when a heart is gained
    var popScale by remember { mutableStateOf(1f) }

    LaunchedEffect(burstTrigger) {
        if (burstTrigger > 0) {
            // Phase 1: Tremble with red glow (0-300ms)
            shatterPhase = 1
            shatterAlpha = 1f
            shatterScale = 1f
            shatterRotation = 0f
            glowAlpha = 1f
            repeat(6) {
                trembleOffset = if (it % 2 == 0) 5f else -5f
                kotlinx.coroutines.delay(50)
            }
            trembleOffset = 0f
            glowAlpha = 0f

            // Phase 2: Gradual growth to 2x + shockwave + flash (300-700ms)
            shatterPhase = 2
            shatterScale = 2f
            flashAlpha = 1f
            shockwaveScale = 0.2f
            shockwaveAlpha = 1f
            shockwave2Scale = 0.1f
            shockwave2Alpha = 0.7f
            kotlinx.coroutines.delay(50)
            flashAlpha = 0f
            shockwaveScale = 1f
            shockwaveAlpha = 0f
            shockwave2Scale = 0.8f
            shockwave2Alpha = 0f
            shatterAlpha = 0f
            shatterRotation = 20f
            kotlinx.coroutines.delay(350)

            // Phase 3: Particle explosion (700-1300ms)
            shatterPhase = 3
            // Generate ~20 particles with random angles, speeds, sizes and colors
            val colors = listOf(Danger, DangerDark, Danger.copy(alpha = 0.8f), Danger.copy(alpha = 0.6f))
            particles = List(20) {
                HeartParticle(
                    angle = Random.nextFloat() * 360f,
                    speed = Random.nextFloat() * 0.5f + 0.5f,
                    sizeDp = Random.nextFloat() * 3f + 1.5f,
                    color = colors.random(),
                    gravity = Random.nextFloat() * 0.4f + 0.15f
                )
            }
            fragmentProgress = 0f
            kotlinx.coroutines.delay(50)
            fragmentProgress = 1f
            kotlinx.coroutines.delay(550)

            // Done
            shatterPhase = 0
            particles = emptyList()
        }
    }

    LaunchedEffect(popTrigger) {
        if (popTrigger > 0) {
            popScale = 0.3f
            kotlinx.coroutines.delay(50)
            popScale = 1.3f
            kotlinx.coroutines.delay(150)
            popScale = 1f
        }
    }

    val animatedShatterScale by animateFloatAsState(
        targetValue = shatterScale,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "shatterScale"
    )
    val animatedShatterAlpha by animateFloatAsState(
        targetValue = shatterAlpha,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "shatterAlpha"
    )
    val animatedShatterRotation by animateFloatAsState(
        targetValue = shatterRotation,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "shatterRot"
    )
    val animatedTremble by animateFloatAsState(
        targetValue = trembleOffset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh),
        label = "tremble"
    )
    val animatedFlash by animateFloatAsState(
        targetValue = flashAlpha,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "flash"
    )
    val animatedShockwaveScale by animateFloatAsState(
        targetValue = shockwaveScale,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "shockwaveScale"
    )
    val animatedShockwaveAlpha by animateFloatAsState(
        targetValue = shockwaveAlpha,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "shockwaveAlpha"
    )
    val animatedShockwave2Scale by animateFloatAsState(
        targetValue = shockwave2Scale,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "shockwave2Scale"
    )
    val animatedShockwave2Alpha by animateFloatAsState(
        targetValue = shockwave2Alpha,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "shockwave2Alpha"
    )
    val animatedFragmentProgress by animateFloatAsState(
        targetValue = fragmentProgress,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "fragmentProgress"
    )
    val animatedGlow by animateFloatAsState(
        targetValue = glowAlpha,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "glow"
    )
    val animatedPopScale by animateFloatAsState(
        targetValue = popScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "popScale"
    )

    // 32dp container - enough room for heart and effects
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(32.dp)
    ) {
        // Red glow behind heart during tremble
        if (animatedGlow > 0f) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Danger.copy(alpha = animatedGlow * 0.3f))
            )
        }

        // Shockwave ring 1
        if (shatterPhase >= 2 && animatedShockwaveAlpha > 0f) {
            Canvas(modifier = Modifier.size(30.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val maxRadius = size.minDimension / 2
                val radius = maxRadius * animatedShockwaveScale
                drawCircle(
                    color = Danger.copy(alpha = animatedShockwaveAlpha),
                    radius = radius,
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Shockwave ring 2
        if (shatterPhase >= 2 && animatedShockwave2Alpha > 0f) {
            Canvas(modifier = Modifier.size(30.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val maxRadius = size.minDimension / 2
                val radius = maxRadius * animatedShockwave2Scale
                drawCircle(
                    color = Danger.copy(alpha = animatedShockwave2Alpha),
                    radius = radius,
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // Particle explosion — 20 particles flying outward with gravity + fade
        if (shatterPhase == 3 && particles.isNotEmpty()) {
            Canvas(modifier = Modifier.size(60.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val p = animatedFragmentProgress
                val maxDist = size.minDimension * 0.45f
                particles.forEach { particle ->
                    val rad = Math.toRadians(particle.angle.toDouble()).toFloat()
                    val dist = p * particle.speed * maxDist
                    val fx = cx + cos(rad) * dist
                    // gravity pulls particles downward as they fly out
                    val fy = cy + sin(rad) * dist + particle.gravity * p * p * size.height * 0.5f
                    val alpha = (1f - p).coerceIn(0f, 1f)
                    val radius = particle.sizeDp.dp.toPx() * (1f - p * 0.4f)
                    drawCircle(
                        color = particle.color.copy(alpha = alpha),
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(fx, fy)
                    )
                }
            }
        }

        // Flash overlay
        if (animatedFlash > 0f) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Danger.copy(alpha = animatedFlash))
            )
        }

        // The heart itself
        if (isAlive) {
            val infiniteTransition = rememberInfiniteTransition(label = "heartGlow")
            val heartPulse by infiniteTransition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
                label = "heartPulse"
            )
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Vida",
                tint = Danger,
                modifier = Modifier
                    .size(24.dp)
                    .scale(animatedPopScale * heartPulse)
            )
        } else if (shatterPhase in 1..2 && animatedShatterAlpha > 0f) {
            val trembleX = if (shatterPhase == 1) animatedTremble else 0f
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Vida perdida",
                tint = Danger.copy(alpha = animatedShatterAlpha),
                modifier = Modifier
                    .size(24.dp)
                    .scale(animatedShatterScale)
                    .graphicsLayer {
                        rotationZ = animatedShatterRotation
                        translationX = trembleX
                    }
            )
        }
    }
}

@Composable
fun ComboBar(
    fill: Float,
    overchargeActive: Boolean,
    overchargeCharges: Int,
    combo: Int = 0,
    streak: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Combo multiplier + streak text above the bar
        if (combo > 0) {
            val streakText = if (streak > 0) " · ${streak % 5}/5" else ""
            val comboColor = if (overchargeActive) Warning else if (combo >= 10) Danger else if (combo >= 5) Orange else PrimaryLight
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 3.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = "Combo", tint = comboColor, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(3.dp))
                Text(
                    "x$combo$streakText",
                    color = comboColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(TrackColor)
        ) {
            val gradient = if (overchargeActive) {
                Brush.horizontalGradient(listOf(Warning, WarningDark, Warning))
            } else if (fill < 0.3f) {
                Brush.horizontalGradient(listOf(Primary, PrimaryLight))
            } else if (fill < 0.7f) {
                Brush.horizontalGradient(listOf(Orange, OrangeDark))
            } else {
                Brush.horizontalGradient(listOf(Danger, Warning))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(gradient)
            )
        }
        if (overchargeActive) {
            Text(
                "⚡ OVERCHARGE x$overchargeCharges",
                color = Warning,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Primary,
    height: Int = 8
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape((height / 2).dp))
            .background(TrackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height.dp)
                .clip(RoundedCornerShape((height / 2).dp))
                .background(color)
        )
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(BgCard, BgDark)))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = TextDim, fontSize = 11.sp)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurface)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun StatCardWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(BgCard, BgDark)))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = TextDim, fontSize = 11.sp)
    }
}

@Composable
fun LoadingOverlay() {
    // Uses Dialog so back-handling, focus and window insets are managed by the platform,
    // and touches outside are blocked without a no-op clickable.
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScrimStrong),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DotLottieAnimation(
                    source = DotLottieSource.Asset("law_and_justice.json"),
                    autoplay = true,
                    loop = true,
                    speed = 1.25f,
                    useFrameInterpolation = false,
                    playMode = Mode.FORWARD,
                    modifier = Modifier.size(120.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.loading_questions), color = TextLight, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun XpSummaryOverlay(
    breakdown: XpBreakdown,
    onDismiss: () -> Unit
) {
    var revealedLines by remember { mutableStateOf(0) }
    var displayTotal by remember { mutableStateOf(0) }
    var particleTrigger by remember { mutableStateOf<Any?>(null) }
    var finished by remember { mutableStateOf(false) }

    // Secuencia de revelado: una línea cada 700ms con count-up del total.
    // Se cancela automáticamente al salir del Composition (al pulsar Saltar/fuera).
    LaunchedEffect(breakdown) {
        revealedLines = 0
        displayTotal = 0
        finished = false
        breakdown.lines.forEachIndexed { idx, line ->
            delay(700)
            revealedLines = idx + 1
            particleTrigger = Any()
            val start = displayTotal
            val end = start + line.value
            if (end > start) {
                val steps = 30
                for (i in 1..steps) {
                    val t = i.toFloat() / steps
                    val eased = 1 - (1 - t) * (1 - t) * (1 - t)
                    displayTotal = (start + (end - start) * eased).toInt()
                    delay(15)
                }
            }
            displayTotal = end
        }
        delay(400)
        finished = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrimStrong)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(BgCard, BgDark)))
                .border(2.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .shadow(16.dp, RoundedCornerShape(20.dp))
                .clickable(enabled = false) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fila superior: título + botón Saltar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✨ XP GANADA", color = AccentLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (!finished) {
                    Text(
                        "Saltar ⏭",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            // Total grande con count-up + glow pulsante
            val glowPulse by rememberInfiniteTransition(label = "glow").animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "pulse"
            )
            Text(
                "+$displayTotal",
                color = AccentLight.copy(alpha = glowPulse),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )
            Text("XP", color = TextMuted, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = BgDark, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            // Lista de líneas (van apareciendo una a una)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                breakdown.lines.forEachIndexed { idx, line ->
                    AnimatedVisibility(
                        visible = idx < revealedLines,
                        enter = slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            initialOffsetY = { it / 2 }
                        ) + fadeIn(tween(200)),
                        exit = fadeOut(tween(200))
                    ) {
                        XpSummaryRow(line)
                    }
                }
            }

            // Total final + botón Continuar (solo al terminar)
            AnimatedVisibility(
                visible = finished,
                enter = fadeIn(tween(300)) + expandVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = BgDark, thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("+${breakdown.total} XP", color = AccentLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(20.dp))
                    GameButton(
                        text = "Continuar",
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        color1 = Accent,
                        color2 = WarningDark,
                        onClick = onDismiss
                    )
                }
            }
        }
    }

    // Particle burst detrás del panel (al revelar cada línea)
    ParticleBurst(
        trigger = particleTrigger,
        color = AccentLight,
        particleCount = 18,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun XpSummaryRow(line: XpLine) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ScrimStrong,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        line.color.copy(alpha = 0.25f),
                        ScrimStrong,
                        line.color.copy(alpha = 0.25f)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(line.icon, fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                line.label,
                color = TextLight,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (line.value > 0) {
                Text("+${line.value}", color = line.color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("aplicado", color = line.color.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        }
    }
}
