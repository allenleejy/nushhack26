package com.nushhack26.app.ui.give

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.sharp.QrCode2
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import com.nushhack26.app.nearby.TapAvailability
import com.nushhack26.app.ui.components.HandoverSteps
import com.nushhack26.app.ui.components.ParcelField
import com.nushhack26.app.ui.components.ParcelLabel
import com.nushhack26.app.ui.components.TapOffPanel
import com.nushhack26.app.ui.components.TapPhase
import com.nushhack26.app.ui.components.TapPulse
import com.nushhack26.app.ui.components.rememberHaptics
import com.nushhack26.app.ui.theme.LocalReduceMotion
import com.nushhack26.app.ui.theme.TenetMotion
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ChevronRight
import androidx.compose.material.icons.sharp.Cloud
import androidx.compose.material.icons.sharp.CloudOff
import androidx.compose.material.icons.sharp.ContentCopy
import androidx.compose.material.icons.sharp.Fingerprint
import androidx.compose.material.icons.sharp.QrCodeScanner
import androidx.compose.material.icons.sharp.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.BuildConfig
import com.nushhack26.app.auth.AccessRules
import com.nushhack26.app.ui.components.DiamondVariant
import com.nushhack26.app.ui.components.EmptyState
import com.nushhack26.app.ui.components.HazardDiamond
import com.nushhack26.app.ui.components.HandoverStepper
import com.nushhack26.app.ui.components.ListDivider
import com.nushhack26.app.ui.components.ListRow
import com.nushhack26.app.ui.components.LoadingState
import com.nushhack26.app.ui.components.PrimaryButton
import com.nushhack26.app.ui.components.QrPanel
import com.nushhack26.app.ui.components.QrScanner
import com.nushhack26.app.ui.components.RowLeading
import com.nushhack26.app.ui.components.SecondaryButton
import com.nushhack26.app.ui.components.SignResult
import com.nushhack26.app.ui.components.SquareList
import com.nushhack26.app.ui.components.SyncChip
import com.nushhack26.app.ui.components.SyncChipState
import com.nushhack26.app.ui.components.TextLinkButton
import com.nushhack26.app.ui.components.rememberSignPrompt
import com.nushhack26.app.ui.theme.ChipCutShape
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Hand over (docs/spec.md section 3, G1–G5): choose, sign, tap (or show the offer and scan the acceptance). */
@Composable
fun GiveScreen(container: AppContainer, itemId: String, onDone: () -> Unit) {
    val viewModel: GiveViewModel = viewModel(
        key = "give-$itemId",
        factory = viewModelFactory {
            initializer {
                GiveViewModel(
                    itemId, container.giveRepository, container.identity, { container.deviceKeys },
                    container.offerRelay, container.isOnline, container.serverConfigured,
                )
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val waitingOn = state.offer?.payload?.takeIf { state.step == GiveStep.Sent && !state.declined && state.relayError == null }
    LaunchedEffect(waitingOn) {
        if (waitingOn != null) lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watchRelay() }
    }
    val tapLink = container.tapLink
    val availability by tapLink.available.collectAsStateWithLifecycle()
    val tapReady = rememberTapReady(tapLink, active = state.step == GiveStep.Offer)
    // Serves the offer by tap while G3 shows it; once tapped, it keeps serving past the expiry
    // so an acceptance signed in time still arrives.
    val serving = state.offer?.payload?.takeIf { tapReady && state.step == GiveStep.Offer && (!state.offerExpired || state.tapped) }
    LaunchedEffect(serving) {
        if (serving != null) tapLink.serve(serving).collect { viewModel.onTapEvent(it) }
    }
    var qrMode by rememberSaveable(itemId) { mutableStateOf(false) }
    LaunchedEffect(state.step) { if (state.step == GiveStep.Scan) qrMode = true }
    val haptics = rememberHaptics()
    var lastStep by remember { mutableStateOf(state.step) }
    LaunchedEffect(state.step) {
        if (lastStep == GiveStep.Review && (state.step == GiveStep.Offer || state.step == GiveStep.Sent)) haptics.confirm()
        lastStep = state.step
    }
    LaunchedEffect(state.scanError) { if (state.scanError != null) haptics.reject() }
    val sign = rememberSignPrompt()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    GiveContent(
        state.copy(tapReady = tapReady),
        qrMode = qrMode,
        tap = availability,
        onQrMode = { qrMode = it },
        onTapResume = { tapLink.refresh() },
    ) { action ->
        when (action) {
            GiveAction.Sign -> scope.launch {
                val subtitle = viewModel.prepareSign() ?: return@launch
                when (sign("Sign this handover", subtitle)) {
                    SignResult.Approved -> viewModel.signAndShow()
                    is SignResult.Cancelled -> viewModel.signCancelled()
                }
            }
            GiveAction.ShowQrInstead -> {
                qrMode = true
                viewModel.onAction(action)
            }
            GiveAction.CopyCode -> state.offer?.let { offer ->
                context.getSystemService(ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText("Tenet offer", offer.payload))
            }
            GiveAction.Finish -> onDone()
            else -> viewModel.onAction(action)
        }
    }
}

/**
 * [qrMode] is the screen's "Show QR code instead" switch; with [tap] at NoNfc the QR flow is the
 * only way. [onTapResume] re-checks NFC and Bluetooth when the person comes back from Settings.
 */
@Composable
fun GiveContent(
    state: GiveUiState,
    modifier: Modifier = Modifier,
    qrMode: Boolean = false,
    tap: TapAvailability = TapAvailability.Ready,
    onQrMode: (Boolean) -> Unit = {},
    onTapResume: () -> Unit = {},
    onAction: (GiveAction) -> Unit = {},
) {
    val colors = Tenet.colors
    val qr = qrMode || tap == TapAvailability.NoNfc
    when {
        state.loading -> LoadingState(modifier)
        state.empty != null -> EmptyState(
            title = if (state.empty == GiveViewModel.NOT_HOLDER) "Not yours to give" else "No receivers",
            message = state.empty,
            modifier = modifier,
            actionLabel = "Close",
            onAction = { onAction(GiveAction.Finish) },
        )
        else -> Column(modifier.fillMaxSize().background(colors.bg000)) {
            val (steps, stepNo) = stepperFor(state, qr)
            HandoverStepper(steps, stepNo, Modifier.padding(TenetDimens.space4))
            val reduce = LocalReduceMotion.current
            AnimatedContent(
                targetState = state,
                contentKey = { it.step },
                transitionSpec = {
                    if (reduce) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        val forward = targetState.step.ordinal >= initialState.step.ordinal
                        val spec = tween<IntOffset>(TenetMotion.STEP_MS, easing = TenetMotion.Emphasized)
                        slideInHorizontally(spec) { if (forward) it else -it } togetherWith
                            slideOutHorizontally(spec) { if (forward) -it else it }
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "give-step",
            ) { s ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = TenetDimens.space4),
                    verticalArrangement = Arrangement.spacedBy(TenetDimens.space4),
                ) {
                    when (s.step) {
                        GiveStep.Choose -> ChooseStep(s, onAction)
                        GiveStep.Review -> ReviewStep(s, qr)
                        GiveStep.Sent -> SentStep(s, onAction)
                        GiveStep.Offer -> if (qr) OfferStep(s, tap, onQrMode, onAction) else TapStep(s, tap, onTapResume)
                        GiveStep.Scan -> ScanStep(s, onAction)
                        GiveStep.Done -> DoneStep(s)
                    }
                    Box(Modifier.padding(bottom = TenetDimens.space2))
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(TenetDimens.space4),
                verticalArrangement = Arrangement.spacedBy(TenetDimens.space2),
            ) {
                state.snack?.let { snack ->
                    LaunchedEffect(snack) {
                        delay(4_000)
                        onAction(GiveAction.DismissSnack)
                    }
                    GiveSnackbar(snack)
                }
                BottomAction(state, qr, onQrMode, onAction)
            }
        }
    }
    if (state.pickerOpen && state.step == GiveStep.Choose) {
        GiveBottomSheet(
            title = "Hand ${state.item?.code.orEmpty()} to",
            note = "${state.receivers.count { it.selectable }} can receive",
            onDismiss = { onAction(GiveAction.ClosePicker) },
        ) { ReceiverList(state, onAction) }
    }
}

/** Tap is CHOOSE · TAP, online CHOOSE · SEND, and the QR fallback CHOOSE · SHOW OFFER · SCAN ACCEPTANCE. */
private fun stepperFor(state: GiveUiState, qr: Boolean): Pair<List<String>, Int> = when (state.step) {
    GiveStep.Choose -> (if (qr) HandoverSteps.giverQr else HandoverSteps.giverTap) to 1
    GiveStep.Review -> when {
        state.online -> HandoverSteps.giverOnline
        qr -> HandoverSteps.giverQr
        else -> HandoverSteps.giverTap
    } to 1
    GiveStep.Sent -> HandoverSteps.giverOnline to 2
    GiveStep.Offer -> (if (qr) HandoverSteps.giverQr else HandoverSteps.giverTap) to 2
    GiveStep.Scan -> HandoverSteps.giverQr to 3
    GiveStep.Done -> when {
        state.done?.viaServer == true -> HandoverSteps.giverOnline to 3
        qr -> HandoverSteps.giverQr to 4
        else -> HandoverSteps.giverTap to 3
    }
}

@Composable
private fun BottomAction(state: GiveUiState, qr: Boolean, onQrMode: (Boolean) -> Unit, onAction: (GiveAction) -> Unit) {
    when (state.step) {
        GiveStep.Choose -> PrimaryButton(
            "Review handover",
            onClick = { onAction(GiveAction.Review) },
            enabled = state.canReview,
            disabledReason = when {
                state.selectedReceiver?.selectable != true -> "Choose a receiver"
                else -> "Choose a due time"
            },
        )
        GiveStep.Review -> PrimaryButton(
            when {
                state.online -> "Sign and send"
                qr -> "Sign and show QR"
                else -> "Sign"
            },
            onClick = { onAction(GiveAction.Sign) },
            icon = Icons.Sharp.Fingerprint,
            enabled = !state.leaseExpired && !state.signing,
            disabledReason = if (state.leaseExpired) "Connect once" else "Signing",
        )
        GiveStep.Sent -> if (state.declined) PrimaryButton("Choose someone else", onClick = { onAction(GiveAction.ChooseSomeoneElse) })
        GiveStep.Offer -> if (qr) {
            PrimaryButton("Scan acceptance", onClick = { onAction(GiveAction.ScanAcceptance) }, icon = Icons.Sharp.QrCodeScanner)
        } else {
            SecondaryButton("Show QR code instead", onClick = { onQrMode(true) }, icon = Icons.Sharp.QrCode2)
            if (state.offerExpired) TextLinkButton("Sign a new offer", onClick = { onAction(GiveAction.SignNewOffer) })
            else TextLinkButton("Cancel offer", onClick = { onAction(GiveAction.CancelOffer) })
        }
        GiveStep.Scan -> SecondaryButton("Show the offer again", onClick = { onAction(GiveAction.BackToOffer) })
        GiveStep.Done -> PrimaryButton("Done", onClick = { onAction(GiveAction.Finish) })
    }
}

@Composable
private fun ItemHeader(state: GiveUiState) {
    val item = state.item ?: return
    ListRow(
        title = item.code,
        supporting = "Sealed source ${item.sourceSerial} (${item.isotope}) · seal ${item.sealNo}",
        leading = RowLeading.SealedSource,
        modifier = Modifier.border(TenetDimens.strokeControl, Tenet.colors.line),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChooseStep(state: GiveUiState, onAction: (GiveAction) -> Unit) {
    val colors = Tenet.colors
    ItemHeader(state)
    val receiver = state.selectedReceiver
    val heading = state.receivers.map { it.group }.distinct().joinToString(" or ")
    SectionLabel(heading)
    SquareList {
        ListRow(
            title = receiver?.name ?: "Choose who receives ${state.item?.code.orEmpty()}",
            supporting = receiver?.let { "${AccessRules.roleName(it.role).replaceFirstChar(Char::uppercase)} · ${it.group}" },
            leading = receiver?.let { RowLeading.Initials(initials(it.name)) } ?: RowLeading.None,
            onClick = { onAction(GiveAction.OpenPicker) },
            trailing = { Icon(Icons.Sharp.ChevronRight, contentDescription = "Choose", tint = colors.ink) },
        )
    }
    if (state.isIssue) {
        SectionLabel("Return by")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2), verticalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
            DueChip.entries.forEach { chip ->
                val selected = state.due == chip
                Box(
                    Modifier
                        .heightIn(min = TenetDimens.targetMin)
                        .background(if (selected) colors.signal else colors.bg100, ChipCutShape)
                        .border(TenetDimens.strokeControl, if (selected) colors.signalEdge else colors.lineStrong, ChipCutShape)
                        .clickable(role = Role.RadioButton) { onAction(GiveAction.SelectDue(chip)) }
                        .padding(horizontal = TenetDimens.space4, vertical = TenetDimens.space3),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(chip.label.uppercase(), style = TenetType.label, color = if (selected) colors.onSignal else colors.ink)
                }
            }
        }
    }
}

/** The picker's rows: one section per group, people who may not receive greyed out with the reason. */
@Composable
fun ReceiverList(state: GiveUiState, onAction: (GiveAction) -> Unit) {
    state.receivers.groupBy { it.group }.forEach { (group, people) ->
        SectionLabel(group)
        SquareList {
            people.forEachIndexed { index, person ->
                if (index > 0) ListDivider()
                ListRow(
                    title = person.name,
                    supporting = if (person.selectable) AccessRules.roleName(person.role).replaceFirstChar(Char::uppercase) else person.text,
                    leading = RowLeading.Initials(initials(person.name)),
                    selected = person.id == state.selectedReceiverId,
                    blocked = !person.selectable,
                    onClick = { onAction(GiveAction.SelectReceiver(person.id)) },
                )
            }
        }
    }
}

@Composable
private fun ReviewStep(state: GiveUiState, qr: Boolean) {
    val colors = Tenet.colors
    val summary = state.summary ?: return
    HandoverCard(summary)
    if (state.leaseExpired) {
        Text(AccessRules.LEASE_EXPIRED_TEXT, style = TenetType.bodyLg, color = colors.alertText)
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
            Icon(Icons.Sharp.Fingerprint, contentDescription = null, tint = colors.signalText, modifier = Modifier.size(TenetDimens.iconLg))
            Text("Place your finger to sign this handover", style = TenetType.bodyLg, color = colors.ink)
        }
        val name = state.receiverFirstName
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
            Icon(
                if (state.online) Icons.Sharp.Cloud else Icons.Sharp.CloudOff,
                contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(TenetDimens.iconLg),
            )
            Text(
                when {
                    state.online -> "Online · $name accepts on their phone, no codes to scan"
                    qr -> "Offline · you'll swap QR codes"
                    else -> "Offline · hold your phone to $name's"
                },
                style = TenetType.body, color = colors.inkMuted,
            )
        }
    }
}

