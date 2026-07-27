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
import com.opoleyes.data.Constants
import com.opoleyes.data.local.DataProvider
import com.opoleyes.data.model.Test
import com.opoleyes.data.repository.StatsRepository
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*

@Composable
fun TemaSelectScreen(navController: NavController, gameViewModel: GameViewModel) {
    val context = navController.context
    val statsRepo = StatsRepository(context)
    val tests = remember { DataProvider.getTemaTests(context) }
    var query by remember { mutableStateOf("") }
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    val filteredTests = if (query.isBlank()) tests else tests.filter {
        (it.title.ifEmpty { it.name }).contains(query, ignoreCase = true) ||
        (it.tema?.toString() ?: "").contains(query)
    }

    val grouped = remember(filteredTests) {
        Constants.LEY_GROUPS.mapNotNull { (leyName, range) ->
            val groupTests = filteredTests.filter { test ->
                val num = test.name.removePrefix("Tema N").toIntOrNull() ?: return@filter false
                num in range
            }
            if (groupTests.isNotEmpty()) leyName to groupTests else null
        }
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
            if (gameViewModel.startAllLawsGame()) navController.navigate(Routes.GAME)
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn {
            grouped.forEach { (leyName, groupTests) ->
                item(key = "header_$leyName") {
                    val isExpanded = expandedGroups[leyName] ?: false
                    val avgProgress = if (query.isBlank()) {
                        groupTests.map { statsRepo.getLeyProgress(it.id) }.average().toInt()
                    } else 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(Primary, PurpleDark)))
                            .clickable { expandedGroups[leyName] = !isExpanded }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isExpanded) "▼" else "▶",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            leyName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${groupTests.size}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        if (avgProgress > 0) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "$avgProgress%",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                if (expandedGroups[leyName] == true) {
                    item(key = "content_$leyName") {
                        Column {
                            groupTests.forEach { test ->
                                val progress = statsRepo.getLeyProgress(test.id)
                                TemaCard("📖", test.title.ifEmpty { test.name }, progress) {
                                    if (gameViewModel.startTemaGame(test.id)) navController.navigate(Routes.GAME)
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
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
            Text(title, color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
