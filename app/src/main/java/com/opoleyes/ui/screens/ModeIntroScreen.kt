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

private data class IntroItem(
    val icon: String,
    val text: String
)

private data class IntroContent(
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val items: List<IntroItem>,
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            content.items.forEach { item ->
                IntroItemRow(item)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(12.dp))

            Text(
                content.footer,
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

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
private fun IntroItemRow(item: IntroItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(BgCard, BgDark)))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.icon, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            item.text,
            color = TextLight,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
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
    val maxLives = Constants.MAX_LIVES_BY_RANK[rankIndex] ?: 3
    val maxDiff = Constants.MAX_DIFFICULTY_BY_RANK[rankIndex] ?: 5
    val powerUps = Constants.AVAILABLE_POWERUPS_BY_RANK[rankIndex]
        ?: listOf("shield", "doubleScore", "fiftyFifty", "hint")

    val rankName = Constants.getRankByIndex(rankIndex).name
    val title = when (rankIndex) {
        0 -> "¡Bienvenido a Supervivencia!"
        1 -> "Supervivencia: Principiante"
        2 -> "Supervivencia: Modo Completo"
        else -> "Supervivencia"
    }
    val subtitle = "Rango: $rankName"

    val items = mutableListOf<IntroItem>()
    if (rankIndex == 0) {
        items.add(IntroItem("❤️", "$maxLives corazones · Primer fallo NO cuenta"))
        items.add(IntroItem("", "Dificultad progresa de 1 a $maxDiff"))
        items.add(IntroItem("🎯", "50/50 disponible desde el inicio"))
        items.add(IntroItem("�", "Cada 5 aciertos seguidos: vida extra"))
    } else if (rankIndex == 1) {
        items.add(IntroItem("❤️", "$maxLives corazones · Cada fallo resta 1 · Combos recuperan vida"))
        items.add(IntroItem("", "Dificultad progresa de 1 a $maxDiff"))
        items.add(IntroItem("🛡️", "Pista y 50/50 disponibles"))
        items.add(IntroItem("🔥", "Cada 5 aciertos seguidos: vida extra"))
    } else {
        items.add(IntroItem("❤️", "$maxLives corazones · Cada fallo resta 1 · Combos recuperan vida"))
        items.add(IntroItem("", "Dificultad progresa de 1 a $maxDiff"))
        if (powerUps.isEmpty())
            items.add(IntroItem("🚫", "Sin power-ups"))
        else
            items.add(IntroItem("🛡️", "Todos los power-ups disponibles"))
        items.add(IntroItem("🔥", "Cada 5 aciertos seguidos: vida extra"))
    }

    val footer = when (rankIndex) {
        0 -> "¡Sin presión! $maxLives corazones y tu primer fallo no cuenta. Tienes 50/50 gratis para aprender."
        1 -> "¡Nueva ayuda! Pista elimina una respuesta incorrecta. Sigue practicando."
        2 -> "3 corazones y todos los power-ups. ¡A por todas!"
        else -> "Modo Supervivencia completo. ¡Buena suerte!"
    }

    val accentColor = when (rankIndex) { 0 -> Success; 1 -> Warning; else -> Danger }

    return IntroContent(title, subtitle, accentColor, items, footer)
}

private fun buildTimetrialIntro(rankIndex: Int): IntroContent {
    val items = listOf(
        IntroItem("⏱️", "180 segundos. El reloj no se detiene."),
        IntroItem("✅", "Acierto: +15s · Fallo: -10s"),
        IntroItem("🛡️", "Todos los power-ups disponibles"),
        IntroItem("🔥", "Cada 5 aciertos seguidos: +20s extra")
    )

    return IntroContent(
        title = "Contrarreloj",
        subtitle = "Responde rápido y con precisión",
        accentColor = Primary,
        items = items,
        footer = "El tiempo es tu enemigo. Velocidad sin perder precisión."
    )
}

private fun buildQuickIntro(@Suppress("UNUSED_PARAMETER") rankIndex: Int): IntroContent {
    val items = listOf(
        IntroItem("⚡", "5 preguntas de tus fallos previos"),
        IntroItem("❤️", "3 corazones. 3 fallos = se acaba."),
        IntroItem("🎁", "5/5 aciertos = power-up gratis"),
        IntroItem("🛡️", "Todos los power-ups disponibles")
    )

    return IntroContent(
        title = "Repaso Express",
        subtitle = "Repasa tus errores rápidamente",
        accentColor = Warning,
        items = items,
        footer = "Ideal para repasar lo que aún no dominas."
    )
}

private fun buildExamIntro(@Suppress("UNUSED_PARAMETER") rankIndex: Int): IntroContent {
    val items = listOf(
        IntroItem("📝", "Examen real: sin vidas, sin power-ups, sin pausa"),
        IntroItem("📊", "Corrección al final del todo"),
        IntroItem("🔢", "10 a 50 preguntas, configurable"),
        IntroItem("🎯", "60%+ para aprobar y desbloquear más")
    )

    return IntroContent(
        title = "Mini Examen",
        subtitle = "Pon a prueba tu conocimiento",
        accentColor = Success,
        items = items,
        footer = "Sin pistas, sin ayudas. Como en el examen real."
    )
}

private fun buildSimulacroIntro(@Suppress("UNUSED_PARAMETER") rankIndex: Int): IntroContent {
    val items = listOf(
        IntroItem("🎯", "Réplica del examen de Auxilio Judicial"),
        IntroItem("⏱️", "100 minutos de tiempo límite"),
        IntroItem("⚖️", "Preguntas distribuidas por pesos oficiales"),
        IntroItem("🏆", "Acierto +0.60 · Fallo -0.15 · Aprobado: 30 pts")
    )

    return IntroContent(
        title = "Simulacro",
        subtitle = "Examen Oficial Simulado",
        accentColor = Accent,
        items = items,
        footer = "La experiencia más cercana al examen real."
    )
}
