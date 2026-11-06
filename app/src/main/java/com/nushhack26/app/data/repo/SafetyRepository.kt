package com.nushhack26.app.data.repo

import androidx.room.withTransaction
import com.nushhack26.app.crypto.Canonical
import com.nushhack26.app.crypto.Roster
import com.nushhack26.app.crypto.Signatures
import com.nushhack26.app.data.local.AlertEntity
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.MissingReportEntity
import com.nushhack26.app.data.local.OutboxEntry
import com.nushhack26.app.data.local.ResolutionEntity
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.remote.toPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Everything the safety officer's screens read, as last synced to this phone. */
data class SafetyData(
    val alerts: List<AlertEntity> = emptyList(),
    val items: List<ItemEntity> = emptyList(),
    val handovers: List<HandoverEntity> = emptyList(),
    val resolutions: List<ResolutionEntity> = emptyList(),
    val roster: Roster = Roster(),
)

/** The outbox and the records its entries carry. */
data class OutboxData(
    val entries: List<OutboxEntry> = emptyList(),
    val handovers: List<HandoverEntity> = emptyList(),
    val resolutions: List<ResolutionEntity> = emptyList(),
    val missingReports: List<MissingReportEntity> = emptyList(),
    val items: List<ItemEntity> = emptyList(),
    val roster: Roster = Roster(),
)

/** The radiation safety officer's Alerts and Fork screens, and the Outbox (card 06). */
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

    fun observeSafetyData(): Flow<SafetyData> = combine(
        observeAlerts(), observeItems(), observeHandovers(), observeResolutions(), observeRoster(),
    ) { alerts, items, handovers, resolutions, roster ->
        SafetyData(alerts, items, handovers, resolutions, roster.copy(items = items))
    }

    fun observeOutbox(): Flow<OutboxData> = combine(
        db.outboxDao().observeAll(),
        observeHandovers(),
        observeResolutions(),
        db.missingReportDao().observeAll(),
        combine(observeItems(), observeRoster()) { items, roster -> roster.copy(items = items) },
    ) { entries, handovers, resolutions, reports, roster ->
        OutboxData(entries, handovers, resolutions, reports, roster.items, roster)
    }

    /** Saves a signed decision with its outbox entry, in one transaction. */
    suspend fun saveResolution(resolution: ResolutionEntity) {
        db.withTransaction {
            db.resolutionDao().insert(resolution.copy(syncStatus = SyncMark.WAITING))
            db.outboxDao().enqueueInsert(ServerTables.RESOLUTIONS, resolution.id, resolution.toPayload())
        }
        requestSync()
    }
}

/** Both signatures on a handover, checked on this phone against the roster's keys. */
data class SignatureCheck(
    val giverOk: Boolean,
    val receiverOk: Boolean,
    val giverKeyFp: String?,
    val receiverKeyFp: String?,
) {
    val allOk: Boolean get() = giverOk && receiverOk
}

/** Checks the giver's offer signature, the receiver's acceptance signature and the record hash (docs/sync.md A.2). */
fun checkSignatures(h: HandoverEntity, roster: Roster): SignatureCheck {
    val giver = roster.deviceOf(h.fromPersonId, h.fromDeviceId)
    val receiver = roster.deviceOf(h.toPersonId, h.toDeviceId)
    val giverOk = giver != null && h.giverSig != null &&
        Signatures.verify(giver.publicKey, Canonical.offerString(h), h.giverSig)
    val receiverOk = receiver != null && h.receiverSig != null &&
        Signatures.verify(receiver.publicKey, Canonical.acceptanceString(h), h.receiverSig) &&
        h.recordHash == Canonical.recordHash(h)
    return SignatureCheck(giverOk, receiverOk, giver?.keyFp, receiver?.keyFp)
}

/** "H1" for the first handover after the register record, "H2" for the next, and so on. Null for the register record. */
fun handoverNumber(id: String?, handovers: List<HandoverEntity>): Int? {
    val byId = handovers.associateBy { it.id }
    var cursor = byId[id] ?: return null
    if (cursor.kind == HandoverKind.REGISTER) return null
    var count = 0
    val seen = mutableSetOf<String>()
    while (seen.add(cursor.id) && cursor.kind != HandoverKind.REGISTER) {
        count++
        cursor = byId[cursor.prevId] ?: break
    }
    return count
}

/** The person whose enrolled phone pushed [createdBy]'s session: "delivered by Wei Ming's phone". */
fun deliveredBy(createdBy: String?, roster: Roster) =
    createdBy?.let { uid -> roster.devices.firstOrNull { it.createdBy == uid } }?.let { roster.person(it.personId) }
