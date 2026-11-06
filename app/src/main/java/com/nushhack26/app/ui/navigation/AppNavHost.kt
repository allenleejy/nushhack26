package com.nushhack26.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.alerts.AlertsScreen
import com.nushhack26.app.ui.dailyrecord.DailyRecordScreen
import com.nushhack26.app.ui.debug.DebugScreen
import com.nushhack26.app.ui.devices.DevicesScreen
import com.nushhack26.app.ui.fork.ForkScreen
import com.nushhack26.app.ui.give.GiveScreen
import com.nushhack26.app.ui.me.MeScreen
import com.nushhack26.app.ui.missing.ReportMissingScreen
import com.nushhack26.app.ui.mycustody.MyCustodyScreen
import com.nushhack26.app.ui.outbox.OutboxScreen
import com.nushhack26.app.ui.receive.ReceiveScreen
import com.nushhack26.app.ui.store.StoreScreen
import com.nushhack26.app.ui.theme.LocalReduceMotion
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetMotion
import com.nushhack26.app.ui.timeline.TimelineScreen

/** What the tab pages ask of the frame around them. */
class TabActions(
    val openItem: (String) -> Unit,
    val handOver: (String) -> Unit,
    val openFork: (String) -> Unit,
    /** Switch to the RECEIVE tab, or push Receive for a role without one. */
    val receive: () -> Unit,
    /** The RECEIVE tab finished (done, closed, declined): reset it and go to the first tab. */
    val endReceive: () -> Unit,
    /** True once an offer has arrived on the RECEIVE tab: the bar hides until it ends. */
    val receiveFlowChanged: (Boolean) -> Unit,
)

/**
 * Every screen but the tabs is pushed here. A new screen slides in from the right over the old
 * one, which moves a quarter to the left; back slides it out to the right. No fades.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    container: AppContainer,
    modifier: Modifier = Modifier,
    home: @Composable () -> Unit,
) {
    val openItem: (String) -> Unit = { navController.navigate(TimelineRoute(it)) }
    val back: () -> Unit = { navController.popBackStack() }
    val reduce = LocalReduceMotion.current
    val push = tween<IntOffset>(TenetMotion.PUSH_MS, easing = TenetMotion.Emphasized)
    val pop = tween<IntOffset>(TenetMotion.POP_MS, easing = TenetMotion.Emphasized)

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = { if (reduce) EnterTransition.None else slideIntoContainer(SlideDirection.Start, push) },
        exitTransition = { if (reduce) ExitTransition.None else slideOutOfContainer(SlideDirection.Start, push) { it / 4 } },
        popEnterTransition = { if (reduce) EnterTransition.None else slideIntoContainer(SlideDirection.End, pop) { it / 4 } },
        popExitTransition = { if (reduce) ExitTransition.None else slideOutOfContainer(SlideDirection.End, pop) },
    ) {
        composable<HomeRoute> { Screen { home() } }
        // The tab screens on their own, for a role that reaches them without the tab.
        composable<StoreRoute> {
            Screen { StoreScreen(container, readOnly = false, onIssue = { navController.navigate(GiveRoute(it)) }, onReceive = { navController.navigate(ReceiveRoute) }, onOpenItem = openItem) }
        }
        composable<CamerasRoute> { Screen { StoreScreen(container, readOnly = true, onIssue = {}, onReceive = {}, onOpenItem = openItem) } }
        composable<OutboxRoute> { Screen { OutboxScreen(container) } }
        composable<ReceiveRoute> { Screen { ReceiveScreen(container, onDone = back) } }
        composable<GiveRoute> { entry -> Screen { GiveScreen(container, entry.toRoute<GiveRoute>().itemId, onDone = back) } }
        composable<ReceiveRelayedRoute> { entry ->
            Screen { ReceiveScreen(container, onDone = back, relayedOfferId = entry.toRoute<ReceiveRelayedRoute>().offerId) }
        }
        composable<TimelineRoute> { entry ->
            Screen { TimelineScreen(container, entry.toRoute<TimelineRoute>().itemId, onReportMissing = { navController.navigate(ReportMissingRoute(it)) }) }
        }
        composable<ForkRoute> { entry -> Screen { ForkScreen(container, entry.toRoute<ForkRoute>().alertId, onDone = back) } }
        composable<DailyRecordRoute> { Screen { DailyRecordScreen(container) } }
        composable<ReportMissingRoute> { entry -> Screen { ReportMissingScreen(container, entry.toRoute<ReportMissingRoute>().itemId, onDone = back) } }
        composable<MeRoute> { Screen { MeScreen(container, onOpenDebug = { navController.navigate(DebugRoute) }) } }
        composable<DebugRoute> { Screen { DebugScreen(container) } }
    }
}

/** One page of the tab pager. [active] is true once the pager has settled on it. */
@Composable
fun TabPage(tab: TopLevelTab, container: AppContainer, actions: TabActions, active: Boolean, receiveSession: Int) {
    Screen {
        when (tab) {
            TopLevelTab.Custody -> MyCustodyScreen(container, onHandOver = actions.handOver, onReceive = actions.receive, onOpenItem = actions.openItem)
            TopLevelTab.Items -> StoreScreen(container, readOnly = false, onIssue = actions.handOver, onReceive = actions.receive, onOpenItem = actions.openItem)
            TopLevelTab.Receive -> ReceiveScreen(
                container,
                onDone = actions.endReceive,
                asTab = true,
                active = active,
                session = receiveSession,
                onFlowChange = actions.receiveFlowChanged,
            )
            TopLevelTab.Outbox -> OutboxScreen(container)
            TopLevelTab.SupervisorItems -> StoreScreen(container, readOnly = true, onIssue = {}, onReceive = {}, onOpenItem = actions.openItem)
            TopLevelTab.Alerts -> AlertsScreen(container, onOpenFork = actions.openFork, onOpenItem = actions.openItem)
            TopLevelTab.Records -> DailyRecordScreen(container)
            TopLevelTab.Devices -> DevicesScreen(container)
        }
    }
}

/** An opaque page, so a screen sliding over another never shows the one underneath. */
@Composable
private fun Screen(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Tenet.colors.bg000)) { content() }
}
