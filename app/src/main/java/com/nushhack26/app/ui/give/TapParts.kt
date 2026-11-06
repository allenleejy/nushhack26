package com.nushhack26.app.ui.give

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nushhack26.app.nearby.TapAvailability
import com.nushhack26.app.nearby.TapLink
import com.nushhack26.app.ui.theme.ChipCutShape
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType

/** Asked at most once per app run, so a refusal just hides the tap lines (docs/sync.md A.11). */
@Volatile private var askedTapPermissions = false

/**
 * Whether G3 or R1 can offer the tap. The first time one opens ([active]) without the Nearby
 * permissions, asks for them once.
 */
@Composable
fun rememberTapReady(tapLink: TapLink, active: Boolean): Boolean {
    val availability by tapLink.available.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tapLink.refresh() }
    LaunchedEffect(active, availability) {
        if (active && availability == TapAvailability.NeedsPermission && !askedTapPermissions) {
            askedTapPermissions = true
            launcher.launch(tapLink.requiredPermissions())
        }
    }
    return availability == TapAvailability.Ready
}

/** The tap line: a ListRow-style row with the NFC mark, under the QR panel or the viewfinder. */
@Composable
fun TapLine(text: String, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = TenetDimens.targetRow)
            .background(colors.bg100)
            .border(TenetDimens.strokeHairline, colors.line)
            .padding(horizontal = TenetDimens.space4, vertical = TenetDimens.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
    ) {
        Icon(Icons.Sharp.Nfc, contentDescription = null, tint = colors.ink, modifier = Modifier.size(TenetDimens.iconLg))
        Text(text, style = TenetType.body, color = colors.ink, modifier = Modifier.weight(1f))
    }
}

/** "Linked to Siti's phone", in SyncChip's synced style. */
@Composable
fun LinkedMark(text: String, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Row(
        modifier
            .heightIn(min = 32.dp)
            .clip(ChipCutShape)
            .background(colors.okBg, ChipCutShape)
            .padding(horizontal = TenetDimens.space2, vertical = TenetDimens.space1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TenetDimens.space1),
    ) {
        Icon(Icons.Sharp.Nfc, contentDescription = null, tint = colors.ok, modifier = Modifier.size(18.dp))
        Text(text.uppercase(), style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold), color = colors.ok)
    }
}
