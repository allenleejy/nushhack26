package com.nushhack26.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.debug.DebugScreen
import com.nushhack26.app.ui.home.HomeScreen
import com.nushhack26.app.ui.me.MeScreen
import com.nushhack26.app.ui.tasks.TasksScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> { HomeScreen() }
        composable<TasksRoute> { TasksScreen() }
        composable<MeRoute> { MeScreen(container, onOpenDebug = { navController.navigate(DebugRoute) }) }
        composable<DebugRoute> { DebugScreen(container) }
    }
}
