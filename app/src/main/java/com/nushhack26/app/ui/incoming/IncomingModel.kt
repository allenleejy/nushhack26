package com.nushhack26.app.ui.incoming

import com.nushhack26.app.data.remote.RelayedOffer
import com.nushhack26.app.data.repo.BannerLookup

/** One offer as the R0 banner shows it. */
data class IncomingOfferUi(
    val id: String,
    val giverName: String,
    val itemCode: String,
    val expiresMs: Long,
    /** "Kestrel"; empty until the camera's name has synced. */
    val itemName: String = "",
) {
    fun countdown(nowMs: Long): String {
        val seconds = ((expiresMs - nowMs).coerceAtLeast(0) + 999) / 1_000
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }
}

/** Which relayed offers the banner shows (docs/spec.md R0). Pure. */
object IncomingOffers {
    /** Still open: not on this phone yet, not declined here, and not past [RelayedOffer.expiresMs]. */
    fun visible(offers: List<RelayedOffer>, lookup: BannerLookup, nowMs: Long): List<IncomingOfferUi> =
        offers
            .filter { it.id !in lookup.doneOfferIds && it.expiresMs > nowMs }
            .map { offer ->
                IncomingOfferUi(
                    id = offer.id,
                    giverName = lookup.people[offer.fromPersonId]?.name ?: "A colleague",
                    itemCode = lookup.items[offer.itemId]?.code ?: "a camera",
                    expiresMs = offer.expiresMs,
                    itemName = lookup.items[offer.itemId]?.name.orEmpty(),
                )
            }

    /** The newest one: every relayed offer is valid for the same time, so the latest expiry. */
    fun newest(offers: List<RelayedOffer>, lookup: BannerLookup, nowMs: Long): IncomingOfferUi? =
        visible(offers, lookup, nowMs).maxByOrNull { it.expiresMs }
}
