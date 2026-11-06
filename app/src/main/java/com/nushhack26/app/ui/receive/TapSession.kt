package com.nushhack26.app.ui.receive

import com.nushhack26.app.nearby.ReceiverTapSession
import kotlinx.coroutines.flow.StateFlow

/** The receiver's end of the tap link (card 16's [ReceiverTapSession]), as R1–R5 use it. */
interface TapSession {
    val linked: StateFlow<Boolean>
    /** True once the giver's phone has the string; false if no link is up within 10 s. */
    suspend fun send(text: String): Boolean
    fun close()
}

fun ReceiverTapSession.asTapSession(): TapSession {
    val session = this
    return object : TapSession {
        override val linked: StateFlow<Boolean> = session.linked
        override suspend fun send(text: String): Boolean = session.send(text)
        override fun close() = session.close()
    }
}
