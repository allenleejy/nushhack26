package com.nushhack26.app.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.data.repo.CustodyRepository
import com.nushhack26.app.ui.components.ReceiptContent
import com.nushhack26.app.ui.components.ReceiptStamp
import com.nushhack26.app.ui.store.LOCAL_DB_ERROR
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface RecordUiState {
    data object Loading : RecordUiState
    data object NotFound : RecordUiState
    data class Error(val message: String) : RecordUiState
    data class Loaded(val content: ReceiptContent, val stamp: ReceiptStamp) : RecordUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecordViewModel(
    repository: CustodyRepository,
    handoverId: String,
    now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    val state: StateFlow<RecordUiState> = repository.observeHandovers()
        .map { all -> all.firstOrNull { it.id == handoverId }?.itemId }
        .distinctUntilChanged()
        .flatMapLatest { itemId ->
            if (itemId == null) {
                flowOf(RecordUiState.NotFound)
            } else {
                combine(repository.observeChain(itemId), repository.observeItems(), repository.observeRoster()) { chain, items, roster ->
                    val handover = chain.firstOrNull { it.id == handoverId } ?: return@combine RecordUiState.NotFound
                    val (content, stamp) = recordReceipt(handover, chain, items.firstOrNull { it.id == itemId }, roster, now())
                    RecordUiState.Loaded(content, stamp)
                }
            }
        }
        .catch { emit(RecordUiState.Error(LOCAL_DB_ERROR)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordUiState.Loading)
}
