package com.nushhack26.app.ui.alerts

import androidx.compose.runtime.Composable
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.components.EmptyState

/** Alerts Placeholder: card 06 builds this screen (docs/spec.md section 3). */
@Composable
fun AlertsScreen(container: AppContainer, onOpenFork: (alertId: String) -> Unit, onOpenItem: (itemId: String) -> Unit) {
    EmptyState(title = "Alerts", message = "Card 06 builds this screen.")
}
