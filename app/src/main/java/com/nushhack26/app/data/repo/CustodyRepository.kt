package com.nushhack26.app.data.repo

import com.nushhack26.app.crypto.CustodyState
import com.nushhack26.app.crypto.CustodyStatus
import com.nushhack26.app.crypto.Roster
import com.nushhack26.app.crypto.deriveCustody
import com.nushhack26.app.data.local.AlertEntity
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.PendingOfferEntity
import com.nushhack26.app.data.local.ResolutionEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Every camera with who holds it, as of [nowMs]. */
data class CustodyBoard(
    val nowMs: Long,
    val states: List<CustodyState>,
    val handovers: List<HandoverEntity>,
    val roster: Roster,
)

/** One camera's chain, for the timeline. [item] is null until the camera has synced. */
data class ItemChain(
    val nowMs: Long,
    val item: ItemEntity?,
    val state: CustodyState?,
    val handovers: List<HandoverEntity>,
    val resolutions: List<ResolutionEntity>,
    val roster: Roster,
)

/** A camera this phone's person holds, with the head record that made them the holder. */
data class HeldDue(val itemId: String, val code: String, val headId: String, val dueMs: Long)

/** Store, My custody and the timeline (card 05). Read-only: handovers are saved by cards 03 and 04. */
class CustodyRepository(private val db: AppDatabase) {
    fun observeItems(): Flow<List<ItemEntity>> = db.itemDao().observeAll()

    fun observeHandovers(): Flow<List<HandoverEntity>> = db.handoverDao().observeAll()

    fun observeChain(itemId: String): Flow<List<HandoverEntity>> = db.handoverDao().observeForItem(itemId)

    fun observeResolutions(): Flow<List<ResolutionEntity>> = db.resolutionDao().observeAll()

    fun observeAlerts(): Flow<List<AlertEntity>> = db.alertDao().observeAll()

    fun observePendingOffers(): Flow<List<PendingOfferEntity>> = db.pendingOfferDao().observeAll()

    fun observeRoster(): Flow<Roster> = db.observeRoster()

    /**
     * Every camera with its [CustodyState], re-derived every second while collected, so
     * countdowns and overdue update. [serverView] uses synced records only.
     */
    fun observeBoard(serverView: Boolean = false): Flow<CustodyBoard> = combine(
        observeItems(), observeHandovers(), observeResolutions(), observeRoster(), ticker(),
    ) { items, handovers, resolutions, roster, now ->
        CustodyBoard(now, items.map { deriveCustody(it, handovers, resolutions, now, serverView) }, handovers, roster)
    }

    /** One camera's records and resolutions, for the timeline. */
    fun observeItemChain(itemId: String): Flow<ItemChain> = combine(
        db.itemDao().observe(itemId), observeChain(itemId), observeResolutions(), observeRoster(), ticker(),
    ) { item, handovers, resolutions, roster, now ->
        val mine = resolutions.filter { it.itemId == itemId }
        ItemChain(now, item, item?.let { deriveCustody(it, handovers, mine, now) }, handovers, mine, roster)
    }

    /** Complete handovers I gave, newest first: my proof that I handed a camera over. */
    fun observeReceipts(personId: String): Flow<List<HandoverEntity>> = observeHandovers().map { all -> receiptsOf(all, personId) }

    /** My offers still waiting for their acceptance scan. */
    fun observeMyPendingOffers(): Flow<List<PendingOfferEntity>> =
        combine(observePendingOffers(), observeHandovers()) { offers, handovers ->
            val completed = handovers.filter { it.receiverSig != null }.map { it.id }.toSet()
            offers.filter { it.id !in completed }
        }

    /** The cameras [personId] holds with a due time. Changes only when custody does, not every second. */
    fun observeHeldDues(personId: String): Flow<List<HeldDue>> = combine(
        observeItems(), observeHandovers(), observeResolutions(),
    ) { items, handovers, resolutions -> heldDuesOf(items, handovers, resolutions, personId) }.distinctUntilChanged()

    suspend fun heldDues(personId: String): List<HeldDue> =
        heldDuesOf(db.itemDao().all(), db.handoverDao().all(), db.resolutionDao().all(), personId)

    companion object {
        fun ticker(periodMs: Long = 1_000): Flow<Long> = flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(periodMs)
            }
        }

        fun receiptsOf(handovers: List<HandoverEntity>, personId: String): List<HandoverEntity> = handovers
            .filter { it.fromPersonId == personId && it.receiverSig != null && !it.hiddenByTamper }
            .sortedByDescending { it.acceptedMs ?: it.offeredMs ?: 0 }

        fun heldDuesOf(
            items: List<ItemEntity>,
            handovers: List<HandoverEntity>,
            resolutions: List<ResolutionEntity>,
            personId: String,
        ): List<HeldDue> = items.mapNotNull { item ->
            // Status only decides Out vs Overdue here, so any time works.
            val state = deriveCustody(item, handovers, resolutions, 0)
            val head = state.head
            val due = state.dueMs
            if ((state.status == CustodyStatus.Out || state.status == CustodyStatus.Overdue) &&
                state.holderId == personId && head != null && due != null
            ) HeldDue(item.id, item.code, head.id, due) else null
        }
    }
}
