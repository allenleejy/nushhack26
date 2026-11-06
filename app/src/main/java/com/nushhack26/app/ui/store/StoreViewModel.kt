package com.nushhack26.app.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.crypto.CustodyStatus
import com.nushhack26.app.data.repo.CustodyBoard
import com.nushhack26.app.data.repo.CustodyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One camera in the store list. */
data class StoreRow(
    val itemId: String,
    val code: String,
    /** "IR-0457 · Ir-192": the source serial, then the isotope. */
    val source: String,
    val status: CustodyStatus,
    val holderName: String?,
    val dueMs: Long?,
    val synced: Boolean,
    /** "Kestrel"; empty until synced. */
    val name: String = "",
) {
    val displayName: String get() = name.ifBlank { code }
}

sealed interface StoreUiState {
    data object Loading : StoreUiState
    data class Error(val message: String) : StoreUiState
    data class Ready(val nowMs: Long, val rows: List<StoreRow>) : StoreUiState
}

class StoreViewModel(repository: CustodyRepository, serverView: Boolean) : ViewModel() {
    val state: StateFlow<StoreUiState> = repository.observeBoard(serverView)
        .map<CustodyBoard, StoreUiState> { board -> StoreUiState.Ready(board.nowMs, storeRows(board)) }
        .catch { emit(StoreUiState.Error(LOCAL_DB_ERROR)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoreUiState.Loading)
}

/** Problems first (Disputed, Overdue), then Out, then In store; by code within each. */
fun storeRows(board: CustodyBoard): List<StoreRow> = board.states
    .map { s ->
        StoreRow(
            itemId = s.item.id,
            code = s.item.code,
            source = "${s.item.sourceSerial} · ${s.item.isotope}",
            status = s.status,
            holderName = s.holderId?.let { board.roster.person(it)?.name ?: "Unknown person" },
            dueMs = s.dueMs,
            synced = s.allSynced,
            name = s.item.name,
        )
    }
    .sortedWith(compareBy({ statusOrder(it.status) }, { it.code }))

private fun statusOrder(status: CustodyStatus) = when (status) {
    CustodyStatus.Disputed -> 0
    CustodyStatus.Overdue -> 1
    CustodyStatus.Out -> 2
    CustodyStatus.InStore -> 3
    CustodyStatus.Unknown -> 4
}
