package com.nushhack26.app.ui.me

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer

sealed interface MeAction {
    data object VersionTapped : MeAction
}

@Composable
fun MeScreen(container: AppContainer, onOpenDebug: () -> Unit) {
    val viewModel: MeViewModel = viewModel(
        factory = viewModelFactory { initializer { MeViewModel(container.session, container.serverConfigured) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var taps by remember { mutableIntStateOf(0) }
    MeContent(state) { action ->
        when (action) {
            MeAction.VersionTapped -> {
                taps++
                if (taps >= 5) {
                    taps = 0
                    onOpenDebug()
                }
            }
        }
    }
}

@Composable
fun MeContent(state: MeUiState, modifier: Modifier = Modifier, onAction: (MeAction) -> Unit = {}) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Signed in as", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = state.userId?.let { "User ${it.take(8)}" } ?: "Will sign in when online",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("user_id"),
                )
                if (!state.serverConfigured) {
                    Text("Server not configured", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Text(
            text = "Version ${state.version}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable { onAction(MeAction.VersionTapped) }
                .padding(vertical = 14.dp)
                .testTag("version"),
        )
    }
}
