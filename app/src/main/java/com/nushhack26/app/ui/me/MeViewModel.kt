package com.nushhack26.app.ui.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.BuildConfig
import com.nushhack26.app.data.remote.SessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MeUiState(
    val serverConfigured: Boolean,
    val userId: String?,
    val version: String = "${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE})",
)

class MeViewModel(session: SessionManager, serverConfigured: Boolean) : ViewModel() {
    val state: StateFlow<MeUiState> = session.userId
        .map { MeUiState(serverConfigured, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeUiState(serverConfigured, session.userId.value))
}
