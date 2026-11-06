package com.nushhack26.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// The phone's copy of every server table (docs/sync.md section B), plus the phone-only
// columns that track sync. Times the server sets are epoch milliseconds here.

object Roles {
    const val STORE_OFFICER = "store_officer"
    const val RADIOGRAPHER = "radiographer"
    const val RSO = "rso"
}

object HandoverKind {
    const val REGISTER = "register"
    const val ISSUE = "issue"
    const val TRANSFER = "transfer"
    const val RETURN = "return"
}

/** The status the server gives a handover. Null on the phone until it has synced. */
object ServerStatus {
    const val ACCEPTED = "accepted"
    const val FORKED = "forked"
    const val OVERRULED = "overruled"
    const val ORPHAN = "orphan"
}

/** Where a record stands with the server, from this phone's point of view. */
object SyncMark {
    const val WAITING = "waiting"
    const val SYNCED = "synced"
    const val REJECTED = "rejected"
}

/** How a handover reached this phone. */
object Origin {
    const val MINE = "mine"
    const val RECEIVED_IN_OFFER = "received_in_offer"
    const val PULLED = "pulled"
}

object AlertKind {
    const val FORK = "fork"
    const val SEAL_MISMATCH = "seal_mismatch"
    const val ORPHAN = "orphan"
    const val REJECTED = "rejected"
}

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String,
    val name: String,
    val role: String,
    val staffNo: String,
    val neaRegNo: String? = null,
    val active: Boolean = true,
    val serverSeq: Long? = null,
    /** When the person's authorisation to hold sealed sources lapses (docs/sync.md A.8). Null: no expiry. */
    val authorisedUntilMs: Long? = null,
)

@Entity(tableName = "devices", indices = [Index("personId")])
data class DeviceEntity(
    @PrimaryKey val id: String,
    val personId: String,
    /** X.509 SubjectPublicKeyInfo, base64url without padding. */
    val publicKey: String,
    val keyFp: String,
    val keySecurity: String,
    val appVersion: String = "",
    val performedAt: Long,
    val createdBy: String? = null,
    val serverSeq: Long? = null,
    val syncStatus: String = SyncMark.WAITING,
    val syncReason: String? = null,
    /** When the safety officer withdrew this device (docs/sync.md A.12); null while it's in use. */
    val revokedAtMs: Long? = null,
)

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val code: String,
    val sourceSerial: String,
    val isotope: String,
    val sealNo: String,
    val storeName: String,
    val serverSeq: Long? = null,
    /** The name people say ("Kestrel"); empty until synced. Display only, never signed. */
    val name: String = "",
) {
    /** The name if it has one, else the code: "Kestrel", or "C-12". */
    val displayName: String get() = name.ifBlank { code }
}

/** One link in a camera's custody chain. The signed fields never change once complete. */
@Entity(tableName = "handovers", indices = [Index("itemId"), Index("prevId"), Index("nonce")])
data class HandoverEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val kind: String,
    val prevId: String? = null,
    val prevHash: String? = null,
    val fromPersonId: String? = null,
    val fromDeviceId: String? = null,
    val toPersonId: String,
    val toDeviceId: String? = null,
    val sealNo: String? = null,
    val dueMs: Long? = null,
    val nonce: String? = null,
    val offeredMs: Long? = null,
    val expiresMs: Long? = null,
    val acceptedMs: Long? = null,
    val giverSig: String? = null,
    val receiverSig: String? = null,
    /** Empty until the receiver has signed. */
    val recordHash: String = "",
    // Set by the server.
    val status: String? = null,
    val statusReason: String? = null,
    val serverSeq: Long? = null,
    val receivedAt: Long? = null,
    val createdBy: String? = null,
    // Phone only.
    val syncStatus: String = SyncMark.WAITING,
    val syncReason: String? = null,
    val origin: String = Origin.MINE,
    /** Demo only: the tampered phone hides handovers it gave (docs/sync.md A.5). */
    val hiddenByTamper: Boolean = false,
)

/** Written only by the server; phones pull it. */
@Entity(tableName = "alerts", indices = [Index("itemId")])
data class AlertEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val itemId: String? = null,
    /** The person the alert names, for example the giver of a fork. */
    val personId: String? = null,
    val handoverIds: List<String> = emptyList(),
    val forkPrevId: String? = null,
    val reason: String,
    val opTable: String? = null,
    /** The refused record as JSON, for a rejected alert. */
    val opPayload: String? = null,
    val resolutionId: String? = null,
    val closedAt: Long? = null,
    val performedAt: Long? = null,
    val serverSeq: Long? = null,
)

@Entity(tableName = "resolutions", indices = [Index("alertId"), Index("itemId")])
data class ResolutionEntity(
    @PrimaryKey val id: String,
    val alertId: String,
    val itemId: String,
    val chosenHandoverId: String,
    val decidedByPersonId: String,
    val decidedByDeviceId: String,
    val note: String = "",
    val decidedMs: Long,
    val sig: String,
    val createdBy: String? = null,
    val serverSeq: Long? = null,
    val syncStatus: String = SyncMark.WAITING,
    val syncReason: String? = null,
)

@Entity(tableName = "missing_reports", indices = [Index("itemId")])
data class MissingReportEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val reportedByPersonId: String,
    val reportedByDeviceId: String,
    val lastHandoverId: String? = null,
    val note: String = "",
    val reportedMs: Long,
    val serverSeq: Long? = null,
    val syncStatus: String = SyncMark.WAITING,
    val syncReason: String? = null,
)

/** Phone only: an offer the giver has signed and shown, waiting for the acceptance scan. */
@Entity(tableName = "pending_offers", indices = [Index("itemId")])
data class PendingOfferEntity(
    /** The handover id the offer will become. */
    @PrimaryKey val id: String,
    val itemId: String,
    val toPersonId: String,
    /** The signed offer as a [HandoverEntity] in server-row JSON (no receiver half yet). */
    val offerJson: String,
    /** The record that made the giver the holder, or null when it's a register record. */
    val proofId: String? = null,
    val expiresMs: Long,
    val createdAt: Long,
)

class Converters {
    private val json = Json
    private val listSerializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromList(value: List<String>): String = json.encodeToString(listSerializer, value)

    @TypeConverter
    fun toList(value: String): List<String> = json.decodeFromString(listSerializer, value)
}
