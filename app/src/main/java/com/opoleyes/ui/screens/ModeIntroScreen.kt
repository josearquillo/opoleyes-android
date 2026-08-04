package com.opoleyes.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.R
import com.opoleyes.data.Constants
import com.opoleyes.data.model.GameMode
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*

private data class IntroCard(
    val icon: String,
    val title: String,
    val desc: String,
    val visual: String = ""
)

private data class IntroContent(
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val cards: List<IntroCard>,
    val footer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeIntroScreen(navController: NavController, gameViewModel: GameViewModel) {
    val mode = gameViewModel.pendingMode
    val rankIndex = gameViewModel.getEngineRankIndex()
    val content = remember(mode, rankIndex) { buildIntroContent(mode, rankIndex) }

    var dontShowAgain by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(content.title, color = TextLight, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        gameViewModel.exitGame()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = TextLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark,
                    titleContentColor = TextLight
                )
            )
        },
        containerColor = BgDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                content.subtitle,
                color = content.accentColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            content.cards.forEach { card ->
                IntroInfoCard(card)
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                content.footer,
                color = TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // "Don't show again" checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dontShowAgain = !dontShowAgain },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = dontShowAgain,
                    onCheckedChange = { dontShowAgain = it },
                    colors = CheckboxDefaults.colors(checkedColor = content.accentColor)
                )
                Text(
                    stringResource(R.string.intro_dont_show_again),
                    color = TextMuted,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Play button
            Button(
                onClick = {
                    gameViewModel.dismissModeIntro(mode, dontShowAgain)
                    val route = if (mode == GameMode.EXAM) Routes.EXAM else Routes.GAME
                    navController.navigate(route) {
                        popUpTo(Routes.MODE_INTRO) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = content.accentColor)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.intro_play),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun IntroInfoCard(card: IntroCard) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(BgCard, BgDark)))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
            Text(card.icon, fontSize = 26.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(card.title, color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(card.desc, color = TextMuted, fontSize = 13.sp)
        }
        if (card.visual.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(
                card.visual,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AccentLight,
                textAlign = TextAlign.End,
                maxLines = 2,
                modifier = Modifier.widthIn(max = 72.dp)
            )
        }
    }
}

private fun buildIntroContent(mode: GameMode, rankIndex: Int): IntroContent {
    return when (mode) {
        GameMode.SURVIVAL -> buildSurvivalIntro(rankIndex)
        GameMode.TIMETRIAL -> buildTimetrialIntro(rankIndex)
        GameMode.QUICK -> buildQuickIntro(rankIndex)
        GameMode.EXAM -> buildExamIntro(rankIndex)
        GameMode.SIMULACRO -> buildSimulacroIntro(rankIndex)
    }
}

private fun buildSurvivalIntro(rankIndex: Int): IntroContent {
    val maxOptions = Constants.MAX_OPTIONS_BY_RANK[rankIndex] ?: 4
    val maxLives = Constants.MAX_LIVES_BY_RANK[rankIndex] ?: 3
    val maxDiff = Constants.MAX_DIFFICULTY_BY_RANK[rankIndex] ?: 5
    val powerUps = Constants.AVAILABLE_POWERUPS_BY_RANK[rankIndex]
        ?: listOf("shield", "doubleScore", "fiftyFifty", "hint")

    val hearts = "❤️".repeat(maxLives)
    val optionLetters = (0 until maxOptions).joinToString("") { listOf("A", "B", "C", "D")[it] }

    val powerUpText = when {
        powerUps.isEmpty() -> "Sin power-ups"
        powerUps.size <= 2 -> "Escudo y Doble Puntos"
        else -> "Escudo, Doble Puntos, 50/50 y Pista"
    }
    val powerUpIcons = when {
        powerUps.isEmpty() -> "🚫"
        powerUps.size <= 2 -> "🛡️ ✨"
        else -> "🛡️ ✨ ✂️ 💡"
    }

    val rankName = Constants.getRankByIndex(rankIndex).name
    val title = when (rankIndex) {
        0 -> "¡Bienvenido a Supervivencia!"
        1 -> "Supervivencia: Principiante"
        2 -> "Supervivencia: Modo Completo"
        else -> "Supervivencia"
    }
    val subtitle = "Rango: $rankName"

    val cards = mutableListOf<IntroCard>()
    cards.add(IntroCard("❤️", "Corazones", "Empiezas con $maxLives corazones. Cada fallo te quita uno. Los combos recuperan vida.", hearts))
    cards.add(IntroCard("🔤", "Opciones por pregunta", "Cada pregunta muestra $maxOptions opciones. Solo una es correcta.", optionLetters))
    cards.add(IntroCard("📊", "Dificultad de las preguntas", "Las preguntas más fáciles aparecen primero. Cada 5 aciertos, la dificultad sube (máx: $maxDiff).", "1 → $maxDiff"))
    cards.add(IntroCard(powerUpIcons, "Power-ups", powerUpText, if (powerUps.isEmpty()) "" else "×${powerUps.size}"))
    cards.add(IntroCard("🔥", "Combos y racha", "Acierta seguido para subir el combo. Cada 5 aciertos seguidos recuperas una vida o ganas tiempo.", "×5"))

    val footer = when (rankIndex) {
        0 -> "¡Sin presión! Tienes 5 corazones y solo 2 opciones. Aprende las preguntas básicas."
        1 -> "¡Más opciones! Ahora 3 respuestas y power-ups básicos para ayudarte."
        2 -> "¡Modo completo! 4 opciones, 3 corazones y todos los power-ups. ¡Demuestra lo que sabes!"
        else -> "Modo Supervivencia completo. ¡Buena suerte!"
    }

    val accentColor = when (rankIndex) { 0 -> Success; 1 -> Warning; else -> Danger }

    return IntroContent(title, subtitle, accentColor, cards, footer)
}

