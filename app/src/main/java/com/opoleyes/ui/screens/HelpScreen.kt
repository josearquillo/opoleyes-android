package com.opoleyes.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.BuildConfig
import com.opoleyes.R
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HelpScreen(navController: NavController, gameViewModel: GameViewModel) {
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
            "📝 Modo Examen: Simula el examen oficial. Sin vidas, sin power-ups, corrección al final.",
            "🏆 Modo Reto: 2 minutos, máxima dificultad, preguntas de todas las leyes a la vez."
        )),
        HelpSectionData("✨", "Ayudas (Power-ups)", listOf(
            "💡 Pista: Quita una respuesta incorrecta de la pantalla.",
            "🛡️ Escudo: Actívalo manualmente pulsando su botón. Si fallas la siguiente respuesta, se consume y no te penaliza. Es mutuamente excluyente con otras ayudas.",
            "🎯 50/50: Elimina dos respuestas incorrectas de golpe.",
            "✨ x2 pts: La próxima respuesta que aciertes vale el doble de puntos.",
            "❤️ Recuperación: Cada 5 aciertos seguidos recuperas 1 vida automáticamente (Solo Supervivencia). Si ya tienes 3 vidas, recibes una carga de 50/50 en su lugar.",
            "⏱️ En Contrarreloj y Reto, cada 5 aciertos seguidos te da +20 segundos extra.",
            "📦 Las ayudas se consiguen al subir de rango y al abrir cofres. Se gastan al usarlas. Solo puedes usar una ayuda por pregunta."
        )),
        HelpSectionData("🔥", "Combo", listOf(
            "Cada vez que aciertes seguidas, tu combo sube.",
            "Cuanto más alto sea el combo, más puntos ganas por respuesta (10 × combo).",
            "La barra de combo inferior se llena cada 10 aciertos consecutivos.",
            "Al llenarla, obtienes 3 cargas gratis que puedes canjear por vida extra o tiempo adicional."
        )),
        HelpSectionData("🌱", "Rangos", listOf(
            "Hay 12 rangos, desde Novato (0 XP) hasta Leyenda (100.000 XP).",
            "Cada rango desbloquea nuevos modos de juego y ayudas.",
            "Gana XP acertando preguntas y completando misiones diarias."
        )),
        HelpSectionData("🎁", "Cofres", listOf(
            "📦 Cofre de madera: Entre 50 y 150 XP. Lo recibes al terminar una partida normal.",
            "�️ Cofre de plata: Entre 150 y 350 XP y una ayuda extra. Lo recibes si bates tu récord de puntuación.",
            "🎁 Cofre de oro: Entre 300 y 600 XP, dos ayudas extra y doble XP. Lo recibes si bates tu récord con un 90% de aciertos respondiendo al menos 5 preguntas."
        )),
        HelpSectionData("📋", "Misiones diarias", listOf(
            "Cada día recibes misiones nuevas y diferentes.",
            "Puedes tener entre 1 y 3 misiones según tu rango.",
            "Cada misión completada te da 50 XP. Si completas todas, recibes 200 XP extra.",
            "Las misiones de Supervivencia requieren acertar al menos 5 preguntas, no basta con solo jugar.",
            "¡Intenta completarlas todos los días seguidos para mantener tu racha!"
        )),
        HelpSectionData("ℹ️", "Información", listOf(
            "Las preguntas están actualizadas a la legislación vigente de 2025.",
            "La legislación puede cambiar con el tiempo. Consulta siempre las leyes vigentes antes del examen.",
            "Esta aplicación no sustituye el estudio oficial ni garantiza la aprobación del examen."
        ))
    )

    var debugMode by remember { mutableStateOf(gameViewModel.isDebugMode()) }
    var debugToast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(debugToast) {
        if (debugToast != null) {
            kotlinx.coroutines.delay(2000)
            debugToast = null
        }
    }

    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help), color = TextLight, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            val debugOnText = stringResource(R.string.debug_on)
            val debugOffText = stringResource(R.string.debug_off)
            Text(
                stringResource(R.string.help),
                color = TextLight,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = {
                        if (BuildConfig.DEBUG) {
                            debugMode = !debugMode
                            gameViewModel.setDebugMode(debugMode)
                            debugToast = if (debugMode) debugOnText else debugOffText
                        }
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
        Text("${data.icon} ${data.title}", color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        data.lines.forEach { line ->
            Text(line, color = TextMuted, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
        }
    }
}

private data class HelpSectionData(val icon: String, val title: String, val lines: List<String>)
