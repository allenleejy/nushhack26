package com.nushhack26.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.CallSplit
import androidx.compose.material.icons.sharp.CloudDone
import androidx.compose.material.icons.sharp.CloudOff
import androidx.compose.material.icons.sharp.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.theme.ChipCutShape
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType

enum class SyncChipState { Synced, Waiting, Conflict, Offline }

/** Where this phone's outbox stands, for the top app bar. */
fun SyncStatus.chipState(): SyncChipState = when {
    !isOnline -> SyncChipState.Offline
    pendingCount > 0 -> SyncChipState.Waiting
    else -> SyncChipState.Synced
}

/**
 * Where one record, or this phone's outbox, stands against the server
 * (design/components/SyncChip). Every state has an icon and a word, never colour alone.
 * [count] is shown for Waiting; [conflictLabel] is FORK for custody, CONFLICT otherwise.
 */
@Composable
fun SyncChip(
    state: SyncChipState,
    modifier: Modifier = Modifier,
    count: Int? = null,
    conflictLabel: String = "FORK",
) {
    val colors = Tenet.colors
    val icon = when (state) {
        SyncChipState.Synced -> Icons.Sharp.CloudDone
        SyncChipState.Waiting -> Icons.Sharp.Schedule
        SyncChipState.Conflict -> Icons.AutoMirrored.Sharp.CallSplit
        SyncChipState.Offline -> Icons.Sharp.CloudOff
    }
    val text = when (state) {
        SyncChipState.Synced -> "SYNCED"
        SyncChipState.Waiting -> if (count != null && count > 0) "$count WAITING" else "WAITING"
        SyncChipState.Conflict -> conflictLabel.uppercase()
        SyncChipState.Offline -> "OFFLINE"
    }
    val content = when (state) {
        SyncChipState.Synced -> colors.ok
        SyncChipState.Waiting -> colors.pending
        SyncChipState.Conflict -> colors.onAlert
        SyncChipState.Offline -> colors.ink
    }
    val shape = if (state == SyncChipState.Synced || state == SyncChipState.Conflict) ChipCutShape else RectangleShape
    val frame = when (state) {
        SyncChipState.Synced -> Modifier.background(colors.okBg, shape)
        SyncChipState.Conflict -> Modifier.background(colors.alert, shape)
        SyncChipState.Offline -> Modifier.background(colors.bg200).border(TenetDimens.strokeControl, colors.lineStrong, RectangleShape)
        SyncChipState.Waiting -> Modifier.dashedBorder(colors.pending)
    }

    Row(
        modifier = modifier
            .height(32.dp)
            .clip(shape)
            .then(frame)
            .padding(horizontal = TenetDimens.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TenetDimens.space1),
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(text, style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold), color = content, maxLines = 1, softWrap = false)
    }
}
