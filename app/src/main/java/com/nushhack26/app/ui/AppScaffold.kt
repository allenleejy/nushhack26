package com.nushhack26.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nushhack26.app.AppContainer
import com.nushhack26.app.data.repo.IdentityState
import com.nushhack26.app.ui.components.SyncStatusBar
import com.nushhack26.app.ui.enrol.EnrolScreen
import com.nushhack26.app.ui.navigation.AppNavHost
import com.nushhack26.app.ui.navigation.DailyRecordRoute
import com.nushhack26.app.ui.navigation.DebugRoute
import com.nushhack26.app.ui.navigation.ForkRoute
import com.nushhack26.app.ui.navigation.GiveRoute
import com.nushhack26.app.ui.navigation.ReceiveRoute
import com.nushhack26.app.ui.navigation.ReportMissingRoute
import com.nushhack26.app.ui.navigation.TimelineRoute
import com.nushhack26.app.ui.navigation.TopLevelTab

const val APP_NAME = "Countersign"

/** The app frame: Enrol until the phone has an identity, then the role's tabs. */
@Composable
fun AppScaffold(container: AppContainer) {
    val identityState by container.identity.state.collectAsStateWithLifecycle(initialValue = IdentityState.Loading)
    when (val state = identityState) {
        IdentityState.Loading -> Box(Modifier.fillMaxSize())
        IdentityState.NotEnrolled -> EnrolFrame(container)
        is IdentityState.Enrolled -> key(state.identity.role) {
            RoleScaffold(container, TopLevelTab.forRole(state.identity.role))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnrolFrame(container: AppContainer) {
    val syncStatus by container.syncStatus.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text(APP_NAME) })
                SyncStatusBar(syncStatus)
            }
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) { EnrolScreen(container) }
    }
}

/** Top bar, sync status, the current screen and the bottom tabs for one role. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleScaffold(container: AppContainer, tabs: List<TopLevelTab>) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val syncStatus by container.syncStatus.collectAsStateWithLifecycle()
    val currentTab = tabs.firstOrNull { destination?.hasRoute(it.routeClass) == true }

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
                        if (currentTab != null && currentTab != TopLevelTab.Me) {
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
                    tabs.forEach { tab ->
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
        AppNavHost(navController, container, startDestination = tabs.first().route, modifier = Modifier.padding(innerPadding))
    }
}

private fun titleFor(destination: NavDestination?, tab: TopLevelTab?): String = when {
    tab != null -> tab.label
    destination == null -> APP_NAME
    destination.hasRoute(GiveRoute::class) -> "Hand over"
    destination.hasRoute(ReceiveRoute::class) -> "Receive"
    destination.hasRoute(TimelineRoute::class) -> "Custody chain"
    destination.hasRoute(ForkRoute::class) -> "Fork"
    destination.hasRoute(DailyRecordRoute::class) -> "Daily record"
    destination.hasRoute(ReportMissingRoute::class) -> "Report missing"
    destination.hasRoute(DebugRoute::class) -> "Debug"
    else -> APP_NAME
}
