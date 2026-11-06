package com.nushhack26.app.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.AppContainer
import com.nushhack26.app.BuildConfig
import com.nushhack26.app.crypto.Signatures
import com.nushhack26.app.data.local.OutboxEntry
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.ui.components.SignResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DebugUiState(
    val version: String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TYPE})",
    val serverConfigured: Boolean = false,
    val userId: String? = null,
    val identity: Identity? = null,
    val isOnline: Boolean = true,
    val lastSyncAt: Long? = null,
    val lastError: String? = null,
    val outbox: List<OutboxEntry> = emptyList(),
    val tampered: Boolean = false,
    val testSigning: String? = null,
)

sealed interface DebugAction {
    data object SyncNow : DebugAction
    data object ClearLocalData : DebugAction
    data class SetTampered(val on: Boolean) : DebugAction

    /** The screen shows the fingerprint prompt first, then passes its result to [DebugViewModel.testSigning]. */
    data object TestSigning : DebugAction
}

class DebugViewModel(private val container: AppContainer) : ViewModel() {
    private val testSigning = MutableStateFlow<String?>(null)

    val state: StateFlow<DebugUiState> = combine(
        combine(container.session.userId, container.identity.identity, container.identity.tampered, ::Triple),
        container.syncStatus,
        container.database.outboxDao().observeAll(),
        testSigning,
    ) { (userId, identity, tampered), status, outbox, signing ->
        DebugUiState(
            serverConfigured = container.serverConfigured,
            userId = userId,
            identity = identity,
            isOnline = status.isOnline,
            lastSyncAt = status.lastSyncAt,
            lastError = status.lastError,
            outbox = outbox,
            tampered = tampered,
            testSigning = signing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebugUiState())

    fun onAction(action: DebugAction) {
        when (action) {
            DebugAction.SyncNow -> container.syncScheduler.syncNow()
            DebugAction.ClearLocalData -> viewModelScope.launch { container.clearLocalData() }
            is DebugAction.SetTampered -> viewModelScope.launch { container.identity.setTampered(action.on) }
            DebugAction.TestSigning -> Unit
        }
    }

    /** Signs and checks a test string with this phone's key, after the fingerprint prompt. */
    fun testSigning(prompt: SignResult) {
        if (prompt is SignResult.Cancelled) {
            testSigning.value = "Not signed: ${prompt.message}"
            return
        }
        viewModelScope.launch {
            testSigning.value = withContext(Dispatchers.Default) {
                runCatching {
                    val keys = container.deviceKeys
                    val info = keys.keyInfo() ?: error("No key yet. Enrol first.")
                    val text = "CS1-TEST|${System.currentTimeMillis()}"
                    val signature = keys.sign(text)
                    check(Signatures.verify(info.publicKey, text, signature)) { "The signature didn't check" }
                    "OK · ${info.security} · key ${info.keyFp}"
                }.getOrElse { "Failed: ${it.message ?: it::class.simpleName}" }
            }
        }
    }
}
