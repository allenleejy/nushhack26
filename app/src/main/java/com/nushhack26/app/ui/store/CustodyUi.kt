package com.nushhack26.app.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.CallSplit
import androidx.compose.material.icons.sharp.Alarm
import androidx.compose.material.icons.sharp.Inventory2
import androidx.compose.material.icons.sharp.Schedule
import androidx.compose.material.icons.sharp.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nushhack26.app.crypto.CustodyStatus
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.ui.components.dashedBorder
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.hazardStripes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

// Shared by Store, My custody and the timeline (card 05).

/** A camera inside a sentence: "Kestrel (C-12)", or "C-12" while it has no name (docs/spec.md, camera names). */
fun cameraPhrase(name: String, code: String): String = if (name.isBlank() || name == code) code else "$name ($code)"

val ItemEntity.phrase: String get() = cameraPhrase(name, code)

/** A title's labelled code line: "CAMERA C-12 · SOURCE IR-0457 · IR-192". Blank parts are left out. */
fun codeLine(vararg parts: String?): String = parts.filterNot { it.isNullOrBlank() }.joinToString(" · ").uppercase()

/** A phone or server time as 24-hour HH:mm, no seconds. */
fun clockText(ms: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(Instant.ofEpochMilli(ms))

/**
 * A duration in short words: "5 h 58 min", "38 min". Under 10 minutes it shows m:ss, so the
 * demo's 90-second loan visibly counts down.
 */
fun durationText(ms: Long): String {
    val totalSec = abs(ms) / 1000
    val minutes = totalSec / 60
    return when {
        minutes >= 60 -> "${minutes / 60} h ${minutes % 60} min"
        minutes >= 10 -> "$minutes min"
        else -> "%d:%02d".format(minutes, totalSec % 60)
    }
}

/** "1:02 left" before the due time, "4 min late" after it. */
fun remainingText(dueMs: Long, nowMs: Long): String =
    if (dueMs >= nowMs) "${durationText(dueMs - nowMs)} left" else "${durationText(nowMs - dueMs)} late"

/** In store, Out, Overdue (orange) or Disputed (striped orange), with an icon and a word. */
@Composable
fun CustodyStatusChip(status: CustodyStatus, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    val (text, icon) = when (status) {
        CustodyStatus.InStore -> "In store" to Icons.Sharp.Inventory2
        CustodyStatus.Out -> "Out" to Icons.Sharp.SwapHoriz
        CustodyStatus.Overdue -> "Overdue" to Icons.Sharp.Alarm
        CustodyStatus.Disputed -> "Disputed" to Icons.AutoMirrored.Sharp.CallSplit
        CustodyStatus.Unknown -> "Not synced" to Icons.Sharp.Schedule
    }
    val frame = when (status) {
        CustodyStatus.InStore -> Modifier.background(colors.okBg).border(TenetDimens.strokeControl, colors.ok)
        CustodyStatus.Out -> Modifier.background(colors.bg200).border(TenetDimens.strokeControl, colors.lineStrong)
        CustodyStatus.Overdue -> Modifier.background(colors.alert)
        CustodyStatus.Disputed -> Modifier.hazardStripes(colors.alert, colors.alertBg).border(TenetDimens.strokeControl, colors.alert)
        CustodyStatus.Unknown -> Modifier.dashedBorder(colors.pending)
    }
    val content = when (status) {
        CustodyStatus.InStore -> colors.ok
        CustodyStatus.Out -> colors.ink
        CustodyStatus.Overdue -> colors.onAlert
        CustodyStatus.Disputed -> colors.onAlert
        CustodyStatus.Unknown -> colors.pending
    }
    Box(modifier.heightIn(min = 32.dp).then(frame), contentAlignment = Alignment.Center) {
        // Stripes never sit behind text: the word gets its own solid patch.
        val patch = if (status == CustodyStatus.Disputed) Modifier.padding(4.dp).background(colors.alert) else Modifier
        ChipLabel(text, icon, content, patch.padding(horizontal = TenetDimens.space2, vertical = 4.dp))
    }
}

/** The per-record "Not synced yet" mark: dashed and neutral, like a waiting SyncChip. */
@Composable
fun NotSyncedMark(modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    ChipLabel(
        "Not synced yet", Icons.Sharp.Schedule, colors.pending,
        modifier.heightIn(min = 32.dp).dashedBorder(colors.pending).padding(horizontal = TenetDimens.space2, vertical = 4.dp),
    )
}

@Composable
private fun ChipLabel(text: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TenetDimens.space1)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(text.uppercase(), style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

/** Square bg-200 skeleton blocks, no shimmer (design/components/LoadingSkeleton). */
@Composable
fun SkeletonRows(count: Int, rowHeight: Dp, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Column(modifier.fillMaxWidth().padding(TenetDimens.space4), verticalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
        repeat(count) {
            Row(
                Modifier.fillMaxWidth().height(rowHeight).background(colors.bg100).padding(TenetDimens.space3),
                horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(40.dp).background(colors.bg200))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
                    Box(Modifier.fillMaxWidth(0.6f).height(16.dp).background(colors.bg200))
                    Box(Modifier.fillMaxWidth(0.9f).height(12.dp).background(colors.bg200))
                }
                Box(Modifier.width(72.dp).height(24.dp).background(colors.bg200))
            }
        }
    }
}

const val LOCAL_DB_ERROR = "Couldn't read this phone's records. Restart the app."
