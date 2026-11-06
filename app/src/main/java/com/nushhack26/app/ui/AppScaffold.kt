package com.nushhack26.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nushhack26.app.AppContainer
import com.nushhack26.app.data.repo.IdentityState
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.components.SyncChip
import com.nushhack26.app.ui.components.SyncStatusBar
import com.nushhack26.app.ui.components.chipState
import com.nushhack26.app.ui.enrol.EnrolScreen
import com.nushhack26.app.ui.navigation.AppNavHost
import com.nushhack26.app.ui.navigation.DailyRecordRoute
import com.nushhack26.app.ui.navigation.DebugRoute
import com.nushhack26.app.ui.navigation.ForkRoute
import com.nushhack26.app.ui.navigation.GiveRoute
import com.nushhack26.app.ui.navigation.MeRoute
import com.nushhack26.app.ui.navigation.OutboxRoute
import com.nushhack26.app.ui.navigation.ReceiveRoute
import com.nushhack26.app.ui.navigation.ReportMissingRoute
import com.nushhack26.app.ui.navigation.TimelineRoute
import com.nushhack26.app.ui.navigation.TopLevelTab
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.hazardStripes

const val APP_NAME = "Tenet"

/** The app frame: Enrol until the phone has an identity, then the role's tabs. */
@Composable
fun AppScaffold(container: AppContainer) {
    val identityState by container.identity.state.collectAsStateWithLifecycle(initialValue = IdentityState.Loading)
    when (val state = identityState) {
        IdentityState.Loading -> Box(Modifier.fillMaxSize().background(Tenet.colors.bg000))
        IdentityState.NotEnrolled -> EnrolFrame(container)
        is IdentityState.Enrolled -> key(state.identity.role) {
            RoleScaffold(container, state.identity.role)
        }
    }
}

@Composable
private fun EnrolFrame(container: AppContainer) {
    val syncStatus by container.syncStatus.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = Tenet.colors.bg000,
        topBar = { TenetTopBar(APP_NAME, syncStatus, navigationIcon = {}, onChipClick = { container.syncScheduler.syncNow() }) },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) { EnrolScreen(container) }
    }
}

/** Top app bar, offline banner, the current screen and the role's three tabs. */
@Composable
private fun RoleScaffold(container: AppContainer, role: String) {
    val tabs = TopLevelTab.forRole(role)
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val syncStatus by container.syncStatus.collectAsStateWithLifecycle()
    val currentTab = tabs.firstOrNull { !it.opensFlow && destination?.hasRoute(it.routeClass) == true }
    val inHandover = destination?.let { it.hasRoute(GiveRoute::class) || it.hasRoute(ReceiveRoute::class) } == true

    Scaffold(
        containerColor = Tenet.colors.bg000,
        topBar = {
            TenetTopBar(
                title = titleFor(destination, currentTab),
                status = syncStatus,
                inHandover = inHandover,
                navigationIcon = {
                    when {
                        currentTab != null -> IconButton(onClick = { navController.navigate(MeRoute) }) {
                            Icon(Icons.Sharp.Menu, contentDescription = "Me")
                        }
                        destination == null -> Unit
                        inHandover -> IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Sharp.Close, contentDescription = "Close")
                        }
                        else -> IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Sharp.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                onChipClick = {
                    container.syncScheduler.syncNow()
                    if (destination?.hasRoute(OutboxRoute::class) != true) navController.navigate(OutboxRoute)
                },
            )
        },
        bottomBar = {
            if (currentTab != null) TenetBottomNav(tabs, currentTab, navController)
        },
    ) { innerPadding ->
        AppNavHost(
            navController,
            container,
            startDestination = TopLevelTab.startFor(role).route,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * design/components/TopAppBar: bg-100, the title in uppercase mono, the phone's SyncChip on
 * the right, a 2dp rule below, or a hazard band inside the handover flow. Then the offline banner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TenetTopBar(
    title: String,
    status: SyncStatus,
    navigationIcon: @Composable () -> Unit,
    onChipClick: () -> Unit,
    inHandover: Boolean = false,
) {
    val colors = Tenet.colors
    Column {
        TopAppBar(
            title = { Text(title.uppercase(), style = TenetType.title) },
            navigationIcon = navigationIcon,
            actions = {
                SyncChip(
                    state = status.chipState(),
                    count = status.pendingCount,
                    modifier = Modifier
                        .padding(end = TenetDimens.space2)
                        .clickable(onClickLabel = "Sync now and open the outbox", onClick = onChipClick),
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colors.bg100,
                titleContentColor = colors.ink,
                navigationIconContentColor = colors.ink,
                actionIconContentColor = colors.ink,
            ),
        )
        if (inHandover) {
            Box(Modifier.fillMaxWidth().height(6.dp).hazardStripes(colors.onSignal, colors.signal))
        } else {
            HorizontalDivider(thickness = TenetDimens.strokeControl, color = colors.lineStrong)
        }
        SyncStatusBar(status)
    }
}

/** design/components/BottomNav: three square cells; the selected one filled signal-bg with a 6dp signal bar on top. */
@Composable
private fun TenetBottomNav(tabs: List<TopLevelTab>, currentTab: TopLevelTab, navController: NavHostController) {
    val colors = Tenet.colors
    Column {
        HorizontalDivider(thickness = TenetDimens.strokeControl, color = colors.lineStrong)
        NavigationBar(containerColor = colors.bg100, tonalElevation = 0.dp) {
            tabs.forEach { tab ->
                val selected = tab == currentTab
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (tab.opensFlow) {
                            navController.navigate(tab.route)
                        } else {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.label.uppercase(), style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = colors.signalText,
                        selectedTextColor = colors.ink,
                        unselectedIconColor = colors.inkMuted,
                        unselectedTextColor = colors.inkMuted,
                    ),
                    modifier = if (selected) {
                        Modifier
                            .background(colors.signalBg)
                            .drawBehind { drawRect(colors.signal, size = Size(size.width, 6.dp.toPx())) }
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

private fun titleFor(destination: NavDestination?, tab: TopLevelTab?): String = when {
    tab != null -> tab.label
    destination == null -> APP_NAME
    destination.hasRoute(GiveRoute::class) -> "Hand over"
    destination.hasRoute(ReceiveRoute::class) -> "Receive"
    destination.hasRoute(TimelineRoute::class) -> "Custody chain"
    destination.hasRoute(ForkRoute::class) -> "Fork"
    destination.hasRoute(OutboxRoute::class) -> "Outbox"
    destination.hasRoute(DailyRecordRoute::class) -> "Records"
    destination.hasRoute(ReportMissingRoute::class) -> "Report missing"
    destination.hasRoute(MeRoute::class) -> "Me"
    destination.hasRoute(DebugRoute::class) -> "Debug"
    else -> APP_NAME
}
