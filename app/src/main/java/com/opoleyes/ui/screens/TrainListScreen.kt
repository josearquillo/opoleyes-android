package com.opoleyes.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.opoleyes.data.local.DataProvider
import com.opoleyes.ui.navigation.TrainingViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*

@Composable
fun TrainListScreen(navController: NavController, trainingViewModel: TrainingViewModel) {
    val context = navController.context
    val tests = remember { DataProvider.getTemaTests(context) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Selecciona tema", color = TextLight, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            items(tests) { test ->
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
    }
}
