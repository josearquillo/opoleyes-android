package com.opoleyes.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.opoleyes.ui.screens.*

object Routes {
    const val LOADING = "loading"
    const val ERROR = "error"
    const val HOME = "home"
    const val MODE_SELECT = "modeselect"
    const val TEMA_SELECT = "temaselect"
    const val GAME = "game"
    const val GAME_OVER = "gameover"
    const val EXAM = "exam"
    const val EXAM_RESULT = "examresult"
    const val PROFILE = "profile"
    const val HELP = "help"
    const val SIMULACRO_INTRO = "simulacrointro"
    const val MODE_INTRO = "modeintro"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val gameViewModel: GameViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GameViewModel(context.applicationContext as Application) as T
        }
    )

    NavHost(
        navController = navController,
        startDestination = Routes.LOADING,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),
        enterTransition = {
            slideInHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing), initialOffsetX = { it / 3 }) +
            fadeIn(tween(350)) + scaleIn(initialScale = 0.92f, animationSpec = tween(350))
        },
        exitTransition = { fadeOut(tween(200)) + scaleOut(targetScale = 0.95f, animationSpec = tween(200)) },
        popEnterTransition = {
            slideInHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing), initialOffsetX = { -it / 3 }) +
            fadeIn(tween(350)) + scaleIn(initialScale = 0.92f, animationSpec = tween(350))
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing), targetOffsetX = { it / 3 }) +
            fadeOut(tween(200)) + scaleOut(targetScale = 0.95f, animationSpec = tween(200))
        },
    ) {
        composable(Routes.LOADING) { LoadingScreen(navController, gameViewModel) }
        composable(Routes.ERROR) { ErrorScreen(navController) }
        composable(Routes.HOME) { HomeScreen(navController, gameViewModel) }
        composable(Routes.MODE_SELECT) { ModeSelectScreen(navController, gameViewModel) }
        composable(Routes.TEMA_SELECT) { TemaSelectScreen(navController, gameViewModel) }
        composable(Routes.GAME) { GameScreen(navController, gameViewModel) }
        composable(Routes.GAME_OVER) { GameOverScreen(navController, gameViewModel) }
        composable(Routes.EXAM) { ExamScreen(navController, gameViewModel) }
        composable(Routes.EXAM_RESULT) { ExamResultScreen(navController, gameViewModel) }
        composable(Routes.PROFILE) { ProfileScreen(navController, gameViewModel) }
        composable(Routes.HELP) { HelpScreen(navController, gameViewModel) }
        composable(Routes.SIMULACRO_INTRO) { SimulacroIntroScreen(navController, gameViewModel) }
        composable(Routes.MODE_INTRO) { ModeIntroScreen(navController, gameViewModel) }
    }
}
