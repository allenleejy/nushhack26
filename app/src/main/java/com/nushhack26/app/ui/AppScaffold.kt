package com.nushhack26.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.components.SyncStatusBar
import com.nushhack26.app.ui.navigation.AppNavHost
import com.nushhack26.app.ui.navigation.DebugRoute
import com.nushhack26.app.ui.navigation.HomeRoute
import com.nushhack26.app.ui.navigation.TopLevelTab

/** The app frame: top bar, sync status, the current screen and the bottom tabs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val syncStatus by container.syncStatus.collectAsStateWithLifecycle()
    val currentTab = TopLevelTab.entries.firstOrNull { destination?.hasRoute(it.routeClass) == true }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(titleFor(destination, currentTab)) },
                    navigationIcon = {
                        if (currentTab == null && destination != null) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (destination?.hasRoute(HomeRoute::class) == true) {
                            IconButton(onClick = { container.syncScheduler.syncNow() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Sync now")
                            }
                        }
                    },
                )
                SyncStatusBar(syncStatus)
            }
        },
        bottomBar = {
            if (currentTab != null) {
                NavigationBar {
                    TopLevelTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        AppNavHost(navController, container, Modifier.padding(innerPadding))
    }
}

private fun titleFor(destination: NavDestination?, tab: TopLevelTab?): String = when {
    tab != null -> tab.label
    destination?.hasRoute(DebugRoute::class) == true -> "Debug"
    else -> ""
}
