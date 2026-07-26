package com.opotest.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.opotest.ui.screens.*

object Routes {
    const val LOADING = "loading"
    const val ERROR = "error"
    const val HOME = "home"
    const val MODE_SELECT = "modeselect"
    const val TEMA_SELECT = "temaselect"
    const val GAME = "game"
    const val GAME_OVER = "gameover"
    const val PROFILE = "profile"
    const val HELP = "help"
    const val TRAIN_SELECT = "trainselect"
    const val TRAIN_LIST = "trainlist"
    const val TEST_BROWSER = "testbrowser"
    const val FLAG_REVIEW = "flagreview"
    const val WRONG_REVIEW = "wrongreview"
    const val RESULTS = "results"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val gameViewModel: GameViewModel = viewModel()
    val trainingViewModel: TrainingViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.LOADING,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),
        enterTransition = { slideInHorizontally(animationSpec = tween(300), initialOffsetX = { it / 4 }) + fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(300), initialOffsetX = { -it / 4 }) + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { it / 4 }) + fadeOut(tween(200)) },
    ) {
        composable(Routes.LOADING) { LoadingScreen(navController) }
        composable(Routes.ERROR) { ErrorScreen(navController) }
        composable(Routes.HOME) { HomeScreen(navController, gameViewModel) }
        composable(Routes.MODE_SELECT) { ModeSelectScreen(navController, gameViewModel) }
        composable(Routes.TEMA_SELECT) { TemaSelectScreen(navController, gameViewModel) }
        composable(Routes.GAME) { GameScreen(navController, gameViewModel) }
        composable(Routes.GAME_OVER) { GameOverScreen(navController, gameViewModel) }
        composable(Routes.PROFILE) { ProfileScreen(navController) }
        composable(Routes.HELP) { HelpScreen(navController) }
        composable(Routes.TRAIN_SELECT) { TrainSelectScreen(navController, trainingViewModel) }
        composable(Routes.TRAIN_LIST) { TrainListScreen(navController, trainingViewModel) }
        composable(Routes.TEST_BROWSER) { TestBrowserScreen(navController, trainingViewModel) }
        composable(Routes.FLAG_REVIEW) { FlagReviewScreen(navController, trainingViewModel) }
        composable(Routes.WRONG_REVIEW) { WrongReviewScreen(navController, trainingViewModel) }
        composable(Routes.RESULTS) { ResultsScreen(navController, trainingViewModel) }
    }
}
