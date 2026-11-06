package com.nushhack26.app.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Timeline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.crypto.CustodyStatus
import com.nushhack26.app.ui.components.EmptyState
import com.nushhack26.app.ui.components.ErrorState
import com.nushhack26.app.ui.components.HazardDiamond
import com.nushhack26.app.ui.components.TextLinkButton
import com.nushhack26.app.ui.store.CustodyStatusChip
import com.nushhack26.app.ui.store.SkeletonRows
import com.nushhack26.app.ui.store.clockText
import com.nushhack26.app.ui.store.remainingText
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType

/** T Timeline (docs/spec.md section 3): one camera's custody chain, oldest first. */
@Composable
fun TimelineScreen(container: AppContainer, itemId: String, onReportMissing: (itemId: String) -> Unit) {
    val viewModel: TimelineViewModel = viewModel(
        key = "timeline-$itemId",
        factory = viewModelFactory { initializer { TimelineViewModel(container.custodyRepository, itemId) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    TimelineContent(state, onReportMissing = { onReportMissing(itemId) })
}

@Composable
fun TimelineContent(state: TimelineUiState, modifier: Modifier = Modifier, onReportMissing: () -> Unit = {}) {
    val colors = Tenet.colors
    Column(modifier.fillMaxSize().background(colors.bg000)) {
        when (state) {
            TimelineUiState.Loading -> SkeletonRows(count = 4, rowHeight = 96.dp)
            is TimelineUiState.Error -> ErrorState(state.message)
            TimelineUiState.NotFound -> EmptyState(
                title = "Camera not on this phone",
                message = "It appears here after the phone syncs the register.",
                icon = Icons.Sharp.Timeline,
            )
            is TimelineUiState.Ready -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(TenetDimens.space4),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item { Header(state.header, state.nowMs, onReportMissing) }
                if (state.nodes.isEmpty()) {
                    item {
                        EmptyState(title = "No records yet", message = "This camera's register record hasn't synced to this phone.", icon = Icons.Sharp.Timeline)
                    }
                }
                itemsIndexed(state.nodes) { index, node ->
                    val last = index == state.nodes.lastIndex
                    when (node) {
                        is TimelineNode.Record -> TimelineRow(node.entry, last = last)
                        is TimelineNode.Fork -> ForkBlock(node.branches)
                        is TimelineNode.Resolution -> ResolutionRow(node)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(header: TimelineHeader, nowMs: Long, onReportMissing: () -> Unit) {
    val colors = Tenet.colors
    Column(Modifier.fillMaxWidth().padding(bottom = TenetDimens.space4), verticalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
            HazardDiamond(size = TenetDimens.diamondMd)
            Column(Modifier.weight(1f)) {
                Text(header.code.uppercase(), style = TenetType.headline, color = colors.ink)
                Text(header.source.uppercase(), style = TenetType.recordLabel, color = colors.inkMuted)
                Text("SEAL ${header.sealNo}".uppercase(), style = TenetType.recordLabel, color = colors.inkMuted)
            }
        }
        CustodyStatusChip(header.status)
        header.holderName?.let { Text("Held by $it", style = TenetType.bodyStrong, color = colors.ink) }
        header.dueMs?.let {
            val late = header.status == CustodyStatus.Overdue
            Text("Due ${clockText(it)} · ${remainingText(it, nowMs)}", style = TenetType.body, color = if (late) colors.alertText else colors.inkMuted)
        }
        TextLinkButton("Report missing", onClick = onReportMissing)
    }
}
