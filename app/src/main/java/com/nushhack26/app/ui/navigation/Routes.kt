package com.nushhack26.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Contactless
import androidx.compose.material.icons.sharp.Description
import androidx.compose.material.icons.sharp.Inventory2
import androidx.compose.material.icons.sharp.Outbox
import androidx.compose.material.icons.sharp.PhonelinkLock
import androidx.compose.material.icons.sharp.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.nushhack26.app.data.local.Roles
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// Every screen in the app (docs/spec.md section 3). Enrol isn't a route: AppScaffold shows
// it until the phone has an identity. Me opens from the initials square in the top app bar.

/** The role's three tabs, side by side in a pager you can swipe (AppScaffold). */
@Serializable data object HomeRoute

@Serializable data object StoreRoute
@Serializable data object CamerasRoute
@Serializable data object MyCustodyRoute
@Serializable data object AlertsRoute
@Serializable data object OutboxRoute
@Serializable data object MeRoute
@Serializable data object DebugRoute
@Serializable data class GiveRoute(val itemId: String)
@Serializable data object ReceiveRoute
/** Receive an offer sent through the server (docs/sync.md A.10), opened from the incoming-offer banner. */
@Serializable data class ReceiveRelayedRoute(val offerId: String)
@Serializable data class TimelineRoute(val itemId: String)
@Serializable data class ForkRoute(val alertId: String)
@Serializable data object DailyRecordRoute
/** The safety officer's enrolled phones, where a lost one is withdrawn (docs/sync.md A.12). */
@Serializable data object DevicesRoute
@Serializable data class ReportMissingRoute(val itemId: String)

/**
 * The three bottom tabs for each role (design/components/BottomNav), the pages of [HomeRoute].
 * RECEIVE waits for a tap with the bar showing; once an offer arrives, the bar hides for the
 * rest of the handover. [route] is the same screen pushed on its own, for roles without the tab.
 */
enum class TopLevelTab(val route: Any, val label: String, val icon: ImageVector) {
    Custody(MyCustodyRoute, "Custody", Icons.Sharp.Inventory2),
    Items(StoreRoute, "Items", Icons.Sharp.Inventory2),
    Receive(ReceiveRoute, "Receive", Icons.Sharp.Contactless),
    Outbox(OutboxRoute, "Outbox", Icons.Sharp.Outbox),
    SupervisorItems(CamerasRoute, "Items", Icons.Sharp.Inventory2),
    Alerts(AlertsRoute, "Alerts", Icons.Sharp.Warning),
    Records(DailyRecordRoute, "Records", Icons.Sharp.Description),
    Devices(DevicesRoute, "Devices", Icons.Sharp.PhonelinkLock),
    ;

    val routeClass: KClass<*> get() = route::class

    companion object {
        fun forRole(role: String): List<TopLevelTab> = when (role) {
            Roles.STORE_OFFICER -> listOf(Items, Receive, Outbox)
            Roles.RSO -> listOf(SupervisorItems, Alerts, Devices)
            else -> listOf(Custody, Receive, Outbox)
        }

        /** The safety officer opens on Alerts: it's what the demo's reconnect shows. */
        fun startFor(role: String): TopLevelTab = when (role) {
            Roles.STORE_OFFICER -> Items
            Roles.RSO -> Alerts
            else -> Custody
        }
    }
}
