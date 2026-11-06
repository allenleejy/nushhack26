package com.nushhack26.app.ui.receive

import androidx.compose.runtime.Composable
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.components.EmptyState

/** Receive Placeholder: card 04 builds this screen (docs/spec.md section 3). */
@Composable
fun ReceiveScreen(container: AppContainer, onDone: () -> Unit) {
    EmptyState(title = "Receive", message = "Card 04 builds this screen.")
}
