package com.nushhack26.app.ui.tasks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nushhack26.app.ui.components.EmptyState

/** Placeholder until the spec names this screen. */
@Composable
fun TasksScreen() {
    TasksContent()
}

@Composable
fun TasksContent(modifier: Modifier = Modifier) {
    EmptyState(
        title = "No tasks yet",
        icon = Icons.AutoMirrored.Filled.List,
        modifier = modifier,
    )
}