private fun buildTimetrialIntro(rankIndex: Int): IntroContent {
    val powerUpIcons = "🛡️ ✨ ✂️ 💡"

    val cards = listOf(
        IntroCard("⏱️", "Tiempo limitado", "Tienes 180 segundos. El reloj no se detiene.", "180s"),
        IntroCard("✅", "Acierto", "Cada acierto suma 15 segundos extra.", "+15s"),
        IntroCard("❌", "Fallo", "Cada fallo resta 10 segundos. ¡Cuidado!", "-10s"),
        IntroCard("🔤", "Opciones", "4 opciones por pregunta. Elige rápido.", "A B C D"),
        IntroCard(powerUpIcons, "Power-ups disponibles", "Todos los power-ups están disponibles.", "×4"),
        IntroCard("🔥", "Racha", "Cada 5 aciertos seguidos ganas 20 segundos extra.", "+20s")
    )

    return IntroContent(
        title = "Contrarreloj",
        subtitle = "Responde rápido y con precisión",
        accentColor = Primary,
        cards = cards,
        footer = "El tiempo es tu enemigo. Prioriza la velocidad sin perder precisión."
    )
}

private fun buildQuickIntro(@Suppress("UNUSED_PARAMETER") rankIndex: Int): IntroContent {
    val cards = listOf(
        IntroCard("⚡", "5 preguntas", "Repaso Express te muestra 5 preguntas de tus fallos previos.", "5️⃣"),
        IntroCard("🔄", "De tus errores", "Las preguntas se eligen de las que has fallado antes o no has contestado.", "📝"),
        IntroCard("❤️", "Corazones", "Tienes 3 corazones. Si fallas 3 veces, se acaba el repaso.", "❤️❤️❤️"),
        IntroCard("🎁", "Recompensa 5/5", "Si aciertas las 5 preguntas, recibes un power-up gratis.", "🏆"),
        IntroCard("🛡️✨✂️💡", "Power-ups", "Todos los power-ups disponibles.", "×4")
    )

    return IntroContent(
        title = "Repaso Express",
        subtitle = "Repasa tus errores rápidamente",
        accentColor = Warning,
        cards = cards,
        footer = "Ideal para repasar lo que aún no dominas. ¡5/5 = power-up gratis!"
    )
}

private fun buildExamIntro(@Suppress("UNUSED_PARAMETER") rankIndex: Int): IntroContent {
    val cards = listOf(
        IntroCard("📝", "Modo Examen", "Simula un examen real. Sin vidas, sin power-ups, sin pausa.", "📝"),
        IntroCard("📊", "Corrección al final", "No sabes si aciertas o fallas hasta terminar todas las preguntas.", "🔍"),
        IntroCard("🔢", "Número de preguntas", "Configurable: 10, 20, 30, 40 o 50 preguntas.", "10-50"),
        IntroCard("🎯", "Puntuación", "Cada acierto vale 10 XP. Aprueba con 60%+ para desbloquear más.", "60%"),
        IntroCard("📚", "Por ley", "Elige una ley específica o todas a la vez.", "📖")
    )

    return IntroContent(
        title = "Mini Examen",
        subtitle = "Pon a prueba tu conocimiento",
        accentColor = Success,
        cards = cards,
        footer = "Sin pistas, sin ayudas. Solo tú y las preguntas. ¡Como en el examen real!"
    )
}

private fun buildSimulacroIntro(@Suppress("UNUSED_PARAMETER") rankIndex: Int): IntroContent {
    // Simulacro has its own dedicated screen, but we provide content just in case.
    val cards = listOf(
        IntroCard("🎯", "Examen Oficial Simulado", "Réplica exacta del examen de Auxilio Judicial.", "100"),
        IntroCard("⏱️", "100 minutos", "Tiempo limitado como en el examen real.", "100min"),
        IntroCard("⚖️", "Preguntas por ley", "Distribuidas según los pesos oficiales.", "📐"),
        IntroCard("🏆", "Puntuación oficial", "Cada acierto +0.60, cada fallo -0.15. Aprobado: 30 puntos.", "60pts")
    )

    return IntroContent(
        title = "Simulacro",
        subtitle = "Examen Oficial Simulado",
        accentColor = Accent,
        cards = cards,
        footer = "La experiencia más cercana al examen real. ¡Mucha suerte!"
    )
}
