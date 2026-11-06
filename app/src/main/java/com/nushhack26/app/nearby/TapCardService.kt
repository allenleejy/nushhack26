package com.nushhack26.app.nearby

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * The giver's phone as an NFC card (docs/sync.md A.11). Placeholder: card 16 serves the
 * `CS1T:` tap payload here. Until then every command answers "file not found".
 */
class TapCardService : HostApduService() {
    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray = byteArrayOf(0x6A, 0x82.toByte())

    override fun onDeactivated(reason: Int) = Unit
}
