package com.nushhack26.app.crypto

import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.ResolutionEntity

/**
 * The exact strings that get signed and hashed (docs/sync.md A.2). The server's
 * sync_push builds the same strings, so they must match byte for byte. Card 01.
 */
object Canonical {
    /** CS1-OFFER|…, signed by the giver. */
    fun offerString(h: HandoverEntity): String = TODO("card 01")

    /** CS1-ACCEPT|<offer>|giver_sig|to_device_id|seal_no|accepted_ms, signed by the receiver. */
    fun acceptanceString(h: HandoverEntity): String = TODO("card 01")

    /** hex(SHA-256(acceptance + "|" + receiver_sig)). */
    fun recordHash(h: HandoverEntity): String = TODO("card 01")

    /** The hash of a server-made register record. */
    fun registerHash(id: String, itemId: String, toPersonId: String): String = TODO("card 01")

    /** CS1-RESOLVE|…, signed by the safety officer. */
    fun resolutionString(r: ResolutionEntity): String = TODO("card 01")

    /** Six digits from a record hash, shown as "48 21 07". */
    fun matchCode(recordHash: String): String = TODO("card 01")

    fun sha256Hex(text: String): String = TODO("card 01")
}
