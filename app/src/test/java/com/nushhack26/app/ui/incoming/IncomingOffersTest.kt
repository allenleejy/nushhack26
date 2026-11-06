package com.nushhack26.app.ui.incoming

import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.Roles
import com.nushhack26.app.data.remote.RelayedOffer
import com.nushhack26.app.data.repo.BannerLookup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingOffersTest {
    private val siti = PersonEntity("p-siti", "Siti Aminah", Roles.STORE_OFFICER, "S-1021")
    private val c12 = ItemEntity("i-12", "C-12", "IR-0457", "Ir-192", "S-40117", "Tuas Store 1")
    private val lookup = BannerLookup(mapOf(siti.id to siti), mapOf(c12.id to c12))
    private val now = 1_000_000L

    private fun offer(id: String, expiresMs: Long, from: String = siti.id, item: String = c12.id) =
        RelayedOffer(id, item, from, "CS1O:{}", expiresMs)

    @Test
    fun showsTheNewestOpenOfferWithNamesFromRoom() {
        val shown = IncomingOffers.newest(listOf(offer("o1", now + 60_000), offer("o2", now + 170_000)), lookup, now)
        assertEquals(IncomingOfferUi("o2", "Siti Aminah", "C-12", now + 170_000), shown)
        assertEquals("2:50", shown!!.countdown(now))
    }

    @Test
    fun hidesExpiredOffersAndOnesAlreadyOnThisPhoneOrDeclined() {
        val offers = listOf(offer("expired", now - 1), offer("local", now + 60_000), offer("declined", now + 90_000))
        assertNull(IncomingOffers.newest(offers, lookup.copy(doneOfferIds = setOf("local", "declined")), now))
    }

    @Test
    fun unknownGiverOrCameraStillShows() {
        val shown = IncomingOffers.newest(listOf(offer("o1", now + 5_500, from = "p-x", item = "i-x")), lookup, now)
        assertEquals("A colleague", shown!!.giverName)
        assertEquals("a camera", shown.itemCode)
        assertEquals("0:06", shown.countdown(now))
    }
}
