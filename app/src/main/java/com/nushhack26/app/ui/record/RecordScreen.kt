package com.nushhack26.app.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ReceiptLong
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.components.CustodyReceipt
import com.nushhack26.app.ui.components.EmptyState
import com.nushhack26.app.ui.components.ErrorState
import com.nushhack26.app.ui.components.LoadingState
import com.nushhack26.app.ui.components.ReceiptStyle
import com.nushhack26.app.ui.theme.Tenet

/** One handover as its printed custody receipt (card 30), opened from the timeline or the outbox. */
@Composable
fun RecordScreen(container: AppContainer, handoverId: String) {
    val viewModel: RecordViewModel = viewModel(
        key = "record-$handoverId",
        factory = viewModelFactory { initializer { RecordViewModel(container.custodyRepository, handoverId) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecordContent(state)
}

@Composable
fun RecordContent(state: RecordUiState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(Tenet.colors.bg000)) {
        when (state) {
            RecordUiState.Loading -> LoadingState()
            RecordUiState.NotFound -> EmptyState(
                title = "Record not on this phone",
                message = "Sync, then try again.",
                icon = Icons.AutoMirrored.Sharp.ReceiptLong,
            )
            is RecordUiState.Error -> ErrorState(state.message)
            is RecordUiState.Loaded -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                CustodyReceipt(state.content, state.stamp, ReceiptStyle.Record)
            }
        }
    }
}
