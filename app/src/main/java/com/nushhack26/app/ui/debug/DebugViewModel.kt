package com.nushhack26.app.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.AppContainer
import com.nushhack26.app.BuildConfig
import com.nushhack26.app.data.local.OutboxEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DebugUiState(
    val version: String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TYPE})",
    val serverConfigured: Boolean = false,
    val userId: String? = null,
    val isOnline: Boolean = true,
    val lastSyncAt: Long? = null,
    val lastError: String? = null,
    val outbox: List<OutboxEntry> = emptyList(),
)

sealed interface DebugAction {
    data object SyncNow : DebugAction
    data object ClearLocalData : DebugAction
}

class DebugViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<DebugUiState> = combine(
        container.session.userId,
        container.syncStatus,
        container.database.outboxDao().observeAll(),
    ) { userId, status, outbox ->
        DebugUiState(
            serverConfigured = container.serverConfigured,
            userId = userId,
            isOnline = status.isOnline,
            lastSyncAt = status.lastSyncAt,
            lastError = status.lastError,
            outbox = outbox,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebugUiState())

    fun onAction(action: DebugAction) {
        when (action) {
            DebugAction.SyncNow -> container.syncScheduler.syncNow()
            DebugAction.ClearLocalData -> viewModelScope.launch { container.clearLocalData() }
        }
    }
}
