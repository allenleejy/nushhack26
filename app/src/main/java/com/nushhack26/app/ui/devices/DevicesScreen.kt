package com.nushhack26.app.ui.devices

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Block
import androidx.compose.material.icons.sharp.PhoneAndroid
import androidx.compose.material.icons.sharp.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.alerts.NoticeBanner
import com.nushhack26.app.ui.alerts.ScreenHeading
import com.nushhack26.app.ui.alerts.lastSyncText
import com.nushhack26.app.ui.components.DangerButton
import com.nushhack26.app.ui.components.EmptyState
import com.nushhack26.app.ui.components.ErrorState
import com.nushhack26.app.ui.components.ListDivider
import com.nushhack26.app.ui.components.ListRow
import com.nushhack26.app.ui.components.LoadingState
import com.nushhack26.app.ui.components.RowLeading
import com.nushhack26.app.ui.components.SecondaryButton
import com.nushhack26.app.ui.components.SquareList
import com.nushhack26.app.ui.components.dashedBorder
import com.nushhack26.app.ui.components.formatTime
import com.nushhack26.app.ui.give.GiveSnack
import com.nushhack26.app.ui.give.GiveSnackbar
import com.nushhack26.app.ui.theme.ChipCutShape
import com.nushhack26.app.ui.theme.PrimaryButtonShape
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.hatching
import com.nushhack26.app.ui.theme.hazardStripes
import kotlinx.coroutines.delay

/** O3 Devices: the safety officer's enrolled phones, and Withdraw for a lost one (docs/spec.md O3, docs/sync.md A.12). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(container: AppContainer) {
    val viewModel: DevicesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { DevicesViewModel(container.revocation, container.identity.identity, container.syncStatus) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction
    state.snack?.let { snack ->
        LaunchedEffect(snack) {
            delay(if (snack is GiveSnack.Problem) 8_000 else 4_000)
            onAction(DevicesAction.SnackShown)
        }
    }
    DevicesContent(state, onAction = onAction)
    if (state.confirming != null) {
        ModalBottomSheet(
            onDismissRequest = { onAction(DevicesAction.Cancel) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RectangleShape,
            containerColor = Tenet.colors.bg200,
            dragHandle = null,
        ) { WithdrawSheetContent(state, onAction = onAction) }
    }
}

@Composable
fun DevicesContent(state: DevicesUiState, modifier: Modifier = Modifier, onAction: (DevicesAction) -> Unit = {}) {
    val colors = Tenet.colors
    val status = state.status
    Box(modifier.fillMaxSize().background(colors.bg000)) {
        when {
            state.loading -> LoadingState(message = "Loading phones")
            state.error != null -> ErrorState(state.error)
            else -> Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(TenetDimens.space4),
                verticalArrangement = Arrangement.spacedBy(TenetDimens.space4),
            ) {
                val inUse = state.rows.count { it.withdrawnAtMs == null }
                ScreenHeading(
                    title = "Devices",
                    meta = "$inUse in use · ${lastSyncText(status.lastSyncAt)}",
                )
                if (!status.isOnline) NoticeBanner(WITHDRAW_OFFLINE_TEXT, Modifier.testTag("devices_offline"))
                if (state.rows.isEmpty()) {
                    EmptyState(
                        title = "No phones yet",
                        message = DEVICES_EMPTY_TEXT,
                        icon = Icons.Sharp.PhoneAndroid,
                        modifier = Modifier.testTag("devices_empty"),
                    )
                } else {
                    SquareList(Modifier.testTag("devices_list")) {
                        state.rows.forEachIndexed { index, row ->
                            if (index > 0) ListDivider()
                            DeviceRow(row, online = status.isOnline, onAction = onAction)
                        }
                    }
                }
                // Room for the snackbar over the last row.
                if (state.snack != null) Box(Modifier.height(96.dp))
            }
        }
        state.snack?.let {
            GiveSnackbar(
                it,
                modifier = Modifier.align(Alignment.BottomCenter).padding(TenetDimens.space4).testTag("devices_snack"),
            )
        }
    }
}

@Composable
private fun DeviceRow(row: DeviceRowUi, online: Boolean, onAction: (DevicesAction) -> Unit) {
    val colors = Tenet.colors
    Column(Modifier.background(colors.bg100).testTag("device_${row.deviceId}")) {
        ListRow(
            title = row.name,
            supporting = listOf(row.role, row.keyLine, row.enrolledLine).joinToString("\n"),
            leading = RowLeading.Initials(initials(row.name)),
        )
        Column(
            Modifier.padding(start = TenetDimens.space4, end = TenetDimens.space4, bottom = TenetDimens.space3),
            verticalArrangement = Arrangement.spacedBy(TenetDimens.space2),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
                modifier = Modifier.heightIn(min = TenetDimens.targetMin),
            ) {
                DeviceChip(row.withdrawnAtMs)
                if (row.isThisPhone) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TenetDimens.space1)) {
                        Icon(Icons.Sharp.Smartphone, contentDescription = null, tint = colors.signalText, modifier = Modifier.size(20.dp))
                        Text("THIS PHONE", style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold), color = colors.signalText)
                    }
                }
            }
            when {
                !row.canWithdraw -> Unit
                online -> DangerButton(
                    "Withdraw",
                    onClick = { onAction(DevicesAction.Withdraw(row.deviceId)) },
                    icon = Icons.Sharp.Block,
                    modifier = Modifier.testTag("withdraw_${row.deviceId}"),
                )
                else -> DisabledWithdraw()
            }
        }
    }
}

/** IN USE in the synced style, or WITHDRAWN 14:40 with alert stripes. */
@Composable
fun DeviceChip(withdrawnAtMs: Long?, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    val label = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold)
    if (withdrawnAtMs == null) {
        Row(
            modifier.height(32.dp).clip(ChipCutShape).background(colors.okBg, ChipCutShape).padding(horizontal = TenetDimens.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("IN USE", style = label, color = colors.ok, maxLines = 1, softWrap = false)
        }
    } else {
        Row(
            modifier.height(IntrinsicSize.Min).heightIn(min = 32.dp).background(colors.alertBg).border(TenetDimens.strokeControl, colors.alert),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(10.dp).fillMaxHeight().hazardStripes(colors.onAlert, colors.alert))
            Text(
                "WITHDRAWN ${formatTime(withdrawnAtMs)}",
                style = label,
                color = colors.alertText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = TenetDimens.space2, vertical = TenetDimens.space1),
            )
        }
    }
}

