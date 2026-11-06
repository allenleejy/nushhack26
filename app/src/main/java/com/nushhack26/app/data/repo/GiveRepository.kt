package com.nushhack26.app.data.repo

import androidx.room.withTransaction
import com.nushhack26.app.crypto.Roster
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.Origin
import com.nushhack26.app.data.local.PendingOfferEntity
import com.nushhack26.app.data.local.ResolutionEntity
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.remote.toPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Hand over (card 03): the giver's side. */
class GiveRepository(
    private val db: AppDatabase,
    private val identity: IdentityRepository,
    private val requestSync: () -> Unit,
) {
    fun observeItem(itemId: String): Flow<ItemEntity?> = db.itemDao().observe(itemId)

    fun observeChain(itemId: String): Flow<List<HandoverEntity>> = db.handoverDao().observeForItem(itemId)

    fun observeResolutions(): Flow<List<ResolutionEntity>> = db.resolutionDao().observeAll()

    fun observeRoster(): Flow<Roster> = db.observeRoster()

    /** One handover by id: the receiver's record can arrive by sync before the relay answers (A.10). */
    fun handover(id: String): Flow<HandoverEntity?> =
        db.handoverDao().observeAll().map { all -> all.firstOrNull { it.id == id } }.distinctUntilChanged()

    fun observePendingOffer(itemId: String): Flow<PendingOfferEntity?> = db.pendingOfferDao().observeLatestForItem(itemId)

    /** Saves the signed offer before its QR is shown, so it survives the app closing. */
    suspend fun savePendingOffer(offer: HandoverEntity, proofId: String?) {
        db.pendingOfferDao().insert(
            PendingOfferEntity(
                id = offer.id,
                itemId = offer.itemId,
                toPersonId = offer.toPersonId,
                offerJson = offer.toPayload(),
                proofId = proofId,
                expiresMs = offer.expiresMs ?: 0L,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun cancelPendingOffer(id: String) = db.pendingOfferDao().delete(id)

    /**
     * Saves the complete handover after the acceptance scan, with its outbox entry, and
     * clears the pending offer. On the demo's tampered phone the record is hidden and never
     * queued (docs/sync.md A.5).
     */
    suspend fun completeHandover(record: HandoverEntity) {
        db.withTransaction {
            if (db.handoverDao().get(record.id) == null) {
                val tampered = identity.isTampered()
                db.handoverDao().insert(
                    record.copy(origin = Origin.MINE, syncStatus = SyncMark.WAITING, hiddenByTamper = tampered),
                )
                if (!tampered) db.outboxDao().enqueueInsert(ServerTables.HANDOVERS, record.id, record.toPayload())
            }
            db.pendingOfferDao().delete(record.id)
        }
        requestSync()
    }
}
