package com.nushhack26.app.ui.missing

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
import androidx.compose.material.icons.sharp.CloudOff
import androidx.compose.material.icons.sharp.Lock
import androidx.compose.material.icons.sharp.ReportProblem
import androidx.compose.material.icons.automirrored.sharp.Send
import androidx.compose.material.icons.sharp.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.ui.alerts.NoticeBanner
import com.nushhack26.app.ui.components.EmptyState
import com.nushhack26.app.ui.components.ErrorState
import com.nushhack26.app.ui.components.HazardDiamond
import com.nushhack26.app.ui.components.PrimaryButton
import com.nushhack26.app.ui.store.SkeletonRows
import com.nushhack26.app.ui.store.clockText
import com.nushhack26.app.ui.store.codeLine
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.hazardStripes

/** Report missing (docs/spec.md section 3, reg 26(3)): from a camera's Timeline, with the last signed holder filled in. Card 32. */
@Composable
fun ReportMissingScreen(container: AppContainer, itemId: String, onDone: () -> Unit) {
    val viewModel: ReportMissingViewModel = viewModel(
        key = "missing-$itemId",
        factory = viewModelFactory {
            initializer {
                ReportMissingViewModel(itemId, container.custodyRepository, container.safetyRepository, container.identity)
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportMissingContent(state) { action ->
        when (action) {
            is ReportMissingAction.Note -> viewModel.setNote(action.text)
            ReportMissingAction.Send -> viewModel.send()
            ReportMissingAction.Done -> onDone()
        }
    }
}

@Composable
fun ReportMissingContent(state: ReportMissingUiState, modifier: Modifier = Modifier, onAction: (ReportMissingAction) -> Unit = {}) {
    val colors = Tenet.colors
    Column(modifier.fillMaxSize().background(colors.bg000)) {
        when (state) {
            ReportMissingUiState.Loading -> SkeletonRows(count = 3, rowHeight = 96.dp)
            is ReportMissingUiState.Error -> ErrorState(state.message)
            ReportMissingUiState.NotFound -> EmptyState(
                title = "Camera not on this phone",
                message = "This camera isn't on this phone. Sync once.",
                icon = Icons.Sharp.Warning,
            )
            is ReportMissingUiState.Ready -> Ready(state, onAction)
        }
    }
}

@Composable
private fun Ready(state: ReportMissingUiState.Ready, onAction: (ReportMissingAction) -> Unit) {
    val sent = state.sent
    // A fresh scroll for the confirmation, so it never opens part-way down the form.
    val scroll = key(sent != null) { rememberScrollState() }
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(TenetDimens.space4),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space4),
    ) {
        if (sent != null) {
            SentConfirmation(state.camera, sent)
            if (state.note.isNotBlank()) Text("Note: ${state.note.trim()}", style = TenetType.body, color = Tenet.colors.ink)
            PrimaryButton(label = "Done", onClick = { onAction(ReportMissingAction.Done) }, modifier = Modifier.testTag("missing_done"))
            return@Column
        }
        state.earlier.forEach {
            NoticeBanner("Reported missing by ${it.byName} · ${clockText(it.reportedMs)}", problem = true)
        }
        Text("Report ${state.camera.code} missing", style = TenetType.headline, color = Tenet.colors.ink)
        CameraCard(state.camera)
        HolderBlock(state.holder)
        NoteField(state.note) { onAction(ReportMissingAction.Note(it)) }
        if (state.error != null) NoticeBanner(state.error, problem = true)
        PrimaryButton(
            label = "Send report",
            icon = Icons.AutoMirrored.Sharp.Send,
            onClick = { onAction(ReportMissingAction.Send) },
            enabled = !state.sending,
            disabledReason = "Saving",
            modifier = Modifier.testTag("send_report"),
        )
        Text(
            "Saved on this phone at once. It goes to the safety officer when the phone is online.",
            style = TenetType.body,
            color = Tenet.colors.inkMuted,
        )
    }
}

/** The camera under a hazard band: name, code, source and seal. */
@Composable
private fun CameraCard(camera: MissingCamera) {
    val colors = Tenet.colors
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).border(TenetDimens.strokeControl, colors.alert)) {
        Box(Modifier.width(24.dp).fillMaxHeight().hazardStripes(colors.alert, colors.alertBg))
        Row(
            Modifier.weight(1f).background(colors.bg100).padding(TenetDimens.space3),
            horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HazardDiamond(size = TenetDimens.diamondMd)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TenetDimens.space1)) {
                Text(camera.displayName.uppercase(), style = TenetType.title, color = colors.ink)
                Text(
                    codeLine("Camera ${camera.code}", "Source ${camera.source}", "Seal ${camera.sealNo}"),
                    style = TenetType.recordLabel,
                    color = colors.inkMuted,
                )
            }
        }
    }
}

