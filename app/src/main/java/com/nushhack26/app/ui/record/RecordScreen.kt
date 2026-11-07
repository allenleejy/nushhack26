package com.nushhack26.app.ui.record

import androidx.compose.runtime.Composable
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.components.EmptyState

/** One handover as its printed custody receipt (card 30 fills this in). */
@Composable
fun RecordScreen(container: AppContainer, handoverId: String) {
    EmptyState(title = "Receipt", message = "Card 30 prints handover ${handoverId.take(8)} here.")
}
