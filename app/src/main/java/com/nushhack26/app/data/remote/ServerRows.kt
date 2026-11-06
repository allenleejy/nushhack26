package com.nushhack26.app.data.remote

import com.nushhack26.app.data.local.AlertEntity
import com.nushhack26.app.data.local.DeviceEntity
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.MissingReportEntity
import com.nushhack26.app.data.local.Origin
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.ResolutionEntity
import com.nushhack26.app.data.local.SyncMark
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.time.OffsetDateTime

// The server's rows, column for column (docs/sync.md section B), and the mappers to and
// from the Room entities. A record's outbox payload is its row as JSON.

val ServerJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Parses a Postgres timestamptz as sent by to_jsonb, such as 2026-11-06T14:10:05.448123+00:00. */
fun parseServerTime(value: String?): Long? =
    value?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }

fun Long.toServerTime(): String = Instant.ofEpochMilli(this).toString()

@Serializable
data class PersonRow(
    val id: String,
    val name: String,
    val role: String,
    @SerialName("staff_no") val staffNo: String,
    @SerialName("nea_reg_no") val neaRegNo: String? = null,
    val active: Boolean = true,
    @SerialName("server_seq") val serverSeq: Long? = null,
    @SerialName("authorised_until_ms") val authorisedUntilMs: Long? = null,
)

@Serializable
data class DeviceRow(
    val id: String,
    @SerialName("person_id") val personId: String,
    @SerialName("public_key") val publicKey: String,
    @SerialName("key_fp") val keyFp: String,
    @SerialName("key_security") val keySecurity: String,
    @SerialName("app_version") val appVersion: String = "",
    @SerialName("performed_at") val performedAt: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("server_seq") val serverSeq: Long? = null,
    @SerialName("revoked_at") val revokedAt: String? = null,
)

@Serializable
data class ItemRow(
    val id: String,
    val code: String,
    @SerialName("source_serial") val sourceSerial: String,
    val isotope: String,
    @SerialName("seal_no") val sealNo: String,
    @SerialName("store_name") val storeName: String,
    @SerialName("server_seq") val serverSeq: Long? = null,
)

@Serializable
data class HandoverRow(
    val id: String,
    @SerialName("item_id") val itemId: String,
    val kind: String,
    @SerialName("prev_id") val prevId: String? = null,
    @SerialName("prev_hash") val prevHash: String? = null,
    @SerialName("from_person_id") val fromPersonId: String? = null,
    @SerialName("from_device_id") val fromDeviceId: String? = null,
    @SerialName("to_person_id") val toPersonId: String,
    @SerialName("to_device_id") val toDeviceId: String? = null,
    @SerialName("seal_no") val sealNo: String? = null,
    @SerialName("due_ms") val dueMs: Long? = null,
    val nonce: String? = null,
    @SerialName("offered_ms") val offeredMs: Long? = null,
    @SerialName("expires_ms") val expiresMs: Long? = null,
    @SerialName("accepted_ms") val acceptedMs: Long? = null,
    @SerialName("giver_sig") val giverSig: String? = null,
    @SerialName("receiver_sig") val receiverSig: String? = null,
    @SerialName("record_hash") val recordHash: String = "",
    val status: String? = null,
    @SerialName("status_reason") val statusReason: String? = null,
    @SerialName("server_seq") val serverSeq: Long? = null,
    @SerialName("received_at") val receivedAt: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
)

@Serializable
data class AlertRow(
    val id: String,
    val kind: String,
    @SerialName("item_id") val itemId: String? = null,
    @SerialName("person_id") val personId: String? = null,
    @SerialName("handover_ids") val handoverIds: List<String> = emptyList(),
    @SerialName("fork_prev_id") val forkPrevId: String? = null,
    val reason: String,
    @SerialName("op_table") val opTable: String? = null,
    @SerialName("op_payload") val opPayload: JsonElement? = null,
    @SerialName("resolution_id") val resolutionId: String? = null,
    @SerialName("closed_at") val closedAt: String? = null,
    @SerialName("performed_at") val performedAt: String? = null,
    @SerialName("server_seq") val serverSeq: Long? = null,
)

@Serializable
data class ResolutionRow(
    val id: String,
    @SerialName("alert_id") val alertId: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("chosen_handover_id") val chosenHandoverId: String,
    @SerialName("decided_by_person_id") val decidedByPersonId: String,
    @SerialName("decided_by_device_id") val decidedByDeviceId: String,
    val note: String = "",
    @SerialName("decided_ms") val decidedMs: Long,
    val sig: String,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("server_seq") val serverSeq: Long? = null,
)

@Serializable
data class MissingReportRow(
    val id: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("reported_by_person_id") val reportedByPersonId: String,
    @SerialName("reported_by_device_id") val reportedByDeviceId: String,
    @SerialName("last_handover_id") val lastHandoverId: String? = null,
    val note: String = "",
    @SerialName("reported_ms") val reportedMs: Long,
    @SerialName("server_seq") val serverSeq: Long? = null,
)

// Server row → entity. Pulled rows count as synced; the sync engine keeps the local-only
// columns of a record the phone already has (docs/sync.md section E).

