package com.nushhack26.app.sync

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.local.Origin
import com.nushhack26.app.data.local.SyncKeys
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.remote.HandoverRow
import com.nushhack26.app.data.remote.SyncApi
import com.nushhack26.app.data.remote.PushOp
import com.nushhack26.app.data.remote.OpResult
import com.nushhack26.app.data.remote.PullPage
import com.nushhack26.app.data.remote.PersonRow
import com.nushhack26.app.data.remote.SupabaseSyncApi
import com.nushhack26.app.data.remote.NotReadyException
import com.nushhack26.app.data.remote.MissingReportRow
import com.nushhack26.app.data.local.OutboxEntry
import com.nushhack26.app.data.local.OutboxKind
import com.nushhack26.app.data.remote.toPayload
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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The sync engine against the fake server (docs/sync.md sections D and E): nothing lost,
 * nothing doubled, conflicts kept and shown.
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

    @Test
    fun offlineLeavesTheOutboxAlone() = runTest {
        val ravi = phone()
        ravi.receive(h1)
        server.offline = true

        runCatching { engine(ravi).run() }

        assertEquals(1, ravi.outboxDao().observeCount().first())
        assertEquals(SyncMark.WAITING, ravi.handoverDao().get("h1")!!.syncStatus)
        assertNotNull(ravi.syncStateDao().get(SyncKeys.LAST_ERROR))
        assertEquals(1, ravi.outboxDao().nextBatch(100).single().attempts)
        assertEquals("offline", ravi.outboxDao().nextBatch(100).single().lastError)
    }

    @Test
    fun pushesOldestFirstInBatchesOfOneHundred() = runTest {
        val db = phone()
        val device = Demo.devices.first().toEntity()
        repeat(205) { index ->
            val row = device.copy(id = "device-$index")
            db.deviceDao().insert(row)
            db.outboxDao().insert(OutboxEntry(row.id, "devices", row.id, OutboxKind.INSERT,
                row.toPayload(), null, (205 - index).toLong()))
        }
        val batches = mutableListOf<List<String>>()
        val api = object : SyncApi by server {
            override suspend fun push(ops: List<PushOp>): List<OpResult> {
                batches.add(ops.map { it.opId })
                return server.push(ops)
            }
        }
        SyncEngine(db, api).run()
        assertEquals(listOf(100, 100, 5), batches.map { it.size })
        assertEquals((204 downTo 0).map { "device-$it" }, batches.flatten())
        assertTrue(db.deviceDao().all().all { it.syncStatus == SyncMark.SYNCED })
    }

    @Test
    fun pullsEveryPageAndEmptySyncClearsNetworkError() = runTest {
        repeat(2001) { server.addPerson(PersonRow("person-$it", "Person", "rso", "$it")) }
        val db = phone()
        val cursors = mutableListOf<Long>()
        val api = object : SyncApi by server {
            override suspend fun pull(since: Long): PullPage {
                cursors.add(since)
                return server.pull(since)
            }
        }
        val engine = SyncEngine(db, api, now = { 123L })
        engine.run()
        assertEquals(2, cursors.size)
        assertEquals(2006, db.peopleDao().all().size)
        server.offline = true
        assertTrue(runCatching { engine.run() }.isFailure)
        server.offline = false
        engine.run()
        assertEquals(null, db.syncStateDao().get(SyncKeys.LAST_ERROR))
        assertEquals("123", db.syncStateDao().get(SyncKeys.LAST_SYNC_AT))
    }

    @Test
    fun differentServerHashWinsButKeepsLocalFlagsAndReportsError() = runTest {
        val db = phone()
        db.handoverDao().insert(h1.toEntity().copy(hiddenByTamper = true, origin = Origin.MINE))
        server.addHandover(h1.copy(recordHash = "server-hash"))
        engine(db).run()
        val local = db.handoverDao().get(h1.id)!!
        assertEquals("server-hash", local.recordHash)
        assertTrue(local.hiddenByTamper)
        assertEquals(Origin.MINE, local.origin)
        assertEquals("Server copy of h1 differs from this phone's", db.syncStateDao().get(SyncKeys.LAST_ERROR))
    }

    @Test
    fun incompletePushResponseKeepsBatchWaiting() = runTest {
        val db = phone()
        db.receive(h1)
        val api = object : SyncApi by server {
            override suspend fun push(ops: List<PushOp>) = emptyList<OpResult>()
        }
        assertTrue(runCatching { SyncEngine(db, api).run() }.isFailure)
        assertEquals(SyncMark.WAITING, db.handoverDao().get(h1.id)!!.syncStatus)
        assertEquals(1, db.outboxDao().nextBatch(100).single().attempts)
    }

    @Test
    fun unconfiguredApiIsNotReadyForPushOrPull() = runTest {
        val api = SupabaseSyncApi(null)
        assertTrue(runCatching { api.push(emptyList()) }.exceptionOrNull() is NotReadyException)
        assertTrue(runCatching { api.pull(0) }.exceptionOrNull() is NotReadyException)
    }

    @Test
    fun pullsResolutionClosedAlertAndMissingReport() = runTest {
        server.push(listOf(h1.op(), h2.op(), h2b.op()))
        val fork = server.alerts.values.single { it.kind == "fork" }
        val resolution = Demo.resolution("r1", fork.id, "h2b")
        server.push(listOf(resolution.op()))
        val db = phone()
        val report = MissingReportRow("m1", Demo.C12, Demo.RAVI, Demo.RAVI_PHONE,
            note = "Not found", reportedMs = 123L).toEntity()
        db.missingReportDao().insert(report)
        db.outboxDao().insert(OutboxEntry(report.id, "missing_reports", report.id, OutboxKind.INSERT,
            report.toPayload(), null, 1L))
        engine(db).run()
        assertEquals(SyncMark.SYNCED, db.missingReportDao().get("m1")!!.syncStatus)
        assertEquals(SyncMark.SYNCED, db.resolutionDao().get("r1")!!.syncStatus)
        assertNotNull(db.alertDao().get(fork.id)!!.closedAt)
        assertEquals("r1", db.alertDao().get(fork.id)!!.resolutionId)
        assertEquals("accepted", db.handoverDao().get("h2b")!!.status)
        assertEquals("overruled", db.handoverDao().get("h2")!!.status)
    }
}