/** G3-online: the offer went through the server; this phone finishes by itself when it's accepted. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentStep(state: GiveUiState, onAction: (GiveAction) -> Unit) {
    val colors = Tenet.colors
    val summary = state.summary ?: return
    val name = state.receiverFirstName
    HandoverCard(summary) {
        when {
            state.declined -> Row(horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
                Icon(Icons.Sharp.Warning, contentDescription = null, tint = colors.alertText)
                Text("$name declined. Nothing was handed over.", style = TenetType.bodyLg, color = colors.ink)
            }
            state.offerExpired -> Row(horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
                Icon(Icons.Sharp.Warning, contentDescription = null, tint = colors.alertText)
                Text(
                    "Offer expired. If $name already accepted, scan their code. Otherwise sign a new offer.",
                    style = TenetType.bodyLg, color = colors.ink,
                )
            }
            else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
                WaitingMark(state.secondsLeft)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TenetDimens.space1)) {
                    Text("Sent to $name's phone", style = TenetType.bodyStrong, color = colors.ink)
                    Text("Waiting for $name to scan the seal and accept · ${countdown(state.secondsLeft)}", style = TenetType.body, color = colors.inkMuted)
                }
            }
        }
    }
    state.relayError?.let { ErrorBox(it) }
    if (!state.declined) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
            TextLinkButton("Show QR instead", onClick = { onAction(GiveAction.ShowQrInstead) })
            if (state.offerExpired) TextLinkButton("Sign a new offer", onClick = { onAction(GiveAction.SignNewOffer) })
            else TextLinkButton("Cancel offer", onClick = { onAction(GiveAction.CancelOffer) })
        }
    }
}

/** The hazard diamond, pulsing slowly with the countdown while the offer waits on the other phone. */
@Composable
private fun WaitingMark(secondsLeft: Long) {
    val pulse by animateFloatAsState(
        targetValue = if (secondsLeft % 2 == 0L) 1f else 0.4f,
        animationSpec = tween(900, easing = LinearEasing),
        label = "waiting-alpha",
    )
    HazardDiamond(Modifier.alpha(pulse), variant = DiamondVariant.Outlined)
}

