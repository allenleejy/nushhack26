package com.nushhack26.app.crypto

import com.nushhack26.app.data.local.DeviceEntity
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.Roles
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.data.repo.ItemChain
import com.nushhack26.app.ui.timeline.TimelineNode
import com.nushhack26.app.ui.timeline.TimelineUiState
import com.nushhack26.app.ui.timeline.buildTimeline
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64

/** Card 09: a double handover and a withdrawn phone are caught with no network. */
class OfflineChecksTest {
    private val siti = PersonEntity("a0000000-0000-4000-8000-000000000001", "Siti Aminah", Roles.STORE_OFFICER, "S-1021")
    private val ravi = PersonEntity("a0000000-0000-4000-8000-000000000002", "Ravi Kumar", Roles.RADIOGRAPHER, "R-2210")
    private val wei = PersonEntity("a0000000-0000-4000-8000-000000000003", "Tan Wei Ming", Roles.RADIOGRAPHER, "R-2234")
    private val item = ItemEntity("b0000000-0000-4000-8000-000000000012", "C-12", "IR-0457", "Ir-192", "S-40117", "Tuas Store 1")
    private val other = ItemEntity("b0000000-0000-4000-8000-000000000007", "C-07", "IR-0311", "Ir-192", "S-40007", "Tuas Store 1")
    private val keys = List(3) { KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair() }
    private val people = listOf(siti, ravi, wei)
    private val devices = people.mapIndexed { index, person ->
        val publicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(keys[index].public.encoded)
        DeviceEntity("e0000000-0000-4000-8000-00000000000${index + 1}", person.id, publicKey, Signatures.keyFp(publicKey), "software", performedAt = 0)
    }
    private val roster = Roster(people, devices, listOf(item, other))
    private val now = 1_794_466_800_000L
    private val register = registerOf(item, "c0000000-0000-4000-8000-000000000012")
    private val otherRegister = registerOf(other, "c0000000-0000-4000-8000-000000000007")

    private fun registerOf(camera: ItemEntity, id: String) = HandoverEntity(
        id, camera.id, HandoverKind.REGISTER, toPersonId = siti.id,
        recordHash = Canonical.registerHash(id, camera.id, siti.id), serverSeq = 1, syncStatus = SyncMark.SYNCED,
    )

    private fun identity(index: Int): Identity = people[index].let { Identity(it.id, devices[index].id, it.name, it.role) }
    private fun sign(key: KeyPair, text: String): String = Signature.getInstance("SHA256withECDSA").run {
        initSign(key.private)
        update(text.toByteArray(Charsets.UTF_8))
        Base64.getUrlEncoder().withoutPadding().encodeToString(sign())
    }

    private fun offer(head: HandoverEntity, from: Int, to: Int, time: Long = now): HandoverEntity {
        val unsigned = HandoverBuilder.newOffer(head, identity(from), people[to], now + 3_600_000, time)
        return HandoverBuilder.withGiverSig(unsigned, sign(keys[from], Canonical.offerString(unsigned)))
    }

    private fun complete(offer: HandoverEntity, receiver: Int, acceptedMs: Long = offer.offeredMs!! + 30_000): HandoverEntity {
        val accepted = HandoverBuilder.acceptance(offer, devices[receiver].id, item.sealNo, acceptedMs)
        return HandoverBuilder.complete(accepted, sign(keys[receiver], Canonical.acceptanceString(accepted)))
    }

