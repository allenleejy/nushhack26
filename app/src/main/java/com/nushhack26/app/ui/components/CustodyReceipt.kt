package com.nushhack26.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetType

/** A signer on the receipt: "RAVI KUMAR", and on the record view "KEY 3F:A9:12:7C" under it. */
data class ReceiptParty(val name: String, val key: String? = null)

/**
 * What one custody receipt prints (docs/spec.md section 3, "The custody receipt"). Every value is
 * already formatted; the receipt uppercases. A null optional line isn't printed.
 */
data class ReceiptContent(
    /** RECORD #5: this handover's place in the item's chain, counting the register as #1. */
    val recordNo: Int?,
    /** FOLLOWS #4; null on the register record. */
    val followsNo: Int?,
    /** "07.11.2026" */
    val date: String,
    /** SIGNED 13:02 */
    val signedTime: String,
    /** "Kestrel"; blank falls back to [itemCode]. */
    val itemName: String,
    /** "C-12" */
    val itemCode: String,
    /** "IR-0457 · IR-192" */
    val source: String?,
    /** "S-40117" */
    val sealNo: String?,
    val sealMatches: Boolean = true,
    /** Null on the register record. */
    val from: ReceiptParty?,
    val to: ReceiptParty,
    /** "19:00" */
    val returnBy: String?,
    /** "NFC TAP", "QR CODE" or "SERVER"; record view only, when known. */
    val via: String? = null,
    /** "TAN WEI MING", or "YOU" on the receiver's Done screen. */
    val heldBy: String,
    /** "2 OF 2 VERIFIED"; record view only. */
    val signatures: String? = null,
    val signaturesOk: Boolean = true,
    /** The full record hash: printed as "SHA 4F2A 91C0 77E3 0B1D" and seeds the barcode. Record view only. */
    val hash: String? = null,
    /** "OFFLINE" under the title when the handover was signed with no network. Record view only. */
    val offline: Boolean = false,
    /** "482 107" */
    val verifyCode: String,
    /** "Saved on this phone · not synced yet"; record view only. */
    val footer: String? = null,
)

/** The countersigned stamp: HANDED OVER / 13:02 / TO WEI MING. */
data class ReceiptStamp(val word: String, val time: String, val line: String)

enum class ReceiptStyle {
    /** G5 and R Done: torn off the roll at both ends, printed out from under the hazard band, stamp and wave. */
    Done,
    /** The record view: fed out of a printer slot, keys, signatures, barcode and hash, stamp, no wave. */
    Record,
}

/**
 * The custody receipt, set like a printed till roll (card 29 fills this in from the design's
 * "Done · torn receipt" and "Record receipt" boards). This stub draws the lines, still.
 */
@Composable
fun CustodyReceipt(
    content: ReceiptContent,
    stamp: ReceiptStamp,
    style: ReceiptStyle,
    modifier: Modifier = Modifier,
) {
    val colors = Tenet.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.bg100)
            .border(2.dp, colors.lineStrong)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val lines = buildList {
            add("TENET · CUSTODY RECEIPT")
            add("RECORD #${content.recordNo ?: "-"} · ${content.date}")
            add("1 × ${content.itemName.ifBlank { content.itemCode }} · CAMERA ${content.itemCode}")
            content.sealNo?.let { add("SEAL $it") }
            content.from?.let { add("FROM ${it.name}") }
            add("TO ${content.to.name}")
            content.returnBy?.let { add("RETURN BY $it") }
            add("HELD BY ${content.heldBy}")
            add("${stamp.word} · ${stamp.time} · ${stamp.line}")
            if (style == ReceiptStyle.Record) content.signatures?.let { add("SIGNATURES $it") }
            add("VERIFY CODE ${content.verifyCode}")
        }
        lines.forEach { Text(it.uppercase(), style = TenetType.record, color = colors.ink) }
    }
}
