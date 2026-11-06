package com.nushhack26.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.nushhack26.app.data.local.Roles
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// Every screen in the app (docs/spec.md section 3). Enrol isn't a route: AppScaffold shows
// it until the phone has an identity.

@Serializable data object StoreRoute
@Serializable data object CamerasRoute
@Serializable data object MyCustodyRoute
@Serializable data object AlertsRoute
@Serializable data object MeRoute
@Serializable data object DebugRoute
@Serializable data class GiveRoute(val itemId: String)
@Serializable data object ReceiveRoute
@Serializable data class TimelineRoute(val itemId: String)
@Serializable data class ForkRoute(val alertId: String)
@Serializable data object DailyRecordRoute
@Serializable data class ReportMissingRoute(val itemId: String)

/** The bottom navigation tabs. Which ones show depends on the person's role. */
enum class TopLevelTab(val route: Any, val label: String, val icon: ImageVector) {
    Store(StoreRoute, "Store", Icons.Filled.Home),
    MyCustody(MyCustodyRoute, "My custody", Icons.Filled.Home),
    Alerts(AlertsRoute, "Alerts", Icons.Filled.Warning),
    Cameras(CamerasRoute, "Cameras", Icons.AutoMirrored.Filled.List),
    Me(MeRoute, "Me", Icons.Filled.Person),
    ;

    val routeClass: KClass<*> get() = route::class

    companion object {
        fun forRole(role: String): List<TopLevelTab> = when (role) {
            Roles.STORE_OFFICER -> listOf(Store, Me)
            Roles.RSO -> listOf(Alerts, Cameras, Me)
            else -> listOf(MyCustody, Me)
        }
    }
}
