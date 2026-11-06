package com.nushhack26.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.data.local.OutboxEntry
import com.nushhack26.app.ui.components.formatTime
import com.nushhack26.app.ui.components.rememberSignPrompt
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import android.os.SystemClock
import com.nushhack26.app.nearby.TapPayload
import com.nushhack26.app.nearby.ReceiverTapSession

/** Hidden screen for bug reports and the demo's tampered phone: tap the version on Me 5 times. */
@Composable
fun DebugScreen(container: AppContainer) {
    val viewModel: DebugViewModel = viewModel(factory = viewModelFactory { initializer { DebugViewModel(container) } })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sign = rememberSignPrompt()
    val scope = rememberCoroutineScope()
    var serving by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf(emptyList<String>()) }
    var payloadText by remember { mutableStateOf<String?>(null) }
    var tapped by remember { mutableStateOf<TapPayload?>(null) }
    var session by remember { mutableStateOf<ReceiverTapSession?>(null) }
    val availability by container.tapLink.available.collectAsStateWithLifecycle()
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { container.tapLink.refresh() }
    fun event(text: String) { log = (log + text).takeLast(12) }
    LaunchedEffect(serving) {
        if (serving) container.tapLink.serve("CS1O:test-${System.currentTimeMillis()}").collect { event(it.toString()) }
    }
    container.tapLink.rememberTapReader(reading) {
        event("Tap to read: ${container.tapLink.lastTapReadAt - container.tapLink.lastTapStartedAt} ms")
        payloadText = TapPayload.encode(it.offer, it.key)
        tapped = it
        reading = false
    }
    LaunchedEffect(tapped) {
        val payload = tapped ?: return@LaunchedEffect
        val start = container.tapLink.lastTapStartedAt
        session?.close()
        val joined = container.tapLink.join(payload)
        session = joined
        try {
            val linked = withTimeoutOrNull(10_000) { joined.linked.first { it } }
            event(if (linked == true) "Tap to linked: ${SystemClock.elapsedRealtime() - start} ms" else "Link timed out")
            event("Send: ${linked == true && joined.send("CS1A:test-${System.currentTimeMillis()}")}")
        } finally { joined.close() }
    }
    DisposableEffect(Unit) { onDispose { session?.close() } }
    DebugContent(state.copy(tapAvailability = availability, tapServing = serving, tapReading = reading, tapLog = log, tapPayload = payloadText)) { action ->
        when (action) {
            DebugAction.GrantTapPermissions -> permissions.launch(container.tapLink.requiredPermissions())
            DebugAction.ToggleTapServe -> { reading = false; tapped = null; session?.close(); serving = !serving }
            DebugAction.ToggleTapRead -> {
                serving = false
                reading = !reading
                if (reading) { tapped = null; session?.close() }
            }
            DebugAction.TestSigning -> scope.launch { viewModel.testSigning(sign("Test signing", "Signs a test string with this phone's key")) }
            else -> viewModel.onAction(action)
        }
    }
}

@Composable
fun DebugContent(state: DebugUiState, modifier: Modifier = Modifier, onAction: (DebugAction) -> Unit = {}) {
    var confirmClear by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text("Tap test", style = MaterialTheme.typography.titleMedium) }
        item { Fact("Availability", state.tapAvailability.name) }
        item {
            OutlinedButton(onClick = { onAction(DebugAction.GrantTapPermissions) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Grant permissions") }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAction(DebugAction.ToggleTapServe) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (state.tapServing) "Stop" else "Serve test offer")
                }
                OutlinedButton(onClick = { onAction(DebugAction.ToggleTapRead) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (state.tapReading) "Stop read" else "Read")
                }
            }
        }
        state.tapPayload?.let { item { Fact("Tap payload", it) } }
        item { Text(state.tapLog.joinToString("\n").ifEmpty { "No tap events yet" }, style = MaterialTheme.typography.bodyMedium) }
        item { HorizontalDivider() }
        item { Fact("Build", state.version) }
        item { Fact("Server", if (state.serverConfigured) "Configured" else "Not configured") }
        item { Fact("User id", state.userId ?: "Not signed in") }
        item {
            Fact(
                "Enrolled as",
                state.identity?.let { "${it.name} (${it.role}) · device ${it.deviceId.take(8)}" } ?: "Not enrolled",
            )
        }
        item { Fact("Network", if (state.isOnline) "Online" else "Offline") }
        item { Fact("Last sync", state.lastSyncAt?.let(::formatTime) ?: "Never") }
        item { Fact("Last error", state.lastError ?: "None") }
        item {
            FlowRow(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { onAction(DebugAction.SyncNow) }) { Text("Sync now") }
                OutlinedButton(onClick = { confirmClear = true }) { Text("Clear local data") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tampered phone", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Keep an old copy of the token and hide my handovers (demo)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.tampered, onCheckedChange = { onAction(DebugAction.SetTampered(it)) })
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onAction(DebugAction.TestSigning) }) { Text("Test signing") }
                state.testSigning?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        item { Text("Outbox (${state.outbox.size})", style = MaterialTheme.typography.titleMedium) }
        if (state.outbox.isEmpty()) {
            item { Text("Outbox empty", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(state.outbox, key = { it.opId }) { OutboxRow(it) }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear local data?") },
            text = { Text("Changes still waiting in the outbox are lost. Sync first. This phone stays enrolled.") },
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
