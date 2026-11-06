package com.nushhack26.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.data.local.OutboxEntry
import com.nushhack26.app.ui.components.formatTime

/** Hidden screen for bug reports: tap the version on Me 5 times. */
@Composable
fun DebugScreen(container: AppContainer) {
    val viewModel: DebugViewModel = viewModel(factory = viewModelFactory { initializer { DebugViewModel(container) } })
    val state by viewModel.state.collectAsStateWithLifecycle()
    DebugContent(state, onAction = viewModel::onAction)
}

@Composable
fun DebugContent(state: DebugUiState, modifier: Modifier = Modifier, onAction: (DebugAction) -> Unit = {}) {
    var confirmClear by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Fact("Build", state.version) }
        item { Fact("Server", if (state.serverConfigured) "Configured" else "Not configured") }
        item { Fact("User id", state.userId ?: "Not signed in") }
        item { Fact("Network", if (state.isOnline) "Online" else "Offline") }
        item { Fact("Last sync", state.lastSyncAt?.let(::formatTime) ?: "Never") }
        item { Fact("Last error", state.lastError ?: "None") }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAction(DebugAction.SyncNow) }) { Text("Sync now") }
                OutlinedButton(onClick = { confirmClear = true }) { Text("Clear local data") }
            }
        }
        item { Text("Outbox (${state.outbox.size})", style = MaterialTheme.typography.titleMedium) }
        if (state.outbox.isEmpty()) {
            item { Text("Empty", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(state.outbox, key = { it.opId }) { OutboxRow(it) }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear local data?") },
            text = { Text("Changes still waiting in the outbox are lost. Sync first.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onAction(DebugAction.ClearLocalData)
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun OutboxRow(entry: OutboxEntry) {
    Column(Modifier.fillMaxWidth()) {
        Text("${entry.kind} ${entry.tableName} ${entry.recordId.take(8)}", style = MaterialTheme.typography.bodyMedium)
        Text(
            "op ${entry.opId.take(8)} · base v${entry.baseVersion ?: "-"} · ${formatTime(entry.createdAt)} · tries ${entry.attempts}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        entry.lastError?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error) }
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}
