package com.nushhack26.app.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

/**
 * While Tenet is on screen, syncs every [POLL_MS] so records another phone just synced show up
 * by themselves, the way the incoming-offer banner polls the relay. The app frame runs [run]
 * only while it's started, so nothing polls in the background.
 *
 * - [sync] is the same engine run the worker does: pushes anything waiting, then pulls.
 * - Offline or not enrolled, it just waits for the next tick.
 * - "not enrolled" from the server runs [onNotEnrolled], which wipes a withdrawn phone.
 * - Any other failure is left to the worker's retries; the next tick tries again.
 */
class LivePull(
    private val sync: suspend () -> Unit,
    private val isOnline: StateFlow<Boolean>,
    private val enrolled: suspend () -> Boolean,
    private val onNotEnrolled: suspend () -> Unit,
) {
    suspend fun run(): Nothing {
        while (true) {
            if (isOnline.value && enrolled()) {
                try {
                    sync()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (e.message?.contains("not enrolled") == true) onNotEnrolled()
                }
            }
            delay(POLL_MS)
        }
    }

    companion object {
        const val POLL_MS = 4_000L
    }
}
