package com.nushhack26.app.ui.fork

import androidx.compose.runtime.Composable
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.components.EmptyState

/** Fork Placeholder: card 06 builds this screen (docs/spec.md section 3). */
@Composable
fun ForkScreen(container: AppContainer, alertId: String, onDone: () -> Unit) {
    EmptyState(title = "Fork", message = "Card 06 builds this screen.")
}
