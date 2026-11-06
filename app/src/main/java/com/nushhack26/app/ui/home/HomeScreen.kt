package com.nushhack26.app.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nushhack26.app.ui.components.EmptyState

/** Placeholder until the spec names the main screen. */
@Composable
fun HomeScreen() {
    HomeContent()
}

@Composable
fun HomeContent(modifier: Modifier = Modifier) {
    EmptyState(
        title = "Nothing here yet",
        message = "Work you do on this phone is saved here first, then synced.",
        icon = Icons.Filled.Home,
        modifier = modifier,
    )
}