@Composable
private fun DisabledWithdraw() {
    val colors = Tenet.colors
    val text = "WITHDRAW · NEEDS THE NETWORK"
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = TenetDimens.targetRow)
            .hatching(colors.line, colors.bg200)
            .dashedBorder(colors.lineStrong)
            .semantics {
                disabled()
                contentDescription = WITHDRAW_OFFLINE_TEXT
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = TenetType.label,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.background(colors.bg000).padding(horizontal = TenetDimens.space2, vertical = TenetDimens.space1),
        )
    }
}

@Composable
fun WithdrawSheetContent(state: DevicesUiState, modifier: Modifier = Modifier, onAction: (DevicesAction) -> Unit = {}) {
    val colors = Tenet.colors
    val row = state.confirming ?: return
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.bg200)
            .border(BorderStroke(TenetDimens.strokeControl, colors.signalEdge), RectangleShape)
            .padding(TenetDimens.space4),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space3),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(TenetDimens.handle, 4.dp).background(colors.lineStrong))
        Column(verticalArrangement = Arrangement.spacedBy(TenetDimens.space1)) {
            Text("WITHDRAW PHONE", style = TenetType.title, color = colors.ink)
            Text(row.name.uppercase(), style = TenetType.recordLabel, color = colors.inkMuted)
        }
        Box(Modifier.fillMaxWidth().height(TenetDimens.strokeControl).background(colors.lineStrong))
        Text(withdrawQuestion(row.name), style = TenetType.bodyLg, color = colors.ink)
        Text(row.keyLine, style = TenetType.record, color = colors.inkMuted)
        Button(
            onClick = { if (!state.withdrawing) onAction(DevicesAction.ConfirmWithdraw) },
            modifier = Modifier.fillMaxWidth().heightIn(min = TenetDimens.targetPrimary).testTag("confirm_withdraw"),
            shape = PrimaryButtonShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.alert, contentColor = colors.onAlert),
        ) {
            Icon(Icons.Sharp.Block, contentDescription = null, modifier = Modifier.size(TenetDimens.iconMd))
            Text(
                (if (state.withdrawing) "Withdrawing…" else "Withdraw phone").uppercase(),
                style = TenetType.label,
                modifier = Modifier.padding(start = TenetDimens.space2),
            )
        }
        if (!state.withdrawing) SecondaryButton("Cancel", onClick = { onAction(DevicesAction.Cancel) })
    }
}

private fun initials(name: String): String =
    name.split(' ').filter { it.isNotEmpty() && it != "Dr" }.take(2).joinToString("") { it.take(1) }
