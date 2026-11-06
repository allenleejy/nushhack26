package com.nushhack26.app.ui.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.AppContainer
import com.nushhack26.app.BuildConfig
import com.nushhack26.app.auth.AccessLease
import com.nushhack26.app.crypto.KeyInfo
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.sync.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

data class MeUiState(
    /** True until the identity has loaded from Room. */
    val loading: Boolean = false,
    val serverConfigured: Boolean = true,
    val userId: String? = null,
    val identity: Identity? = null,
    /** This person's roster row; null until the roster has synced (for example after "Clear local data"). */
    val person: PersonEntity? = null,
    val lease: AccessLease? = null,
    val key: KeyInfo? = null,
    val lockApplies: Boolean = false,
    val sync: SyncStatus = SyncStatus(),
    val now: Long = System.currentTimeMillis(),
    val version: String = BuildConfig.VERSION_NAME,
    val debugBuild: Boolean = BuildConfig.DEBUG,
)

class MeViewModel(container: AppContainer) : ViewModel() {
    private val key = flow { emit(runCatching { container.deviceKeys.keyInfo() }.getOrNull()) }
        .flowOn(Dispatchers.IO)
        .onStart { emit(null) }

    val state: StateFlow<MeUiState> = combine(
        combine(container.session.userId, container.identity.identity, container.database.peopleDao().observeAll(), ::Triple),
        container.identity.lease,
        container.syncStatus,
        key,
    ) { (userId, identity, people), lease, sync, key ->
        MeUiState(
            serverConfigured = container.serverConfigured,
            userId = userId,
            identity = identity,
            person = identity?.let { me -> people.firstOrNull { it.id == me.personId } },
            lease = lease,
            key = key,
            lockApplies = container.appLock.lockApplies,
            sync = sync,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeUiState(loading = true, serverConfigured = container.serverConfigured))
}
