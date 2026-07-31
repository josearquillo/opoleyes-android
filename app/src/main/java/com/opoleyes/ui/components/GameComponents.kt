package com.opoleyes.ui.components

import androidx.compose.animation.core.*
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
        targetValue = if (showEffect) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "correctPulse"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "correctGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = modifier
            .scale(if (showEffect) pulseScale else 1f)
            .clip(shape)
            .background(bgColor)
            .border(borderWidth, borderColor, shape)
            .then(
                if (showEffect) Modifier.border(2.dp, Warning.copy(alpha = glowPulse), shape)
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
fun ComboBar(
    fill: Float,
    overchargeActive: Boolean,
    overchargeCharges: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 60.dp)) {
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
