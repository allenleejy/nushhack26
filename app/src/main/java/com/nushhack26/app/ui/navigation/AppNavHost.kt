package com.nushhack26.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.alerts.AlertsScreen
import com.nushhack26.app.ui.dailyrecord.DailyRecordScreen
import com.nushhack26.app.ui.debug.DebugScreen
import com.nushhack26.app.ui.fork.ForkScreen
import com.nushhack26.app.ui.give.GiveScreen
import com.nushhack26.app.ui.me.MeScreen
import com.nushhack26.app.ui.missing.ReportMissingScreen
import com.nushhack26.app.ui.mycustody.MyCustodyScreen
import com.nushhack26.app.ui.outbox.OutboxScreen
import com.nushhack26.app.ui.receive.ReceiveScreen
import com.nushhack26.app.ui.store.StoreScreen
import com.nushhack26.app.ui.timeline.TimelineScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    container: AppContainer,
    startDestination: Any,
    modifier: Modifier = Modifier,
) {
    val openItem: (String) -> Unit = { navController.navigate(TimelineRoute(it)) }
    val receive: () -> Unit = { navController.navigate(ReceiveRoute) }
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable<StoreRoute> {
            StoreScreen(container, readOnly = false, onIssue = { navController.navigate(GiveRoute(it)) }, onReceive = receive, onOpenItem = openItem)
        }
        composable<CamerasRoute> {
            StoreScreen(container, readOnly = true, onIssue = {}, onReceive = {}, onOpenItem = openItem)
        }
        composable<MyCustodyRoute> {
            MyCustodyScreen(container, onHandOver = { navController.navigate(GiveRoute(it)) }, onReceive = receive, onOpenItem = openItem)
        }
        composable<GiveRoute> { entry -> GiveScreen(container, entry.toRoute<GiveRoute>().itemId, onDone = back) }
        composable<ReceiveRoute> { ReceiveScreen(container, onDone = back) }
        composable<TimelineRoute> { entry ->
            TimelineScreen(container, entry.toRoute<TimelineRoute>().itemId, onReportMissing = { navController.navigate(ReportMissingRoute(it)) })
        }
        composable<AlertsRoute> {
            AlertsScreen(container, onOpenFork = { navController.navigate(ForkRoute(it)) }, onOpenItem = openItem)
        }
        composable<ForkRoute> { entry -> ForkScreen(container, entry.toRoute<ForkRoute>().alertId, onDone = back) }
        composable<DailyRecordRoute> { DailyRecordScreen(container) }
        composable<ReportMissingRoute> { entry -> ReportMissingScreen(container, entry.toRoute<ReportMissingRoute>().itemId, onDone = back) }
        composable<OutboxRoute> { OutboxScreen(container) }
        composable<MeRoute> { MeScreen(container, onOpenDebug = { navController.navigate(DebugRoute) }) }
        composable<DebugRoute> { DebugScreen(container) }
    }
}
