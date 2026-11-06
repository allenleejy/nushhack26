package com.nushhack26.app.data.repo

import androidx.room.withTransaction
import com.nushhack26.app.crypto.Roster
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.Origin
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.remote.toPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Receive (card 04): the receiver's side. */
class ReceiveRepository(
    private val db: AppDatabase,
    private val requestSync: () -> Unit,
) {
    fun observeRoster(): Flow<Roster> = db.observeRoster()

    /** People, device keys and items, so the checks can name the camera. */
    suspend fun roster(): Roster = db.roster().copy(items = db.itemDao().all())

    /** One handover as it changes, for the "Not synced yet" mark after accepting. */
    fun observeHandover(itemId: String, id: String): Flow<HandoverEntity?> =
        db.handoverDao().observeForItem(itemId).map { list -> list.firstOrNull { it.id == id } }

    suspend fun item(itemId: String): ItemEntity? = db.itemDao().get(itemId)

    /** Every handover on this phone, for the one-time-number and history checks. */
    suspend fun localHandovers(): List<HandoverEntity> = db.handoverDao().all()

    /**
     * Saves the giver's proof record (if this phone doesn't have it) and the new record,
     * each with an outbox entry, in one transaction. Either phone can then deliver both.
     */
    suspend fun saveAccepted(proof: HandoverEntity?, record: HandoverEntity) {
        db.withTransaction {
            if (proof != null && db.handoverDao().get(proof.id) == null) {
                db.handoverDao().insert(proof.copy(origin = Origin.RECEIVED_IN_OFFER, syncStatus = SyncMark.WAITING))
                db.outboxDao().enqueueInsert(ServerTables.HANDOVERS, proof.id, proof.toPayload())
            }
            if (db.handoverDao().get(record.id) == null) {
                db.handoverDao().insert(record.copy(origin = Origin.MINE, syncStatus = SyncMark.WAITING))
                db.outboxDao().enqueueInsert(ServerTables.HANDOVERS, record.id, record.toPayload())
            }
        }
        requestSync()
    }
}
