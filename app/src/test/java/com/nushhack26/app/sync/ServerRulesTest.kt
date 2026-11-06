package com.nushhack26.app.sync

import com.nushhack26.app.data.remote.OpResultKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The server's clash rules (docs/sync.md section C), checked on the fake server the sync
 * engine tests run against. These run now; supabase/migrations holds the same rules in SQL.
 */
class ServerRulesTest {
    private lateinit var server: FakeSyncServer

    private val h1 = Demo.handover("h1", Demo.c12Register, Demo.SITI, Demo.RAVI, "issue")
    private val h2 = Demo.handover("h2", h1, Demo.RAVI, Demo.WEI_MING, "transfer")
    private val h2b = Demo.handover("h2b", h1, Demo.RAVI, Demo.SITI, "return")

    @Before
    fun setUp() {
        server = FakeSyncServer().apply { seedDemo() }
    }

    @Test
    fun handoverIsAccepted() = runTest {
        val result = server.push(listOf(h1.op())).single()
        assertEquals(OpResultKind.APPLIED, result.result)
        assertEquals("accepted", server.handovers.getValue("h1").status)
    }

    @Test
    fun repeatedOpIsADuplicate() = runTest {
        server.push(listOf(h1.op()))
        val again = server.push(listOf(h1.op())).single()
        assertEquals(OpResultKind.DUPLICATE, again.result)
        assertEquals(1, server.handovers.values.count { it.id == "h1" })
    }

    @Test
    fun secondHandoverAfterTheSameRecordIsAForkNamingTheGiver() = runTest {
        server.push(listOf(h1.op(), h2.op()))
        val result = server.push(listOf(h2b.op())).single()

        assertEquals(OpResultKind.CONFLICT, result.result)
        assertEquals("forked", server.handovers.getValue("h2b").status)
        assertEquals("accepted", server.handovers.getValue("h2").status)
        val fork = server.alerts.values.single { it.kind == "fork" }
        assertEquals(Demo.RAVI, fork.personId)
        assertEquals(listOf("h2", "h2b"), fork.handoverIds)
        assertEquals("h1", fork.forkPrevId)
    }

    @Test
    fun reusedNonceIsRejectedAsAReplay() = runTest {
        server.push(listOf(h1.op()))
        val replay = Demo.handover("h1-copy", Demo.c12Register, Demo.SITI, Demo.RAVI, "issue", nonce = h1.nonce!!)
        val result = server.push(listOf(replay.op())).single()

        assertEquals(OpResultKind.REJECTED, result.result)
        assertEquals("replayed offer", result.reason)
        assertTrue(server.alerts.values.any { it.kind == "rejected" && it.reason == "replayed offer" })
        assertTrue("h1-copy" !in server.handovers)
    }

    @Test
    fun giverWhoWasNotTheHolderIsRejected() = runTest {
        server.push(listOf(h1.op()))
        val bogus = Demo.handover("hx", h1, Demo.WEI_MING, Demo.JUN, "transfer")
        assertEquals("giver was not the holder", server.push(listOf(bogus.op())).single().reason)
    }

    @Test
    fun childBeforeParentWaitsAsOrphanThenBothAreAccepted() = runTest {
        val early = server.push(listOf(h2.op())).single()
        assertEquals(OpResultKind.CONFLICT, early.result)
        assertEquals("orphan", server.handovers.getValue("h2").status)

        server.push(listOf(h1.op()))
        assertEquals("accepted", server.handovers.getValue("h1").status)
        assertEquals("accepted", server.handovers.getValue("h2").status)
        assertTrue(server.alerts.values.filter { it.kind == "orphan" }.all { it.closedAt != null })
    }

    @Test
    fun wrongSealIsAcceptedWithAnAlert() = runTest {
        val wrongSeal = Demo.handover("h1", Demo.c12Register, Demo.SITI, Demo.RAVI, "issue", seal = "S-40999")
        assertEquals(OpResultKind.APPLIED, server.push(listOf(wrongSeal.op())).single().result)
        assertNotNull(server.alerts.values.singleOrNull { it.kind == "seal_mismatch" })
    }

    @Test
    fun phoneClockADayFastStillFollowsTheChain() = runTest {
        val fast = Demo.handover("h1", Demo.c12Register, Demo.SITI, Demo.RAVI, "issue", acceptedMs = 1_794_466_830_000 + 86_400_000)
        assertEquals(OpResultKind.APPLIED, server.push(listOf(fast.op())).single().result)
        assertEquals(fast.acceptedMs, server.handovers.getValue("h1").acceptedMs)
    }

    @Test
    fun resolutionPicksTheForkedBranchAndASecondIsRejected() = runTest {
        server.push(listOf(h1.op(), h2.op(), h2b.op()))
        val fork = server.alerts.values.single { it.kind == "fork" }

        val first = server.push(listOf(Demo.resolution("r1", fork.id, chosen = "h2b").op())).single()
        assertEquals(OpResultKind.APPLIED, first.result)
        assertEquals("accepted", server.handovers.getValue("h2b").status)
        assertEquals("overruled", server.handovers.getValue("h2").status)
        assertNotNull(server.alerts.getValue(fork.id).closedAt)

        val second = server.push(listOf(Demo.resolution("r2", fork.id, chosen = "h2").op())).single()
        assertEquals("already resolved", second.reason)
    }

    @Test
    fun unknownDeviceIsRejected() = runTest {
        val stranger = h1.copy(fromDeviceId = "e0000000-0000-4000-8000-0000000000ff")
        assertEquals("unknown signer", server.push(listOf(stranger.op())).single().reason)
    }

    @Test
    fun changedRecordFailsTheHashCheck() = runTest {
        server.hashOk = { it.sealNo == Demo.C12_SEAL }
        val changed = h1.copy(sealNo = "S-00000")
        assertEquals("hash mismatch", server.push(listOf(changed.op())).single().reason)
    }

    @Test
    fun sameIdWithDifferentContentIsRejected() = runTest {
        server.push(listOf(h1.op()))
        val altered = h1.copy(recordHash = "hash-altered")
        assertEquals("conflicting copy of the same record", server.push(listOf(altered.op())).single().reason)
    }

    @Test
    fun pullFromZeroReturnsEverythingInOrder() = runTest {
        server.push(listOf(h1.op()))
        val page = server.pull(0)
        assertEquals(5, page.rows.people.size)
        assertEquals(2, page.rows.handovers.size)
        assertEquals(page.rows.handovers.maxOf { it.serverSeq!! }, page.maxSeq)
        assertEquals(0, server.pull(page.maxSeq).rows.handovers.size)
    }
}
