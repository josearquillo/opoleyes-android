package com.opoleyes.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.opoleyes.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val maxLife: Float
)

@Composable
fun ParticleBurst(
    trigger: Any?,
    color: Color = Success,
    particleCount: Int = 20,
    modifier: Modifier = Modifier
) {
    var particles by remember { mutableStateOf(listOf<Particle>()) }
    var animTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(trigger) {
        if (trigger != null) {
            val newParticles = mutableListOf<Particle>()
            repeat(particleCount) {
                val angle = Random.nextFloat() * 360f
                val speed = Random.nextFloat() * 300f + 100f
                val radians = Math.toRadians(angle.toDouble())
                newParticles.add(
                    Particle(
                        x = 0.5f,
                        y = 0.5f,
                        vx = (cos(radians) * speed).toFloat(),
                        vy = (sin(radians) * speed).toFloat(),
                        color = color.copy(alpha = Random.nextFloat() * 0.5f + 0.5f),
                        size = Random.nextFloat() * 12f + 4f,
                        maxLife = Random.nextFloat() * 0.5f + 0.5f
                    )
                )
            }
            particles = newParticles
            animTrigger++
        }
    }

    val animatedProgress = remember(animTrigger) {
        Animatable(0f)
    }

    LaunchedEffect(animTrigger) {
        if (animTrigger > 0) {
            animatedProgress.snapTo(0f)
            animatedProgress.animateTo(1f, animationSpec = tween(800, easing = LinearEasing))
            particles = emptyList()
        }
    }

    if (particles.isNotEmpty()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val t = animatedProgress.value
            particles.forEach { p ->
                val progress = t / p.maxLife
                if (progress > 1f) return@forEach
                val x = p.x * size.width + p.vx * progress
                val y = p.y * size.height + p.vy * progress + 200f * progress * progress
                val alpha = (1f - progress).coerceIn(0f, 1f)
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.size * (1f - progress * 0.5f),
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
fun ConfettiBurst(
    trigger: Any?,
    modifier: Modifier = Modifier,
    durationMs: Int = 3000
) {
    var particles by remember { mutableStateOf(listOf<Particle>()) }
    var animTrigger by remember { mutableStateOf(0) }

    val colors = listOf(
        Success, ConfettiBlue, ConfettiAmber,
        Danger, ConfettiPurple, ConfettiCyan,
        ConfettiPink, ConfettiLime
    )

    LaunchedEffect(trigger) {
        if (trigger != null) {
            val newParticles = mutableListOf<Particle>()
            repeat(60) {
                val angle = Random.nextFloat() * 360f
                val speed = Random.nextFloat() * 400f + 150f
                val radians = Math.toRadians(angle.toDouble())
                newParticles.add(
                    Particle(
                        x = 0.5f,
                        y = 0.3f,
                        vx = (cos(radians) * speed).toFloat(),
                        vy = (sin(radians) * speed).toFloat() - 200f,
                        color = colors.random(),
                        size = Random.nextFloat() * 10f + 6f,
                        maxLife = 1f
                    )
                )
            }
            particles = newParticles
            animTrigger++
        }
    }

    val animatedProgress = remember(animTrigger) {
        Animatable(0f)
    }

    LaunchedEffect(animTrigger) {
        if (animTrigger > 0) {
            animatedProgress.snapTo(0f)
            animatedProgress.animateTo(1f, animationSpec = tween(durationMs, easing = LinearEasing))
            particles = emptyList()
        }
    }

    if (particles.isNotEmpty()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val t = animatedProgress.value
            particles.forEach { p ->
                val progress = t
                val x = p.x * size.width + p.vx * progress
                val y = p.y * size.height + p.vy * progress + 400f * progress * progress
                val alpha = (1f - progress).coerceIn(0f, 1f)
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - p.size / 2, y - p.size / 2),
                    size = Size(p.size, p.size * 0.6f)
                )
            }
        }
    }
}

@Composable
fun ShakeBox(
    shakeTrigger: Any?,
    intensity: Float = 12f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger != null) {
            shake.snapTo(0f)
            @Suppress("DEPRECATION")
            shake.animateTo(1f, animationSpec = keyframes {
                durationMillis = 400
                0f at 0 with LinearEasing
                intensity at 50 with LinearEasing
                -intensity at 100 with LinearEasing
                intensity * 0.7f at 150 with LinearEasing
                -intensity * 0.7f at 200 with LinearEasing
                intensity * 0.4f at 250 with LinearEasing
                -intensity * 0.4f at 300 with LinearEasing
                0f at 400 with LinearEasing
            })
        }
    }
    Box(modifier = modifier.offset { IntOffset(shake.value.toInt(), 0) }) {
        content()
    }
}
