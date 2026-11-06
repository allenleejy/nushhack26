package com.nushhack26.app.data.repo

import com.nushhack26.app.crypto.DeviceKeys
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.local.DeviceEntity
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.remote.DeviceAdminApi
import com.nushhack26.app.data.remote.toEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** One enrolled phone on the roster, with the person it belongs to (null until the roster syncs). */
data class DeviceListing(val device: DeviceEntity, val person: PersonEntity?)

/**
 * Device revocation on the phone (docs/sync.md A.12 and D.6). Online only, never queued.
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
    /** Every device on this phone's copy of the roster, newest enrolment first. */
    val devices: Flow<List<DeviceListing>> = combine(
        database.deviceDao().observeAll(),
        database.peopleDao().observeAll(),
    ) { devices, people ->
        val byId = people.associateBy { it.id }
        devices.sortedByDescending { it.performedAt }.map { DeviceListing(it, byId[it.personId]) }
    }

    suspend fun revoke(deviceId: String): RelayOutcome {
        val result = try {
            withTimeout(TIMEOUT_MS) { api.revoke(deviceId) }
        } catch (e: TimeoutCancellationException) {
            return RelayOutcome.Unavailable("timed out")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return RelayOutcome.Unavailable(e.message ?: e.javaClass.simpleName)
        }
        return when (result.result) {
            REVOKED, DUPLICATE -> {
                result.device?.toEntity()?.let { device ->
                    withContext(Dispatchers.IO) { database.deviceDao().upsertAll(listOf(device)) }
                }
                requestSync()
                RelayOutcome.Sent
            }
            else -> RelayOutcome.Refused(result.reason ?: result.result)
        }
    }

    suspend fun checkWithdrawn(): Boolean = try {
        val status = withTimeout(TIMEOUT_MS) { api.status() }
        val mine = identity.current()?.deviceId
        // device_status answers for the newest device this session enrolled; never wipe for another one.
        if (!status.revoked || (mine != null && status.deviceId != null && status.deviceId != mine)) {
            false
        } else {
            withContext(Dispatchers.IO) {
                deviceKeys().deleteKey()
                database.clearAllTables()
                identity.setWithdrawnNotice(WITHDRAWN_NOTICE)
            }
            true
        }
    } catch (e: TimeoutCancellationException) {
        false
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    val withdrawnNotice: Flow<String?> = identity.withdrawnNotice

    /** Enrol calls this once the phone has enrolled again. */
    suspend fun clearWithdrawnNotice() = identity.clearWithdrawnNotice()

    companion object {
        const val TIMEOUT_MS = 6_000L
        const val WITHDRAWN_NOTICE = "This phone was withdrawn by your safety officer. Ask them for a new enrolment code."
        private const val REVOKED = "revoked"
        private const val DUPLICATE = "duplicate"
    }
}
