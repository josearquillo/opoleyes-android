package com.opoleyes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
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
val Danger = Color(0xFFef4444)
val DangerDark = Color(0xFF991b1b)
val Warning = Color(0xFFfbbf24)
val WarningDark = Color(0xFF92400e)
val TextLight = Color(0xFFe2e8f0)
val TextMuted = Color(0xFF94a3b8)
val TextDim = Color(0xFF64748b)
val SurfaceVariant = Color(0xFF334155)
val Orange = Color(0xFFfb923c)
val OrangeDark = Color(0xFFf97316)
val Cyan = Color(0xFF06b6d4)
val Purple = Color(0xFF7c3aed)
val PurpleDark = Color(0xFF5b21b6)

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
fun OpoleyesTheme(content: @Composable () -> Unit) {
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(LocalContext.current).copy(
            primary = Primary,
            secondary = Accent,
            tertiary = Warning
        )
    } else {
        DarkColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = OPOLEYESTypography,
        shapes = OPOLEYESShapes,
        content = content
    )
}
