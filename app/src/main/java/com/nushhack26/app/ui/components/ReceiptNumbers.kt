package com.nushhack26.app.ui.components

import com.nushhack26.app.data.local.HandoverEntity

/**
 * The numbers a custody receipt prints, shared by the Done screens (card 29) and the record view
 * (card 30). Display only: nothing here is signed or hashed.
 */
object ReceiptNumbers {
    /**
     * RECORD #n for [handover]: its place along the item's chain, following `prevId` back to the
     * register, which is #1. [chain] is every handover of the item on this phone
     * (`CustodyRepository.observeChain`). Null when a link is missing here or loops.
     */
    fun recordNo(handover: HandoverEntity, chain: List<HandoverEntity>): Int? {
        val byId = chain.associateBy { it.id }
        val seen = mutableSetOf(handover.id)
        var current = handover
        var n = 1
        while (true) {
            val prevId = current.prevId ?: return n
            current = byId[prevId] ?: return null
            if (!seen.add(current.id)) return null
            n++
        }
    }

    /** "SHA 4F2A 91C0 77E3 0B1D": the first 16 hex digits of a record hash, in fours. Blank stays blank. */
    fun shortHash(recordHash: String): String =
        recordHash.take(16).uppercase().chunked(4).joinToString(" ").let { if (it.isEmpty()) "" else "SHA $it" }

    /** "48 21 07" (`Canonical.matchCode`) → "482 107". */
    fun verifyCode(matchCode: String): String = matchCode.filter(Char::isDigit).chunked(3).joinToString(" ")
}
