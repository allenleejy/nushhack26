package com.nushhack26.app.ui.mycustody

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Inventory2
import androidx.compose.material.icons.sharp.Contactless
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.alarm.OverdueAlarm
import com.nushhack26.app.ui.components.EmptyState
import com.nushhack26.app.ui.components.ErrorState
import com.nushhack26.app.ui.components.PrimaryButton
import com.nushhack26.app.ui.store.SkeletonRows
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType

/** P1 My custody (docs/spec.md section 3): the cameras I hold, their countdowns, and my receipts. */
@Composable
fun MyCustodyScreen(container: AppContainer, onHandOver: (itemId: String) -> Unit, onReceive: () -> Unit, onOpenItem: (itemId: String) -> Unit) {
    val viewModel: MyCustodyViewModel = viewModel(
        factory = viewModelFactory { initializer { MyCustodyViewModel(container.custodyRepository, container.identity) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    OverdueAlarm.RequestPermissionWhenHolding(holding = (state as? MyCustodyUiState.Ready)?.held?.isNotEmpty() == true)
    MyCustodyContent(state, onHandOver = onHandOver, onReceive = onReceive, onOpenItem = onOpenItem)
}

@Composable
fun MyCustodyContent(
    state: MyCustodyUiState,
    modifier: Modifier = Modifier,
    onHandOver: (itemId: String) -> Unit = {},
    onReceive: () -> Unit = {},
    onOpenItem: (itemId: String) -> Unit = {},
) {
    val colors = Tenet.colors
    Column(modifier.fillMaxSize().background(colors.bg000)) {
        when (state) {
            MyCustodyUiState.Loading -> {
                Header(onReceive)
                SkeletonRows(count = 2, rowHeight = 200.dp)
            }
            is MyCustodyUiState.Error -> {
                Header(onReceive)
                ErrorState(state.message)
            }
            is MyCustodyUiState.Ready -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = TenetDimens.space8),
                verticalArrangement = Arrangement.spacedBy(TenetDimens.space4),
            ) {
                item { Header(onReceive) }
                items(state.pending, key = { "offer-${it.itemId}" }) { offer ->
                    PendingOfferBanner(offer, state.nowMs, onOpen = { onHandOver(offer.itemId) }, Modifier.padding(horizontal = TenetDimens.space4))
                }
                if (state.held.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No cameras held",
                            message = "You don't hold anything. Open RECEIVE when someone hands you a camera.",
                            icon = Icons.Sharp.Inventory2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                items(state.held, key = { it.itemId }) { camera ->
                    CustodyCard(
                        camera, state.nowMs,
                        onHandOver = { onHandOver(camera.itemId) }, onOpen = { onOpenItem(camera.itemId) },
                        modifier = Modifier.padding(horizontal = TenetDimens.space4),
                    )
                }
                if (state.receipts.isNotEmpty()) {
                    item {
                        Text(
                            "RECEIPTS", style = TenetType.title, color = colors.ink,
                            modifier = Modifier.padding(start = TenetDimens.space4, end = TenetDimens.space4, top = TenetDimens.space4),
                        )
                    }
                    items(state.receipts, key = { "receipt-${it.handoverId}" }) { receipt ->
                        Receipt(receipt, Modifier.padding(horizontal = TenetDimens.space4))
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(onReceive: () -> Unit) {
    Column(Modifier.padding(TenetDimens.space4), verticalArrangement = Arrangement.spacedBy(TenetDimens.space3)) {
        Text("MY CUSTODY", style = TenetType.headline, color = Tenet.colors.ink)
        PrimaryButton("Receive", onClick = onReceive, icon = Icons.Sharp.Contactless)
    }
}
