package com.nushhack26.app.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Inventory2
import androidx.compose.material.icons.sharp.QrCodeScanner
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.crypto.CustodyStatus
import com.nushhack26.app.ui.components.EmptyState
import com.nushhack26.app.ui.components.ErrorState
import com.nushhack26.app.ui.components.HazardDiamond
import com.nushhack26.app.ui.components.ListDivider
import com.nushhack26.app.ui.components.PrimaryButton
import com.nushhack26.app.ui.components.SecondaryButton
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType

/** S1 Store (docs/spec.md section 3): every camera and who holds it. [readOnly] is the RSO's Cameras tab. */
@Composable
fun StoreScreen(container: AppContainer, readOnly: Boolean, onIssue: (itemId: String) -> Unit, onReceive: () -> Unit, onOpenItem: (itemId: String) -> Unit) {
    val viewModel: StoreViewModel = viewModel(
        key = "store-$readOnly",
        factory = viewModelFactory { initializer { StoreViewModel(container.custodyRepository, serverView = readOnly) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    StoreContent(state, readOnly, onIssue = onIssue, onReceive = onReceive, onOpenItem = onOpenItem)
}

@Composable
fun StoreContent(
    state: StoreUiState,
    readOnly: Boolean,
    modifier: Modifier = Modifier,
    onIssue: (itemId: String) -> Unit = {},
    onReceive: () -> Unit = {},
    onOpenItem: (itemId: String) -> Unit = {},
) {
    val colors = Tenet.colors
    Column(modifier.fillMaxSize().background(colors.bg000)) {
        Column(Modifier.padding(TenetDimens.space4), verticalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
            Text(if (readOnly) "CAMERAS" else "STORE", style = TenetType.headline, color = colors.ink)
            if (readOnly) {
                Text("As the server knows it", style = TenetType.body, color = colors.inkMuted)
            } else {
                PrimaryButton("Receive return", onClick = onReceive, icon = Icons.Sharp.QrCodeScanner)
            }
        }
        when (state) {
            StoreUiState.Loading -> SkeletonRows(count = 4, rowHeight = 96.dp)
            is StoreUiState.Error -> ErrorState(state.message)
            is StoreUiState.Ready -> if (state.rows.isEmpty()) {
                EmptyState(
                    title = "No cameras yet",
                    message = "Cameras appear here after the phone syncs the register.",
                    icon = Icons.Sharp.Inventory2,
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = TenetDimens.space4),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(state.rows, key = { it.itemId }) { row ->
                        StoreItemRow(row, state.nowMs, readOnly, onIssue = { onIssue(row.itemId) }, onOpen = { onOpenItem(row.itemId) })
                        ListDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreItemRow(row: StoreRow, nowMs: Long, readOnly: Boolean, onIssue: () -> Unit, onOpen: () -> Unit) {
    val colors = Tenet.colors
    val small = TenetType.body.copy(fontSize = 14.sp, lineHeight = 20.sp)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = TenetDimens.targetRow)
            .background(colors.bg100)
            .clickable(onClickLabel = "Open the timeline", role = Role.Button, onClick = onOpen)
            .padding(TenetDimens.space4),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
            HazardDiamond(size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(row.code.uppercase(), style = TenetType.title, color = colors.ink)
                Text(row.source.uppercase(), style = TenetType.recordLabel, color = colors.inkMuted)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2),
            verticalArrangement = Arrangement.spacedBy(TenetDimens.space2),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            CustodyStatusChip(row.status)
            if (!row.synced) NotSyncedMark()
        }
        when (row.status) {
            CustodyStatus.Out, CustodyStatus.Overdue -> {
                Text("Held by ${row.holderName}", style = TenetType.bodyStrong, color = colors.ink)
                row.dueMs?.let { due ->
                    val late = row.status == CustodyStatus.Overdue
                    Text(
                        "Due ${clockText(due)} · ${remainingText(due, nowMs)}",
                        style = small.copy(fontWeight = if (late) FontWeight.Bold else FontWeight.Normal),
                        color = if (late) colors.alertText else colors.inkMuted,
                    )
                }
            }
            CustodyStatus.Disputed -> Text("Two handovers claim this camera. A supervisor must decide.", style = small, color = colors.alertText)
            CustodyStatus.InStore -> row.holderName?.let { Text("In the store with $it", style = small, color = colors.inkMuted) }
            CustodyStatus.Unknown -> Text("The register record hasn't reached this phone.", style = small, color = colors.inkMuted)
        }
        if (!readOnly && row.status == CustodyStatus.InStore) {
            SecondaryButton("Issue", onClick = onIssue, modifier = Modifier.fillMaxWidth())
        }
    }
}
