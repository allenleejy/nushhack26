package com.nushhack26.app.ui.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Fingerprint
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nushhack26.app.AppContainer
import com.nushhack26.app.auth.AccessLease
import com.nushhack26.app.auth.UnlockResult
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.ui.components.DiamondVariant
import com.nushhack26.app.ui.components.HazardDiamond
import com.nushhack26.app.ui.components.PrimaryButton
import com.nushhack26.app.ui.components.SignResult
import com.nushhack26.app.ui.components.rememberSignPrompt
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class LockUiState(
    val name: String,
    /** Null until the roster has synced (for example right after "Clear local data"). */
    val staffNo: String? = null,
    val lease: AccessLease? = null,
    val error: String? = null,
    val unlocking: Boolean = false,
)

sealed interface LockAction {
    data object Unlock : LockAction
}

/**
 * L1 Lock (docs/spec.md section 3, docs/sync.md A.7). Drawn over the whole app while it's
 * locked: opaque, eats every touch, and Back does nothing. UNLOCK is the only way out: the
 * fingerprint prompt, then [com.nushhack26.app.auth.Unlocker.verify] checks a freshly signed
 * challenge against the enrolled key, with no network.
 */
@Composable
fun LockScreen(container: AppContainer, identity: Identity) {
    val sign = rememberSignPrompt()
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var unlocking by remember { mutableStateOf(false) }
    val lease by container.identity.lease.collectAsStateWithLifecycle(initialValue = null)
    val staffNo by remember(identity.personId) {
        container.database.peopleDao().observeAll().map { people -> people.firstOrNull { it.id == identity.personId }?.staffNo }
    }.collectAsStateWithLifecycle(initialValue = null)

    BackHandler(enabled = true) {}
    LockContent(
        state = LockUiState(identity.name, staffNo, lease, error, unlocking),
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) { action ->
        when (action) {
            LockAction.Unlock -> if (!unlocking) {
                unlocking = true
                scope.launch {
                    try {
                        error = when (sign("Unlock Tenet", identity.name)) {
                            is SignResult.Cancelled -> "Fingerprint cancelled. Tenet stays locked."
                            SignResult.Approved -> when (val unlock = container.unlocker.verify()) {
                                UnlockResult.Unlocked -> null
                                is UnlockResult.Refused -> unlock.message
                            }
                        }
                    } finally {
                        unlocking = false
                    }
                }
            }
        }
    }
}

@Composable
fun LockContent(state: LockUiState, modifier: Modifier = Modifier, onAction: (LockAction) -> Unit = {}) {
    val colors = Tenet.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg000)
            .safeDrawingPadding()
            .padding(TenetDimens.space4)
            .testTag("lock_screen"),
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TenetDimens.space4, Alignment.CenterVertically),
        ) {
            HazardDiamond(variant = DiamondVariant.Filled, size = TenetDimens.diamondLg)
            Spacer(Modifier.height(TenetDimens.space2))
            Text("LOCKED", style = TenetType.headline, color = colors.ink)
            Text(
                listOfNotNull(state.name, state.staffNo).joinToString(" · ").uppercase(),
                style = TenetType.label,
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("lock_name"),
            )
            Text(
                lockAccessLine(state.lease),
                style = TenetType.body,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("lock_access"),
            )
            if (state.lease?.expired == true) LeaseExpiredBanner()
        }
        Column(verticalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
            state.error?.let {
                Text(it, style = TenetType.body, color = colors.alertText, modifier = Modifier.fillMaxWidth().testTag("lock_error"))
            }
            Text(
                "Place your finger to unlock",
                style = TenetType.body,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                label = if (state.unlocking) "Unlocking" else "Unlock",
                icon = Icons.Sharp.Fingerprint,
                onClick = { onAction(LockAction.Unlock) },
                modifier = Modifier.testTag("unlock"),
            )
        }
    }
}
