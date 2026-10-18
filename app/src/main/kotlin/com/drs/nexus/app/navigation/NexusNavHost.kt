package com.drs.nexus.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.drs.nexus.app.data.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.drs.nexus.app.ui.screens.AboutScreen
import com.drs.nexus.app.ui.screens.FavoritesScreen
import com.drs.nexus.app.ui.screens.HomeScreen
import com.drs.nexus.app.ui.screens.HistoryScreen
import com.drs.nexus.app.ui.screens.OnboardingScreen
import com.drs.nexus.app.ui.screens.SettingsScreen
import com.drs.nexus.app.ui.screens.SplashScreen
import com.drs.nexus.app.ui.screens.ToolsScreen
import com.drs.nexus.app.ui.screens.tools.ToolDetailScreen

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val TOOLS = "tools"
    const val HISTORY = "history"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val TOOL = "tool/{toolId}"
    fun tool(toolId: String) = "tool/$toolId"
}

@Composable
fun NexusNavHost(
    navController: NavHostController,
    container: AppContainer,
    startDestination: String,
    contentPadding: PaddingValues = PaddingValues()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(top = contentPadding.calculateTopPadding())
    ) {
        composable(Routes.SPLASH) {
            val onboarded = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                onboarded.value = container.settingsRepository.settings.first().onboardingDone
            }
            SplashScreen(
                onFinished = {
                    navController.navigate(if (onboarded.value) Routes.HOME else Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    container.settingsRepository.setOnboardingDone(true)
                }
                navController.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(container = container, navController = navController)
        }
        composable(Routes.TOOLS) {
            ToolsScreen(container = container, navController = navController)
        }
        composable(Routes.HISTORY) {
            HistoryScreen(container = container, navController = navController)
        }
        composable(Routes.FAVORITES) {
            FavoritesScreen(container = container, navController = navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(container = container, navController = navController)
        }
        composable(Routes.ABOUT) {
            AboutScreen(navController = navController)
        }
        composable(
            route = Routes.TOOL,
            arguments = listOf(navArgument("toolId") { type = NavType.StringType })
        ) { entry ->
            val toolId = entry.arguments?.getString("toolId") ?: "json_formatter"
            ToolDetailScreen(toolId = toolId, container = container, navController = navController)
        }
    }
}
