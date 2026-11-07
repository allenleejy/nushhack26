package com.nushhack26.app.ui.receive

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nushhack26.app.crypto.Canonical
import com.nushhack26.app.crypto.DeviceKeys
import com.nushhack26.app.crypto.HandoverBuilder
import com.nushhack26.app.crypto.KeyInfo
import com.nushhack26.app.crypto.QrPayloads
import com.nushhack26.app.crypto.Signatures
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.local.DeviceEntity
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.Roles
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.remote.OfferRelayApi
import com.nushhack26.app.data.remote.RelayResult
import com.nushhack26.app.data.remote.RelayStatus
import com.nushhack26.app.data.remote.RelayedOffer
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.data.repo.IdentityRepository
import com.nushhack26.app.data.repo.ReceiveRepository
import com.nushhack26.app.data.repo.RelayRepository
import com.nushhack26.app.ui.components.SignResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/** Card 15: a relayed offer goes through the same checks, and the acceptance goes back through the server. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RelayedReceiveTest {
    private val siti = PersonEntity("a0000000-0000-4000-8000-000000000001", "Siti Aminah", Roles.STORE_OFFICER, "S-1021")
    private val ravi = PersonEntity("a0000000-0000-4000-8000-000000000002", "Ravi Kumar", Roles.RADIOGRAPHER, "R-2210")
    private val wei = PersonEntity("a0000000-0000-4000-8000-000000000003", "Tan Wei Ming", Roles.RADIOGRAPHER, "R-2234")
    private val people = listOf(siti, ravi, wei)
    private val item = ItemEntity("b0000000-0000-4000-8000-000000000012", "C-12", "IR-0457", "Ir-192", "S-40117", "Tuas Store 1")
    private val keys = List(3) { KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair() }
    private val devices = people.mapIndexed { i, person ->
        val publicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(keys[i].public.encoded)
        DeviceEntity("e0000000-0000-4000-8000-00000000000${i + 1}", person.id, publicKey, Signatures.keyFp(publicKey), "software", performedAt = 0)
    }
    private val registerId = "c0000000-0000-4000-8000-000000000012"
    private val register = HandoverEntity(
        registerId, item.id, HandoverKind.REGISTER, toPersonId = siti.id,
        recordHash = Canonical.registerHash(registerId, item.id, siti.id), serverSeq = 1, syncStatus = SyncMark.SYNCED,
    )

    private val main = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var db: AppDatabase
    private lateinit var api: FakeRelayApi
    private lateinit var repo: ReceiveRepository
    private lateinit var identity: IdentityRepository

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(main)
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        db.peopleDao().upsertAll(people)
        db.deviceDao().upsertAll(devices)
        db.itemDao().upsertAll(listOf(item))
        db.handoverDao().insert(register)
        identity = IdentityRepository(db.syncStateDao())
        identity.setIdentity(Identity(ravi.id, devices[1].id, ravi.name, ravi.role))
        identity.setEnrolment(devices[1].publicKey, System.currentTimeMillis())
        repo = ReceiveRepository(db, requestSync = {})
        api = FakeRelayApi()
    }

    @After
    fun tearDown() {
        appScope.cancel()
        db.close()
        Dispatchers.resetMain()
        main.close()
    }

    private fun sign(key: KeyPair, text: String): String = Signature.getInstance("SHA256withECDSA").run {
        initSign(key.private)
        update(text.toByteArray(Charsets.UTF_8))
        Base64.getUrlEncoder().withoutPadding().encodeToString(sign())
    }

    /** Siti's relayed offer: signed for three minutes, as card 14 signs it. */
    private fun relayedOffer(to: PersonEntity = ravi): RelayedOffer {
        val now = System.currentTimeMillis()
        val giver = Identity(siti.id, devices[0].id, siti.name, siti.role)
        val unsigned = HandoverBuilder.newOffer(register, giver, to, now + 3_600_000, now)
            .copy(expiresMs = now + RelayRepository.RELAYED_OFFER_VALID_MS)
        val signed = HandoverBuilder.withGiverSig(unsigned, sign(keys[0], Canonical.offerString(unsigned)))
        return RelayedOffer(signed.id, item.id, siti.id, QrPayloads.offer(signed, null), signed.expiresMs!!)
    }

    private fun viewModel(offerId: String): ReceiveViewModel {
        val relay = RelayRepository(api, MutableStateFlow(true), identity, appScope)
        val ravisKeys = object : DeviceKeys {
            override fun keyInfo(): KeyInfo? = null
            override fun createKey(): KeyInfo = error("not used")
            override fun sign(text: String): String = sign(keys[1], text)
        }
        return runBlocking(main) { ReceiveViewModel(repo, identity, { ravisKeys }, relay = relay, relayedOfferId = offerId, appScope = appScope) }
    }

    private fun ReceiveViewModel.await(predicate: (ReceiveUiState) -> Boolean): ReceiveUiState =
        runBlocking { withTimeout(10_000) { state.first(predicate) } }

    private fun ReceiveViewModel.on(action: ReceiveAction) = runBlocking(main) { onAction(action) }

    private fun ReceiveViewModel.checksDone() =
        await { it.step == ReceiveStep.Checks && it.revealedLines >= it.lines.size }

    /** R2 passed → R3 seal → R4 → fingerprint. */
    private fun ReceiveViewModel.acceptWithSeal() {
        assertTrue(checksDone().passed)
        on(ReceiveAction.ScanSeal)
        on(ReceiveAction.SealScanned(QrPayloads.seal(item.code, item.sealNo)))
        await { it.seal?.matches == true }
        on(ReceiveAction.Next)
        await { it.step == ReceiveStep.Accept }
        runBlocking(main) {
            assertTrue(prepareAccept())
            finishAccept(SignResult.Approved)
        }
    }

    private fun handoversOnPhone() = runBlocking { db.handoverDao().all() }

    @Test
    fun acceptanceGoesBackThroughTheServer() {
        val offer = relayedOffer().also { api.inbox = listOf(it) }
        val vm = viewModel(offer.id)
        val lines = vm.checksDone().lines
        assertEquals(6, lines.size)
        assertTrue(lines.joinToString { it.text }, lines.all { it.ok })
        assertTrue(lines[3].text.startsWith("Valid for 2:"))

        vm.acceptWithSeal()

        val done = vm.await { it.step == ReceiveStep.Done }
        assertTrue(done.sentOnline)
        val (id, acceptance) = api.accepted.single()
        assertEquals(offer.id, id)
        assertTrue(acceptance.startsWith("CS1A:"))
        val record = handoversOnPhone().single { it.id == offer.id }
        assertEquals(done.matchCode, Canonical.matchCode(record.recordHash))
        assertTrue(api.closed.isEmpty())
    }

    @Test
    fun aFailedSendFallsBackToTheAcceptanceQr() {
        val offer = relayedOffer().also { api.inbox = listOf(it) }
        api.acceptResult = "rejected"
        val vm = viewModel(offer.id)

        vm.acceptWithSeal()

        val shown = vm.await { it.step == ReceiveStep.ShowAcceptance }
        assertEquals("Couldn't reach the server. Show this to Siti instead.", shown.fallbackText)
        assertTrue(shown.acceptanceQr!!.startsWith("CS1A:"))
        assertFalse(shown.sentOnline)
        assertTrue(handoversOnPhone().any { it.id == offer.id })
    }

    @Test
    fun declineClosesTheOfferAndStoresNothing() {
        val offer = relayedOffer().also { api.inbox = listOf(it) }
        val vm = viewModel(offer.id)
        vm.checksDone()

        vm.on(ReceiveAction.Decline)

        val declined = vm.await { it.step == ReceiveStep.Declined }
        assertEquals("Declined. Siti still holds C-12.", declined.declinedText)
        assertEquals(listOf(offer.id), api.closed)
        assertEquals(listOf(registerId), handoversOnPhone().map { it.id })
        val hidden = runBlocking { repo.observeBannerLookup().first() }.doneOfferIds
        assertTrue(offer.id in hidden)
    }

    @Test
    fun anOfferNoLongerOpenNamesTheGiver() {
        val offer = relayedOffer()
        repo.rememberRelayedGiver(offer.id, siti.id)
        val vm = viewModel(offer.id)

        val shown = vm.await { it.step == ReceiveStep.Unavailable }
        assertEquals("This offer is no longer open. Ask Siti to send it again.", shown.message)
        assertTrue(api.closed.isEmpty())
    }

    @Test
    fun anOfferNoLongerOpenWithAnUnknownGiver() {
        val vm = viewModel(relayedOffer().id)
        assertEquals("This offer is no longer open. Ask the giver to send it again.", vm.await { it.step == ReceiveStep.Unavailable }.message)
    }

    @Test
    fun aRefusedRelayedOfferIsClosedAndNothingIsStored() {
        val offer = relayedOffer(to = wei).also { api.inbox = listOf(it) }
        val vm = viewModel(offer.id)

        val checks = vm.checksDone()
        assertFalse(checks.passed)
        assertEquals("Not intended recipient · for Tan Wei Ming", checks.lines[2].text)
        runBlocking { withTimeout(10_000) { while (api.closed.isEmpty()) kotlinx.coroutines.delay(20) } }
        assertEquals(listOf(offer.id), api.closed)
        assertEquals(listOf(registerId), handoversOnPhone().map { it.id })
        assertNull(api.accepted.firstOrNull())
    }
}

private class FakeRelayApi : OfferRelayApi {
    @Volatile var inbox: List<RelayedOffer> = emptyList()
    @Volatile var acceptResult = "accepted"
    val accepted = CopyOnWriteArrayList<Pair<String, String>>()
    val closed = CopyOnWriteArrayList<String>()

    override suspend fun post(id: String, itemId: String, toPersonId: String, offer: String, expiresMs: Long) = RelayResult("posted")
    override suspend fun inbox(): List<RelayedOffer> = inbox
    override suspend fun accept(id: String, acceptance: String): RelayResult {
        accepted += id to acceptance
        return RelayResult(acceptResult, reason = if (acceptResult == "rejected") "not open" else null)
    }
    override suspend fun close(id: String): RelayResult {
        closed += id
        return RelayResult("closed")
    }
    override suspend fun status(id: String) = RelayStatus("open")
}
