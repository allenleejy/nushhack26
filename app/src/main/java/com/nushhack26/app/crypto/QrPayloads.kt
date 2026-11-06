package com.nushhack26.app.crypto

import com.nushhack26.app.data.local.HandoverEntity

/** What a scanned QR code turned out to be. */
sealed interface ScannedCode {
    /** A signed offer, plus the record that made the giver the holder (null if that's a register record). */
    data class Offer(val offer: HandoverEntity, val proof: HandoverEntity?) : ScannedCode

    data class Acceptance(
        val id: String,
        val toDeviceId: String,
        val sealNo: String,
        val acceptedMs: Long,
        val receiverSig: String,
    ) : ScannedCode

    data class Seal(val itemCode: String, val sealNo: String) : ScannedCode

    data class Unknown(val raw: String) : ScannedCode
}

/** The CS1O:, CS1A: and CS1S: codes (docs/sync.md A.3). Card 01 fills in the bodies. */
object QrPayloads {
    /** At most 1,100 characters with a proof record. */
    fun offer(offer: HandoverEntity, proof: HandoverEntity?): String = TODO("card 01")

    fun acceptance(record: HandoverEntity): String = TODO("card 01")

    fun seal(itemCode: String, sealNo: String): String = "CS1S:$itemCode:$sealNo"

    fun parse(text: String): ScannedCode = TODO("card 01")
}
