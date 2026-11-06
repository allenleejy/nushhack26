package com.nushhack26.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The offline banner under the top app bar (design/components/OfflineBanner). Offline is a
 * normal working mode, so it's calm, never red. Online, it's hidden: the SyncChip in the top
 * app bar shows the outbox.
 */
@Composable
fun SyncStatusBar(status: SyncStatus, modifier: Modifier = Modifier) {
    if (status.isOnline) return
    val colors = Tenet.colors
    Column(modifier.fillMaxWidth().background(colors.bg200).testTag("offline_banner")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TenetDimens.targetRow)
                .padding(horizontal = TenetDimens.space4, vertical = TenetDimens.space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
        ) {
            Text(
                "OFFLINE",
                style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold),
                color = colors.ink,
                modifier = Modifier
                    .border(TenetDimens.strokeControl, colors.ink, RectangleShape)
                    .padding(horizontal = TenetDimens.space2, vertical = TenetDimens.space1),
            )
            Column {
                Text(waitingSentence(status.pendingCount), style = TenetType.body, color = colors.ink)
                Text(
                    (status.lastSyncAt?.let { "Last sync ${formatTime(it)}" } ?: "Not synced yet").uppercase(),
                    style = TenetType.recordLabel,
                    color = colors.inkMuted,
                    modifier = Modifier.testTag("sync_summary"),
                )
            }
        }
        HorizontalDivider(thickness = TenetDimens.strokeControl, color = colors.lineStrong)
    }
}

private fun waitingSentence(pending: Int): String = when (pending) {
    0 -> "Your work is saved on this phone."
    1 -> "1 record saved on this phone"
    else -> "$pending records saved on this phone"
}

/** One line for Me, Outbox and Debug: what's waiting and when this phone last synced. */
fun syncSummary(status: SyncStatus): String {
    val waiting = when (status.pendingCount) {
        0 -> if (status.lastSyncAt == null) "No changes waiting" else "All changes synced"
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
