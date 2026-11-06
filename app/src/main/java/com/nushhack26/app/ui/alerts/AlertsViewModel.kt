package com.nushhack26.app.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.data.repo.SafetyRepository
import com.nushhack26.app.sync.SyncStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class AlertsViewModel(
    repository: SafetyRepository,
    syncStatus: Flow<SyncStatus>,
    private val syncNow: () -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    /** Overdue is a matter of time, so the list is rebuilt every 30 seconds as well as on every change. */
    private val ticker = flow {
        while (true) {
            emit(clock())
            delay(30_000)
        }
    }

    val state: StateFlow<AlertsUiState> = combine(repository.observeSafetyData(), syncStatus, ticker) { data, status, _ ->
        AlertsUiState(loading = false, cards = buildAlertCards(data, clock()), status = status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertsUiState())

    fun onSyncNow() = syncNow()
}
