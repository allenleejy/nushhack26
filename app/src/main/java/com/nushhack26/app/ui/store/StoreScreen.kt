package com.nushhack26.app.ui.store

import androidx.compose.runtime.Composable
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.components.EmptyState

/** Store Placeholder: card 05 builds this screen (docs/spec.md section 3). */
@Composable
fun StoreScreen(container: AppContainer, readOnly: Boolean, onIssue: (itemId: String) -> Unit, onReceive: () -> Unit, onOpenItem: (itemId: String) -> Unit) {
    EmptyState(title = "Store", message = "Card 05 builds this screen.")
}
