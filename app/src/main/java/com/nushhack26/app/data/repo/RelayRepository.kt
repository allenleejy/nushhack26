package com.nushhack26.app.data.repo

import com.nushhack26.app.data.remote.OfferRelayApi
import com.nushhack26.app.data.remote.RelayStatus
import com.nushhack26.app.data.remote.RelayedOffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeout

/** What a relay call came to. Anything but [Sent] means: fall back to the QR codes. */
sealed interface RelayOutcome {
    data object Sent : RelayOutcome
    data class Refused(val reason: String) : RelayOutcome
    data class Unavailable(val reason: String) : RelayOutcome
}

/**
 * The online handover (docs/sync.md A.10). The server only carries the offer and acceptance
 * strings; the phones check every signature and save records exactly as with QR codes.
 * Every call gives up after [TIMEOUT_MS], so a weak signal falls back to QR quickly.
 */
class RelayRepository(
    private val api: OfferRelayApi,
    private val isOnline: StateFlow<Boolean>,
    private val identity: IdentityRepository,
    scope: CoroutineScope,
) {
    /**
     * Offers made out to me, checked every [POLL_MS] while anything collects this (the app
     * frame does, while it's on screen) and the phone is online and enrolled.
     */
    val incoming: StateFlow<List<RelayedOffer>> = flow {
        while (true) {
            val offers = if (identity.current() == null) emptyList() else guarded { api.inbox() }
            // Offline shows nothing; a failed call keeps what was last shown.
            when {
                offers != null -> emit(offers)
                !isOnline.value -> emit(emptyList())
            }
            delay(POLL_MS)
        }
    }.distinctUntilChanged().stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** One offer from the inbox, fetched now (the receive screen opened from the banner). */
    suspend fun incomingOffer(id: String): RelayedOffer? =
        incoming.value.firstOrNull { it.id == id } ?: guarded { api.inbox() }?.firstOrNull { it.id == id }

    /** Giver: send the `CS1O:` string to the receiver's phone. */
    suspend fun post(id: String, itemId: String, toPersonId: String, offer: String, expiresMs: Long): RelayOutcome =
        outcome { api.post(id, itemId, toPersonId, offer, expiresMs).let { it.result to it.reason } }

    /** Receiver: send the `CS1A:` string back to the giver's phone. */
    suspend fun accept(id: String, acceptance: String): RelayOutcome =
        outcome { api.accept(id, acceptance).let { it.result to it.reason } }

    /** Receiver declines, or giver cancels. Best effort: an offer also lapses at its expiry. */
    suspend fun close(id: String): RelayOutcome =
        outcome { api.close(id).let { (if (it.result == "unchanged") "closed" else it.result) to it.reason } }

    /** Giver: where the offer stands, or null when the server can't be reached. */
    suspend fun status(id: String): RelayStatus? = guarded { api.status(id) }

    private suspend fun <T> guarded(block: suspend () -> T): T? {
        if (!isOnline.value) return null
        return try {
            withTimeout(TIMEOUT_MS) { block() }
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) null else throw e
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun outcome(block: suspend () -> Pair<String, String?>): RelayOutcome {
        if (!isOnline.value) return RelayOutcome.Unavailable("offline")
        return try {
            val (result, reason) = withTimeout(TIMEOUT_MS) { block() }
            if (result in SENT_RESULTS) RelayOutcome.Sent else RelayOutcome.Refused(reason ?: result)
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) RelayOutcome.Unavailable("timed out") else throw e
        } catch (e: Exception) {
            RelayOutcome.Unavailable(e.message ?: e.javaClass.simpleName)
        }
    }

    companion object {
        const val POLL_MS = 2_000L
        const val TIMEOUT_MS = 6_000L

        /** How long a relayed offer stays valid: long enough to walk to the camera and scan its seal. */
        const val RELAYED_OFFER_VALID_MS = 180_000L

        private val SENT_RESULTS = setOf("posted", "duplicate", "accepted", "closed")
    }
}
