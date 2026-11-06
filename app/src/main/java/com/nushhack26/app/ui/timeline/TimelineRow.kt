package com.nushhack26.app.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.CallSplit
import androidx.compose.material.icons.sharp.GppBad
import androidx.compose.material.icons.sharp.Gavel
import androidx.compose.material.icons.sharp.HelpOutline
import androidx.compose.material.icons.sharp.Verified
import androidx.compose.material.icons.sharp.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.ui.store.NotSyncedMark
import com.nushhack26.app.ui.store.clockText
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.hatching
import com.nushhack26.app.ui.theme.hazardStripes

/**
 * One record on the custody chain (design/components/TimelineRow): a square node on a rail,
 * what happened, when, the signature check and the sync mark. Overruled records are hatched
 * and struck out; rejected ones are red with the server's reason.
 */
@Composable
fun TimelineRow(entry: TimelineEntry, modifier: Modifier = Modifier, compact: Boolean = false, last: Boolean = false) {
    val colors = Tenet.colors
    val small = TenetType.body.copy(fontSize = 14.sp, lineHeight = 20.sp)
    val (ground, edge) = when (entry.look) {
        EntryLook.Normal -> Modifier.background(colors.bg100) to colors.line
        EntryLook.Overruled -> Modifier.hatching(colors.line, colors.bg200) to colors.lineStrong
        EntryLook.Rejected -> Modifier.background(colors.alertBg) to colors.alert
        EntryLook.Orphan -> Modifier.background(colors.bg200) to colors.lineStrong
    }
    val struck = entry.look == EntryLook.Overruled
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        if (!compact) {
            Column(Modifier.width(24.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.padding(top = TenetDimens.space4).size(12.dp).background(if (entry.look == EntryLook.Rejected) colors.alert else colors.ink))
                if (!last) Box(Modifier.width(TenetDimens.strokeControl).weight(1f).background(colors.lineStrong))
            }
        }
        Column(
            Modifier.weight(1f).padding(bottom = TenetDimens.space2).then(ground).border(TenetDimens.strokeHairline, edge).padding(TenetDimens.space3),
            verticalArrangement = Arrangement.spacedBy(TenetDimens.space1),
        ) {
            Text(entry.kind.uppercase(), style = TenetType.recordLabel, color = colors.inkMuted)
            Text(
                entry.title, style = TenetType.bodyStrong.copy(textDecoration = if (struck) TextDecoration.LineThrough else null),
                color = if (struck) colors.inkMuted else colors.ink,
            )
            Text(
                listOfNotNull(entry.detail, entry.timeMs?.let { clockText(it) }).joinToString(" · "),
                style = small, color = colors.inkMuted,
            )
            val extra = listOfNotNull(entry.dueMs?.let { "Due ${clockText(it)}" }, entry.sealNo?.let { "Seal $it" })
            if (extra.isNotEmpty()) Text(extra.joinToString(" · "), style = small, color = colors.inkMuted)
            if (entry.matchCode.isNotEmpty()) Text("MATCH ${entry.matchCode}", style = TenetType.recordLabel, color = colors.ink)
            SigLine(entry.sig, entry.keyWithdrawn)
            when {
                entry.look == EntryLook.Overruled -> Patch("OVERRULED BY SUPERVISOR", colors.ink, colors.bg000)
                entry.look == EntryLook.Rejected -> {
                    Patch("REJECTED BY SERVER", colors.onAlert, colors.alert)
                    entry.reason?.let { Text(it, style = small, color = colors.alertText) }
                }
                entry.look == EntryLook.Orphan -> Text("Not linked to this camera's chain on this phone.", style = small, color = colors.inkMuted)
                entry.syncStatus == SyncMark.WAITING -> NotSyncedMark()
            }
        }
    }
}

@Composable
private fun Patch(text: String, ink: Color, ground: Color) {
    Text(text, style = TenetType.recordLabel, color = ink, modifier = Modifier.background(ground).padding(horizontal = TenetDimens.space2, vertical = 2.dp))
}

