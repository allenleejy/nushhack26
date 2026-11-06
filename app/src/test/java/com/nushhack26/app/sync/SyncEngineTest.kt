package com.nushhack26.app.sync

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.local.Origin
import com.nushhack26.app.data.local.SyncKeys
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.remote.HandoverRow
import com.nushhack26.app.data.remote.toEntity
import com.nushhack26.app.data.repo.GiveRepository
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.data.repo.IdentityRepository
import com.nushhack26.app.data.repo.ReceiveRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The sync engine against the fake server (docs/sync.md sections D and E): nothing lost,
 * nothing doubled, conflicts kept and shown. Card 02 removes the @Ignore lines.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SyncEngineTest {
    private lateinit var server: FakeSyncServer
    private val phones = mutableListOf<AppDatabase>()

    private val h1 = Demo.handover("h1", Demo.c12Register, Demo.SITI, Demo.RAVI, "issue")
    private val h2 = Demo.handover("h2", h1, Demo.RAVI, Demo.WEI_MING, "transfer")
    private val h2b = Demo.handover("h2b", h1, Demo.RAVI, Demo.SITI, "return")

    @Before
    fun setUp() {
        server = FakeSyncServer().apply { seedDemo() }
    }

    @After
    fun tearDown() = phones.forEach { it.close() }

    private fun phone(): AppDatabase =
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also(phones::add)

    private fun engine(db: AppDatabase) = SyncEngine(db, server)

    /** Saves a handover the way card 04 does on the receiver's phone: with its outbox entry. */
    private suspend fun AppDatabase.receive(record: HandoverRow, proof: HandoverRow? = null) =
        ReceiveRepository(this, requestSync = {}).saveAccepted(proof?.toEntity(), record.toEntity())

    @Ignore("card 02")
    @Test
    fun recordMadeOfflineSyncsOnce() = runTest {
        val ravi = phone()
        engine(ravi).run()
        ravi.receive(h1)

        engine(ravi).run()

        assertEquals(1, server.handovers.values.count { it.id == "h1" })
        val local = ravi.handoverDao().get("h1")!!
        assertEquals(SyncMark.SYNCED, local.syncStatus)
        assertEquals("accepted", local.status)
        assertEquals(0, ravi.outboxDao().observeCount().first())
    }

    @Ignore("card 02")
    @Test
    fun retryAfterADroppedConnectionDoublesNothing() = runTest {
        val ravi = phone()
        ravi.receive(h1)
        server.dropNextResponse = true

        runCatching { engine(ravi).run() }
        assertEquals(1, ravi.outboxDao().observeCount().first())

        engine(ravi).run()
        assertEquals(1, server.handovers.values.count { it.id == "h1" })
        assertEquals(SyncMark.SYNCED, ravi.handoverDao().get("h1")!!.syncStatus)
        assertEquals(0, ravi.outboxDao().observeCount().first())
    }

    @Ignore("card 02")
    @Test
    fun giverAndReceiverBothDeliverTheSameRecord() = runTest {
        val ravi = phone()
        val weiMing = phone()
        ravi.receive(h1)
        weiMing.receive(h2, proof = h1)

        engine(weiMing).run()
        engine(ravi).run()

        assertEquals(1, server.handovers.values.count { it.id == "h1" })
        assertEquals(1, server.handovers.values.count { it.id == "h2" })
        assertEquals(SyncMark.SYNCED, ravi.handoverDao().get("h1")!!.syncStatus)
    }

    @Ignore("card 02")
    @Test
    fun forkFromTwoPhonesIsAConflictNamingTheGiver() = runTest {
        val weiMing = phone()
        val siti = phone()
        weiMing.receive(h2, proof = h1)
        siti.receive(h2b, proof = h1)

        engine(weiMing).run()
        engine(siti).run()

        assertEquals("forked", siti.handoverDao().get("h2b")!!.status)
        assertEquals(SyncMark.SYNCED, siti.handoverDao().get("h2b")!!.syncStatus)
        engine(weiMing).run()
        val fork = weiMing.alertDao().observeAll().first().single { it.kind == "fork" }
        assertEquals(Demo.RAVI, fork.personId)
    }

    @Ignore("card 02")
    @Test
    fun rejectedRecordIsKeptAndMarked() = runTest {
        val ravi = phone()
        val jun = phone()
        ravi.receive(h1)
        engine(ravi).run()
        val replay = Demo.handover("h1-copy", Demo.c12Register, Demo.SITI, Demo.JUN, "issue", nonce = h1.nonce!!)
        jun.receive(replay)

        engine(jun).run()

        val local = jun.handoverDao().get("h1-copy")!!
        assertEquals(SyncMark.REJECTED, local.syncStatus)
        assertEquals("replayed offer", local.syncReason)
        assertEquals(0, jun.outboxDao().observeCount().first())
    }

    @Ignore("card 02")
    @Test
    fun firstPullBringsTheRosterAndTheChain() = runTest {
        val fresh = phone()
        engine(fresh).run()

        assertEquals(5, fresh.peopleDao().all().size)
        assertEquals(5, fresh.deviceDao().all().size)
        assertNotNull(fresh.itemDao().get(Demo.C12))
        assertEquals(Origin.PULLED, fresh.handoverDao().get(Demo.c12Register.id)!!.origin)
        assertTrue((fresh.syncStateDao().get(SyncKeys.LAST_SERVER_SEQ)?.toLong() ?: 0L) > 0L)
        assertNotNull(fresh.syncStateDao().get(SyncKeys.LAST_SYNC_AT))
    }

    @Ignore("card 02")
    @Test
    fun wrongPhoneClockKeepsItsTimeAndTheChainOrder() = runTest {
        val ravi = phone()
        val fast = Demo.handover("h1", Demo.c12Register, Demo.SITI, Demo.RAVI, "issue", acceptedMs = 1_794_466_830_000 + 86_400_000)
        ravi.receive(fast)

        engine(ravi).run()

        val local = ravi.handoverDao().get("h1")!!
        assertEquals("accepted", local.status)
        assertEquals(fast.acceptedMs, local.acceptedMs)
    }

    @Ignore("card 02")
    @Test
    fun pulledCopyKeepsThePhonesOwnColumns() = runTest {
        // Ravi's tampered phone hid h2 and never queued it; Wei Ming's phone delivers it.
        val ravi = phone()
        val identity = IdentityRepository(ravi.syncStateDao())
        identity.setIdentity(Identity(Demo.RAVI, Demo.RAVI_PHONE, "Ravi Kumar", "radiographer"))
        identity.setTampered(true)
        ravi.receive(h1)
        GiveRepository(ravi, identity, requestSync = {}).completeHandover(h2.toEntity())
        val weiMing = phone()
        weiMing.receive(h2, proof = h1)

        engine(weiMing).run()
        engine(ravi).run()

        val local = ravi.handoverDao().get("h2")!!
        assertTrue(local.hiddenByTamper)
        assertEquals("accepted", local.status)
    }

    @Ignore("card 02")
    @Test
    fun offlineLeavesTheOutboxAlone() = runTest {
        val ravi = phone()
        ravi.receive(h1)
        server.offline = true

        runCatching { engine(ravi).run() }

        assertEquals(1, ravi.outboxDao().observeCount().first())
        assertEquals(SyncMark.WAITING, ravi.handoverDao().get("h1")!!.syncStatus)
        assertNotNull(ravi.syncStateDao().get(SyncKeys.LAST_ERROR))
    }
}
