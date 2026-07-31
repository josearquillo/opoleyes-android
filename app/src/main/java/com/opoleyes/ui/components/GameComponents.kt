package com.opoleyes.ui.components

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
import com.opoleyes.ui.theme.*

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
        answered && isCorrect && !userWasCorrect -> Brush.verticalGradient(listOf(BgCard, BgCardDark))
        answered && isWrong -> Brush.verticalGradient(listOf(DangerDark, Danger))
        !answered && isSelected -> Brush.verticalGradient(listOf(Primary, PurpleDark))
        !answered && isHintRemoved -> Brush.verticalGradient(listOf(HintRemoved, HintRemovedDark))
        else -> Brush.verticalGradient(listOf(BgCard, BgCardDark))
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
fun HudBar(
    score: Int,
    combo: Int,
    lives: Int,
    timer: Float,
    mode: com.opoleyes.data.model.GameMode,
    questionNum: Int,
    streak: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (mode == com.opoleyes.data.model.GameMode.SURVIVAL || mode == com.opoleyes.data.model.GameMode.QUICK) {
                repeat(lives) { Icon(Icons.Default.Favorite, contentDescription = "Vida", tint = Danger, modifier = Modifier.size(18.dp)) }
            }
        }
        Text(
            "$score pts",
            color = PrimaryLight,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        if (mode == com.opoleyes.data.model.GameMode.TIMETRIAL || mode == com.opoleyes.data.model.GameMode.CHALLENGE) {
            val timerColor = if (timer < 10) Danger else TextLight
            val timerText = "${timer.toInt()}s"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = "Tiempo: $timerText", tint = timerColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                Text(timerText, color = timerColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        if (streak > 0) {
            val streakLeft = 5 - (streak % 5)
            val streakColor = if (streakLeft == 1) Warning else TextMuted
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = "Racha ${streak % 5} de 5", tint = streakColor, modifier = Modifier.size(13.dp))
                Text("${streak % 5}/5", color = streakColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Text("Pregunta $questionNum", color = TextDim, fontSize = 13.sp)
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
    val maxLives = 3
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
            .background(Color.Black.copy(alpha = 0.5f))
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
        if (mode == com.opoleyes.data.model.GameMode.TIMETRIAL || mode == com.opoleyes.data.model.GameMode.CHALLENGE) {
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
    // Shatter phase: 0=idle, 1=tremble, 2=expand+flash, 3=fragments, 4=done
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

    // Pop-in animation when a heart is gained
    var popScale by remember { mutableStateOf(1f) }

    LaunchedEffect(burstTrigger) {
        if (burstTrigger > 0) {
            // Phase 1: Tremble with red glow (0-400ms)
            shatterPhase = 1
            shatterAlpha = 1f
            shatterScale = 1f
            shatterRotation = 0f
            glowAlpha = 1f
            repeat(8) {
                trembleOffset = if (it % 2 == 0) 6f else -6f
                kotlinx.coroutines.delay(50)
            }
            trembleOffset = 0f
            glowAlpha = 0f

            // Phase 2: Expansion + double shockwave + flash (400-700ms)
            shatterPhase = 2
            shatterScale = 1.8f
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
            shatterRotation = 30f
            kotlinx.coroutines.delay(300)

            // Phase 3: Fragments flying out (700-1200ms)
            shatterPhase = 3
            fragmentProgress = 0f
            kotlinx.coroutines.delay(50)
            fragmentProgress = 1f
            kotlinx.coroutines.delay(450)

            // Done
            shatterPhase = 0
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
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
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
        animationSpec = tween(450, easing = FastOutSlowInEasing),
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

    // 24dp container - compact but enough room for effects
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(24.dp)
    ) {
        // Red glow behind heart during tremble
        if (animatedGlow > 0f) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Danger.copy(alpha = animatedGlow * 0.3f))
            )
        }

        // Shockwave ring 1
        if (shatterPhase >= 2 && animatedShockwaveAlpha > 0f) {
            Canvas(modifier = Modifier.size(24.dp)) {
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
            Canvas(modifier = Modifier.size(24.dp)) {
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

        // Fragments flying outward
        if (shatterPhase == 3) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val p = animatedFragmentProgress
                val fragmentSize = (1f - p) * 2.5.dp.toPx()
                val maxDist = size.minDimension * 0.4f
                val directions = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
                directions.forEach { angle ->
                    val rad = Math.toRadians(angle.toDouble()).toFloat()
                    val dist = p * maxDist
                    val fx = cx + Math.cos(rad.toDouble()).toFloat() * dist
                    val fy = cy + Math.sin(rad.toDouble()).toFloat() * dist
                    drawCircle(
                        color = Danger.copy(alpha = (1f - p) * 0.9f),
                        radius = fragmentSize,
                        center = androidx.compose.ui.geometry.Offset(fx, fy)
                    )
                }
            }
        }

        // Flash overlay
        if (animatedFlash > 0f) {
            Box(
                modifier = Modifier
                    .size(22.dp)
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
                    .size(18.dp)
                    .scale(animatedPopScale * heartPulse)
            )
        } else if (shatterPhase in 1..2 && animatedShatterAlpha > 0f) {
            val trembleX = if (shatterPhase == 1) animatedTremble else 0f
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Vida perdida",
                tint = Danger.copy(alpha = animatedShatterAlpha),
                modifier = Modifier
                    .size(18.dp)
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
                .background(Color.White.copy(alpha = 0.1f))
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
            .background(Color.White.copy(alpha = 0.1f))
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
            .background(Brush.verticalGradient(listOf(BgCard, BgCardDark)))
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
            .background(Color.White.copy(alpha = 0.06f))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun RankBadge(rank: com.opoleyes.data.model.Rank, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Primary, Accent)))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text("${rank.icon} ${rank.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun CircularProgressRing(
    progress: Float,
    size: Int = 48,
    strokeWidth: Int = 4,
    ringColor: Color = Primary,
    trackColor: Color = Color.White.copy(alpha = 0.1f),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(progress, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }
    Box(modifier = modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size.dp)) {
            val sweep = 360f * animatedProgress.value
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        content()
    }
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
            .background(Brush.verticalGradient(listOf(BgCard, BgCardDark)))
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
                .background(Color.Black.copy(alpha = 0.7f)),
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
