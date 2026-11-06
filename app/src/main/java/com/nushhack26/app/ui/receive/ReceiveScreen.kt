package com.nushhack26.app.ui.receive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.FlashlightOff
import androidx.compose.material.icons.sharp.FlashlightOn
import androidx.compose.material.icons.sharp.PersonOff
import androidx.compose.material.icons.sharp.QrCodeScanner
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.components.EmptyState
import com.nushhack26.app.ui.components.ErrorState
import com.nushhack26.app.ui.components.HandoverRole
import com.nushhack26.app.ui.components.HandoverStepper
import com.nushhack26.app.ui.components.LoadingState
import com.nushhack26.app.ui.components.PrimaryButton
import com.nushhack26.app.ui.components.QrPanel
import com.nushhack26.app.ui.components.QrScanner
import com.nushhack26.app.ui.components.SealCheckRow
import com.nushhack26.app.ui.components.SecondaryButton
import com.nushhack26.app.ui.components.SyncChip
import com.nushhack26.app.ui.components.SyncChipState
import com.nushhack26.app.ui.components.TextLinkButton
import com.nushhack26.app.ui.components.rememberSignPrompt
import com.nushhack26.app.auth.AccessRules
import com.nushhack26.app.ui.give.LinkedMark
import com.nushhack26.app.ui.give.TapLine
import com.nushhack26.app.ui.give.rememberTapReady
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DECLINED_CLOSE_MS = 2_500L
private val LINKED_STEPS = setOf(ReceiveStep.Checks, ReceiveStep.ScanSeal, ReceiveStep.Accept, ReceiveStep.Sending)

/**
 * R0–R5. As the RECEIVE tab ([asTab]): [active] is true only while the pager has settled on it,
 * a new [session] starts a fresh receive, and [onFlowChange] says when an offer has arrived (the
 * frame then hides the bottom bar and stops the swipe until [onDone]).
 */
@Composable
fun ReceiveScreen(
    container: AppContainer,
    onDone: () -> Unit,
    relayedOfferId: String? = null,
    asTab: Boolean = false,
    active: Boolean = true,
    session: Int = 0,
    onFlowChange: (Boolean) -> Unit = {},
) {
    val viewModel: ReceiveViewModel = viewModel(
        key = if (asTab) "receive-tab-$session" else null,
        factory = viewModelFactory {
            initializer {
                ReceiveViewModel(
                    container.receiveRepository, container.identity, { container.deviceKeys },
                    relay = relayedOfferId?.let { container.offerRelay }, relayedOfferId = relayedOfferId, appScope = container.scope,
                )
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tapLink = container.tapLink
    val onR1 = state.step == ReceiveStep.ScanOffer && relayedOfferId == null && active
    val tapReady = rememberTapReady(tapLink, active = onR1)
    val inFlow = state.step !in setOf(ReceiveStep.Loading, ReceiveStep.Unavailable, ReceiveStep.ScanOffer)
    LaunchedEffect(inFlow) { onFlowChange(inFlow) }
    tapLink.rememberTapReader(enabled = onR1 && tapReady && !state.rosterEmpty) { payload ->
        viewModel.onTapped(payload.offer) { tapLink.join(payload).asTapSession() }
    }
    DisposableEffect(viewModel) { onDispose { viewModel.closeTap() } }
    val sign = rememberSignPrompt()
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.step) {
        if (state.step == ReceiveStep.Declined) {
            delay(DECLINED_CLOSE_MS)
            onDone()
        }
    }
    ReceiveContent(state.copy(tapReady = tapReady && onR1)) { action ->
        when (action) {
            ReceiveAction.Accept -> scope.launch {
                if (!viewModel.prepareAccept()) return@launch
                val summary = viewModel.state.value.summary
                val result = sign(
                    "Accept ${summary?.itemCode.orEmpty()}",
                    "${summary?.giverShortName.orEmpty()} → you · sealed source ${summary?.sourceSerial.orEmpty()}",
                )
                viewModel.finishAccept(result)
            }
            ReceiveAction.Close -> onDone()
            else -> viewModel.onAction(action)
        }
    }
}

/** Stateless: draws one step of the receiver's flow. */
@Composable
fun ReceiveContent(state: ReceiveUiState, modifier: Modifier = Modifier, onAction: (ReceiveAction) -> Unit = {}) {
    when (state.step) {
        ReceiveStep.Loading -> LoadingState(modifier, message = "Loading roster")
        ReceiveStep.Unavailable -> ErrorState(state.message ?: "Receive isn't available.", modifier)
        else -> StepFrame(state, modifier, onAction)
    }
}

@Composable
private fun StepFrame(state: ReceiveUiState, modifier: Modifier, onAction: (ReceiveAction) -> Unit) {
    val stepNumber = when (state.step) {
        ReceiveStep.ScanOffer, ReceiveStep.Checking, ReceiveStep.Checks -> 1
        ReceiveStep.ScanSeal -> 2
        ReceiveStep.Accept, ReceiveStep.Sending, ReceiveStep.ShowAcceptance -> 3
        else -> 4
    }
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(TenetDimens.space4),
            verticalArrangement = Arrangement.spacedBy(TenetDimens.space4),
        ) {
            if (state.step != ReceiveStep.Declined) HandoverStepper(HandoverRole.Receiver, stepNumber)
            if (state.linked && state.step in LINKED_STEPS) {
                LinkedMark("Linked to ${state.summary?.giverShortName ?: "the giver"}'s phone")
            }
            when (state.step) {
                ReceiveStep.ScanOffer -> ScanOfferBody(state, onAction)
                ReceiveStep.Checking -> Text("Checking offline…", style = TenetType.bodyLg, color = Tenet.colors.ink)
                ReceiveStep.Checks -> ChecksBody(state)
                ReceiveStep.ScanSeal -> ScanSealBody(state, onAction)
                ReceiveStep.Accept -> AcceptBody(state)
                ReceiveStep.Sending -> Text(
                    "Sending to ${state.summary?.giverShortName ?: "the giver"}…",
                    style = TenetType.bodyLg,
                    color = Tenet.colors.ink,
                )
                ReceiveStep.ShowAcceptance -> ShowBody(state)
                ReceiveStep.Done -> DoneBody(state)
                ReceiveStep.Declined -> DeclinedBody(state)
                else -> Unit
            }
            state.message?.let { ErrorLine(it) }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = TenetDimens.space4, vertical = TenetDimens.space2),
            verticalArrangement = Arrangement.spacedBy(TenetDimens.space2),
        ) {
            BottomActions(state, onAction)
        }
    }
}

