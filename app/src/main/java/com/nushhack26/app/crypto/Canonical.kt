package com.nushhack26.app.crypto

import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.ResolutionEntity
import java.security.MessageDigest
import java.util.Locale

/**
 * The exact strings that get signed and hashed (docs/sync.md A.2). The server's
 * sync_push builds the same strings, so they must match byte for byte. Card 01.
 */
object Canonical {
    /** CS1-OFFER|…, signed by the giver. */
    fun offerString(h: HandoverEntity): String = fields(
        "CS1-OFFER", h.id, h.itemId, h.kind, h.prevId, h.prevHash, h.fromPersonId,
        h.fromDeviceId, h.toPersonId, h.dueMs, h.nonce, h.offeredMs, h.expiresMs,
    )

    /** CS1-ACCEPT|<offer>|giver_sig|to_device_id|seal_no|accepted_ms, signed by the receiver. */
    fun acceptanceString(h: HandoverEntity): String = fields(
        "CS1-ACCEPT", offerString(h), h.giverSig, h.toDeviceId, h.sealNo, h.acceptedMs,
    )

    /** hex(SHA-256(acceptance + "|" + receiver_sig)). */
    fun recordHash(h: HandoverEntity): String = sha256Hex(fields(acceptanceString(h), h.receiverSig))

    /** The hash of a server-made register record. */
    fun registerHash(id: String, itemId: String, toPersonId: String): String =
        sha256Hex(fields("CS1-REGISTER", id, itemId, toPersonId))

    /** CS1-RESOLVE|…, signed by the safety officer. */
    fun resolutionString(r: ResolutionEntity): String = fields(
        "CS1-RESOLVE", r.id, r.alertId, r.itemId, r.chosenHandoverId,
        r.decidedByPersonId, r.decidedByDeviceId, sha256Hex(r.note), r.decidedMs,
    )

    /** Six digits from a record hash, shown as "48 21 07". */
    fun matchCode(recordHash: String): String {
        require(recordHash.matches(Regex("[0-9a-f]{64}"))) { "Invalid record hash" }
        return (recordHash.take(8).toLong(16) % 1_000_000)
            .toString().padStart(6, '0').chunked(2).joinToString(" ")
    }

    fun sha256Hex(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private fun fields(vararg values: Any?): String = values.joinToString("|") { it?.toString().orEmpty() }
}
