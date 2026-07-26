package com.opotest.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.opotest.data.Constants
import com.opotest.data.local.DataProvider
import com.opotest.ui.components.GameButton
import com.opotest.ui.navigation.TrainingViewModel
import com.opotest.ui.navigation.Routes
import com.opotest.ui.theme.*

@Composable
fun TrainSelectScreen(navController: NavController, trainingViewModel: TrainingViewModel) {
    val context = navController.context
    val tests = remember { DataProvider.getTemaTests(context) }
    var selectedTab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var selectedCount by remember { mutableStateOf(20) }
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    val filteredTests = if (query.isBlank()) tests else tests.filter {
        (it.title.ifEmpty { it.name }).contains(query, ignoreCase = true)
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
        TabRow(selectedTabIndex = selectedTab, containerColor = BgCard, contentColor = Primary) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Por ley", color = TextLight, modifier = Modifier.padding(8.dp)) }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Personalizado", color = TextLight, modifier = Modifier.padding(8.dp)) }
        }
        Spacer(Modifier.height(16.dp))

        if (selectedTab == 0) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar ley...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight, unfocusedTextColor = TextLight,
                    focusedBorderColor = Primary, unfocusedBorderColor = SurfaceVariant, cursorColor = Primary
                )
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                grouped.forEach { (leyName, groupTests) ->
                    item(key = "header_$leyName") {
                        val isExpanded = expandedGroups[leyName] ?: false
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
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    if (expandedGroups[leyName] == true) {
                        item(key = "content_$leyName") {
                            Column {
                                groupTests.forEach { test ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Brush.verticalGradient(listOf(BgCard, BgCardDark)))
                                            .clickable {
                                                if (trainingViewModel.startTraining(test.id))
                                                    navController.navigate(Routes.TEST_BROWSER)
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("📖", fontSize = 20.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Text(test.title.ifEmpty { test.name }, color = TextLight, fontSize = 14.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Text("Cantidad de preguntas:", color = TextLight, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10, 20, 30, 50, 100).forEach { count ->
                    GameButton(
                        text = count.toString(),
                        modifier = Modifier.weight(1f).height(44.dp),
                        color1 = if (selectedCount == count) Primary else SurfaceVariant,
                        color2 = if (selectedCount == count) PurpleDark else BgCard
                    ) { selectedCount = count }
                }
            }
            Spacer(Modifier.height(24.dp))
            GameButton(
                text = "Empezar",
                icon = "🎯",
                modifier = Modifier.fillMaxWidth().height(56.dp),
                color1 = Primary,
                color2 = PurpleDark
            ) {
                if (trainingViewModel.startTrainingCustom("", selectedCount))
                    navController.navigate(Routes.TEST_BROWSER)
            }
        }
    }
}
