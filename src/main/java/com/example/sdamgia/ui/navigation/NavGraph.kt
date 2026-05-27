package com.example.sdamgia.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.sdamgia.ui.screens.MainGameScreen
import com.example.sdamgia.ui.screens.ProblemScreen
import com.example.sdamgia.ui.screens.SettingsScreen
import com.example.sdamgia.ui.screens.ShopScreen
import com.example.sdamgia.viewmodel.GameViewModel

object Routes {
    const val MAIN = "main"
    const val SHOP = "shop"
    const val PROBLEM = "problem"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
        modifier = modifier,
        enterTransition = { slideInHorizontally { it } + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally { it } + fadeOut(tween(300)) },
        popEnterTransition = { slideInHorizontally { -it } + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally { -it } + fadeOut(tween(300)) }
    ) {
        composable(Routes.MAIN) {
            MainGameScreen(
                viewModel = viewModel,
                onNavigateToShop = { navController.navigate(Routes.SHOP) },
                onNavigateToProblem = { navController.navigate(Routes.PROBLEM) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SHOP) {
            ShopScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.PROBLEM) {
            ProblemScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
        }
    }
}