@Composable
private fun ScanOfferBody(state: ReceiveUiState, onAction: (ReceiveAction) -> Unit) {
    if (state.rosterEmpty) {
        EmptyState(
            title = "No roster yet",
            message = "Sync this phone once, so it can check handover codes offline.",
            icon = Icons.Sharp.PersonOff,
            actionLabel = "Scan anyway",
            onAction = { onAction(ReceiveAction.ScanAnyway) },
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    QrScanner(
        onCode = { onAction(ReceiveAction.OfferScanned(it)) },
        torchOn = state.torchOn,
        hint = "Point at the giver's code",
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.tapReady) TapLine("Or hold your phone to the giver's")
    SecondaryButton(
        if (state.torchOn) "Torch off" else "Torch on",
        onClick = { onAction(ReceiveAction.ToggleTorch) },
        icon = if (state.torchOn) Icons.Sharp.FlashlightOff else Icons.Sharp.FlashlightOn,
    )
}

@Composable
private fun ChecksBody(state: ReceiveUiState) {
    val shown = state.lines.take(state.revealedLines)
    val allShown = shown.size == state.lines.size
    if (allShown && !state.passed) {
        RefusedLabel(state.refusedHeader, state.lines)
    } else {
        Text("Checked offline, on this phone", style = TenetType.bodyLg, color = Tenet.colors.ink)
        CheckList(shown)
        if (!allShown) Text("Checking offline…", style = TenetType.body, color = Tenet.colors.inkMuted)
    }
}

@Composable
private fun ScanSealBody(state: ReceiveUiState, onAction: (ReceiveAction) -> Unit) {
    val code = state.summary?.itemCode.orEmpty()
    val seal = state.seal
    if (seal == null) {
        QrScanner(
            onCode = { onAction(ReceiveAction.SealScanned(it)) },
            torchOn = state.torchOn,
            hint = "Scan the seal on camera $code",
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            if (state.torchOn) "Torch off" else "Torch on",
            onClick = { onAction(ReceiveAction.ToggleTorch) },
            icon = if (state.torchOn) Icons.Sharp.FlashlightOff else Icons.Sharp.FlashlightOn,
        )
    } else {
        SealCheckRow(seal.matches, code, seal.scanned, seal.expected)
        Text(seal.text, style = TenetType.bodyLg, color = if (seal.matches) Tenet.colors.ink else Tenet.colors.alertText)
    }
}

@Composable
private fun AcceptBody(state: ReceiveUiState) {
    val summary = state.summary ?: return
    WhoAndWhatCard(summary)
    LiabilityLine(summary.sourceSerial, summary.isotope, summary.itemCode, state.nowMs)
    if (state.leaseExpired) ErrorLine(AccessRules.LEASE_EXPIRED_TEXT)
    else Text("Place your finger to accept", style = TenetType.body, color = Tenet.colors.inkMuted)
}

@Composable
private fun ShowBody(state: ReceiveUiState) {
    val summary = state.summary ?: return
    val qr = state.acceptanceQr ?: return
    state.fallbackText?.let { Text(it, style = TenetType.bodyLg, color = Tenet.colors.ink) }
    QrPanel(qr, caption = "Acceptance · ${summary.itemCode}", instruction = "Show this to ${summary.giverShortName}")
    state.matchCode?.let { MatchCode(it, summary.giverShortName) }
}

@Composable
private fun DoneBody(state: ReceiveUiState) {
    val summary = state.summary ?: return
    if (state.sentTap) {
        Text("Sent to ${summary.giverShortName}'s phone, no network", style = TenetType.bodyLg, color = Tenet.colors.ink)
        state.matchCode?.let { MatchCode(it, summary.giverShortName) }
        DoneLabel(summary, matchCode = null, header = "Accepted · tapped, no network")
    } else if (state.sentOnline) {
        Text("Sent to ${summary.giverShortName} · ask them to check the match code", style = TenetType.bodyLg, color = Tenet.colors.ink)
        state.matchCode?.let { MatchCode(it, summary.giverShortName) }
        DoneLabel(summary, matchCode = null, header = "Accepted · sent through the server")
    } else {
        DoneLabel(summary, state.matchCode)
    }
    SyncChip(state.recordSync)
    Text(
        if (state.recordSync == SyncChipState.Synced) "Synced" else "Not synced yet",
        style = TenetType.body,
        color = Tenet.colors.inkMuted,
    )
}

@Composable
private fun DeclinedBody(state: ReceiveUiState) {
    state.declinedText?.lines()?.forEachIndexed { i, line ->
        Text(line, style = if (i == 0) TenetType.bodyLg else TenetType.body, color = if (i == 0) Tenet.colors.ink else Tenet.colors.inkMuted)
    }
    Text("Nothing was signed.", style = TenetType.body, color = Tenet.colors.inkMuted)
}

/** Cancel, plus Decline on R2–R4 when the offer came through the server. */
@Composable
private fun LeaveActions(state: ReceiveUiState, onAction: (ReceiveAction) -> Unit) {
    if (!state.relayed) {
        TextLinkButton("Cancel", onClick = { onAction(ReceiveAction.Close) })
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextLinkButton("Cancel", onClick = { onAction(ReceiveAction.Close) })
        when {
            state.declining -> TextLinkButton("Declining…", onClick = {})
            !state.signing -> TextLinkButton("Decline", onClick = { onAction(ReceiveAction.Decline) })
        }
    }
}

@Composable
private fun BottomActions(state: ReceiveUiState, onAction: (ReceiveAction) -> Unit) {
    val code = state.summary?.itemCode.orEmpty()
    when (state.step) {
        ReceiveStep.ScanOffer, ReceiveStep.Checking -> TextLinkButton("Cancel", onClick = { onAction(ReceiveAction.Close) })
        ReceiveStep.Checks -> {
            val allShown = state.revealedLines >= state.lines.size
            if (allShown && state.passed) {
                PrimaryButton("Scan seal", onClick = { onAction(ReceiveAction.ScanSeal) }, icon = Icons.Sharp.QrCodeScanner)
                LeaveActions(state, onAction)
            } else if (allShown && state.relayed) {
                TextLinkButton("Close", onClick = { onAction(ReceiveAction.Close) })
            } else if (allShown) {
                SecondaryButton("Scan another code", onClick = { onAction(ReceiveAction.ScanAgain) })
                TextLinkButton("Close", onClick = { onAction(ReceiveAction.Close) })
            }
        }
        ReceiveStep.ScanSeal -> {
            val seal = state.seal
            when {
                seal == null -> PrimaryButton("Sign and accept $code", onClick = {}, enabled = false, disabledReason = "Scan seal first")
                seal.matches -> PrimaryButton("Next", onClick = { onAction(ReceiveAction.Next) })
                else -> PrimaryButton("Sign and accept $code", onClick = {}, enabled = false, disabledReason = "Seal doesn't match")
            }
            LeaveActions(state, onAction)
        }
        ReceiveStep.Accept -> {
            when {
                state.leaseExpired -> PrimaryButton("Sign and accept $code", onClick = {}, enabled = false, disabledReason = "Connect once")
                state.signing -> PrimaryButton("Sign and accept $code", onClick = {}, enabled = false, disabledReason = "Signing")
                else -> PrimaryButton("Sign and accept $code", onClick = { onAction(ReceiveAction.Accept) })
            }
            LeaveActions(state, onAction)
        }
        ReceiveStep.ShowAcceptance -> PrimaryButton("Done", onClick = { onAction(ReceiveAction.Done) })
        ReceiveStep.Done, ReceiveStep.Declined -> PrimaryButton("Close", onClick = { onAction(ReceiveAction.Close) })
        else -> Unit
    }
}
