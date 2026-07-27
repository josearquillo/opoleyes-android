package com.opoleyes.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.model.Test
import com.opoleyes.data.repository.StatsRepository
import com.opoleyes.ui.components.LoadingOverlay
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*

@Composable
fun TemaSelectScreen(navController: NavController, gameViewModel: GameViewModel) {
    val context = navController.context
    val statsRepo = StatsRepository(context)
    val tests = remember { DataProvider.getTemaTests(context) }
    var query by remember { mutableStateOf("") }
    val isLoading by gameViewModel.isLoading.collectAsState()

    val filteredTests = if (query.isBlank()) tests else tests.filter {
        (it.title.ifEmpty { it.name }).contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar ley...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, contentDescription = null) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                focusedBorderColor = Primary,
                unfocusedBorderColor = SurfaceVariant,
                cursorColor = Primary
            )
        )
        Spacer(Modifier.height(12.dp))

        TemaCard("📚", "Todas las leyes", 0) {
            gameViewModel.startAllLawsGameAsync { ok -> if (ok) navController.navigate(Routes.GAME) }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn {
            items(filteredTests, key = { it.id }) { test ->
                val progress = statsRepo.getLeyProgress(test.id)
                TemaCard("📖", test.title.ifEmpty { test.name }, progress) {
                    gameViewModel.startTemaGameAsync(test.id) { ok -> if (ok) navController.navigate(Routes.GAME) }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    if (isLoading) {
        LoadingOverlay()
    }
}

@Composable
private fun TemaCard(icon: String, title: String, progress: Int, onClick: () -> Unit) {
    val heatColor = when {
        progress >= 80 -> Success
        progress >= 50 -> Warning
        progress >= 25 -> Orange
        progress > 0 -> Primary
        else -> SurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(BgCard, BgCardDark)))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (progress > 0) {
                Text("$progress% dominado", color = heatColor, fontSize = 11.sp)
            }
        }
        if (progress > 0) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(heatColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("$progress%", color = heatColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
