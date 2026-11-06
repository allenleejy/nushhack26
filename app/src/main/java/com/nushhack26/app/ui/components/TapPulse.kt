package com.nushhack26.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Contactless
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType

/** Where the tap stands, on either phone (card 20). */
enum class TapPhase {
    /** Waiting for the other phone: the core breathes and square rings ripple out. */
    Waiting,
    /** The phones touched: punch and click, then flood the screen and call `onFlooded`. */
    Contact,
    /** The giver after the flood: a calm cyan pulse while the receiver scans the seal and signs. */
    Linked,
}

/**
 * The tap target at the centre of G3 and R1 (docs/spec.md section 3, "The tap moment").
 * [title] is uppercase mono ("HOLD TO WEI MING'S PHONE"); [subtitle] is a sentence.
 *
 * STUB from the shared prep: card 20 draws the real pulse, the punch, the haptics and the flood.
 * This version draws a still square and calls [onFlooded] at once when [phase] becomes Contact.
 */
@Composable
fun TapPulse(
    phase: TapPhase,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onFlooded: () -> Unit = {},
) {
    val colors = Tenet.colors
    val flooded = rememberUpdatedState(onFlooded)
    LaunchedEffect(phase) { if (phase == TapPhase.Contact) flooded.value() }
    Column(
        modifier.fillMaxWidth().padding(vertical = TenetDimens.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space4),
    ) {
        Box(
            Modifier.size(160.dp).background(if (phase == TapPhase.Linked) colors.ok else colors.signal),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Sharp.Contactless, contentDescription = null, tint = colors.onSignal, modifier = Modifier.size(48.dp))
        }
        Text(title.uppercase(), style = TenetType.title, color = colors.ink, textAlign = TextAlign.Center)
        subtitle?.let { Text(it, style = TenetType.body, color = colors.inkMuted, textAlign = TextAlign.Center) }
    }
}
