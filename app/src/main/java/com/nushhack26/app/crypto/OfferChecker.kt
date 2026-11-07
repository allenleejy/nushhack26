package com.nushhack26.app.crypto

import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.data.repo.RelayRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    /** A relayed offer is signed for 3 minutes instead of 60 s (docs/sync.md A.10). */
    private const val RELAYED_VALID_MS = RelayRepository.RELAYED_OFFER_VALID_MS

    /** The device's revocation time is the server's clock and a signature time is a phone's (docs/sync.md A.12). */
    private const val CLOCK_ALLOWANCE_MS = 15_000L

    /** The six checks, in order, against this phone's roster and records. */
    fun check(offer: ScannedCode.Offer, me: Identity, roster: Roster, local: List<HandoverEntity>, nowMs: Long): OfferCheck {
        val h = offer.offer
        val giver = roster.person(h.fromPersonId)
        val device = roster.deviceOf(h.fromPersonId, h.fromDeviceId)
        val itemCode = roster.items.firstOrNull { it.id == h.itemId }?.code ?: h.itemId
        val proof = offer.proof ?: local.firstOrNull { it.id == h.prevId && it.kind == HandoverKind.REGISTER }
        val signatureOk = giver != null && device != null && Signatures.verify(device.publicKey, Canonical.offerString(h), h.giverSig.orEmpty())
        val withdrawnAt = device?.let { roster.withdrawnAt(it.id) }?.takeIf { withdrawnBy(it, h.offeredMs) }
        val holds = proof != null && validProof(proof, h, offer.proof == null, roster, local)
        val proofWithdrawn = offer.proof?.let { withdrawnSigner(it, roster) }
        val usedBefore = local.any { it.id == h.id || (h.nonce != null && it.nonce == h.nonce) }
        val earlier = if (usedBefore) null else earlierHandover(h, local)
        val receiver = roster.person(h.toPersonId)?.name ?: h.toPersonId
        val expires = h.expiresMs
        val offered = h.offeredMs
        val valid = offered != null && expires != null && offered >= 0 && expires <= Long.MAX_VALUE - 15_000 &&
            offered <= Long.MAX_VALUE - RELAYED_VALID_MS && (expires == offered + 60_000 || expires == offered + RELAYED_VALID_MS) &&
            nowMs >= offered - 15_000 && nowMs <= expires + 15_000
        val readable = h.kind in listOf(HandoverKind.ISSUE, HandoverKind.TRANSFER, HandoverKind.RETURN) &&
            listOf(h.id, h.itemId, h.prevId, h.fromPersonId, h.fromDeviceId, h.toPersonId).all { !it.isNullOrEmpty() } &&
            h.offeredMs != null && h.expiresMs != null && h.nonce != null && h.giverSig != null
        val lines = listOf(
            CheckLine(readable, if (readable) "Handover code is readable" else "Not a handover code"),
            CheckLine(signatureOk && withdrawnAt == null, when {
                device == null || giver == null -> "Unknown signer. Sync this phone before accepting."
                !signatureOk -> "Signature doesn't match"
                withdrawnAt != null -> withdrawnText(giver.name, withdrawnAt)
                else -> "Signed by ${giver.name} · key ${Signatures.keyFp(device.publicKey)}"
            }),
            CheckLine(holds && proofWithdrawn == null, if (proofWithdrawn != null) proofWithdrawn else if (holds) {
                val holder = giver?.name ?: h.fromPersonId
                if (proof.kind == HandoverKind.REGISTER) "$holder holds $itemCode · registered in store"
                else {
                    val from = roster.person(proof.fromPersonId)?.name?.substringBefore(' ') ?: "Unknown signer"
                    val to = giver?.name?.substringBefore(' ') ?: "Unknown signer"
                    val time = proof.acceptedMs?.let { clock(it) }.orEmpty()
                    "$holder holds $itemCode · signed by $from and $to at $time"
                }
            } else "The giver doesn't hold $itemCode"),
            CheckLine(h.toPersonId == me.personId, if (h.toPersonId == me.personId) "Made out to you" else "Made out to $receiver, not you"),
            CheckLine(valid, when {
                expires == null || offered == null -> "Invalid offer time"
                nowMs > expires -> "Expired ${duration(nowMs - expires)} ago"
                nowMs < offered - 15_000 -> "Not valid yet"
                else -> "Valid for ${duration(expires - nowMs)}"
            }),
            CheckLine(!usedBefore && earlier == null, when {
                usedBefore -> "Already used on this phone"
                earlier != null -> {
                    val by = roster.person(earlier.fromPersonId)?.name ?: giver?.name ?: "The giver"
                    val to = roster.person(earlier.toPersonId)?.name ?: "someone else"
                    "$by already handed $itemCode to $to at ${earlier.acceptedMs?.let { clock(it) } ?: "an earlier time"}. This would be a second handover."
                }
                else -> "Not used before"
            }),
        )
        val failures = lines.indices.filter { !lines[it].ok }
        return OfferCheck(lines, failures.isEmpty(), failures.isNotEmpty() && failures.all { it in 3..5 })
    }

    /** "Seal S-40117 matches C-12", or the mismatch. */
    fun checkSeal(seal: ScannedCode.Seal, item: ItemEntity): CheckLine {
        val ok = seal.itemCode == item.code && seal.sealNo == item.sealNo
        return CheckLine(ok, if (ok) "Seal ${seal.sealNo} matches ${item.code}"
            else "Seal ${seal.sealNo} doesn't match ${item.code} (${item.sealNo}). Don't accept this camera.")
    }

    /** On the giver's phone: is this the named receiver's valid acceptance of [offer]? */
    fun checkAcceptance(acceptance: ScannedCode.Acceptance, offer: HandoverEntity, roster: Roster): CheckLine {
        if (acceptance.id != offer.id) return CheckLine(false, "This acceptance is for another handover")
        val device = roster.deviceOf(offer.toPersonId, acceptance.toDeviceId)
            ?: return CheckLine(false, "Unknown signer. Sync this phone before accepting.")
        val record = runCatching { HandoverBuilder.fromAcceptance(offer, acceptance) }.getOrNull()
            ?: return CheckLine(false, "Not an acceptance code")
        val valid = Signatures.verify(device.publicKey, Canonical.acceptanceString(record), acceptance.receiverSig)
        val withdrawnAt = roster.withdrawnAt(device.id)?.takeIf { withdrawnBy(it, acceptance.acceptedMs) }
        if (valid && withdrawnAt != null) {
            return CheckLine(false, withdrawnText(roster.person(offer.toPersonId)?.name ?: "The receiver", withdrawnAt))
        }
        return CheckLine(valid, if (valid) "Acceptance signature matches" else "Signature doesn't match")
    }

    private fun validProof(proof: HandoverEntity, offer: HandoverEntity, register: Boolean, roster: Roster, local: List<HandoverEntity>): Boolean {
        if (proof.id != offer.prevId || proof.itemId != offer.itemId || proof.toPersonId != offer.fromPersonId) return false
        if (register) {
            return proof.kind == HandoverKind.REGISTER && proof.recordHash == offer.prevHash &&
                proof.recordHash == Canonical.registerHash(proof.id, proof.itemId, proof.toPersonId)
        }
        if (proof.kind !in listOf(HandoverKind.ISSUE, HandoverKind.TRANSFER, HandoverKind.RETURN)) return false
        if (proof.recordHash != offer.prevHash || Canonical.recordHash(proof) != proof.recordHash) return false
        if (proof.toDeviceId == null || proof.sealNo == null || proof.acceptedMs == null) return false
        val from = roster.deviceOf(proof.fromPersonId, proof.fromDeviceId) ?: return false
        val to = roster.deviceOf(proof.toPersonId, proof.toDeviceId) ?: return false
        if (!Signatures.verify(from.publicKey, Canonical.offerString(proof), proof.giverSig.orEmpty()) ||
            !Signatures.verify(to.publicKey, Canonical.acceptanceString(proof), proof.receiverSig.orEmpty())) return false
        val parent = local.firstOrNull { it.id == proof.prevId }
        return parent == null || (parent.itemId == proof.itemId && parent.recordHash == proof.prevHash && parent.toPersonId == proof.fromPersonId)
    }

    /** True when a device withdrawn at [withdrawnAtMs] can't have signed at [signedMs], allowing 15 s for clocks. */
    private fun withdrawnBy(withdrawnAtMs: Long, signedMs: Long?): Boolean =
        signedMs == null || signedMs >= withdrawnAtMs - CLOCK_ALLOWANCE_MS

    /** The withdrawn text if either device on the proof record was withdrawn before it was accepted. */
    private fun withdrawnSigner(proof: HandoverEntity, roster: Roster): String? {
        if (proof.kind == HandoverKind.REGISTER) return null
        return listOf(proof.fromPersonId to proof.fromDeviceId, proof.toPersonId to proof.toDeviceId).firstNotNullOfOrNull { (person, device) ->
            roster.withdrawnAt(device)?.takeIf { withdrawnBy(it, proof.acceptedMs) }
                ?.let { withdrawnText(roster.person(person)?.name ?: "A signer", it) }
        }
    }

    /**
     * A complete handover on this phone that already left the same previous record: the giver
     * signed this camera away once, so this offer would fork the chain.
     */
    private fun earlierHandover(offer: HandoverEntity, local: List<HandoverEntity>): HandoverEntity? =
        local.filter {
            it.id != offer.id && it.prevId != null && it.prevId == offer.prevId && it.itemId == offer.itemId &&
                it.kind != HandoverKind.REGISTER && !it.tamperHidden && it.syncStatus != SyncMark.REJECTED
        }.minByOrNull { it.acceptedMs ?: Long.MAX_VALUE }

    private fun withdrawnText(name: String, atMs: Long): String =
        "$name's phone was withdrawn by the safety officer at ${clock(atMs)}."

    private fun clock(ms: Long): String =
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(ms))

    private fun duration(ms: Long): String {
        val seconds = ms.coerceAtLeast(0) / 1_000
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }
}
