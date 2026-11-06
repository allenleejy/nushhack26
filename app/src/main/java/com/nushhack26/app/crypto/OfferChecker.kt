package com.nushhack26.app.crypto

import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.repo.Identity

/** One line of the receiver's checklist, with the text from docs/spec.md section 3, R2. */
data class CheckLine(val ok: Boolean, val text: String)

data class OfferCheck(
    val lines: List<CheckLine>,
    val passed: Boolean,
    /** True when the only failures are the receiver, the expiry or the one-time number. */
    val looksLikeCopy: Boolean,
)

/** The offline checks (docs/sync.md A.4). Card 01 fills in the bodies. */
object OfferChecker {
    /** The six checks, in order, against this phone's roster and records. */
    fun check(offer: ScannedCode.Offer, me: Identity, roster: Roster, local: List<HandoverEntity>, nowMs: Long): OfferCheck =
        TODO("card 01")

    /** "Seal S-40117 matches C-12", or the mismatch. */
    fun checkSeal(seal: ScannedCode.Seal, item: ItemEntity): CheckLine = TODO("card 01")

    /** On the giver's phone: is this the named receiver's valid acceptance of [offer]? */
    fun checkAcceptance(acceptance: ScannedCode.Acceptance, offer: HandoverEntity, roster: Roster): CheckLine =
        TODO("card 01")
}