@Composable
private fun ErrorBox(text: String) {
    val colors = Tenet.colors
    Row(
        Modifier.fillMaxWidth().background(colors.alertBg).border(TenetDimens.strokeControl, colors.alert).padding(TenetDimens.space3),
        horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Sharp.Warning, contentDescription = null, tint = colors.alertText)
        Text(text, style = TenetType.body, color = colors.ink)
    }
}

private fun tappedText(name: String) = "Tapped · waiting for $name to scan the seal and accept"

private fun countdown(seconds: Long): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfferStep(state: GiveUiState, tap: TapAvailability, onQrMode: (Boolean) -> Unit, onAction: (GiveAction) -> Unit) {
    val colors = Tenet.colors
    val summary = state.summary ?: return
    val offer = state.offer ?: return
    val name = state.receiverFirstName
    HandoverCard(summary) {
        if (state.offerExpired) {
            Row(horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
                Icon(Icons.Sharp.Warning, contentDescription = null, tint = colors.alertText)
                Text(
                    "Offer expired. If $name already accepted, scan their code. Otherwise sign a new offer.",
                    style = TenetType.bodyLg, color = colors.ink,
                )
            }
            if (state.tapped && state.tapReady) TapLine(tappedText(name))
        } else {
            QrPanel(offer.payload, caption = "Offer · ${summary.itemCode}", instruction = "Ask $name to scan this code.")
            when {
                state.tapped && state.tapReady -> TapLine(tappedText(name))
                state.tapReady -> TapLine("Or hold your phone to $name's")
            }
            val left = state.secondsLeft
            Text(
                "For $name only · single use · valid for ${countdown(left)}",
                style = TenetType.bodyStrong, color = colors.ink,
            )
            if (!(state.tapped && state.tapReady)) Text("Waiting for $name to accept", style = TenetType.body, color = colors.inkMuted)
        }
    }
    state.tapError?.let { ErrorBox(it) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2), verticalArrangement = Arrangement.Center) {
        if (state.offerExpired) TextLinkButton("Sign a new offer", onClick = { onAction(GiveAction.SignNewOffer) })
        else TextLinkButton("Cancel offer", onClick = { onAction(GiveAction.CancelOffer) })
        if (tap != TapAvailability.NoNfc) TextLinkButton("Use tap instead", onClick = { onQrMode(false) })
        if (BuildConfig.DEBUG && !state.offerExpired) {
            Row(
                Modifier.heightIn(min = TenetDimens.targetMin).clickable { onAction(GiveAction.CopyCode) }.padding(horizontal = TenetDimens.space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TenetDimens.space1),
            ) {
                Icon(Icons.Sharp.ContentCopy, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
                Text("COPY CODE", style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold), color = colors.inkMuted)
            }
        }
    }
}

