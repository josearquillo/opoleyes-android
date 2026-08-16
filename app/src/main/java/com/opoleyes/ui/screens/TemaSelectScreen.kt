package com.opoleyes.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.R
import com.opoleyes.data.model.Test
import com.opoleyes.ui.components.LoadingOverlay
import com.opoleyes.ui.navigation.GameViewModel
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemaSelectScreen(navController: NavController, gameViewModel: GameViewModel) {
    val tests = remember { gameViewModel.getTemaTests() }
    var query by remember { mutableStateOf("") }
    val isLoading by gameViewModel.isLoading.collectAsState()

    val filteredTests = if (query.isBlank()) tests else tests.filter {
        (it.title.ifEmpty { it.name }).contains(query, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.select_law), color = TextLight, fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_law)) },
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

            LazyColumn {
                item {
                    AllLawsCard {
                        gameViewModel.startAllLawsGameAsync { ok ->
                            if (ok) {
                                if (gameViewModel.shouldShowModeIntro(gameViewModel.engine.mode)) {
                                    navController.navigate(Routes.MODE_INTRO)
                                } else {
                                    navController.navigate(Routes.GAME)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = SurfaceVariant.copy(alpha = 0.4f), thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))
                }
                items(filteredTests, key = { it.id }) { test ->
                    val progress = remember(test.id) { gameViewModel.getLeyProgress(test.id) }
                    TemaCard(Icons.Default.Book, test.title.ifEmpty { test.name }, progress) {
                        gameViewModel.startTemaGameAsync(test.id) { ok ->
                            if (ok) {
                                if (gameViewModel.shouldShowModeIntro(gameViewModel.engine.mode)) {
                                    navController.navigate(Routes.MODE_INTRO)
                                } else {
                                    navController.navigate(Routes.GAME)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (filteredTests.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.no_results),
                                color = TextMuted,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (isLoading) {
        LoadingOverlay()
    }
}

@Composable
private fun AllLawsCard(onClick: () -> Unit) {
    val turquoise = Color(0xFF14b8a6)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(BgCard, turquoise.copy(alpha = 0.08f))))
            .border(1.dp, turquoise.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.LibraryBooks,
            contentDescription = null,
            tint = turquoise,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.all_laws),
            color = TextLight,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TemaCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, progress: Int, onClick: () -> Unit) {
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
            .background(Brush.verticalGradient(listOf(BgCard, BgDark)))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextLight, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (progress > 0) {
                Text(stringResource(R.string.dominated_suffix, progress), color = heatColor, fontSize = 11.sp)
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
