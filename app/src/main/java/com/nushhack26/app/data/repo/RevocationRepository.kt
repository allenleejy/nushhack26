package com.nushhack26.app.data.repo

import com.nushhack26.app.crypto.DeviceKeys
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.remote.DeviceAdminApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Device revocation on the phone (docs/sync.md A.12). Placeholder: card 13 fills it in.
 * - [revoke]: the safety officer withdraws a device; the updated row is saved in Room.
 * - [checkWithdrawn]: the sync worker calls it when the server says "not enrolled". If this
 *   phone's device was withdrawn, wipe the local data, the identity and the key, and return true.
 * - [withdrawnNotice]: the text Enrol shows after a wipe.
 */
class RevocationRepository(
    private val database: AppDatabase,
    private val identity: IdentityRepository,
    private val api: DeviceAdminApi,
    private val deviceKeys: () -> DeviceKeys,
    private val requestSync: () -> Unit,
) {
    suspend fun revoke(deviceId: String): RelayOutcome = RelayOutcome.Unavailable("card 13")

    suspend fun checkWithdrawn(): Boolean = false

    val withdrawnNotice: Flow<String?> = flowOf(null)
}
