package com.nushhack26.app.ui.fork

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material.icons.sharp.Fingerprint
import androidx.compose.material.icons.sharp.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.auth.AccessRules
import com.nushhack26.app.ui.alerts.NoticeBanner
import com.nushhack26.app.ui.components.EmptyState
import com.nushhack26.app.ui.components.ListDivider
import com.nushhack26.app.ui.components.ListRow
import com.nushhack26.app.ui.components.PrimaryButton
import com.nushhack26.app.ui.components.SecondaryButton
import com.nushhack26.app.ui.components.SquareList
import com.nushhack26.app.ui.components.rememberSignPrompt
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType

sealed interface ForkAction {
    data class Choose(val handoverId: String) : ForkAction
    data class Note(val text: String) : ForkAction
    data object SignDecision : ForkAction
    data object ConfirmSign : ForkAction
    data object Cancel : ForkAction
}

/** O2: both branches of a fork side by side, and the safety officer's signed decision (card 06). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForkScreen(container: AppContainer, alertId: String, onDone: () -> Unit) {
    val viewModel: ForkViewModel = viewModel(
        key = "fork-$alertId",
        factory = viewModelFactory {
            initializer {
                ForkViewModel(
                    alertId = alertId,
                    repository = container.safetyRepository,
                    identity = container.identity,
                    deviceKeys = { container.deviceKeys },
                    syncNow = { container.syncScheduler.syncNow() },
                )
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sign = rememberSignPrompt()
    val onAction: (ForkAction) -> Unit = { action ->
        when (action) {
            is ForkAction.Choose -> viewModel.select(action.handoverId)
            is ForkAction.Note -> viewModel.setNote(action.text)
            ForkAction.SignDecision -> viewModel.openConfirm()
            ForkAction.ConfirmSign -> viewModel.decide(sign, onDone)
            ForkAction.Cancel -> viewModel.dismissConfirm()
        }
    }
    ForkContent(state, onAction = onAction)
    if (state.confirming) {
        ModalBottomSheet(
            onDismissRequest = { onAction(ForkAction.Cancel) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RectangleShape,
            containerColor = Tenet.colors.bg100,
            dragHandle = null,
        ) { DecisionSheetContent(state, onAction = onAction) }
    }
}

@Composable
fun ForkContent(state: ForkUiState, modifier: Modifier = Modifier, onAction: (ForkAction) -> Unit = {}) {
    val colors = Tenet.colors
    if (state.loading) {
        Box(modifier.fillMaxSize().background(colors.bg000))
        return
    }
    if (state.missing) {
        EmptyState(
            title = "Fork not on this phone",
            message = "This alert's records haven't synced to this phone yet. Connect and sync, then open it again.",
            icon = Icons.Sharp.Warning,
            modifier = modifier.background(colors.bg000),
        )
        return
    }
    Column(modifier.fillMaxSize().background(colors.bg000)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(TenetDimens.space4),
            verticalArrangement = Arrangement.spacedBy(TenetDimens.space4),
        ) {
            ForkHeader(state)
            if (state.canDecide && state.decision == null && state.leaseExpired) NoticeBanner(AccessRules.LEASE_EXPIRED_TEXT)
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min).border(TenetDimens.strokeControl, colors.lineStrong),
            ) {
                state.branches.forEachIndexed { index, branch ->
                    if (index > 0) Box(Modifier.width(TenetDimens.strokeControl).fillMaxHeight().background(colors.lineStrong))
                    BranchCell(branch, Modifier.weight(1f).fillMaxHeight())
                }
            }
            when {
                state.decision != null -> DecisionBanner(state.decision)
                state.canDecide -> {
                    Text("WHO HOLDS ${state.itemName}?".uppercase(), style = TenetType.label, color = colors.ink)
                    SquareList(Modifier.testTag("fork_choices")) {
                        state.branches.forEachIndexed { index, branch ->
                            if (index > 0) ListDivider()
                            ListRow(
                                title = "${state.itemName} is with ${branch.receiverName}",
                                supporting = "${branch.label} · ${branch.title} · ${branch.signedText}",
                                selected = branch.handoverId == state.selectedId,
                                onClick = { onAction(ForkAction.Choose(branch.handoverId)) },
                            )
                        }
                    }
                    NoteField(state.note) { onAction(ForkAction.Note(it)) }
                }
                else -> Text(
                    "Only a radiation safety officer can decide who holds ${state.camera}.",
                    style = TenetType.body,
                    color = colors.inkMuted,
                )
            }
            if (state.error != null) NoticeBanner(state.error, problem = true)
        }
        if (state.canDecide && state.decision == null) {
            PrimaryButton(
                label = "Sign decision",
                icon = Icons.Sharp.Fingerprint,
                onClick = { onAction(ForkAction.SignDecision) },
                enabled = state.selectedId != null && !state.leaseExpired && !state.signing,
                disabledReason = when {
                    state.leaseExpired -> "Connect first"
                    state.signing -> "Signing"
                    else -> "Choose who holds it"
                },
                modifier = Modifier.padding(TenetDimens.space4).testTag("sign_decision"),
            )
        }
    }
}

/** The confirmation in the bottom sheet: what will be signed, then the fingerprint. */
@Composable
fun DecisionSheetContent(state: ForkUiState, modifier: Modifier = Modifier, onAction: (ForkAction) -> Unit = {}) {
    val colors = Tenet.colors
    val chosen = state.selected ?: return
    val others = state.branches.filter { it.handoverId != chosen.handoverId }
    Column(
        modifier.fillMaxWidth().background(colors.bg100).padding(TenetDimens.space4),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space3),
    ) {
        Text("SIGN DECISION", style = TenetType.title, color = colors.ink)
        Text(state.camera.uppercase(), style = TenetType.recordLabel, color = colors.inkMuted)
        Text("${state.itemName} is with ${chosen.receiverName}.", style = TenetType.bodyLg, color = colors.ink)
        others.forEach {
            Text("${it.label} (${it.title}) will be marked overruled. It stays in the chain.", style = TenetType.body, color = colors.inkMuted)
        }
        if (state.note.isNotBlank()) Text("Note: ${state.note.trim()}", style = TenetType.body, color = colors.ink)
        Text(
            "Your decision is signed with this phone's key. If you're offline it waits in the outbox.",
            style = TenetType.body,
            color = colors.inkMuted,
        )
        PrimaryButton(
            label = "Sign: with ${chosen.title.substringAfter("→ ")}",
            icon = Icons.Sharp.Fingerprint,
            onClick = { onAction(ForkAction.ConfirmSign) },
            enabled = !state.signing,
            disabledReason = "Signing",
            modifier = Modifier.testTag("confirm_sign"),
        )
        SecondaryButton("Cancel", onClick = { onAction(ForkAction.Cancel) })
    }
}

