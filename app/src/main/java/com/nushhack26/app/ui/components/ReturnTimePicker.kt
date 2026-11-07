package com.nushhack26.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetType
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

object ReturnTimeDefaults {
    /** Where the wheels start the first time: two hours from now, rounded up to the next quarter hour. */
    fun initial(nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): LocalTime {
        val t = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalTime().plusHours(2).withSecond(0).withNano(0)
        val up = (15 - t.minute % 15) % 15
        return t.plusMinutes(up.toLong())
    }
}

/**
 * The return time picker (G1, issue only): two drum wheels, hours and minutes, 24-hour
 * (card 31 fills this in). [onConfirm] gets the chosen clock time; a time already passed
 * today means tomorrow (`DueChoice.At`). This stub confirms [initial] straight away.
 */
@Composable
fun ReturnTimePicker(
    initial: LocalTime,
    nowMs: Long,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Tenet.colors
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.background(colors.bg100).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("RETURN BY", style = TenetType.label, color = colors.ink)
            Text("%02d:%02d".format(initial.hour, initial.minute), style = TenetType.headline, color = colors.ink)
            PrimaryButton("Set return time", onClick = { onConfirm(initial) })
            TextLinkButton("Cancel", onClick = onDismiss)
        }
    }
}
