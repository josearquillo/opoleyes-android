package com.opoleyes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Haptic feedback helper for consistent vibration across the app.
 *
 * Usage:
 *   val haptics = rememberHaptics()
 *   haptics.correct()   // on correct answer
 *   haptics.wrong()     // on wrong answer
 *   haptics.reward()    // on chest open / rank up
 */
class Haptics(private val hf: HapticFeedback) {
    fun correct() = hf.performHapticFeedback(HapticFeedbackType.LongPress)
    fun wrong() = hf.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    fun reward() = hf.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberHaptics(): Haptics {
    val hf = LocalHapticFeedback.current
    return remember(hf) { Haptics(hf) }
}
