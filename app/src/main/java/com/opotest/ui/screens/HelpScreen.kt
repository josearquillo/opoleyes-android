package com.opotest.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opotest.ui.theme.*

@Composable
fun HelpScreen(navController: NavController) {
    val sections = listOf(
        HelpSectionData("🎯", "Objetivo", listOf(
            "Responde preguntas de oposiciones de justicia.",
            "Gana XP, sube de rango y desbloquea modos.",
            "Supera tus récords y domina todas las leyes."
        )),
        HelpSectionData("🎮", "Modos", listOf(
            "❤️ Supervivencia: 3 vidas, sin tiempo.",
            "⏱️ Contrarreloj: 180s, +15s acierto, -10s fallo.",
            "⚡ Repaso Express: 20 preguntas de fallos previos.",
            "🏆 Modo Reto: 120s, máxima dificultad, todas las leyes."
        )),
        HelpSectionData("✨", "Power-ups", listOf(
            "💡 Pista: Elimina 1 opción incorrecta.",
            "🛡️ Escudo: Auto-consumo al fallar, sin penalización.",
            "🎯 50/50: Elimina 2 opciones incorrectas.",
            "✨ x2 pts: Doble puntuación en 1 respuesta.",
            "🧊 Congelar: Pausa el timer 10s.",
            "❤️ Recuperación: Recupera 1 vida cada 5 aciertos."
        )),
        HelpSectionData("🔥", "Combo", listOf(
            "Cada acierto consecutivo aumenta el combo.",
            "Puntos = 10 × combo actual.",
            "La barra de combo se llena cada 10 aciertos.",
            "Overcharge: 3 cargas gratis (+vida o +30s)."
        )),
        HelpSectionData("🌱", "Rangos", listOf(
            "12 rangos desde Novato (0 XP) hasta Leyenda (100.000 XP).",
            "Cada rango desbloquea nuevos modos y power-ups.",
            "Gana XP acertando preguntas y completando misiones."
        )),
        HelpSectionData("🪵", "Cofres", listOf(
            "🪵 Madera: 50-150 XP (default).",
            "🥈 Plata: 150-350 XP + 1 power-up (récord).",
            "🥇 Oro: 300-600 XP + 2 power-ups + x2 XP (récord + 90% + 5 preg)."
        )),
        HelpSectionData("📋", "Misiones", listOf(
            "Misiones diarias generadas con seed.",
            "1-3 misiones según rango.",
            "50 XP por misión, +200 XP si completas todas.",
            "Racha de días completando todas."
        ))
    )

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("❓ Ayuda", color = TextLight, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        sections.forEach { section ->
            HelpSection(section)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HelpSection(data: HelpSectionData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(BgCard, BgCardDark)))
            .padding(16.dp)
    ) {
        Text("${data.icon} ${data.title}", color = PrimaryLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        data.lines.forEach { line ->
            Text(line, color = TextMuted, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
        }
    }
}

private data class HelpSectionData(val icon: String, val title: String, val lines: List<String>)
