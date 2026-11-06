package com.nushhack26.app.ui.receive

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
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.GppBad
import androidx.compose.material.icons.sharp.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nushhack26.app.crypto.CheckLine
import com.nushhack26.app.ui.components.DiamondVariant
import com.nushhack26.app.ui.components.HazardDiamond
import com.nushhack26.app.ui.components.formatTime
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.hazardStripes

/** One line per offline check: a green tick or a red cross, in words. */
@Composable
fun CheckList(lines: List<CheckLine>, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Column(
        modifier.fillMaxWidth().border(TenetDimens.strokeHairline, colors.line),
    ) {
        lines.forEachIndexed { i, line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (line.ok) colors.bg100 else colors.alertBg)
                    .padding(horizontal = TenetDimens.space3, vertical = TenetDimens.space2)
                    .semantics { contentDescription = (if (line.ok) "Passed: " else "Failed: ") + line.text },
                horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (line.ok) Icons.Sharp.Check else Icons.Sharp.Close,
                    contentDescription = null,
                    tint = if (line.ok) colors.ok else colors.alertText,
                    modifier = Modifier.size(TenetDimens.iconMd),
                )
                Text(line.text, style = TenetType.body, color = if (line.ok) colors.ink else colors.alertText)
            }
            if (i < lines.lastIndex) Box(Modifier.fillMaxWidth().height(TenetDimens.strokeHairline).background(colors.line))
        }
    }
}

/** R2 refused: what's wrong, every line, and that nothing was signed or stored. */
@Composable
fun RefusedLabel(header: String, lines: List<CheckLine>, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.alertBg)
                .border(TenetDimens.strokeControl, colors.alert)
                .padding(TenetDimens.space4),
            horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Sharp.GppBad, contentDescription = null, tint = colors.alertText, modifier = Modifier.size(TenetDimens.iconLg))
            Text(header.uppercase(), style = TenetType.title, color = colors.alertText)
        }
        CheckList(lines)
        Text("Nothing was signed.", style = TenetType.bodyLg, color = colors.ink)
    }
}

@Composable
fun ErrorLine(text: String, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Text(
        text,
        style = TenetType.body,
        color = colors.alertText,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.alertBg)
            .border(TenetDimens.strokeHairline, colors.alert)
            .padding(TenetDimens.space3),
    )
}

/** Who gives, who receives, and what. */
@Composable
fun WhoAndWhatCard(summary: HandoverSummary, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.bg100)
            .border(TenetDimens.strokeHairline, colors.lineStrong)
            .padding(TenetDimens.space4),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space2),
    ) {
        Text("${summary.itemCode} · ${summary.sourceSerial} · ${summary.isotope}".uppercase(), style = TenetType.title, color = colors.ink)
        Field("From", "${summary.giverName} · ${roleLabel(summary.giverRole)}")
        Field("To", "${summary.receiverName} · ${roleLabel(summary.receiverRole)} (you)")
        Field("Seal", summary.sealNo)
        Field("Return by", summary.dueMs?.let(::formatTime) ?: "No return time")
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column {
        Text(label.uppercase(), style = TenetType.recordLabel, color = Tenet.colors.inkMuted)
        Text(value, style = TenetType.bodyLg, color = Tenet.colors.ink)
    }
}

/** design/components/LiabilityLine: the moment liability moves, directly above Sign. */
@Composable
fun LiabilityLine(sourceSerial: String, isotope: String, itemCode: String, nowMs: Long, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(TenetDimens.strokeControl, colors.signalEdge),
    ) {
        Box(Modifier.width(44.dp).fillMaxHeight().hazardStripes(colors.onSignal, colors.signal))
        Row(
            Modifier.weight(1f).padding(TenetDimens.space3),
            horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
                Text(
                    "Sealed source $sourceSerial".uppercase(),
                    style = TenetType.label.copy(fontWeight = FontWeight.Bold),
                    color = colors.signalText,
                )
                Text(
                    "This is sealed source $sourceSerial (${isotopeName(isotope)}). You're liable for it the moment you sign.",
                    style = TenetType.body,
                    color = colors.ink,
                )
                Text("By signing, you accept liability for $itemCode from ${formatTime(nowMs)}.", style = TenetType.body, color = colors.inkMuted)
            }
            HazardDiamond(variant = DiamondVariant.Filled, size = TenetDimens.diamondLg)
        }
    }
}

/** The six digits both phones show, read aloud. */
@Composable
fun MatchCode(code: String, giverShortName: String, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TenetDimens.space1)) {
        Text("MATCH CODE", style = TenetType.recordLabel, color = colors.inkMuted)
        Text(code, style = TenetType.codeLg, color = colors.ink)
        Text("Check that $giverShortName's screen shows the same.", style = TenetType.body, color = colors.inkMuted)
    }
}

/** R5 done: the receiver now holds the camera. */
@Composable
fun DoneLabel(summary: HandoverSummary, matchCode: String?, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.okBg)
            .border(TenetDimens.strokeControl, colors.ok)
            .padding(TenetDimens.space4),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space2),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Sharp.Verified, contentDescription = null, tint = colors.ok, modifier = Modifier.size(TenetDimens.iconLg))
            Text("Accepted · checked offline".uppercase(), style = TenetType.label, color = colors.ok)
        }
        val due = summary.dueMs?.let { " Return by ${formatTime(it)}." }.orEmpty()
        Text("You hold ${summary.itemCode}.$due", style = TenetType.headline, color = colors.ink)
        matchCode?.let { Text("MATCH CODE $it", style = TenetType.record, color = colors.ink) }
    }
}

fun roleLabel(role: String): String = role.replace('_', ' ')

fun isotopeName(isotope: String): String = when (isotope.lowercase()) {
    "ir-192" -> "iridium-192"
    "se-75" -> "selenium-75"
    "co-60" -> "cobalt-60"
    else -> isotope
}
