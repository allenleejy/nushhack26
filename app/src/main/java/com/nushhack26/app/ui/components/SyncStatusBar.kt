package com.nushhack26.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nushhack26.app.sync.SyncStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The offline banner, the number of changes waiting and the last sync time. */
@Composable
fun SyncStatusBar(status: SyncStatus, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        if (!status.isOnline) {
            Text(
                text = "Offline. Your work is saved on this phone.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("offline_banner"),
            )
        }
        Text(
            text = syncSummary(status),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("sync_summary"),
        )
    }
}

fun syncSummary(status: SyncStatus): String {
    val waiting = when (status.pendingCount) {
        0 -> "All changes synced"
        1 -> "1 change waiting"
        else -> "${status.pendingCount} changes waiting"
    }
    val last = status.lastSyncAt?.let { "Last sync ${formatTime(it)}" } ?: "Not synced yet"
    val problem = if (status.lastError != null && status.isOnline) " · Sync problem" else ""
    return "$waiting · $last$problem"
}

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

fun formatTime(epochMillis: Long): String =
    timeFormat.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
