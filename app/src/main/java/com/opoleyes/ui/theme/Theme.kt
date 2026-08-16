package com.opoleyes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val BgDark = Color(0xFF0f172a)
val BgCard = Color(0xFF1e293b)
val Primary = Color(0xFF7c3aed)
val PrimaryLight = Color(0xFF9d5cf0)
val Accent = Color(0xFFf5b342)
val AccentLight = Color(0xFFfcd34d)
val Success = Color(0xFF22c55e)
val SuccessDark = Color(0xFF15803d)
val SuccessLight = Color(0xFF86efac)
val Danger = Color(0xFFef4444)
val DangerDark = Color(0xFF991b1b)
val Warning = Color(0xFFfbbf24)
val WarningDark = Color(0xFF92400e)
val TextLight = Color(0xFFe2e8f0)
// Bumped from 0xFF94a3b8 (~4.6:1 on BgDark, ~3.9:1 on BgCard) to meet WCAG AA 4.5:1 on both surfaces.
val TextMuted = Color(0xFFa3b1c7)
// Bumped from 0xFF64748b (~2.9:1) to meet WCAG AA 4.5:1 on BgDark.
val TextDim = Color(0xFF8b98ad)
val TextOption = Color(0xFFcbd5e1)
val SurfaceVariant = Color(0xFF334155)
val Orange = Color(0xFFfb923c)
val OrangeDark = Color(0xFFf97316)
val HintRemoved = Color(0xFF2a2a3e)
val HintRemovedDark = Color(0xFF1f1f2e)
val PurpleDark = Color(0xFF5b21b6)

// Chest rarity colors.
val Silver = Color(0xFFcbd5e1)

// Surface overlays (replace hardcoded Color.White/Black alpha throughout the app).
val TrackColor = Color.White.copy(alpha = 0.1f)       // progress bar tracks
val TrackColorDim = Color.White.copy(alpha = 0.08f)   // XP bar track
val GlassSurface = Color.White.copy(alpha = 0.06f)    // GlassCard / subtle surfaces
val Scrim = Color.Black.copy(alpha = 0.55f)           // modal overlays (chest)
val ScrimStrong = Color.Black.copy(alpha = 0.7f)      // HUD / loading overlay / popups
val ScrimHeavy = Color.Black.copy(alpha = 0.85f)      // full-screen modal (rank-up)

// Confetti / particle effect palette (used by GameEffects).
val ConfettiBlue = Color(0xFF3b82f6)
val ConfettiAmber = Color(0xFFf59e0b)
val ConfettiPurple = Color(0xFFa855f7)
val ConfettiCyan = Color(0xFF06b6d4)
val ConfettiPink = Color(0xFFec4899)
val ConfettiLime = Color(0xFF84cc16)

/**
 * Spacing constants for consistent padding across screens.
 * Use [Spacing.screen] for screen-level content padding,
 * [Spacing.card] for card-internal padding,
 * [Spacing.compact] for tight spacing between elements.
 */
object Spacing {
    val screen = 16.dp
    val card = 16.dp
    val comfortable = 20.dp
    val generous = 24.dp
    val compact = 8.dp
    val tight = 6.dp
    val micro = 4.dp
}

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = BgCard,
    onPrimaryContainer = TextLight,
    secondary = Accent,
    onSecondary = Color.White,
    tertiary = Warning,
    onTertiary = Color.Black,
    background = BgDark,
    onBackground = TextLight,
    surface = BgCard,
    onSurface = TextLight,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextMuted,
    error = Danger,
    onError = Color.White,
)

@Composable
fun OpoleyesTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = OPOLEYESTypography,
        shapes = OPOLEYESShapes,
        content = content
    )
}