@Composable
fun SigLine(sig: SigCheck, keyWithdrawn: KeyWithdrawn? = null) {
    val colors = Tenet.colors
    val (text, icon, color) = when (sig) {
        SigCheck.Valid -> Triple("BOTH SIGNED ✓", Icons.Sharp.Verified, colors.ok)
        SigCheck.Invalid -> Triple("SIGNATURE DOESN'T MATCH", Icons.Sharp.GppBad, colors.alertText)
        SigCheck.UnknownKey -> Triple("KEY NOT ON THIS PHONE YET", Icons.Sharp.HelpOutline, colors.pending)
        SigCheck.Unsigned -> Triple("STORE REGISTER", Icons.Sharp.Verified, colors.inkMuted)
    }
    val mark = keyWithdrawn?.let {
        if (it.afterSigning) "KEY WITHDRAWN ${clockText(it.atMs)}" to colors.inkMuted
        else "SIGNED AFTER KEY WITHDRAWN ${clockText(it.atMs)}" to colors.alertText
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TenetDimens.space1)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, style = TenetType.recordLabel, color = color)
    }
    mark?.let { (words, tint) -> Text(words, style = TenetType.recordLabel, color = tint, modifier = Modifier.padding(start = 20.dp)) }
}

/** A fork only this phone knows about yet: a red label above the branches (docs/spec.md T). */
@Composable
fun LocalForkCard(text: String, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Column(modifier.fillMaxWidth().padding(bottom = TenetDimens.space2).border(TenetDimens.strokeControl, colors.alert).background(colors.alertBg)) {
        Row(
            Modifier.fillMaxWidth().background(colors.alert).padding(TenetDimens.space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2),
        ) {
            Icon(Icons.Sharp.Warning, contentDescription = null, tint = colors.onAlert)
            Text("TWO HANDOVERS ON THIS PHONE", style = TenetType.label, color = colors.onAlert)
        }
        Text(text, style = TenetType.body, color = colors.ink, modifier = Modifier.padding(TenetDimens.space3))
        Box(Modifier.fillMaxWidth().height(TenetDimens.space3).hazardStripes(colors.alert, colors.alertBg))
    }
}

/** Two records claim the same camera: the branches side by side under a red marker. */
@Composable
fun ForkBlock(branches: List<List<TimelineEntry>>, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Column(modifier.fillMaxWidth().border(TenetDimens.strokeControl, colors.alert)) {
        Row(
            Modifier.fillMaxWidth().background(colors.alert).padding(TenetDimens.space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2),
        ) {
            Icon(Icons.AutoMirrored.Sharp.CallSplit, contentDescription = null, tint = colors.onAlert)
            Text("FORK · ${branches.size} CLAIMS", style = TenetType.label, color = colors.onAlert)
        }
        Row(Modifier.fillMaxWidth().padding(TenetDimens.space1), horizontalArrangement = Arrangement.spacedBy(TenetDimens.space1)) {
            branches.forEach { branch ->
                Column(Modifier.weight(1f)) {
                    branch.forEach { TimelineRow(it, compact = true) }
                }
            }
        }
    }
}

/** The supervisor's signed decision that settled a fork. */
@Composable
fun ResolutionRow(node: TimelineNode.Resolution, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    val small = TenetType.body.copy(fontSize = 14.sp, lineHeight = 20.sp)
    Column(
        modifier.fillMaxWidth().padding(vertical = TenetDimens.space2).background(colors.bg100)
            .border(TenetDimens.strokeControl, colors.lineStrong).padding(TenetDimens.space3),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space1),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
            Icon(Icons.Sharp.Gavel, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
            Text("RESOLVED", style = TenetType.label, color = colors.ink)
        }
        Text("${node.by} kept ${node.chosenTitle}", style = TenetType.bodyStrong, color = colors.ink)
        Text(listOfNotNull(clockText(node.timeMs), node.note.ifBlank { null }).joinToString(" · "), style = small, color = colors.inkMuted)
        SigLine(node.sig)
        if (!node.synced) NotSyncedMark()
    }
}