    private fun clock(ms: Long) = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(ms))

    private fun withdrawn(index: Int, atMs: Long): Roster =
        roster.copy(devices = devices.mapIndexed { i, d -> if (i == index) d.copy(revokedAtMs = atMs) else d })

    @Test fun secondHandoverFromTheSamePreviousRecordIsRefused() {
        val h1 = complete(offer(register, 0, 1), 1)
        val h2 = complete(offer(h1, 1, 2, now + 60_000), 2)
        // The tampered phone signs a second offer of C-12 from H1, for Siti.
        val second = offer(h1, 1, 0, now + 120_000)
        val check = OfferChecker.check(ScannedCode.Offer(second, h1), identity(0), roster, listOf(register, h1, h2), now + 130_000)
        assertFalse(check.passed)
        assertEquals(6, check.lines.size)
        assertEquals(listOf(5), check.lines.indices.filter { !check.lines[it].ok })
        assertEquals("Ravi Kumar already handed C-12 to Tan Wei Ming at ${clock(h2.acceptedMs!!)}. This would be a second handover.", check.lines[5].text)
        assertTrue(check.looksLikeCopy)
        // Siti hasn't seen H2, so she can accept it, as the demo needs.
        assertTrue(OfferChecker.check(ScannedCode.Offer(second, h1), identity(0), roster, listOf(register, h1), now + 130_000).passed)
    }

    @Test fun hiddenRejectedOrOtherCameraRecordsDontTriggerIt() {
        val h1 = complete(offer(register, 0, 1), 1)
        val h2 = complete(offer(h1, 1, 2, now + 60_000), 2)
        val second = offer(h1, 1, 0, now + 120_000)
        fun check(local: List<HandoverEntity>) =
            OfferChecker.check(ScannedCode.Offer(second, h1), identity(0), roster, local, now + 130_000)
        for (ignored in listOf(h2.copy(hiddenByTamper = true), h2.copy(syncStatus = SyncMark.REJECTED))) {
            assertTrue(check(listOf(register, h1, ignored)).passed)
        }
        val otherIssue = complete(offer(otherRegister, 0, 1), 1)
        val otherTransfer = complete(offer(otherIssue, 1, 2, now + 60_000), 2)
        assertTrue(check(listOf(register, h1, otherRegister, otherIssue, otherTransfer)).passed)
        // A record on another camera that happens to name the same previous id still isn't this camera.
        assertTrue(check(listOf(register, h1, h2.copy(itemId = other.id))).passed)
        // The same record again is "Already used", not a second handover.
        assertEquals("Already used on this phone", check(listOf(register, h1, second)).lines[5].text)
    }

    @Test fun offerFromWithdrawnDeviceIsRefusedOnlyAfterRevocation() {
        val issue = offer(register, 0, 1)
        fun check(book: Roster) = OfferChecker.check(ScannedCode.Offer(issue, null), identity(1), book, listOf(register), now + 1_000)
        val revokedAt = now - 60_000
        val refused = check(withdrawn(0, revokedAt))
        assertFalse(refused.passed)
        assertEquals("Siti Aminah's phone was withdrawn by the safety officer at ${clock(revokedAt)}.", refused.lines[1].text)
        assertFalse(refused.looksLikeCopy)
        // Within the 15 s allowance: still refused.
        assertFalse(check(withdrawn(0, now + 15_000)).passed)
        // Withdrawn later than offered_ms + 15 s: the signature still counts.
        assertTrue(check(withdrawn(0, now + 15_001)).passed)
        // Another person's withdrawn phone doesn't matter.
        assertTrue(check(withdrawn(2, revokedAt)).passed)
    }

    @Test fun proofSignedByWithdrawnDeviceFailsLineTwo() {
        val proof = complete(offer(register, 0, 1), 1)
        val transfer = offer(proof, 1, 2, now + 60_000)
        fun check(book: Roster) = OfferChecker.check(ScannedCode.Offer(transfer, proof), identity(2), book, listOf(register), now + 61_000)
        val revokedAt = proof.acceptedMs!! - 1_000
        val refused = check(withdrawn(0, revokedAt))
        assertFalse(refused.lines[2].ok)
        assertEquals("Siti Aminah's phone was withdrawn by the safety officer at ${clock(revokedAt)}.", refused.lines[2].text)
        assertTrue(refused.lines[1].ok)
        assertTrue(check(withdrawn(0, proof.acceptedMs!! + 15_001)).passed)
    }

    @Test fun acceptanceFromWithdrawnDeviceIsRefusedOnlyAfterRevocation() {
        val offer = offer(register, 0, 1)
        val record = complete(offer, 1)
        val acceptance = QrPayloads.parse(QrPayloads.acceptance(record)) as ScannedCode.Acceptance
        val revokedAt = record.acceptedMs!! - 5_000
        val refused = OfferChecker.checkAcceptance(acceptance, offer, withdrawn(1, revokedAt))
        assertEquals(CheckLine(false, "Ravi Kumar's phone was withdrawn by the safety officer at ${clock(revokedAt)}."), refused)
        assertFalse(OfferChecker.checkAcceptance(acceptance, offer, withdrawn(1, record.acceptedMs!! + 15_000)).ok)
        assertTrue(OfferChecker.checkAcceptance(acceptance, offer, withdrawn(1, record.acceptedMs!! + 15_001)).ok)
        assertTrue(OfferChecker.checkAcceptance(acceptance, offer, withdrawn(0, revokedAt)).ok)
    }

    @Test fun localForkTurnsFalseOnceOneBranchHasAServerSeq() {
        val h1 = complete(offer(register, 0, 1), 1).copy(serverSeq = 2, syncStatus = SyncMark.SYNCED)
        val a = complete(offer(h1, 1, 2, now + 60_000), 2)
        val b = complete(offer(h1, 1, 0, now + 120_000), 0)
        val local = deriveCustody(item, listOf(register, h1, a, b), emptyList(), now)
        assertEquals(CustodyStatus.Disputed, local.status)
        assertTrue(local.localFork)
        val oneSynced = deriveCustody(item, listOf(register, h1, a.copy(serverSeq = 3), b), emptyList(), now)
        assertEquals(CustodyStatus.Disputed, oneSynced.status)
        assertFalse(oneSynced.localFork)
        assertFalse(deriveCustody(item, listOf(register, h1, a), emptyList(), now).localFork)
        assertFalse(deriveCustody(item, listOf(register, h1, a.copy(serverSeq = 3), b.copy(serverSeq = 4)), emptyList(), now, serverView = true).localFork)
    }

    @Test fun timelineWarnsAboveTheBranchesAndMarksWithdrawnKeys() {
        val h1 = complete(offer(register, 0, 1), 1).copy(serverSeq = 2, syncStatus = SyncMark.SYNCED)
        val a = complete(offer(h1, 1, 2, now + 60_000), 2)
        val b = complete(offer(h1, 1, 0, now + 120_000), 0)
        val records = listOf(register, h1, a, b)
        val revokedAt = now + 600_000
        val book = withdrawn(1, revokedAt)
        val state = buildTimeline(ItemChain(now, item, deriveCustody(item, records, emptyList(), now), records, emptyList(), book)) as TimelineUiState.Ready
        val warning = state.nodes.indexOfFirst { it is TimelineNode.LocalFork }
        assertEquals(state.nodes.indexOfFirst { it is TimelineNode.Fork } - 1, warning)
        assertEquals(
            "This phone holds two handovers of C-12 from Ravi Kumar after H1. Only one can be right. The safety officer will decide once this phone syncs.",
            (state.nodes[warning] as TimelineNode.LocalFork).text,
        )
        val issueEntry = (state.nodes[1] as TimelineNode.Record).entry
        assertEquals(revokedAt, issueEntry.keyWithdrawn?.atMs)
        assertTrue(issueEntry.keyWithdrawn!!.afterSigning)
        assertNull((state.nodes[0] as TimelineNode.Record).entry.keyWithdrawn)

        val synced = records.map { if (it.id == a.id) it.copy(serverSeq = 3) else it }
        val later = buildTimeline(ItemChain(now, item, deriveCustody(item, synced, emptyList(), now), synced, emptyList(), roster)) as TimelineUiState.Ready
        assertTrue(later.nodes.none { it is TimelineNode.LocalFork })
    }
}
