package com.nushhack26.app.ui.timeline

import androidx.compose.runtime.Composable
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.components.EmptyState

/** Timeline Placeholder: card 05 builds this screen (docs/spec.md section 3). */
@Composable
fun TimelineScreen(container: AppContainer, itemId: String, onReportMissing: (itemId: String) -> Unit) {
    EmptyState(title = "Timeline", message = "Card 05 builds this screen.")
}
