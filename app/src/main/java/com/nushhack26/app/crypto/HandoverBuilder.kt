package com.nushhack26.app.crypto

import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.repo.Identity

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
    fun newOffer(head: HandoverEntity, giver: Identity, receiver: PersonEntity, dueMs: Long?, nowMs: Long): HandoverEntity =
        TODO("card 01")

    /** The offer with the giver's signature over [Canonical.offerString]. */
    fun withGiverSig(offer: HandoverEntity, giverSig: String): HandoverEntity = TODO("card 01")

    /** The receiver's half: their device, the seal they scanned, and the time. Unsigned. */
    fun acceptance(offer: HandoverEntity, receiverDeviceId: String, sealNo: String, nowMs: Long): HandoverEntity =
        TODO("card 01")

    /** The complete record: the receiver's signature over [Canonical.acceptanceString], and the record hash. */
    fun complete(accepted: HandoverEntity, receiverSig: String): HandoverEntity = TODO("card 01")

    /** On the giver's phone: the pending offer plus the scanned acceptance, as the complete record. */
    fun fromAcceptance(offer: HandoverEntity, acceptance: ScannedCode.Acceptance): HandoverEntity = TODO("card 01")
}