fun PersonRow.toEntity() = PersonEntity(id, name, role, staffNo, neaRegNo, active, serverSeq, authorisedUntilMs)

fun DeviceRow.toEntity() = DeviceEntity(
    id = id, personId = personId, publicKey = publicKey, keyFp = keyFp, keySecurity = keySecurity,
    appVersion = appVersion, performedAt = parseServerTime(performedAt) ?: 0L, createdBy = createdBy,
    serverSeq = serverSeq, syncStatus = SyncMark.SYNCED, revokedAtMs = parseServerTime(revokedAt),
)

fun ItemRow.toEntity() = ItemEntity(id, code, sourceSerial, isotope, sealNo, storeName, serverSeq)

fun HandoverRow.toEntity() = HandoverEntity(
    id = id, itemId = itemId, kind = kind, prevId = prevId, prevHash = prevHash,
    fromPersonId = fromPersonId, fromDeviceId = fromDeviceId, toPersonId = toPersonId, toDeviceId = toDeviceId,
    sealNo = sealNo, dueMs = dueMs, nonce = nonce, offeredMs = offeredMs, expiresMs = expiresMs,
    acceptedMs = acceptedMs, giverSig = giverSig, receiverSig = receiverSig, recordHash = recordHash,
    status = status, statusReason = statusReason, serverSeq = serverSeq,
    receivedAt = parseServerTime(receivedAt), createdBy = createdBy,
    syncStatus = SyncMark.SYNCED, origin = Origin.PULLED,
)

fun AlertRow.toEntity() = AlertEntity(
    id = id, kind = kind, itemId = itemId, personId = personId, handoverIds = handoverIds,
    forkPrevId = forkPrevId, reason = reason, opTable = opTable, opPayload = opPayload?.toString(),
    resolutionId = resolutionId, closedAt = parseServerTime(closedAt), performedAt = parseServerTime(performedAt),
    serverSeq = serverSeq,
)

fun ResolutionRow.toEntity() = ResolutionEntity(
    id = id, alertId = alertId, itemId = itemId, chosenHandoverId = chosenHandoverId,
    decidedByPersonId = decidedByPersonId, decidedByDeviceId = decidedByDeviceId, note = note,
    decidedMs = decidedMs, sig = sig, createdBy = createdBy, serverSeq = serverSeq, syncStatus = SyncMark.SYNCED,
)

fun MissingReportRow.toEntity() = MissingReportEntity(
    id = id, itemId = itemId, reportedByPersonId = reportedByPersonId, reportedByDeviceId = reportedByDeviceId,
    lastHandoverId = lastHandoverId, note = note, reportedMs = reportedMs, serverSeq = serverSeq,
    syncStatus = SyncMark.SYNCED,
)

// Entity → server row, for outbox payloads. Server-owned columns are sent but ignored.

fun DeviceEntity.toRow() = DeviceRow(
    id = id, personId = personId, publicKey = publicKey, keyFp = keyFp, keySecurity = keySecurity,
    appVersion = appVersion, performedAt = performedAt.toServerTime(),
)

fun HandoverEntity.toRow() = HandoverRow(
    id = id, itemId = itemId, kind = kind, prevId = prevId, prevHash = prevHash,
    fromPersonId = fromPersonId, fromDeviceId = fromDeviceId, toPersonId = toPersonId, toDeviceId = toDeviceId,
    sealNo = sealNo, dueMs = dueMs, nonce = nonce, offeredMs = offeredMs, expiresMs = expiresMs,
    acceptedMs = acceptedMs, giverSig = giverSig, receiverSig = receiverSig, recordHash = recordHash,
)

fun ResolutionEntity.toRow() = ResolutionRow(
    id = id, alertId = alertId, itemId = itemId, chosenHandoverId = chosenHandoverId,
    decidedByPersonId = decidedByPersonId, decidedByDeviceId = decidedByDeviceId, note = note,
    decidedMs = decidedMs, sig = sig,
)

fun MissingReportEntity.toRow() = MissingReportRow(
    id = id, itemId = itemId, reportedByPersonId = reportedByPersonId, reportedByDeviceId = reportedByDeviceId,
    lastHandoverId = lastHandoverId, note = note, reportedMs = reportedMs,
)

fun DeviceEntity.toPayload(): String = ServerJson.encodeToString(DeviceRow.serializer(), toRow())
fun HandoverEntity.toPayload(): String = ServerJson.encodeToString(HandoverRow.serializer(), toRow())
fun ResolutionEntity.toPayload(): String = ServerJson.encodeToString(ResolutionRow.serializer(), toRow())
fun MissingReportEntity.toPayload(): String = ServerJson.encodeToString(MissingReportRow.serializer(), toRow())

/** Reads a handover back from its row JSON, for example a pending offer's [offerJson]. */
fun handoverFromJson(json: String): HandoverEntity =
    ServerJson.decodeFromString(HandoverRow.serializer(), json).toEntity().copy(syncStatus = SyncMark.WAITING, origin = Origin.MINE)

fun payloadObject(payload: String): JsonObject = ServerJson.parseToJsonElement(payload).jsonObject
