package com.nushhack26.app.ui.record

import com.nushhack26.app.crypto.Canonical
import com.nushhack26.app.crypto.Roster
import com.nushhack26.app.crypto.Signatures
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.ui.alerts.shortName
import com.nushhack26.app.ui.components.ReceiptContent
import com.nushhack26.app.ui.components.ReceiptNumbers
import com.nushhack26.app.ui.components.ReceiptParty
import com.nushhack26.app.ui.components.ReceiptStamp
import com.nushhack26.app.ui.timeline.SigCheck
import com.nushhack26.app.ui.timeline.checkSignatures
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val RECORD_SYNCED = "Synced"
const val RECORD_NOT_SYNCED = "Saved on this phone · not synced yet"

/** A record signed this long before the server saw it was signed with no network. */
private const val LATE_ARRIVAL_MS = 60_000L

/** One handover printed as its record receipt (card 30): the lines and the stamp. */
fun recordReceipt(
    h: HandoverEntity,
    chain: List<HandoverEntity>,
    item: ItemEntity?,
    roster: Roster,
    nowMs: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): Pair<ReceiptContent, ReceiptStamp> {
    val register = h.kind == HandoverKind.REGISTER
    val signedMs = h.acceptedMs ?: h.offeredMs ?: h.receivedAt ?: nowMs
    val time = format("HH:mm", signedMs, zone)
    val recordNo = ReceiptNumbers.recordNo(h, chain)
    val parent = chain.firstOrNull { it.id == h.prevId }
    val receiver = nameOf(roster, h.toPersonId)
    val (signatures, signaturesOk) = when (checkSignatures(h, parent, roster)) {
        SigCheck.Valid -> "2 OF 2 VERIFIED" to true
        SigCheck.Invalid -> "SIGNATURE INVALID" to false
        SigCheck.UnknownKey -> "SIGNER NOT ON THIS PHONE" to false
        SigCheck.Unsigned -> "REGISTERED BY THE SERVER" to true
    }
    val acceptedMs = h.acceptedMs
    val receivedAt = h.receivedAt
    val arrivedLate = acceptedMs != null && receivedAt != null && receivedAt - acceptedMs > LATE_ARRIVAL_MS
    val content = ReceiptContent(
        recordNo = recordNo,
        followsNo = recordNo?.minus(1)?.takeIf { it >= 1 },
        date = format("dd.MM.yyyy", signedMs, zone),
        signedTime = time,
        itemName = item?.displayName.orEmpty(),
        itemCode = item?.code.orEmpty(),
        source = item?.let { listOf(it.sourceSerial, it.isotope).filter(String::isNotBlank).joinToString(" · ") }?.ifBlank { null },
        sealNo = h.sealNo,
        sealMatches = true,
        from = if (register) null else party(roster, h.fromPersonId, h.fromDeviceId),
        to = party(roster, h.toPersonId, h.toDeviceId),
        returnBy = h.dueMs?.let { format("HH:mm", it, zone) },
        via = null,
        heldBy = receiver,
        signatures = signatures,
        signaturesOk = signaturesOk,
        hash = h.recordHash.ifBlank { null },
        offline = h.serverSeq == null || arrivedLate,
        verifyCode = runCatching { ReceiptNumbers.verifyCode(Canonical.matchCode(h.recordHash)) }.getOrDefault(""),
        footer = if (h.serverSeq != null) RECORD_SYNCED else RECORD_NOT_SYNCED,
    )
    val stamp = if (register) {
        ReceiptStamp("REGISTERED", time, "IN STORE")
    } else {
        ReceiptStamp("COUNTERSIGNED", time, shortName(receiver).uppercase())
    }
    return content to stamp
}

private fun nameOf(roster: Roster, personId: String?): String = roster.person(personId)?.name ?: "Unknown person"

/** A signer and, when this phone has their device's public key, its fingerprint. */
private fun party(roster: Roster, personId: String?, deviceId: String?): ReceiptParty =
    ReceiptParty(nameOf(roster, personId), roster.device(deviceId)?.let { "KEY " + Signatures.keyFp(it.publicKey) })

private fun format(pattern: String, ms: Long, zone: ZoneId): String =
    DateTimeFormatter.ofPattern(pattern).withZone(zone).format(Instant.ofEpochMilli(ms))
