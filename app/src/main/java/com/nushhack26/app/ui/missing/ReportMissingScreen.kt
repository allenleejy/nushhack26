package com.nushhack26.app.ui.missing

import androidx.compose.runtime.Composable
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.components.EmptyState

/** Report missing Placeholder: card 07 builds this screen (docs/spec.md section 3). */
@Composable
fun ReportMissingScreen(container: AppContainer, itemId: String, onDone: () -> Unit) {
    EmptyState(title = "Report missing", message = "Card 07 builds this screen.")
}
