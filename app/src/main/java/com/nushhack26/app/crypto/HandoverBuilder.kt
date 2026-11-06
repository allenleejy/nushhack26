package com.nushhack26.app.crypto

import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.Roles
import com.nushhack26.app.data.repo.Identity
import java.security.SecureRandom
import java.util.UUID

/**
 * Builds a handover in its three stages: the giver's offer, the receiver's acceptance,
 * and the complete record with its hash (docs/sync.md A.2 and A.4). Card 01.
 */
object HandoverBuilder {
    /**
     * A new unsigned offer that follows [head], the record that made [giver] the holder.
     * Makes the id and the 16-byte nonce, sets expires_ms = now + 60 s, works out the kind
     * from the two roles, and sets due_ms (given on issue, copied on transfer, none on return).
     */
    fun newOffer(head: HandoverEntity, giver: Identity, receiver: PersonEntity, dueMs: Long?, nowMs: Long): HandoverEntity {
        require(head.toPersonId == giver.personId) { "The giver must hold this camera" }
        require(head.recordHash.isNotEmpty()) { "The head must be complete" }
        require(receiver.active && receiver.id != giver.personId) { "Choose another active person" }
        val kind = when (giver.role to receiver.role) {
            Roles.STORE_OFFICER to Roles.RADIOGRAPHER -> HandoverKind.ISSUE
            Roles.RADIOGRAPHER to Roles.RADIOGRAPHER -> HandoverKind.TRANSFER
            Roles.RADIOGRAPHER to Roles.STORE_OFFICER -> HandoverKind.RETURN
            else -> throw IllegalArgumentException("These roles cannot hand over a camera")
        }
        return HandoverEntity(
            id = UUID.randomUUID().toString(), itemId = head.itemId, kind = kind,
            prevId = head.id, prevHash = head.recordHash,
            fromPersonId = giver.personId, fromDeviceId = giver.deviceId, toPersonId = receiver.id,
            dueMs = when (kind) {
                HandoverKind.ISSUE -> requireNotNull(dueMs) { "An issue needs a due time" }
                HandoverKind.TRANSFER -> head.dueMs
                else -> null
            },
            nonce = encode(ByteArray(16).also { SecureRandom().nextBytes(it) }),
            offeredMs = nowMs, expiresMs = Math.addExact(nowMs, 60_000),
        )
    }

    /** The offer with the giver's signature over [Canonical.offerString]. */
    fun withGiverSig(offer: HandoverEntity, giverSig: String): HandoverEntity = offer.copy(giverSig = giverSig)

    /** The receiver's half: their device, the seal they scanned, and the time. Unsigned. */
    fun acceptance(offer: HandoverEntity, receiverDeviceId: String, sealNo: String, nowMs: Long): HandoverEntity {
        require('|' !in sealNo) { "A seal cannot contain |" }
        return offer.copy(
            toDeviceId = receiverDeviceId, sealNo = sealNo, acceptedMs = nowMs,
            receiverSig = null, recordHash = "",
        )
    }

    /** The complete record: the receiver's signature over [Canonical.acceptanceString], and the record hash. */
    fun complete(accepted: HandoverEntity, receiverSig: String): HandoverEntity {
        val signed = accepted.copy(receiverSig = receiverSig)
        return signed.copy(recordHash = Canonical.recordHash(signed))
    }

    /** On the giver's phone: the pending offer plus the scanned acceptance, as the complete record. */
    fun fromAcceptance(offer: HandoverEntity, acceptance: ScannedCode.Acceptance): HandoverEntity {
        require(offer.id == acceptance.id) { "This acceptance is for another handover" }
        return complete(
            acceptance(offer, acceptance.toDeviceId, acceptance.sealNo, acceptance.acceptedMs),
            acceptance.receiverSig,
        )
    }
}
