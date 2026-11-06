package com.nushhack26.app.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Bluetooth
import androidx.compose.material.icons.sharp.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.nushhack26.app.nearby.TapAvailability
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType

/**
 * In place of the tap target when tap can't work yet (docs/spec.md section 3, G3 and R1): what's
 * missing and one button to fix it. The QR button below it stays the way on. [onResume] runs when
 * the person comes back from Settings, so the caller can call `tapLink.refresh()`.
 */
@Composable
fun TapOffPanel(availability: TapAvailability, modifier: Modifier = Modifier, onResume: () -> Unit = {}) {
    if (availability == TapAvailability.Ready) return
    val colors = Tenet.colors
    val context = LocalContext.current
    LifecycleResumeEffect(availability) {
        onResume()
        onPauseOrDispose { }
    }
    val (title, text, action) = when (availability) {
        TapAvailability.NfcOff -> Triple("NFC is off", "Turn on NFC to hand over by tap.", "Turn on NFC" to Settings.ACTION_NFC_SETTINGS)
        TapAvailability.BluetoothOff -> Triple(
            "Bluetooth is off", "Tap uses Bluetooth to link the two phones, even in airplane mode.",
            "Turn on Bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
        )
        TapAvailability.NeedsPermission -> Triple(
            "Nearby devices not allowed", "Allow nearby devices so this phone can find the other one.",
            "Open settings" to Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        )
        else -> Triple("No NFC on this phone", "Use the QR code instead.", null)
    }
    Column(
        modifier.fillMaxWidth().background(colors.bg100).border(TenetDimens.strokeControl, colors.lineStrong).padding(TenetDimens.space4),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space3),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (availability == TapAvailability.BluetoothOff) Icons.Sharp.Bluetooth else Icons.Sharp.Nfc,
                contentDescription = null, tint = colors.ink, modifier = Modifier.size(TenetDimens.iconLg),
            )
            Text(title.uppercase(), style = TenetType.title, color = colors.ink)
        }
        Text(text, style = TenetType.body, color = colors.ink)
        action?.let { (label, settingsAction) ->
            SecondaryButton(label, onClick = {
                val intent = Intent(settingsAction)
                if (settingsAction == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) intent.data = Uri.fromParts("package", context.packageName, null)
                context.startActivity(intent)
            })
        }
    }
}
