package com.nushhack26.app.ui.outbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.data.repo.SafetyRepository
import com.nushhack26.app.sync.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class OutboxViewModel(
    repository: SafetyRepository,
    syncStatus: Flow<SyncStatus>,
    private val syncNow: () -> Unit,
) : ViewModel() {
    val state: StateFlow<OutboxUiState> = combine(repository.observeOutbox(), syncStatus) { data, status ->
        OutboxUiState(loading = false, rows = outboxRows(data), status = status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OutboxUiState())

    fun onSyncNow() = syncNow()
}
