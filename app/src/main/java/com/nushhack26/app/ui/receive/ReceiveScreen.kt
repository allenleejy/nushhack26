package com.nushhack26.app.ui.receive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import kotlinx.coroutines.launch

@Composable
fun ReceiveScreen(container: AppContainer, onDone: () -> Unit, relayedOfferId: String? = null) {
    val viewModel: ReceiveViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ReceiveViewModel(container.receiveRepository, container.identity, { container.deviceKeys }) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sign = rememberSignPrompt()
    val scope = rememberCoroutineScope()
    ReceiveContent(state) { action ->
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
        ReceiveStep.Accept, ReceiveStep.ShowAcceptance -> 3
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
            HandoverStepper(HandoverRole.Receiver, stepNumber)
            when (state.step) {
                ReceiveStep.ScanOffer -> ScanOfferBody(state, onAction)
                ReceiveStep.Checking -> Text("Checking offline…", style = TenetType.bodyLg, color = Tenet.colors.ink)
                ReceiveStep.Checks -> ChecksBody(state)
                ReceiveStep.ScanSeal -> ScanSealBody(state, onAction)
                ReceiveStep.Accept -> AcceptBody(state)
                ReceiveStep.ShowAcceptance -> ShowBody(state)
                ReceiveStep.Done -> DoneBody(state)
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
    QrPanel(qr, caption = "Acceptance · ${summary.itemCode}", instruction = "Show this to ${summary.giverShortName}")
    state.matchCode?.let { MatchCode(it, summary.giverShortName) }
}

@Composable
private fun DoneBody(state: ReceiveUiState) {
    val summary = state.summary ?: return
    DoneLabel(summary, state.matchCode)
    SyncChip(state.recordSync)
    Text(
        if (state.recordSync == SyncChipState.Synced) "Synced" else "Not synced yet",
        style = TenetType.body,
        color = Tenet.colors.inkMuted,
    )
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
                TextLinkButton("Cancel", onClick = { onAction(ReceiveAction.Close) })
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
            TextLinkButton("Cancel", onClick = { onAction(ReceiveAction.Close) })
        }
        ReceiveStep.Accept -> {
            when {
                state.leaseExpired -> PrimaryButton("Sign and accept $code", onClick = {}, enabled = false, disabledReason = "Connect once")
                state.signing -> PrimaryButton("Sign and accept $code", onClick = {}, enabled = false, disabledReason = "Signing")
                else -> PrimaryButton("Sign and accept $code", onClick = { onAction(ReceiveAction.Accept) })
            }
            TextLinkButton("Cancel", onClick = { onAction(ReceiveAction.Close) })
        }
        ReceiveStep.ShowAcceptance -> PrimaryButton("Done", onClick = { onAction(ReceiveAction.Done) })
        ReceiveStep.Done -> PrimaryButton("Close", onClick = { onAction(ReceiveAction.Close) })
        else -> Unit
    }
}
