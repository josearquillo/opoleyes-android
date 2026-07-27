package com.opoleyes.ui.screens

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
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HelpScreen(navController: NavController) {
    val sections = listOf(
        HelpSectionData("🎯", "Objetivo", listOf(
            "Responde preguntas de oposiciones de justicia.",
            "Gana puntos de experiencia (XP), sube de rango y desbloquea nuevos modos de juego.",
            "Supera tus récords y domina todas las leyes."
        )),
        HelpSectionData("🎮", "Modos de juego", listOf(
            "❤️ Supervivencia: Tienes 3 vidas y sin límite de tiempo. Falla 3 veces y se acaba.",
            "⏱️ Contrarreloj: 3 minutos. Cada acierto suma 15 segundos, cada fallo resta 10.",
            "⚡ Repaso Express: 20 preguntas de las que has fallado antes, para repasar tus errores.",
            "🏆 Modo Reto: 2 minutos, máxima dificultad, preguntas de todas las leyes a la vez."
        )),
        HelpSectionData("✨", "Ayudas (Power-ups)", listOf(
            "💡 Pista: Quita una respuesta incorrecta de la pantalla.",
            "🛡️ Escudo: Si fallas, se consume automáticamente y no te penaliza.",
            "🎯 50/50: Elimina dos respuestas incorrectas de golpe.",
            "✨ x2 pts: La próxima respuesta que aciertes vale el doble de puntos.",
            "🧊 Congelar: Pausa el cronómetro durante 10 segundos.",
            "❤️ Recuperación: Recupera 1 vida cada 5 aciertos seguidos."
        )),
        HelpSectionData("🔥", "Combo", listOf(
            "Cada vez que aciertes seguidas, tu combo sube.",
            "Cuanto más alto sea el combo, más puntos ganas por respuesta (10 × combo).",
            "La barra de combo se llena cada 10 aciertos consecutivos.",
            "Al llenarla, obtienes 3 cargas gratis que puedes canjear por vida extra o tiempo adicional."
        )),
        HelpSectionData("🌱", "Rangos", listOf(
            "Hay 12 rangos, desde Novato (0 XP) hasta Leyenda (100.000 XP).",
            "Cada rango desbloquea nuevos modos de juego y ayudas.",
            "Gana XP acertando preguntas y completando misiones diarias."
        )),
        HelpSectionData("🎁", "Cofres", listOf(
            "📦 Cofre de madera: Entre 50 y 150 XP. Lo recibes al terminar una partida normal.",
            "📦 Cofre de plata: Entre 150 y 350 XP y una ayuda extra. Lo recibes si bates tu récord de puntuación.",
            "📦 Cofre de oro: Entre 300 y 600 XP, dos ayudas extra y doble XP. Lo recibes si bates tu récord con un 90% de aciertos respondiendo al menos 5 preguntas."
        )),
        HelpSectionData("📋", "Misiones diarias", listOf(
            "Cada día recibes misiones nuevas y diferentes.",
            "Puedes tener entre 1 y 3 misiones según tu rango.",
            "Cada misión completada te da 50 XP. Si completas todas, recibes 200 XP extra.",
            "¡Intenta completarlas todos los días seguidos para mantener tu racha!"
        ))
    )

    val context = navController.context
    val prefs = PreferencesManager(context)
    var debugMode by remember { mutableStateOf(prefs.isDebugMode()) }
    var debugToast by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            "❓ Ayuda",
            color = TextLight,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = {
                    debugMode = !debugMode
                    prefs.setDebugMode(debugMode)
                    debugToast = if (debugMode) "Modo debug activado" else "Modo debug desactivado"
                }
            )
        )
        if (debugToast != null) {
            Spacer(Modifier.height(4.dp))
            Text(debugToast!!, color = if (debugMode) Success else TextMuted, fontSize = 12.sp)
        }
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
