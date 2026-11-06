package com.nushhack26.app.data.repo

import com.nushhack26.app.crypto.Roster
import com.nushhack26.app.data.local.AlertEntity
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.PendingOfferEntity
import com.nushhack26.app.data.local.ResolutionEntity
import kotlinx.coroutines.flow.Flow

/** Store, My custody and the timeline (card 05). Read-only: handovers are saved by cards 03 and 04. */
class CustodyRepository(private val db: AppDatabase) {
    fun observeItems(): Flow<List<ItemEntity>> = db.itemDao().observeAll()

    fun observeHandovers(): Flow<List<HandoverEntity>> = db.handoverDao().observeAll()

    fun observeChain(itemId: String): Flow<List<HandoverEntity>> = db.handoverDao().observeForItem(itemId)

    fun observeResolutions(): Flow<List<ResolutionEntity>> = db.resolutionDao().observeAll()

    fun observeAlerts(): Flow<List<AlertEntity>> = db.alertDao().observeAll()

    fun observePendingOffers(): Flow<List<PendingOfferEntity>> = db.pendingOfferDao().observeAll()

    fun observeRoster(): Flow<Roster> = db.observeRoster()
}
