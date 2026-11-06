package com.nushhack26.app.data.repo

import androidx.room.withTransaction
import com.nushhack26.app.auth.AppLock
import com.nushhack26.app.crypto.KeyInfo
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.remote.EnrolApi
import com.nushhack26.app.data.remote.EnrolDevice
import com.nushhack26.app.data.remote.EnrolResultKind
import com.nushhack26.app.data.remote.NotReadyException
import com.nushhack26.app.data.remote.toEntity
import com.nushhack26.app.data.remote.toServerTime
import java.util.UUID

sealed interface EnrolOutcome {
    data class Enrolled(val identity: Identity) : EnrolOutcome

    /** Wrong code, or no active person with that staff number. */
    data object WrongCode : EnrolOutcome

    /** Five wrong codes in 10 minutes. */
    data object Locked : EnrolOutcome

    /** No network, no session yet, or no server config. Nothing was created. */
    data class NoConnection(val message: String) : EnrolOutcome

    data class Failed(val reason: String) : EnrolOutcome
}

/**
 * Enrol (card 07): prove who you are with your staff number and enrolment code, and put this
 * phone's public key on the roster (docs/sync.md C.2 and D.4). Online only, never queued.
 */
class EnrolRepository(
    private val db: AppDatabase,
    private val identity: IdentityRepository,
    private val api: EnrolApi,
    private val appLock: AppLock,
    private val requestSync: () -> Unit,
) {
    /**
     * Call after the key is made and the person has authenticated. On success it saves the
     * person, the device and the identity in one transaction, unlocks the app and syncs.
     */
    suspend fun enrol(staffNo: String, code: String, key: KeyInfo, appVersion: String): EnrolOutcome {
        val now = System.currentTimeMillis()
        val request = EnrolDevice(
            id = UUID.randomUUID().toString(),
            publicKey = key.publicKey,
            keyFp = key.keyFp,
            keySecurity = key.security,
            appVersion = appVersion,
            performedAt = now.toServerTime(),
        )
        val response = try {
            api.enrol(staffNo.trim(), code.trim(), request)
        } catch (e: NotReadyException) {
            return EnrolOutcome.NoConnection(e.message ?: "Not connected")
        } catch (e: Exception) {
            return EnrolOutcome.NoConnection(e.message ?: e.javaClass.simpleName)
        }

        return when (response.result) {
            EnrolResultKind.APPLIED, EnrolResultKind.DUPLICATE -> {
                val person = response.person?.toEntity() ?: return EnrolOutcome.Failed("The server sent no person")
                val device = response.device?.toEntity() ?: return EnrolOutcome.Failed("The server sent no device")
                val me = Identity(person.id, device.id, person.name, person.role)
                // The person just authenticated to make the key, so the app opens unlocked.
                appLock.markUnlocked()
                db.withTransaction {
                    db.peopleDao().upsertAll(listOf(person))
                    db.deviceDao().upsertAll(listOf(device))
                    identity.setIdentity(me)
                    identity.setEnrolment(key.publicKey, now)
                }
                requestSync()
                EnrolOutcome.Enrolled(me)
            }
            EnrolResultKind.LOCKED -> EnrolOutcome.Locked
            EnrolResultKind.REJECTED ->
                if (response.reason == "wrong code") EnrolOutcome.WrongCode
                else EnrolOutcome.Failed(response.reason ?: "Refused by the server")
            else -> EnrolOutcome.Failed(response.reason ?: "Unexpected answer: ${response.result}")
        }
    }
}