/** Read-only: the holder comes from the signed chain, not from the person reporting. */
@Composable
private fun HolderBlock(holder: LastHolder?) {
    val colors = Tenet.colors
    Column(verticalArrangement = Arrangement.spacedBy(TenetDimens.space1)) {
        Text("LAST SIGNED HOLDER", style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold), color = colors.inkMuted)
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = TenetDimens.targetRow)
                .background(colors.bg200)
                .border(TenetDimens.strokeControl, colors.line)
                .padding(TenetDimens.space3)
                .testTag("missing_holder"),
            horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                if (holder == null) {
                    Text("No signed record on this phone", style = TenetType.bodyStrong, color = colors.ink)
                    Text("Sync once to fill this in.", style = TenetType.body, color = colors.inkMuted)
                } else {
                    Text(holder.name, style = TenetType.bodyStrong, color = colors.ink)
                    Text(holder.role, style = TenetType.body, color = colors.inkMuted)
                    if (holder.detail.isNotBlank()) Text(holder.detail.uppercase(), style = TenetType.recordLabel, color = colors.ink)
                }
            }
            Icon(Icons.Sharp.Lock, contentDescription = "From the signed chain", tint = colors.inkMuted, modifier = Modifier.size(TenetDimens.iconMd))
        }
        Text("From the signed chain. It can't be changed here.", style = TenetType.body, color = colors.inkMuted)
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
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp).testTag("missing_note"),
            textStyle = TenetType.body,
            shape = RectangleShape,
            placeholder = { Text("Where and when it was last seen", style = TenetType.body, color = colors.inkDisabled) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.ink,
                unfocusedTextColor = colors.ink,
                focusedBorderColor = colors.signalEdge,
                unfocusedBorderColor = colors.lineStrong,
                focusedContainerColor = colors.bg100,
                unfocusedContainerColor = colors.bg100,
                cursorColor = colors.signalText,
            ),
        )
        Text("${value.length} / $NOTE_MAX", style = TenetType.recordLabel, color = colors.inkMuted, modifier = Modifier.align(Alignment.End))
    }
}

@Composable
private fun SentConfirmation(camera: MissingCamera, sent: SentReport) {
    val colors = Tenet.colors
    Column(Modifier.fillMaxWidth().background(colors.bg100).border(TenetDimens.strokeControl, colors.alert)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(colors.alert), contentAlignment = Alignment.Center) {
                Icon(Icons.Sharp.ReportProblem, contentDescription = null, tint = colors.onAlert, modifier = Modifier.size(TenetDimens.iconLg))
            }
            Text(
                "Reported ${camera.code} missing · ${clockText(sent.reportedMs)}",
                style = TenetType.title,
                color = colors.ink,
                modifier = Modifier.weight(1f).padding(horizontal = TenetDimens.space3, vertical = TenetDimens.space2).testTag("missing_confirmation"),
            )
        }
        val (icon: ImageVector, text: String, tint) = when (sent.syncStatus) {
            SyncMark.SYNCED -> Triple(Icons.Sharp.CheckCircle, "Sent ✓", colors.ok)
            SyncMark.REJECTED -> Triple(Icons.Sharp.Warning, "Refused by the server: ${sent.reason ?: "no reason given"}", colors.alertText)
            else -> Triple(Icons.Sharp.CloudOff, "Report queued · sends when online", colors.inkMuted)
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = TenetDimens.targetRow).padding(horizontal = TenetDimens.space4).testTag("missing_sync_mark"),
            horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(TenetDimens.iconMd))
            Text(
                text,
                style = TenetType.bodyStrong,
                color = if (sent.syncStatus == SyncMark.REJECTED) colors.alertText else colors.ink,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
