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
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.data.repo.IdentityRepository
import com.nushhack26.app.data.repo.ReceiveRepository
import com.nushhack26.app.ui.components.SignResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

/** Card 17: a tapped offer gets the same checks, and the acceptance goes back over the tap link. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TapReceiveTest {
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
    private lateinit var db: AppDatabase
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
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
        main.close()
    }

    private fun sign(key: KeyPair, text: String): String = Signature.getInstance("SHA256withECDSA").run {
        initSign(key.private)
        update(text.toByteArray(Charsets.UTF_8))
        Base64.getUrlEncoder().withoutPadding().encodeToString(sign())
    }

    /** Siti's offline offer, the `CS1O:` string the tap carries. */
    private fun offer(to: PersonEntity = ravi): String {
        val now = System.currentTimeMillis()
        val giver = Identity(siti.id, devices[0].id, siti.name, siti.role)
        val unsigned = HandoverBuilder.newOffer(register, giver, to, now + 3_600_000, now)
        return QrPayloads.offer(HandoverBuilder.withGiverSig(unsigned, sign(keys[0], Canonical.offerString(unsigned))), null)
    }

    private class FakeTapSession(private val delivers: Boolean) : TapSession {
        override val linked: StateFlow<Boolean> = MutableStateFlow(true)
        val sent = CopyOnWriteArrayList<String>()
        @Volatile var closed = false
        override suspend fun send(text: String): Boolean {
            sent += text
            return delivers
        }
        override fun close() { closed = true }
    }

    private fun viewModel(): ReceiveViewModel {
        val ravisKeys = object : DeviceKeys {
            override fun keyInfo(): KeyInfo? = null
            override fun createKey(): KeyInfo = error("not used")
            override fun sign(text: String): String = sign(keys[1], text)
        }
        return runBlocking(main) { ReceiveViewModel(repo, identity, { ravisKeys }) }
    }

    private fun ReceiveViewModel.await(predicate: (ReceiveUiState) -> Boolean): ReceiveUiState =
        runBlocking { withTimeout(10_000) { state.first(predicate) } }

    private fun ReceiveViewModel.on(action: ReceiveAction) = runBlocking(main) { onAction(action) }

    private fun ReceiveViewModel.tap(offer: String, session: FakeTapSession) {
        await { it.step == ReceiveStep.ScanOffer }
        runBlocking(main) { onTapped(offer) { session } }
    }

    private fun ReceiveViewModel.checksDone() =
        await { it.step == ReceiveStep.Checks && it.revealedLines >= it.lines.size }

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
    fun acceptanceGoesBackOverTheTapLink() {
        val session = FakeTapSession(delivers = true)
        val vm = viewModel()
        vm.tap(offer(), session)
        assertTrue(vm.checksDone().linked)

        vm.acceptWithSeal()

        val done = vm.await { it.step == ReceiveStep.Done }
        assertTrue(done.sentTap)
        val acceptance = session.sent.single()
        assertTrue(acceptance.startsWith("CS1A:"))
        assertEquals(done.acceptanceQr, acceptance)
        val record = handoversOnPhone().single { it.id != registerId }
        assertEquals(done.matchCode, Canonical.matchCode(record.recordHash))
        assertTrue(session.closed)
    }

    @Test
    fun noLinkFallsBackToTheAcceptanceQr() {
        val session = FakeTapSession(delivers = false)
        val vm = viewModel()
        vm.tap(offer(), session)

        vm.acceptWithSeal()

        val shown = vm.await { it.step == ReceiveStep.ShowAcceptance }
        assertEquals("Couldn't reach Siti's phone. Show this instead.", shown.fallbackText)
        assertTrue(shown.acceptanceQr!!.startsWith("CS1A:"))
        assertFalse(shown.sentTap)
        assertTrue(session.closed)
        assertEquals(2, handoversOnPhone().size)
    }

    @Test
    fun aTappedOfferForSomeoneElseIsRefusedAndNothingIsSent() {
        val session = FakeTapSession(delivers = true)
        val vm = viewModel()
        vm.tap(offer(to = wei), session)

        val checks = vm.checksDone()
        assertFalse(checks.passed)
        assertTrue(checks.looksLikeCopy)
        assertFalse(checks.linked)
        assertTrue(session.closed)
        assertTrue(session.sent.isEmpty())
        assertEquals(listOf(registerId), handoversOnPhone().map { it.id })
    }
}
