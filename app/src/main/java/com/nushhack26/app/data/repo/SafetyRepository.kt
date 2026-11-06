package com.nushhack26.app.data.repo

import androidx.room.withTransaction
import com.nushhack26.app.crypto.Roster
import com.nushhack26.app.data.local.AlertEntity
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.ResolutionEntity
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.remote.toPayload
import kotlinx.coroutines.flow.Flow

/** The radiation safety officer's Alerts and Fork screens (card 06). */
class SafetyRepository(
    private val db: AppDatabase,
    private val requestSync: () -> Unit,
) {
    fun observeAlerts(): Flow<List<AlertEntity>> = db.alertDao().observeAll()

    fun observeAlert(id: String): Flow<AlertEntity?> = db.alertDao().observe(id)

    fun observeItems(): Flow<List<ItemEntity>> = db.itemDao().observeAll()

    fun observeHandovers(): Flow<List<HandoverEntity>> = db.handoverDao().observeAll()

    fun observeResolutions(): Flow<List<ResolutionEntity>> = db.resolutionDao().observeAll()

    fun observeRoster(): Flow<Roster> = db.observeRoster()

    /** Saves a signed decision with its outbox entry, in one transaction. */
    suspend fun saveResolution(resolution: ResolutionEntity) {
        db.withTransaction {
            db.resolutionDao().insert(resolution.copy(syncStatus = SyncMark.WAITING))
            db.outboxDao().enqueueInsert(ServerTables.RESOLUTIONS, resolution.id, resolution.toPayload())
        }
        requestSync()
    }
}
