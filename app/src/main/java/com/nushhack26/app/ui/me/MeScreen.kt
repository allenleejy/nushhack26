package com.nushhack26.app.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Lock
import androidx.compose.material.icons.sharp.PersonOff
import androidx.compose.material.icons.sharp.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.auth.AccessRules
import com.nushhack26.app.crypto.KeyInfo
import com.nushhack26.app.ui.components.EmptyState
import com.nushhack26.app.ui.components.LoadingState
import com.nushhack26.app.ui.components.PrimaryButton
import com.nushhack26.app.ui.components.SecondaryButton
import com.nushhack26.app.ui.components.syncSummary
import com.nushhack26.app.ui.lock.LeaseExpiredBanner
import com.nushhack26.app.ui.lock.meAccessLine
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType

sealed interface MeAction {
    data object VersionTapped : MeAction
    data object SyncNow : MeAction
    data object LockNow : MeAction
}

@Composable
fun MeScreen(container: AppContainer, onOpenDebug: () -> Unit) {
    val viewModel: MeViewModel = viewModel(factory = viewModelFactory { initializer { MeViewModel(container) } })
    val state by viewModel.state.collectAsStateWithLifecycle()
    var taps by remember { mutableIntStateOf(0) }
    MeContent(state) { action ->
        when (action) {
            MeAction.VersionTapped -> {
                taps++
                if (taps >= 5) {
                    taps = 0
                    onOpenDebug()
                }
            }
            MeAction.SyncNow -> container.syncScheduler.syncNow()
            MeAction.LockNow -> container.appLock.lock()
        }
    }
}

@Composable
fun MeContent(state: MeUiState, modifier: Modifier = Modifier, onAction: (MeAction) -> Unit = {}) {
    val colors = Tenet.colors
    if (state.loading) {
        LoadingState(modifier.fillMaxSize())
        return
    }
    val identity = state.identity
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg000)
            .verticalScroll(rememberScrollState())
            .padding(TenetDimens.space4),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space6),
    ) {
        if (identity == null) {
            EmptyState(
                title = "Not enrolled",
                message = "This phone isn't linked to a person yet. Enrol it with your staff number and code.",
                icon = Icons.Sharp.PersonOff,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(TenetDimens.space1)) {
                Text(identity.name.uppercase(), style = TenetType.headline, color = colors.ink, modifier = Modifier.testTag("me_name"))
                Text(
                    listOfNotNull(AccessRules.roleName(identity.role), state.person?.staffNo).joinToString(" · ").uppercase(),
                    style = TenetType.record,
                    color = colors.inkMuted,
                    modifier = Modifier.testTag("me_role"),
                )
            }
            if (state.lease?.expired == true) LeaseExpiredBanner()
            MeCard("Access") {
                Text(authorisationLine(state), style = TenetType.body, color = authorisationColor(state), modifier = Modifier.testTag("me_authorised"))
                Text(meAccessLine(state.lease).replaceFirstChar(Char::uppercase), style = TenetType.body, color = colors.inkMuted, modifier = Modifier.testTag("me_lease"))
                if (state.lockApplies) {
                    SecondaryButton("Lock now", icon = Icons.Sharp.Lock, onClick = { onAction(MeAction.LockNow) }, modifier = Modifier.testTag("lock_now"))
                }
            }
            MeCard("Device key") {
                Text(keyLine(state.key), style = TenetType.body, color = colors.ink, modifier = Modifier.testTag("me_key"))
            }
        }
        MeCard("Sync") {
            Text(syncSummary(state.sync).uppercase(), style = TenetType.recordLabel, color = colors.ink, modifier = Modifier.testTag("me_sync"))
            if (!state.serverConfigured) {
                Text("Server not configured", style = TenetType.body, color = colors.alertText)
            } else if (state.userId == null) {
                Text("Will sign in when online", style = TenetType.body, color = colors.inkMuted, modifier = Modifier.testTag("user_id"))
            }
            state.sync.lastError?.let { Text(it, style = TenetType.body, color = colors.alertText, modifier = Modifier.testTag("me_sync_error")) }
            PrimaryButton("Sync now", icon = Icons.Sharp.Sync, onClick = { onAction(MeAction.SyncNow) }, modifier = Modifier.testTag("sync_now"))
        }
        Text(
            text = listOfNotNull("Release ${state.version}", if (state.debugBuild) "debug build" else null).joinToString(" · ").uppercase(),
            style = TenetType.recordLabel,
            color = colors.inkMuted,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TenetDimens.targetMin)
                .clickable { onAction(MeAction.VersionTapped) }
                .padding(vertical = TenetDimens.space4)
                .testTag("version"),
        )
    }
}

/** "Authorised radiographer · valid to 30 Jun 2027", or "· no expiry", or the lapse. */
private fun authorisationLine(state: MeUiState): String {
    val role = AccessRules.roleName(state.identity?.role.orEmpty())
    val person = state.person ?: return "Authorised $role · details appear after the next sync"
    if (!person.active) return "${person.name} is no longer on the active roster"
    val until = person.authorisedUntilMs ?: return "Authorised $role · no expiry"
    if (until < state.now) return "Authorisation lapsed ${AccessRules.longDate(until)}"
    return "Authorised $role · valid to ${AccessRules.longDate(until)}"
}

@Composable
private fun authorisationColor(state: MeUiState) = state.person.let { person ->
    val lapsed = person != null && (!person.active || (person.authorisedUntilMs ?: Long.MAX_VALUE) < state.now)
    if (lapsed) Tenet.colors.alertText else Tenet.colors.ink
}

/** "Device key 3F:A9:12:7C · hardware-backed (TEE)", or the labelled test key. */
private fun keyLine(key: KeyInfo?): String = when (key?.security) {
    null -> "No signing key on this phone"
    "software" -> "Unprotected test key · ${key.keyFp}"
    "strongbox" -> "Device key ${key.keyFp} · hardware-backed (StrongBox)"
    else -> "Device key ${key.keyFp} · hardware-backed (TEE)"
}

/** A label panel: 2dp outline, a header bar, then the content. */
@Composable
private fun MeCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = Tenet.colors
    Column(Modifier.fillMaxWidth().background(colors.bg100).border(TenetDimens.strokeControl, colors.lineStrong, RectangleShape)) {
        Text(
            title.uppercase(),
            style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold),
            color = colors.ink,
            modifier = Modifier.fillMaxWidth().background(colors.bg200).padding(horizontal = TenetDimens.space4, vertical = TenetDimens.space2),
        )
        HorizontalDivider(thickness = TenetDimens.strokeControl, color = colors.lineStrong)
        Column(Modifier.padding(TenetDimens.space4), verticalArrangement = Arrangement.spacedBy(TenetDimens.space3), content = content)
    }
}
