package com.opotest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val OPOTESTTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 48.sp, color = TextLight),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, color = TextLight),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, color = TextLight),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextLight),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextLight),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextLight),
    titleSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextLight),
    bodyLarge = TextStyle(fontSize = 16.sp, color = TextLight),
    bodyMedium = TextStyle(fontSize = 14.sp, color = TextLight),
    bodySmall = TextStyle(fontSize = 12.sp, color = TextMuted),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextLight),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, color = TextMuted),
    labelSmall = TextStyle(fontSize = 10.sp, color = TextDim),
)
