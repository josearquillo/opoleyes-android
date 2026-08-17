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
fun NavGraph(startDestination: String = Routes.LOADING, gameViewModel: GameViewModel? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val vm: GameViewModel = gameViewModel ?: viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GameViewModel(context.applicationContext as Application) as T
        }
    )

    // Push: new screen enters from right (1/3 width), old screen exits left (1/6 width = parallax depth)
    val slideEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing), initialOffsetX = { it / 3 }) +
        fadeIn(tween(300))
    }
    val slideExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing), targetOffsetX = { -it / 6 }) +
        fadeOut(tween(300))
    }
    // Pop: old screen returns from left (1/6), current screen exits right (1/3)
    val slidePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing), initialOffsetX = { -it / 6 }) +
        fadeIn(tween(300))
    }
    val slidePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing), targetOffsetX = { it / 3 }) +
        fadeOut(tween(300))
    }
    // Overlay screens (gameover, examresult, modeintro): subtle fade + scale
    val fadeEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(tween(300)) + scaleIn(initialScale = 0.96f, animationSpec = tween(300))
    }
    val fadeExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(tween(250)) + scaleOut(targetScale = 0.96f, animationSpec = tween(250))
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),
        enterTransition = { slideEnter() },
        exitTransition = { slideExit() },
        popEnterTransition = { slidePopEnter() },
        popExitTransition = { slidePopExit() },
    ) {
        composable(Routes.LOADING) { LoadingScreen(navController, vm) }
        composable(Routes.ERROR) { ErrorScreen(navController) }
        composable(Routes.HOME) { HomeScreen(navController, vm) }
        composable(Routes.MODE_SELECT) { ModeSelectScreen(navController, vm) }
        composable(Routes.TEMA_SELECT) { TemaSelectScreen(navController, vm) }
        composable(Routes.GAME) { GameScreen(navController, vm) }
        composable(
            Routes.GAME_OVER,
            enterTransition = { fadeEnter() },
            exitTransition = { fadeExit() },
            popEnterTransition = { fadeEnter() },
            popExitTransition = { fadeExit() }
        ) { GameOverScreen(navController, vm) }
        composable(Routes.EXAM) { ExamScreen(navController, vm) }
        composable(
            Routes.EXAM_RESULT,
            enterTransition = { fadeEnter() },
            exitTransition = { fadeExit() },
            popEnterTransition = { fadeEnter() },
            popExitTransition = { fadeExit() }
        ) { ExamResultScreen(navController, vm) }
        composable(Routes.PROFILE) { ProfileScreen(navController, vm) }
        composable(Routes.HELP) { HelpScreen(navController, vm) }
        composable(Routes.SIMULACRO_INTRO) { SimulacroIntroScreen(navController, vm) }
        composable(
            Routes.MODE_INTRO,
            enterTransition = { fadeEnter() },
            exitTransition = { fadeExit() },
            popEnterTransition = { fadeEnter() },
            popExitTransition = { fadeExit() }
        ) { ModeIntroScreen(navController, vm) }
    }
}
