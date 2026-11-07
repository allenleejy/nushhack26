package com.nushhack26.app.ui.receive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.PersonOff
import androidx.compose.material.icons.sharp.QrCodeScanner
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.components.EmptyState
import com.nushhack26.app.ui.components.ErrorState
import com.nushhack26.app.nearby.TapAvailability
import com.nushhack26.app.ui.components.HandoverStepper
import com.nushhack26.app.ui.components.HandoverSteps
import com.nushhack26.app.ui.components.ParcelField
import com.nushhack26.app.ui.components.ParcelLabel
import com.nushhack26.app.ui.components.TapOffPanel
import com.nushhack26.app.ui.components.TapPhase
import com.nushhack26.app.ui.components.TapPulse
import com.nushhack26.app.ui.components.TapRole
import com.nushhack26.app.ui.components.formatTime
import com.nushhack26.app.ui.components.rememberHaptics
import com.nushhack26.app.ui.theme.LocalReduceMotion
import com.nushhack26.app.ui.theme.TenetMotion
import androidx.compose.ui.unit.IntOffset
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
    val availability by tapLink.available.collectAsStateWithLifecycle()
    val onR1 = state.step == ReceiveStep.ScanOffer && relayedOfferId == null && active
    val tapReady = rememberTapReady(tapLink, active = onR1)
    val inFlow = state.step !in setOf(ReceiveStep.Loading, ReceiveStep.Unavailable, ReceiveStep.ScanOffer)
    LaunchedEffect(inFlow) { onFlowChange(inFlow) }
    // The phones touched: R1 holds the pulse until the flood covers the screen, then the checks show.
    var tapContact by remember(viewModel) { mutableStateOf(false) }
    tapLink.rememberTapReader(enabled = onR1 && tapReady && !state.rosterEmpty) { payload ->
        if (!viewModel.state.value.qrMode) tapContact = true
        viewModel.onTapped(payload.offer) { tapLink.join(payload).asTapSession() }
    }
    ReceiveHaptics(state)
    DisposableEffect(viewModel) { onDispose { viewModel.closeTap() } }
    val sign = rememberSignPrompt()
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.step) {
        if (state.step == ReceiveStep.Declined) {
            delay(DECLINED_CLOSE_MS)
            onDone()
        }
    }
    val shown = state.copy(
        tapReady = tapReady && onR1, tapAvailability = availability, asTab = asTab, cameraOn = active, tapContact = tapContact,
    )
    ReceiveContent(shown) { action ->
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
            ReceiveAction.Flooded -> tapContact = false
            ReceiveAction.RefreshTap -> tapLink.refresh()
            else -> viewModel.onAction(action)
        }
    }
}

