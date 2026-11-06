package com.nushhack26.app.ui.give

import com.nushhack26.app.data.remote.OfferRelayApi
import com.nushhack26.app.data.remote.RelayResult
import com.nushhack26.app.data.remote.RelayStatus
import com.nushhack26.app.data.remote.RelayStatusKind
import com.nushhack26.app.data.remote.RelayedOffer
import com.nushhack26.app.data.repo.IdentityRepository
import com.nushhack26.app.data.repo.RelayOutcome
import com.nushhack26.app.data.repo.RelayRepository
import com.nushhack26.app.data.local.SyncStateDao
import com.nushhack26.app.data.local.SyncStateEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RelayStepTest {
    @Test
    fun statusesMapToTheGiversNextStep() {
        assertEquals(RelayStep.Wait, relayStep(RelayStatus(RelayStatusKind.OPEN), 0))
        assertEquals(RelayStep.Accepted("CS1A:x"), relayStep(RelayStatus(RelayStatusKind.ACCEPTED, "CS1A:x"), 0))
        assertEquals(RelayStep.Wait, relayStep(RelayStatus(RelayStatusKind.ACCEPTED, null), 0))
        assertEquals(RelayStep.Declined, relayStep(RelayStatus(RelayStatusKind.DECLINED), 0))
        assertEquals(RelayStep.Gone, relayStep(RelayStatus(RelayStatusKind.CANCELLED), 0))
        assertEquals(RelayStep.Gone, relayStep(RelayStatus(RelayStatusKind.UNKNOWN), 0))
    }

    @Test
    fun threeUnansweredChecksInARowMeanTheNetworkIsGone() {
        assertEquals(RelayStep.Wait, relayStep(null, 1))
        assertEquals(RelayStep.Wait, relayStep(null, 2))
        assertEquals(RelayStep.LostNetwork, relayStep(null, 3))
    }

    private class FakeRelayApi(var status: RelayStatus = RelayStatus(RelayStatusKind.OPEN)) : OfferRelayApi {
        val posted = mutableListOf<String>()
        override suspend fun post(id: String, itemId: String, toPersonId: String, offer: String, expiresMs: Long): RelayResult {
            posted += id
            return RelayResult("posted")
        }
        override suspend fun inbox(): List<RelayedOffer> = emptyList()
        override suspend fun accept(id: String, acceptance: String) = RelayResult("accepted")
        override suspend fun close(id: String) = RelayResult("closed")
        override suspend fun status(id: String): RelayStatus = status
    }


    @Test
    fun offlineRelayFallsBackWithoutCallingTheServer() = runTest {
        val api = FakeRelayApi(RelayStatus(RelayStatusKind.ACCEPTED, "CS1A:x"))
        val online = MutableStateFlow(false)
        val relay = RelayRepository(api, online, IdentityRepository(noSyncState()), TestScope())
        assertEquals(RelayOutcome.Unavailable("offline"), relay.post("o1", "i", "p", "CS1O:x", 0))
        assertNull(relay.status("o1"))
        assertEquals(emptyList<String>(), api.posted)

        online.value = true
        assertEquals(RelayOutcome.Sent, relay.post("o1", "i", "p", "CS1O:x", 0))
        assertEquals(RelayStep.Accepted("CS1A:x"), relayStep(relay.status("o1"), 0))
    }
}

private fun noSyncState(): SyncStateDao = object : SyncStateDao {
    override suspend fun put(entry: SyncStateEntry) = Unit
    override suspend fun get(key: String): String? = null
    override fun observe(key: String): Flow<String?> = flowOf(null)
    override suspend fun remove(key: String) = Unit
}
