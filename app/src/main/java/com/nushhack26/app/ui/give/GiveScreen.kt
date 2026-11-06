package com.nushhack26.app.ui.give

import androidx.compose.runtime.Composable
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.components.EmptyState

/** Hand over Placeholder: card 03 builds this screen (docs/spec.md section 3). */
@Composable
fun GiveScreen(container: AppContainer, itemId: String, onDone: () -> Unit) {
    EmptyState(title = "Hand over", message = "Card 03 builds this screen.")
}
