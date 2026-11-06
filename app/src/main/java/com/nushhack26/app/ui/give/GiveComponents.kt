package com.nushhack26.app.ui.give

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowForward
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.hazardStripes

/** design/components/BottomSheet as a modal: the receiver picker. Nothing is saved until its primary button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiveBottomSheet(
    title: String,
    note: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Tenet.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RectangleShape,
        containerColor = colors.bg200,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = null,
    ) {
        SheetBody(title, note, content = content)
    }
}

/** The sheet's own drawing, also used by the screenshot test (dialogs don't render there). */
@Composable
fun SheetBody(title: String, note: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = Tenet.colors
    Column(modifier.fillMaxWidth().background(colors.bg200)) {
        Box(Modifier.fillMaxWidth().height(TenetDimens.strokeControl).background(colors.signalEdge))
        Box(Modifier.fillMaxWidth().padding(top = TenetDimens.space3), contentAlignment = Alignment.Center) {
            Box(Modifier.size(TenetDimens.handle, 4.dp).background(colors.lineStrong))
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = TenetDimens.space4, vertical = TenetDimens.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
        ) {
            Text(title.uppercase(), style = TenetType.title, color = colors.ink, modifier = Modifier.weight(1f))
            Text(note.uppercase(), style = TenetType.recordLabel, color = colors.inkMuted, textAlign = TextAlign.End)
        }
        HorizontalDivider(thickness = TenetDimens.strokeControl, color = colors.lineStrong)
        Column(
            Modifier.fillMaxWidth().padding(TenetDimens.space4),
            verticalArrangement = Arrangement.spacedBy(TenetDimens.space3),
            content = content,
        )
    }
}

/** G2 and G3: giver → receiver, the camera, the due time, and (G3) the QR inside. */
@Composable
fun HandoverCard(summary: HandoverSummary, modifier: Modifier = Modifier, content: (@Composable ColumnScope.() -> Unit)? = null) {
    val colors = Tenet.colors
    Column(
        modifier
            .fillMaxWidth()
            .border(TenetDimens.strokeControl, colors.lineStrong)
            .background(colors.bg100)
            .padding(TenetDimens.space4),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space3),
    ) {
        Text(summary.kind.uppercase(), style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold), color = colors.signalText)
        PersonBlock("From", summary.giverName, summary.giverRole)
        Icon(Icons.AutoMirrored.Sharp.ArrowForward, contentDescription = null, tint = colors.ink, modifier = Modifier.size(TenetDimens.iconMd))
        PersonBlock("To", summary.receiverName, summary.receiverRole)
        HorizontalDivider(thickness = TenetDimens.strokeHairline, color = colors.line)
        Text(summary.itemLine, style = TenetType.body, color = colors.ink)
        summary.dueMs?.let { Text("Return by ${GiveViewModel.hhmm(it)}".uppercase(), style = TenetType.record.copy(fontWeight = FontWeight.Bold), color = colors.ink) }
        content?.invoke(this)
    }
}

@Composable
private fun PersonBlock(label: String, name: String, role: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
        Text(label.uppercase(), style = TenetType.recordLabel, color = Tenet.colors.inkMuted, modifier = Modifier.width(48.dp))
        Column(Modifier.weight(1f)) {
            Text(name.uppercase(), style = TenetType.label, color = Tenet.colors.ink)
            Text(role.uppercase(), style = TenetType.recordLabel, color = Tenet.colors.inkMuted)
        }
    }
}

/** design/components/Snackbar: inverse, square, with a strip that says the kind, and one optional action. */
@Composable
fun GiveSnackbar(snack: GiveSnack, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: () -> Unit = {}) {
    val colors = Tenet.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 56.dp)
            .background(colors.inverse),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val strip = when (snack) {
            is GiveSnack.Confirmation -> Modifier.width(TenetDimens.stripeWidth).background(colors.inverseOk)
            is GiveSnack.Info -> Modifier.width(TenetDimens.stripeWidth).background(colors.onInverse)
            is GiveSnack.Problem -> Modifier.width(14.dp).hazardStripes(colors.onAlert, colors.alert)
        }
        Box(strip.fillMaxHeight())
        Row(
            Modifier.weight(1f).padding(horizontal = TenetDimens.space3, vertical = TenetDimens.space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2),
        ) {
            if (snack is GiveSnack.Confirmation) {
                Icon(Icons.Sharp.Check, contentDescription = null, tint = colors.inverseOk, modifier = Modifier.size(TenetDimens.iconMd))
            }
            Column(Modifier.weight(1f)) {
                when (snack) {
                    is GiveSnack.Confirmation -> {
                        Text(snack.title.uppercase(), style = TenetType.label, color = colors.onInverse)
                        Text(snack.facts.uppercase(), style = TenetType.recordLabel, color = colors.onInverse)
                    }
                    is GiveSnack.Info -> Text(snack.message, style = TenetType.body, color = colors.onInverse)
                    is GiveSnack.Problem -> {
                        Text(snack.title.uppercase(), style = TenetType.label, color = colors.onInverse)
                        Text(snack.message, style = TenetType.body, color = colors.onInverse)
                    }
                }
            }
        }
        if (actionLabel != null) {
            TextButton(onClick = onAction, shape = RectangleShape, modifier = Modifier.heightIn(min = TenetDimens.targetMin)) {
                Text(actionLabel.uppercase(), style = TenetType.label, color = colors.inverseAction)
            }
        }
    }
}