@Composable
private fun ScanStep(state: GiveUiState, onAction: (GiveAction) -> Unit) {
    val colors = Tenet.colors
    val name = state.receiverFirstName
    Text("Scan $name's acceptance code.", style = TenetType.bodyLg, color = colors.ink)
    state.scanError?.let { ErrorBox(it) }
    var rejects by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.scanError) { if (state.scanError != null) rejects++ }
    QrScanner(
        onCode = { onAction(GiveAction.CodeScanned(it)) },
        title = "Scanner · acceptance",
        hint = "Point at $name's code",
        rejectCount = rejects,
    )
    if (state.offerExpired) {
        Text("Offer expired. If $name already accepted, scan their code. Otherwise sign a new offer.", style = TenetType.body, color = colors.inkMuted)
        TextLinkButton("Sign a new offer", onClick = { onAction(GiveAction.SignNewOffer) })
    } else {
        TextLinkButton("Cancel offer", onClick = { onAction(GiveAction.CancelOffer) })
    }
}

/** G3 tap: the tap target, the offer's terms under it, and the panel saying why when tap can't work. */
@Composable
private fun TapStep(state: GiveUiState, tap: TapAvailability, onTapResume: () -> Unit) {
    val colors = Tenet.colors
    val offer = state.offer ?: return
    val name = state.receiverFirstName
    var flooded by remember(offer.payload) { mutableStateOf(false) }
    LaunchedEffect(state.tapped) { if (!state.tapped) flooded = false }
    val expiredUntapped = state.offerExpired && !state.tapped
    when {
        tap != TapAvailability.Ready -> TapOffPanel(tap, onResume = onTapResume)
        expiredUntapped -> Row(horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
            Icon(Icons.Sharp.Warning, contentDescription = null, tint = colors.alertText)
            Text(
                "Offer expired. If $name already accepted, scan their code. Otherwise sign a new offer.",
                style = TenetType.bodyLg, color = colors.ink,
            )
        }
        else -> {
            val phase = when {
                state.tapped && flooded -> TapPhase.Linked
                state.tapped -> TapPhase.Contact
                else -> TapPhase.Waiting
            }
            TapPulse(
                phase = phase,
                title = if (phase == TapPhase.Linked) "Linked" else "Hold to $name's phone",
                subtitle = if (phase == TapPhase.Linked) "$name is scanning the seal and signing." else "Back to back, until both buzz.",
                onFlooded = { flooded = true },
            )
            if (!state.offerExpired) {
                Text(
                    "For $name only · single use · valid for ${countdown(state.secondsLeft)}",
                    style = TenetType.bodyStrong, color = colors.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
            val item = state.item
            if (phase == TapPhase.Waiting && item != null) {
                Text(
                    "${item.displayName} · ${item.code} → $name".uppercase(),
                    style = TenetType.record, color = colors.inkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    state.tapError?.let { ErrorBox(it) }
}

/** G5: the parcel label stamped HANDED OVER, the sync line and the small verify code. */
@Composable
private fun DoneStep(state: GiveUiState) {
    val colors = Tenet.colors
    val done = state.done ?: return
    val summary = state.summary ?: return
    val item = state.item
    ParcelLabel(
        itemName = item?.displayName ?: summary.itemCode,
        itemCode = item?.code ?: summary.itemCode,
        fields = buildList {
            add(ParcelField("From", summary.giverName))
            add(ParcelField("To", summary.receiverName))
            add(ParcelField("Seal", summary.sealNo))
            summary.dueMs?.let { add(ParcelField("Return by", GiveViewModel.hhmm(it))) }
        },
        stampWord = "Handed over",
        stampTime = GiveViewModel.hhmm(done.acceptedMs),
        stampLine = "To ${state.receiverFirstName}",
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
        SyncChip(if (done.synced) SyncChipState.Synced else SyncChipState.Waiting)
        Text(if (done.synced) "Receipt saved · synced" else "Receipt saved · not synced yet", style = TenetType.body, color = colors.ink)
    }
    Text("VERIFY CODE ${verifyCode(done.matchCode)} · same on both phones", style = TenetType.recordLabel, color = colors.inkMuted)
}

/** "48 21 07" → "482 107". */
private fun verifyCode(matchCode: String): String = matchCode.filter(Char::isDigit).chunked(3).joinToString(" ")

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold), color = Tenet.colors.inkMuted)
}

private fun initials(name: String): String =
    name.split(' ').filter { it.isNotEmpty() && it != "Dr" }.take(2).joinToString("") { it.take(1) }