@Composable
private fun ForkHeader(state: ForkUiState) {
    val colors = Tenet.colors
    Column(Modifier.fillMaxWidth().background(colors.bg100).border(TenetDimens.strokeControl, colors.alert)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(colors.alert), contentAlignment = Alignment.Center) {
                Icon(Icons.Sharp.Warning, contentDescription = null, tint = colors.onAlert, modifier = Modifier.size(TenetDimens.iconLg))
            }
            Column(Modifier.weight(1f).padding(horizontal = TenetDimens.space3)) {
                Text("HANDED OVER TWICE", style = TenetType.title, color = colors.alertText)
                Text(state.headline.uppercase(), style = TenetType.recordLabel, color = colors.inkMuted)
            }
        }
        Text(state.evidence, style = TenetType.bodyLg, color = colors.ink, modifier = Modifier.padding(TenetDimens.space4))
    }
}

@Composable
private fun BranchCell(branch: BranchUi, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Column(
        modifier.background(colors.bg100).padding(TenetDimens.space3),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space2),
    ) {
        Text(
            listOfNotNull(branch.label, branch.statusText).joinToString(" · ").uppercase(),
            style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold),
            color = if (branch.statusText.equals("Overruled", ignoreCase = true)) colors.inkMuted else colors.signalText,
        )
        Text(branch.title.uppercase(), style = TenetType.label, color = colors.ink)
        Text(branch.signedText.uppercase(), style = TenetType.record, color = colors.ink)
        Text(branch.deliveredText, style = TenetType.body, color = colors.inkMuted)
        if (branch.sealText != null) {
            Text(
                (branch.sealText + if (branch.sealOk) " ✓" else " ✗").uppercase(),
                style = TenetType.recordLabel,
                color = if (branch.sealOk) colors.ink else colors.alertText,
            )
        }
        SignatureLine("Giver", branch.giver)
        SignatureLine("Receiver", branch.receiver)
    }
}

@Composable
private fun SignatureLine(role: String, line: SignatureLineUi) {
    val colors = Tenet.colors
    Column {
        Text("$role · ${line.who}".uppercase(), style = TenetType.recordLabel, color = colors.inkMuted)
        Text(
            "KEY ${line.keyFp ?: "NOT ON ROSTER"} ${if (line.ok) "✓" else "✗"}",
            style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold),
            color = if (line.ok) colors.ok else colors.alertText,
        )
    }
}

@Composable
private fun DecisionBanner(text: String) {
    val colors = Tenet.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = TenetDimens.targetRow)
            .background(colors.bg100)
            .border(TenetDimens.strokeControl, colors.line)
            .padding(TenetDimens.space3)
            .testTag("fork_decision"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
    ) {
        Icon(Icons.Sharp.CheckCircle, contentDescription = null, tint = colors.ok, modifier = Modifier.size(TenetDimens.iconMd))
        Text(text, style = TenetType.body, color = colors.ink)
    }
}

/** design/components/TextField: square, label above, free text in body. */
@Composable
private fun NoteField(value: String, onChange: (String) -> Unit) {
    val colors = Tenet.colors
    Column(verticalArrangement = Arrangement.spacedBy(TenetDimens.space1)) {
        Text("NOTE (OPTIONAL)", style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold), color = colors.inkMuted)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("fork_note"),
            textStyle = TenetType.body,
            shape = RectangleShape,
            placeholder = { Text("Why this branch, for the record", style = TenetType.body, color = colors.inkDisabled) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.signalEdge,
                unfocusedBorderColor = colors.lineStrong,
                focusedContainerColor = colors.bg100,
                unfocusedContainerColor = colors.bg100,
                cursorColor = colors.signalText,
            ),
        )
        Text("Saved with your signed decision.", style = TenetType.body, color = colors.inkMuted)
    }
}