/** Ticks as each check line appears, rejects a refusal or a wrong seal, confirms a matching seal. Fires with animations off too. */
@Composable
private fun ReceiveHaptics(state: ReceiveUiState) {
    val haptics = rememberHaptics()
    LaunchedEffect(state.revealedLines) {
        if (state.step == ReceiveStep.Checks && state.revealedLines in 1..state.lines.size) haptics.tick()
    }
    val refused = state.step == ReceiveStep.Checks && state.lines.isNotEmpty() && state.revealedLines >= state.lines.size && !state.passed
    LaunchedEffect(refused) { if (refused) haptics.reject() }
    LaunchedEffect(state.sealRejects) { if (state.sealRejects > 0) haptics.reject() }
    val sealMatches = state.step == ReceiveStep.ScanSeal && state.seal?.matches == true
    LaunchedEffect(sealMatches) { if (sealMatches) haptics.confirm() }
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

/** Where a step sits in the flow, so a step change slides the right way. One page per step the person sees. */
private fun page(step: ReceiveStep): Int = when (step) {
    ReceiveStep.ScanOffer -> 0
    ReceiveStep.Checking, ReceiveStep.Checks -> 1
    ReceiveStep.ScanSeal -> 2
    ReceiveStep.Accept -> 3
    ReceiveStep.Sending -> 4
    ReceiveStep.ShowAcceptance -> 5
    ReceiveStep.Done -> 6
    ReceiveStep.Declined -> 7
    else -> -1
}

private fun stepNumber(step: ReceiveStep): Int = when (step) {
    ReceiveStep.ScanOffer, ReceiveStep.Checking, ReceiveStep.Checks -> 1
    ReceiveStep.ScanSeal -> 2
    ReceiveStep.Accept, ReceiveStep.Sending, ReceiveStep.ShowAcceptance -> 3
    else -> 4
}

@Composable
private fun StepFrame(state: ReceiveUiState, modifier: Modifier, onAction: (ReceiveAction) -> Unit) {
    // Until the flood covers the screen, a tap still shows R1's pulse.
    val shown = if (state.tapContact) state.copy(step = ReceiveStep.ScanOffer, message = null) else state
    val steps = when {
        shown.relayed -> HandoverSteps.receiverOnline
        shown.scanningQr -> HandoverSteps.receiverQr
        else -> HandoverSteps.receiverTap
    }
    val reduce = LocalReduceMotion.current
    Column(modifier.fillMaxSize()) {
        if (shown.step != ReceiveStep.Declined) {
            HandoverStepper(
                steps,
                stepNumber(shown.step),
                Modifier.padding(start = TenetDimens.space4, end = TenetDimens.space4, top = TenetDimens.space4),
            )
        }
        AnimatedContent(
            targetState = shown,
            contentKey = { page(it.step) },
            transitionSpec = {
                val from = page(initialState.step)
                val to = page(targetState.step)
                // The flood already revealed the checks, so they don't slide in under it as well.
                if (reduce || initialState.tapContact || from == to) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    val forward = if (to > from) 1 else -1
                    val spec = tween<IntOffset>(TenetMotion.STEP_MS, easing = TenetMotion.Emphasized)
                    slideInHorizontally(spec) { it * forward } togetherWith slideOutHorizontally(spec) { -it * forward }
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = "receive step",
        ) { pageState ->
            StepPage(pageState, onAction)
        }
    }
}

@Composable
private fun StepPage(state: ReceiveUiState, onAction: (ReceiveAction) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(TenetDimens.space4),
            verticalArrangement = Arrangement.spacedBy(TenetDimens.space4),
        ) {
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

/** The scanner, or its empty frame while the tab isn't settled, so no camera runs mid-swipe. */
@Composable
private fun Scanner(
    state: ReceiveUiState,
    title: String,
    hint: String,
    rejectCount: Int,
    onCode: (String) -> Unit,
    onAction: (ReceiveAction) -> Unit,
) {
    if (!state.cameraOn) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Tenet.colors.bg200))
        return
    }
    QrScanner(
        onCode = onCode,
        torchOn = state.torchOn,
        hint = hint,
        title = title,
        onToggleTorch = { onAction(ReceiveAction.ToggleTorch) },
        rejectCount = rejectCount,
        modifier = Modifier.fillMaxWidth(),
    )
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
    when {
        state.scanningQr && !state.tapContact -> {
            Scanner(
                state, title = "Scanner · offer", hint = "Point at the giver's code", rejectCount = state.offerRejects,
                onCode = { onAction(ReceiveAction.OfferScanned(it)) }, onAction = onAction,
            )
            if (state.tapReady) TapLine("Or hold your phone to the giver's")
        }
        state.tapContact || state.tapAvailability == TapAvailability.Ready -> TapPulse(
            phase = if (state.tapContact) TapPhase.Contact else TapPhase.Waiting,
            title = "HOLD TO THE GIVER'S PHONE",
            subtitle = "Back to back, until both buzz.",
            onFlooded = { onAction(ReceiveAction.Flooded) },
            role = TapRole.Receiver,
        )
        else -> TapOffPanel(state.tapAvailability, onResume = { onAction(ReceiveAction.RefreshTap) })
    }
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
    val summary = state.summary
    val code = summary?.itemCode.orEmpty()
    val seal = state.seal
    if (seal != null) {
        SealCheckRow(seal.matches, code, seal.scanned, seal.expected)
        Text(seal.text, style = TenetType.bodyLg, color = if (seal.matches) Tenet.colors.ink else Tenet.colors.alertText)
    }
    // A wrong seal keeps the scanner up: it flashes orange, and the right seal can still be scanned.
    if (seal?.matches != true) {
        Scanner(
            state,
            title = "Scanner · seal",
            hint = "Scan the seal on ${summary?.let(::cameraInSentence) ?: "the camera"}",
            rejectCount = state.sealRejects,
            onCode = { onAction(ReceiveAction.SealScanned(it)) },
            onAction = onAction,
        )
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
    state.verifyCode?.let { VerifyLine(it) }
}

/** Every path ends here: the parcel label stamped RECEIVED, the sync line, then the small verify code. */
@Composable
private fun DoneBody(state: ReceiveUiState) {
    val summary = state.summary ?: return
    ParcelLabel(
        itemName = summary.itemName,
        itemCode = summary.itemCode,
        fields = listOf(
            ParcelField("From", summary.giverName),
            ParcelField("To", summary.receiverName),
            ParcelField("Seal", summary.sealNo),
            ParcelField("Return by", summary.dueMs?.let(::formatTime) ?: "No return time"),
        ),
        stampWord = "Received",
        stampTime = formatTime(state.signedAtMs ?: state.nowMs),
        stampLine = "From ${summary.giverShortName}",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3), verticalAlignment = Alignment.CenterVertically) {
        SyncChip(state.recordSync)
        Text(
            if (state.recordSync == SyncChipState.Synced) "Synced" else "Not synced yet",
            style = TenetType.body,
            color = Tenet.colors.inkMuted,
        )
    }
    state.verifyCode?.let { VerifyLine(it) }
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
    if (state.tapContact) return
    val code = state.summary?.itemCode.orEmpty()
    when (state.step) {
        ReceiveStep.ScanOffer -> {
            if (!state.rosterEmpty) {
                when {
                    !state.scanningQr -> SecondaryButton(
                        "Scan a QR code instead",
                        onClick = { onAction(ReceiveAction.UseQr) },
                        icon = Icons.Sharp.QrCodeScanner,
                    )
                    state.tapAvailability != TapAvailability.NoNfc -> TextLinkButton("Use tap instead", onClick = { onAction(ReceiveAction.UseTap) })
                }
            }
            if (!state.asTab) TextLinkButton("Cancel", onClick = { onAction(ReceiveAction.Close) })
        }
        ReceiveStep.Checking -> TextLinkButton("Cancel", onClick = { onAction(ReceiveAction.Close) })
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
        ReceiveStep.Done -> PrimaryButton("Done", onClick = { onAction(ReceiveAction.Close) })
        ReceiveStep.Declined -> PrimaryButton("Close", onClick = { onAction(ReceiveAction.Close) })
        else -> Unit
    }
}
