package com.nushhack26.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Description
import androidx.compose.material.icons.sharp.Inventory2
import androidx.compose.material.icons.sharp.Outbox
import androidx.compose.material.icons.sharp.QrCodeScanner
import androidx.compose.material.icons.sharp.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.nushhack26.app.data.local.Roles
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// Every screen in the app (docs/spec.md section 3). Enrol isn't a route: AppScaffold shows
// it until the phone has an identity. Me opens from the menu icon in the top app bar.

@Serializable data object StoreRoute
@Serializable data object CamerasRoute
@Serializable data object MyCustodyRoute
@Serializable data object AlertsRoute
@Serializable data object OutboxRoute
@Serializable data object MeRoute
@Serializable data object DebugRoute
@Serializable data class GiveRoute(val itemId: String)
@Serializable data object ReceiveRoute
@Serializable data class TimelineRoute(val itemId: String)
@Serializable data class ForkRoute(val alertId: String)
@Serializable data object DailyRecordRoute
@Serializable data class ReportMissingRoute(val itemId: String)

/**
 * The three bottom tabs for each role (design/components/BottomNav). SCAN opens the receive
 * flow full screen, so the bar hides there, as it does in every handover step.
 */
enum class TopLevelTab(val route: Any, val label: String, val icon: ImageVector, val opensFlow: Boolean = false) {
    Custody(MyCustodyRoute, "Custody", Icons.Sharp.Inventory2),
    Items(StoreRoute, "Items", Icons.Sharp.Inventory2),
    Scan(ReceiveRoute, "Scan", Icons.Sharp.QrCodeScanner, opensFlow = true),
    Outbox(OutboxRoute, "Outbox", Icons.Sharp.Outbox),
    SupervisorItems(CamerasRoute, "Items", Icons.Sharp.Inventory2),
    Alerts(AlertsRoute, "Alerts", Icons.Sharp.Warning),
    Records(DailyRecordRoute, "Records", Icons.Sharp.Description),
    ;

    val routeClass: KClass<*> get() = route::class

    companion object {
        fun forRole(role: String): List<TopLevelTab> = when (role) {
            Roles.STORE_OFFICER -> listOf(Items, Scan, Outbox)
            Roles.RSO -> listOf(SupervisorItems, Alerts, Records)
            else -> listOf(Custody, Scan, Outbox)
        }

        /** The safety officer opens on Alerts: it's what the demo's reconnect shows. */
        fun startFor(role: String): TopLevelTab = when (role) {
            Roles.STORE_OFFICER -> Items
            Roles.RSO -> Alerts
            else -> Custody
        }
    }
}
