package com.nushhack26.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// Every screen in the app. The plan job adds one route per feature screen.

@Serializable data object HomeRoute
@Serializable data object TasksRoute
@Serializable data object MeRoute
@Serializable data object DebugRoute

/** The bottom navigation tabs, in order. */
enum class TopLevelTab(val route: Any, val label: String, val icon: ImageVector) {
    Home(HomeRoute, "Home", Icons.Filled.Home),
    Tasks(TasksRoute, "Tasks", Icons.AutoMirrored.Filled.List),
    Me(MeRoute, "Me", Icons.Filled.Person),
    ;

    val routeClass: KClass<*> get() = route::class
}
