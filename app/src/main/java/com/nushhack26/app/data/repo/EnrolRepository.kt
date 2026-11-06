package com.nushhack26.app.data.repo

import androidx.room.withTransaction
import com.nushhack26.app.crypto.KeyInfo
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.local.DeviceEntity
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.remote.toPayload
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Enrol (card 06): link this phone to one person and put its public key on the roster. */
class EnrolRepository(
    private val db: AppDatabase,
    private val identity: IdentityRepository,
    private val requestSync: () -> Unit,
) {
    fun observeRoster(): Flow<List<PersonEntity>> = db.peopleDao().observeActive()

    /** Saves this phone's device row with its outbox entry, and sets the identity, in one transaction. */
    suspend fun enrol(person: PersonEntity, key: KeyInfo, appVersion: String): Identity {
        val device = DeviceEntity(
            id = UUID.randomUUID().toString(),
            personId = person.id,
            publicKey = key.publicKey,
            keyFp = key.keyFp,
            keySecurity = key.security,
            appVersion = appVersion,
            performedAt = System.currentTimeMillis(),
        )
        val me = Identity(person.id, device.id, person.name, person.role)
        db.withTransaction {
            db.deviceDao().insert(device)
            db.outboxDao().enqueueInsert(ServerTables.DEVICES, device.id, device.toPayload())
            identity.setIdentity(me)
        }
        requestSync()
        return me
    }
}
