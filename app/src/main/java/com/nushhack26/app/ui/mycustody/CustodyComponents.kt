package com.nushhack26.app.ui.mycustody

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Alarm
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material.icons.sharp.Schedule
import androidx.compose.material.icons.sharp.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nushhack26.app.crypto.CustodyStatus
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.ui.components.DangerButton
import com.nushhack26.app.ui.components.HazardDiamond
import com.nushhack26.app.ui.components.SecondaryButton
import com.nushhack26.app.ui.components.dashedBorder
import com.nushhack26.app.ui.store.CustodyStatusChip
import com.nushhack26.app.ui.store.NotSyncedMark
import com.nushhack26.app.ui.store.clockText
import com.nushhack26.app.ui.store.durationText
import com.nushhack26.app.ui.store.remainingText
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.hazardStripes

/**
 * Time left on a loan (design/components/CountdownBar): a big mono readout and a square bar
 * that empties. Past due it turns into a solid orange "OVERDUE" readout with stripes, never colour alone.
 */
@Composable
fun CountdownBar(dueMs: Long, startMs: Long?, nowMs: Long, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    val late = nowMs > dueMs
    val total = (dueMs - (startMs ?: dueMs)).coerceAtLeast(1)
    val fraction = ((dueMs - nowMs).toFloat() / total).coerceIn(0f, 1f)
    Column(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = if (late) "Overdue by ${durationText(nowMs - dueMs)}" else "${durationText(dueMs - nowMs)} left, due ${clockText(dueMs)}"
        },
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space2),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
            Text(
                if (late) "+${durationText(nowMs - dueMs)}" else durationText(dueMs - nowMs),
                style = TenetType.headline, color = if (late) colors.alertText else colors.ink,
            )
            Text(if (late) "LATE" else "LEFT", style = TenetType.label, color = if (late) colors.alertText else colors.inkMuted)
        }
        Box(
            Modifier.fillMaxWidth().height(TenetDimens.stripeBand)
                .border(TenetDimens.strokeControl, if (late) colors.alert else colors.lineStrong)
                .then(if (late) Modifier.hazardStripes(colors.alert, colors.alertBg) else Modifier.background(colors.bg200)),
        ) {
            if (!late) Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(if (fraction < 0.15f) colors.alert else colors.signal))
        }
        Text("DUE ${clockText(dueMs)}", style = TenetType.recordLabel, color = colors.inkMuted)
    }
}

/** A camera I hold (design/components/CustodyCard): identity, status, the countdown and the one way on. */
@Composable
fun CustodyCard(camera: HeldCamera, nowMs: Long, onHandOver: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    val overdue = camera.status == CustodyStatus.Overdue
    Column(
        modifier.fillMaxWidth().background(colors.bg100)
            .border(TenetDimens.strokeControl, if (overdue) colors.alert else colors.lineStrong),
    ) {
        if (overdue) {
            Row(
                Modifier.fillMaxWidth().background(colors.alert).padding(TenetDimens.space3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2),
            ) {
                Icon(Icons.Sharp.Alarm, contentDescription = null, tint = colors.onAlert)
                Text("${camera.code} return overdue · ${durationText(nowMs - (camera.dueMs ?: nowMs))}".uppercase(), style = TenetType.label, color = colors.onAlert)
            }
        }
        Column(Modifier.padding(TenetDimens.space4), verticalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
                HazardDiamond(size = TenetDimens.diamondMd)
                Column(Modifier.weight(1f)) {
                    Text(camera.code.uppercase(), style = TenetType.headline, color = colors.ink)
                    Text(camera.source.uppercase(), style = TenetType.recordLabel, color = colors.inkMuted)
                    Text("SEAL ${camera.sealNo}".uppercase(), style = TenetType.recordLabel, color = colors.inkMuted)
                }
            }
            if (camera.status != CustodyStatus.Disputed) {
                val due = camera.dueMs?.let { " · return by ${clockText(it)}" }.orEmpty()
                Text("You hold ${camera.code} · sealed source ${camera.source.substringAfter(" · ")}$due", style = TenetType.body, color = colors.ink)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2), verticalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
                CustodyStatusChip(camera.status)
                if (!camera.synced) NotSyncedMark()
            }
            camera.dueMs?.let { CountdownBar(it, camera.startMs, nowMs) }
            if (camera.status == CustodyStatus.Disputed) {
                Text(
                    "Two handovers claim this camera. Keep it safe until a supervisor decides who holds it.",
                    style = TenetType.body, color = colors.alertText,
                )
            } else if (overdue) {
                DangerButton("Hand back to store", onClick = onHandOver, modifier = Modifier.fillMaxWidth(), icon = Icons.Sharp.SwapHoriz)
            } else {
                SecondaryButton("Hand over", onClick = onHandOver, modifier = Modifier.fillMaxWidth(), icon = Icons.Sharp.SwapHoriz)
            }
            SecondaryButton("Timeline", onClick = onOpen, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Proof I handed a camera over (design/components/Receipt): who signed, when, seal, match code. */
@Composable
fun Receipt(receipt: ReceiptRow, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    val small = TenetType.body.copy(fontSize = 14.sp, lineHeight = 20.sp)
    val verb = when (receipt.kind) {
        HandoverKind.ISSUE -> "Issued to"
        HandoverKind.RETURN -> "Returned to"
        else -> "Handed to"
    }
    Column(
        modifier.fillMaxWidth().background(colors.bg100).dashedBorder(colors.line).padding(TenetDimens.space4),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space1),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(receipt.code.uppercase(), style = TenetType.label, color = colors.ink, modifier = Modifier.weight(1f))
            if (receipt.isSynced) {
                Icon(Icons.Sharp.CheckCircle, contentDescription = null, tint = colors.ok, modifier = Modifier.padding(end = 4.dp))
                Text("SYNCED", style = TenetType.recordLabel, color = colors.ok)
            } else {
                Icon(Icons.Sharp.Schedule, contentDescription = null, tint = colors.pending, modifier = Modifier.padding(end = 4.dp))
                Text("NOT SYNCED YET", style = TenetType.recordLabel, color = colors.pending)
            }
        }
        Text("$verb ${receipt.toName}", style = TenetType.bodyStrong, color = colors.ink)
        Text(
            listOfNotNull(receipt.acceptedMs?.let { "Signed ${clockText(it)}" }, receipt.sealNo?.let { "seal $it" }).joinToString(" · "),
            style = small, color = colors.inkMuted,
        )
        if (receipt.matchCode.isNotEmpty()) {
            Text("MATCH ${receipt.matchCode}", style = TenetType.record, color = colors.ink)
        }
    }
}

/** An offer of mine still waiting for the receiver's code: open Give to finish it. */
@Composable
fun PendingOfferBanner(offer: PendingOfferRow, nowMs: Long, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Column(
        modifier.fillMaxWidth().background(colors.signalBg).border(TenetDimens.strokeControl, colors.signalEdge).padding(TenetDimens.space4),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space2),
    ) {
        Text("OFFER WAITING · ${offer.code}".uppercase(), style = TenetType.label, color = colors.signalText)
        Text(
            "${offer.toName} hasn't shown you their code yet. ${remainingText(offer.expiresMs, nowMs).replace("left", "until the offer expires")}.",
            style = TenetType.body, color = colors.ink,
        )
        SecondaryButton("Finish handover", onClick = onOpen, modifier = Modifier.fillMaxWidth())
    }
}
