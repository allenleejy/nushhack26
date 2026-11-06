package com.nushhack26.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.sharp.GppBad
import androidx.compose.material.icons.sharp.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.verticalText

// The parts of the handover flow from design/components/HandoverStep: the three-step
// stepper, the seal check row and the QR panel. Cards 03 (giver) and 04 (receiver) use them.

enum class HandoverRole { Giver, Receiver }

/** The giver doesn't scan the seal in our flow (docs/spec.md G1–G5), so step 1 is CHOOSE. */
private val giverSteps = listOf("Choose", "Show offer", "Scan acceptance")
private val receiverSteps = listOf("Scan offer", "Scan seal", "Sign and show")

/** Three square boxes. [currentStep] is 1–3; 4 means all done. */
@Composable
fun HandoverStepper(role: HandoverRole, currentStep: Int, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    val steps = if (role == HandoverRole.Giver) giverSteps else receiverSteps
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
        steps.forEachIndexed { index, label ->
            val number = index + 1
            val done = number < currentStep
            val current = number == currentStep
            val edge = when {
                done -> colors.ok
                current -> colors.signalText
                else -> colors.line
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        when {
                            done -> colors.okBg
                            current -> colors.signalBg
                            else -> Color.Transparent
                        },
                    )
                    .border(TenetDimens.strokeControl, edge)
                    .padding(TenetDimens.space2),
                verticalArrangement = Arrangement.spacedBy(TenetDimens.space1),
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            when {
                                done -> colors.okBg
                                current -> colors.signal
                                else -> Color.Transparent
                            },
                        )
                        .border(TenetDimens.strokeControl, edge),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Icon(Icons.Sharp.Check, contentDescription = "Done", tint = colors.ok, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            "$number",
                            style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold),
                            color = if (current) colors.onSignal else colors.inkMuted,
                        )
                    }
                }
                Text(
                    label.uppercase(),
                    style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold),
                    color = if (done || current) colors.ink else colors.inkMuted,
                    maxLines = 2,
                    // Shrinks a long word like ACCEPTANCE at large font sizes instead of breaking it mid-word.
                    autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = TenetType.recordLabel.fontSize),
                )
            }
        }
    }
}

/** After the seal scan: SEAL MATCHES C-12, or the mismatch with both numbers stacked to compare. */
@Composable
fun SealCheckRow(
    matches: Boolean,
    itemCode: String,
    scannedSeal: String,
    expectedSeal: String,
    modifier: Modifier = Modifier,
) {
    val colors = Tenet.colors
    val tone = if (matches) colors.ok else colors.alertText
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (matches) colors.okBg else colors.alertBg)
            .border(TenetDimens.strokeControl, if (matches) colors.ok else colors.alert)
            .padding(TenetDimens.space4),
        horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (matches) Icons.Sharp.Verified else Icons.Sharp.GppBad,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(TenetDimens.iconLg),
        )
        Column(verticalArrangement = Arrangement.spacedBy(TenetDimens.space1)) {
            Text(
                (if (matches) "Seal matches $itemCode" else "Seal doesn't match $itemCode. Don't sign.").uppercase(),
                style = TenetType.label,
                color = tone,
            )
            if (matches) {
                Text("SEAL $scannedSeal", style = TenetType.record, color = colors.ink)
            } else {
                Text("SCANNED  $scannedSeal", style = TenetType.record, color = colors.ink)
                Text("EXPECTED $expectedSeal", style = TenetType.record, color = colors.ink)
            }
        }
    }
}

/**
 * The QR panel: black on white in both themes, at least 280dp, framed in signal-edge, with a
 * signal side tab naming what it holds (OFFER · C-12), and one sentence above it
 * ("Ask Wei Ming to scan this code."). The screen stays awake at full brightness while shown.
 */
@Composable
fun QrPanel(payload: String, caption: String, instruction: String, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
        Text(instruction, style = TenetType.bodyLg, color = colors.ink)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val qrSide = (maxWidth - TenetDimens.tabWidth - TenetDimens.strokeControl * 2).coerceAtLeast(TenetDimens.qrMin)
            Row(Modifier.border(TenetDimens.strokeControl, colors.signalEdge).padding(TenetDimens.strokeControl)) {
                Box(
                    modifier = Modifier.width(TenetDimens.tabWidth).height(qrSide).background(colors.signal),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        caption.uppercase(),
                        style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold),
                        color = colors.onSignal,
                        maxLines = 1,
                        modifier = Modifier.verticalText(),
                    )
                }
                QrCode(payload, Modifier.size(qrSide).fillMaxHeight())
            }
        }
    }
}
