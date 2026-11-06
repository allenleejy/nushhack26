package com.nushhack26.app.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nushhack26.app.crypto.DeviceKeys
import com.nushhack26.app.crypto.KeyInfo
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.local.DeviceEntity
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.Roles
import com.nushhack26.app.data.local.SyncKeys
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.local.put
import com.nushhack26.app.data.remote.DeviceAdminApi
import com.nushhack26.app.data.remote.DeviceRow
import com.nushhack26.app.data.remote.DeviceStatus
import com.nushhack26.app.data.remote.RevokeResult
import com.nushhack26.app.data.remote.parseServerTime
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.data.repo.IdentityRepository
import com.nushhack26.app.data.repo.RelayOutcome
import com.nushhack26.app.data.repo.RevocationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RevocationRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var identity: IdentityRepository
    private val api = FakeApi()
    private val keys = FakeKeys()
    private var syncs = 0

    private val ravi = PersonEntity("p-ravi", "Ravi Kumar", Roles.RADIOGRAPHER, "R-002")
    private val tan = PersonEntity("p-tan", "Dr Tan Mei Ling", Roles.RSO, "O-001")
    private val ravisPhone = DeviceEntity("d-ravi", ravi.id, "pub-ravi", "3F:A9:12:7C", "tee", performedAt = 1_000, syncStatus = SyncMark.SYNCED)
    private val tansPhone = DeviceEntity("d-tan", tan.id, "pub-tan", "9C:01:44:E2", "strongbox", performedAt = 2_000, syncStatus = SyncMark.SYNCED)

    private val repo by lazy { RevocationRepository(db, identity, api, { keys }, { syncs++ }) }

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        identity = IdentityRepository(db.syncStateDao())
        db.peopleDao().upsertAll(listOf(ravi, tan))
        db.deviceDao().upsertAll(listOf(ravisPhone, tansPhone))
    }

    @After
    fun tearDown() = db.close()

    private fun revokedRow(at: String = "2026-11-07T06:40:00Z") = DeviceRow(
        id = ravisPhone.id, personId = ravi.id, publicKey = ravisPhone.publicKey, keyFp = ravisPhone.keyFp,
        keySecurity = ravisPhone.keySecurity, performedAt = "1970-01-01T00:00:01Z", serverSeq = 9, revokedAt = at,
    )

    @Test
    fun revokedSavesTheRowAndAsksForASync() = runTest {
        api.revoke = { RevokeResult("revoked", device = revokedRow()) }

        assertEquals(RelayOutcome.Sent, repo.revoke("d-ravi"))

        assertEquals(listOf("d-ravi"), api.revoked)
        assertEquals(parseServerTime("2026-11-07T06:40:00Z"), db.deviceDao().get("d-ravi")?.revokedAtMs)
        assertEquals(1, syncs)
        val listing = repo.devices.first().first { it.device.id == "d-ravi" }
        assertEquals("Ravi Kumar", listing.person?.name)
        assertNotNull(listing.device.revokedAtMs)
    }

    @Test
    fun duplicateCountsAsWithdrawn() = runTest {
        api.revoke = { RevokeResult("duplicate", device = revokedRow()) }

        assertEquals(RelayOutcome.Sent, repo.revoke("d-ravi"))
        assertNotNull(db.deviceDao().get("d-ravi")?.revokedAtMs)
        assertEquals(1, syncs)
    }

    @Test
    fun rejectedIsRefusedWithTheServersReason() = runTest {
        api.revoke = { RevokeResult("rejected", reason = "not a safety officer") }

        assertEquals(RelayOutcome.Refused("not a safety officer"), repo.revoke("d-ravi"))
        assertNull(db.deviceDao().get("d-ravi")?.revokedAtMs)
        assertEquals(0, syncs)
    }

    @Test
    fun offlineIsUnavailableAndChangesNothing() = runTest {
        api.revoke = { error("Unable to resolve host") }

        assertTrue(repo.revoke("d-ravi") is RelayOutcome.Unavailable)
        assertNull(db.deviceDao().get("d-ravi")?.revokedAtMs)
        assertEquals(0, syncs)
        assertEquals(0, db.outboxDao().observeCount().first())
    }

    @Test
    fun aSlowServerTimesOutAfterSixSeconds() = runTest {
        api.revoke = { delay(RevocationRepository.TIMEOUT_MS + 1_000); RevokeResult("revoked", device = revokedRow()) }

        assertEquals(RelayOutcome.Unavailable("timed out"), repo.revoke("d-ravi"))
        assertNull(db.deviceDao().get("d-ravi")?.revokedAtMs)
    }

    @Test
    fun aWithdrawnPhoneWipesItsDataIdentityAndKey() = runTest {
        identity.setIdentity(Identity(ravi.id, ravisPhone.id, ravi.name, ravi.role))
        identity.setEnrolment(ravisPhone.publicKey, 1_000)
        db.syncStateDao().put(SyncKeys.LAST_SYNC_AT, "5000")
        api.status = { DeviceStatus(revoked = true, revokedAt = "2026-11-07T06:40:00Z", deviceId = ravisPhone.id) }
        assertNull(repo.withdrawnNotice.first())

        assertTrue(repo.checkWithdrawn())

        assertTrue(keys.deleted)
        assertNull(identity.current())
        assertNull(identity.enrolledPublicKey())
        assertNull(db.syncStateDao().get(SyncKeys.LAST_SYNC_AT))
        assertTrue(db.deviceDao().observeAll().first().isEmpty())
        assertTrue(db.peopleDao().observeAll().first().isEmpty())
        assertEquals(RevocationRepository.WITHDRAWN_NOTICE, repo.withdrawnNotice.first())
        assertEquals(
            "This phone was withdrawn by your safety officer. Ask them for a new enrolment code.",
            repo.withdrawnNotice.first(),
        )

        repo.clearWithdrawnNotice()
        assertNull(repo.withdrawnNotice.first())
    }

    @Test
    fun aPhoneStillInUseKeepsEverything() = runTest {
        identity.setIdentity(Identity(ravi.id, ravisPhone.id, ravi.name, ravi.role))
        api.status = { DeviceStatus(revoked = false, deviceId = ravisPhone.id) }

        assertFalse(repo.checkWithdrawn())

        assertFalse(keys.deleted)
        assertEquals(ravi.name, identity.current()?.name)
        assertEquals(2, db.deviceDao().observeAll().first().size)
        assertNull(repo.withdrawnNotice.first())
    }

    @Test
    fun anOlderWithdrawnDeviceDoesNotWipeThisOne() = runTest {
        identity.setIdentity(Identity(ravi.id, "d-ravi-new", ravi.name, ravi.role))
        api.status = { DeviceStatus(revoked = true, deviceId = ravisPhone.id) }

        assertFalse(repo.checkWithdrawn())
        assertFalse(keys.deleted)
        assertNotNull(identity.current())
    }

    @Test
    fun aStatusErrorReturnsFalseSoTheWorkerRetries() = runTest {
        identity.setIdentity(Identity(ravi.id, ravisPhone.id, ravi.name, ravi.role))
        api.status = { error("Unable to resolve host") }

        assertFalse(repo.checkWithdrawn())
        assertFalse(keys.deleted)
        assertNotNull(identity.current())
    }

    private class FakeApi : DeviceAdminApi {
        var revoke: suspend (String) -> RevokeResult = { error("not set") }
        var status: suspend () -> DeviceStatus = { error("not set") }
        val revoked = mutableListOf<String>()

        override suspend fun revoke(deviceId: String): RevokeResult {
            revoked += deviceId
            return revoke.invoke(deviceId)
        }

        override suspend fun status(): DeviceStatus = status.invoke()
    }

    private class FakeKeys : DeviceKeys {
        var deleted = false
        override fun keyInfo(): KeyInfo? = null
        override fun createKey(): KeyInfo = error("not used")
        override fun sign(text: String): String = error("not used")
        override fun deleteKey() { deleted = true }
    }
}
