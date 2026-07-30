package com.opoleyes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.Build

val BgDark = Color(0xFF0f172a)
val BgCard = Color(0xFF1e293b)
val BgCardDark = Color(0xFF0f172a)
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
val TextMuted = Color(0xFF94a3b8)
// Bumped from 0xFF64748b (~2.9:1) to meet WCAG AA 4.5:1 on BgDark.
val TextDim = Color(0xFF8b98ad)
val TextOption = Color(0xFFcbd5e1)
val SurfaceVariant = Color(0xFF334155)
val Orange = Color(0xFFfb923c)
val OrangeDark = Color(0xFFf97316)
val HintRemoved = Color(0xFF2a2a3e)
val HintRemovedDark = Color(0xFF1f1f2e)
val PurpleDark = Color(0xFF5b21b6)

// Light theme tokens
val BgLight = Color(0xFFf8fafc)
val BgCardLight = Color(0xFFffffff)
val BgCardDarkLight = Color(0xFFe2e8f0)
val TextDark = Color(0xFF0f172a)
val TextMutedLight = Color(0xFF475569)
val TextDimLight = Color(0xFF64748b)
val SurfaceVariantLight = Color(0xFFe2e8f0)

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

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFede9fe),
    onPrimaryContainer = Color(0xFF3b0764),
    secondary = Color(0xFFb45309),
    onSecondary = Color.White,
    tertiary = Color(0xFFb45309),
    onTertiary = Color.White,
    background = BgLight,
    onBackground = TextDark,
    surface = BgCardLight,
    onSurface = TextDark,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextMutedLight,
    error = Danger,
    onError = Color.White,
)

@Composable
fun OpoleyesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = OPOLEYESTypography,
        shapes = OPOLEYESShapes,
        content = content
    )
}
