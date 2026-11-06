package com.nushhack26.app.ui.mycustody

import androidx.compose.runtime.Composable
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.components.EmptyState

/** My custody Placeholder: card 05 builds this screen (docs/spec.md section 3). */
@Composable
fun MyCustodyScreen(container: AppContainer, onHandOver: (itemId: String) -> Unit, onReceive: () -> Unit, onOpenItem: (itemId: String) -> Unit) {
    EmptyState(title = "My custody", message = "Card 05 builds this screen.")
}
