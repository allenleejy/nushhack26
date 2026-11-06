package com.nushhack26.app.ui.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nushhack26.app.AppContainer
import com.nushhack26.app.auth.UnlockResult
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.ui.components.PrimaryButton
import com.nushhack26.app.ui.components.SignResult
import com.nushhack26.app.ui.components.rememberSignPrompt
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import kotlinx.coroutines.launch

/**
 * L1 Lock (docs/spec.md section 3). Placeholder: it already unlocks for real (fingerprint,
 * then the signed challenge in docs/sync.md A.7), and card 07 designs the screen.
 * Drawn over the whole app: opaque, eats every touch, and Back does nothing.
 */
@Composable
fun LockScreen(container: AppContainer, identity: Identity) {
    val sign = rememberSignPrompt()
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = true) {}
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Tenet.colors.bg000)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .safeDrawingPadding()
            .padding(TenetDimens.space4),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space4, androidx.compose.ui.Alignment.CenterVertically),
    ) {
        Text("LOCKED", style = TenetType.headline, color = Tenet.colors.ink)
        Text(identity.name.uppercase(), style = TenetType.label, color = Tenet.colors.ink)
        error?.let { Text(it, style = TenetType.body, color = Tenet.colors.alertText) }
        PrimaryButton(
            label = "UNLOCK",
            onClick = {
                scope.launch {
                    when (val result = sign("Unlock Tenet", identity.name)) {
                        is SignResult.Cancelled -> error = "Fingerprint cancelled. Tenet stays locked."
                        SignResult.Approved -> when (val unlock = container.unlocker.verify()) {
                            UnlockResult.Unlocked -> error = null
                            is UnlockResult.Refused -> error = unlock.message
                        }
                    }
                }
            },
        )
    }
}
