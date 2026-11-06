package com.nushhack26.app.nearby

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

internal object TapServing {
    class Offer(val bytes: ByteArray, val onTapped: () -> Unit)
    @Volatile var offer: Offer? = null
}

class TapCardService : HostApduService() {
    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        val offer = TapServing.offer
        val response = TapApdu.answer(commandApdu, offer?.bytes)
        if (offer != null && TapApdu.isLastRead(commandApdu, offer.bytes)) offer.onTapped()
        return response
    }

    override fun onDeactivated(reason: Int) = Unit
}
