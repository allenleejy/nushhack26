package com.nushhack26.app.sync

import com.nushhack26.app.data.local.OutboxDao
import com.nushhack26.app.data.local.SyncKeys
import com.nushhack26.app.data.local.SyncStateDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class SyncStatus(
    val isOnline: Boolean = true,
    val pendingCount: Int = 0,
    val lastSyncAt: Long? = null,
    val lastError: String? = null,
)

/** The sync status every screen shows: network, changes waiting, last sync time and last error. */
fun syncStatusFlow(
    isOnline: Flow<Boolean>,
    outbox: OutboxDao,
    syncState: SyncStateDao,
): Flow<SyncStatus> = combine(
    isOnline,
    outbox.observeCount(),
    syncState.observe(SyncKeys.LAST_SYNC_AT),
    syncState.observe(SyncKeys.LAST_ERROR),
) { online, pending, lastSync, lastError ->
    SyncStatus(
        isOnline = online,
        pendingCount = pending,
        lastSyncAt = lastSync?.toLongOrNull(),
        lastError = lastError,
    )
}
