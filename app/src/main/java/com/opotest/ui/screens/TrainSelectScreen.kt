package com.opotest.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import com.opotest.data.local.DataProvider
import com.opotest.data.repository.ProgressRepository
import com.opotest.ui.components.GameButton
import com.opotest.ui.navigation.TrainingViewModel
import com.opotest.ui.navigation.Routes
import com.opotest.ui.theme.*

@Composable
fun TrainSelectScreen(navController: NavController, trainingViewModel: TrainingViewModel) {
    val context = navController.context
    val progressRepo = ProgressRepository(context)
    val unlocks = remember { progressRepo.getUnlocks() }
    val tests = remember { DataProvider.getTemaTests(context) }
    var selectedTab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var selectedCount by remember { mutableStateOf(20) }

    val filteredTests = if (query.isBlank()) tests else tests.filter {
        (it.title.ifEmpty { it.name }).contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(selectedTabIndex = selectedTab, containerColor = BgCard, contentColor = Primary) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Por tema", color = TextLight, modifier = Modifier.padding(8.dp)) }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Personalizado", color = TextLight, modifier = Modifier.padding(8.dp)) }
        }
        Spacer(Modifier.height(16.dp))

        if (selectedTab == 0) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar tema...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight, unfocusedTextColor = TextLight,
                    focusedBorderColor = Primary, unfocusedBorderColor = SurfaceVariant, cursorColor = Primary
                )
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredTests) { test ->
                    val title = if (test.tema != null) "Tema ${test.tema}: ${test.title.ifEmpty { test.name }}" else test.title.ifEmpty { test.name }
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
                        Text("📚", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(title, color = TextLight, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        } else {
            Text("Cantidad de preguntas:", color = TextLight, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 20, 30, 50).forEach { count ->
                    GameButton(
                        text = count.toString(),
                        modifier = Modifier.weight(1f).height(48.dp),
                        color1 = if (selectedCount == count) Primary else SurfaceVariant,
                        color2 = if (selectedCount == count) PurpleDark else BgCard
                    ) { selectedCount = count }
                }
            }
            Spacer(Modifier.height(24.dp))
            GameButton(
                text = "Empezar entrenamiento",
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
