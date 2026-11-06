package com.nushhack26.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetAngles
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.hazardStripes

/** One boxed cell on the parcel label: "FROM" → "Ravi Kumar". */
data class ParcelField(val label: String, val value: String)

/**
 * The Done screen on both phones (docs/spec.md section 3, "Done: the parcel label"): warning
 * tape, the camera's name and code, boxed cells, and the stamp ([stampWord] / [stampTime] /
 * [stampLine], for example RECEIVED / 13:02 / FROM RAVI) dropped over the middle.
 *
 * STUB from the shared prep: card 22 draws the real label and the drop, thud and wave.
 * This version draws it still, with the stamp in place.
 */
@Composable
fun ParcelLabel(
    itemName: String,
    itemCode: String,
    fields: List<ParcelField>,
    stampWord: String,
    stampTime: String,
    stampLine: String,
    modifier: Modifier = Modifier,
) {
    val colors = Tenet.colors
    Column(modifier.fillMaxWidth().border(TenetDimens.strokeControl, colors.lineStrong).background(colors.bg100)) {
        Box(Modifier.fillMaxWidth().height(TenetDimens.stripeBand).hazardStripes(colors.onSignal, colors.signal))
        Column(Modifier.padding(TenetDimens.space4), verticalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
            Text(itemName.ifBlank { itemCode }.uppercase(), style = TenetType.headline, color = colors.ink)
            Text("CAMERA $itemCode", style = TenetType.record, color = colors.inkMuted)
            fields.forEach { field ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(field.label.uppercase(), style = TenetType.recordLabel, color = colors.inkMuted)
                    Text(field.value, style = TenetType.record, color = colors.ink)
                }
            }
            Box(Modifier.fillMaxWidth().padding(vertical = TenetDimens.space4), contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .rotate(TenetAngles.angleStamp)
                        .border(TenetDimens.strokeStamp, colors.ok)
                        .padding(horizontal = TenetDimens.space4, vertical = TenetDimens.space2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    listOf(stampWord, stampTime, stampLine).forEach {
                        Text(it.uppercase(), style = TenetType.label, color = colors.ok)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(TenetDimens.stripeBand).hazardStripes(colors.onSignal, colors.signal))
    }
}
