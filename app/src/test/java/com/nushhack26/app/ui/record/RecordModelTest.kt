package com.nushhack26.app.ui.record

import com.nushhack26.app.crypto.Canonical
import com.nushhack26.app.crypto.HandoverBuilder
import com.nushhack26.app.crypto.Roster
import com.nushhack26.app.crypto.Signatures
import com.nushhack26.app.data.local.DeviceEntity
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.Roles
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.ui.components.ReceiptNumbers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.time.ZoneId
import java.util.Base64

/** Card 30: what a handover's record receipt prints. */
class RecordModelTest {
    private val zone = ZoneId.of("Asia/Singapore")
    private val siti = PersonEntity("a0000000-0000-4000-8000-000000000001", "Siti Aminah", Roles.STORE_OFFICER, "S-1021")
    private val ravi = PersonEntity("a0000000-0000-4000-8000-000000000002", "Ravi Kumar", Roles.RADIOGRAPHER, "R-2210")
    private val wei = PersonEntity("a0000000-0000-4000-8000-000000000003", "Tan Wei Ming", Roles.RADIOGRAPHER, "R-2234")
    private val item = ItemEntity("b0000000-0000-4000-8000-000000000012", "C-12", "IR-0457", "Ir-192", "S-40117", "Tuas Store 1", name = "Kestrel")
    private val keys = List(3) { KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair() }
    private val people = listOf(siti, ravi, wei)
    private val devices = people.mapIndexed { index, person ->
        val publicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(keys[index].public.encoded)
        DeviceEntity("e0000000-0000-4000-8000-00000000000${index + 1}", person.id, publicKey, Signatures.keyFp(publicKey), "software", performedAt = 0)
    }
    private val roster = Roster(people, devices, listOf(item))

    /** 07.11.2026 13:02 in Singapore. */
    private val now = 1_794_027_720_000L
    private val register = HandoverEntity(
        "c0000000-0000-4000-8000-000000000012", item.id, HandoverKind.REGISTER, toPersonId = siti.id,
        recordHash = Canonical.registerHash("c0000000-0000-4000-8000-000000000012", item.id, siti.id),
        serverSeq = 1, receivedAt = now - 86_400_000, syncStatus = SyncMark.SYNCED,
    )

    private fun identity(index: Int): Identity = people[index].let { Identity(it.id, devices[index].id, it.name, it.role) }
    private fun sign(key: KeyPair, text: String): String = Signature.getInstance("SHA256withECDSA").run {
        initSign(key.private)
        update(text.toByteArray(Charsets.UTF_8))
        Base64.getUrlEncoder().withoutPadding().encodeToString(sign())
    }

    private fun handover(head: HandoverEntity, from: Int, to: Int, offeredMs: Long): HandoverEntity {
        val unsigned = HandoverBuilder.newOffer(head, identity(from), people[to], offeredMs + 3_600_000, offeredMs)
        val offer = HandoverBuilder.withGiverSig(unsigned, sign(keys[from], Canonical.offerString(unsigned)))
        val accepted = HandoverBuilder.acceptance(offer, devices[to].id, item.sealNo, offeredMs + 30_000)
        return HandoverBuilder.complete(accepted, sign(keys[to], Canonical.acceptanceString(accepted)))
    }

    private val issue = handover(register, 0, 1, now - 3_600_000).synced(seq = 2)
    private val transfer = handover(issue, 1, 2, now - 30_000).synced(seq = 3)
    private val chain = listOf(register, issue, transfer)

    private fun HandoverEntity.synced(seq: Long, receivedAt: Long = acceptedMs!! + 2_000) =
        copy(serverSeq = seq, receivedAt = receivedAt, syncStatus = SyncMark.SYNCED)

    private fun receipt(h: HandoverEntity, r: Roster = roster) = recordReceipt(h, chain, item, r, now, zone)

    @Test fun signedHandoverPrintsNumbersKeysAndVerified() {
        val (content, stamp) = receipt(transfer)
        assertEquals(3, content.recordNo)
        assertEquals(2, content.followsNo)
        assertEquals("07.11.2026", content.date)
        assertEquals("13:02", content.signedTime)
        assertEquals("Kestrel", content.itemName)
        assertEquals("C-12", content.itemCode)
        assertEquals("IR-0457 · Ir-192", content.source)
        assertEquals("S-40117", content.sealNo)
        assertEquals("Ravi Kumar", content.from?.name)
        assertEquals("KEY ${devices[1].keyFp}", content.from?.key)
        assertEquals("Tan Wei Ming", content.to.name)
        assertEquals("KEY ${devices[2].keyFp}", content.to.key)
        assertEquals("Tan Wei Ming", content.heldBy)
        assertEquals("13:02", content.returnBy)
        assertNull(content.via)
        assertEquals("2 OF 2 VERIFIED", content.signatures)
        assertTrue(content.signaturesOk)
        assertEquals(transfer.recordHash, content.hash)
        assertEquals(ReceiptNumbers.verifyCode(Canonical.matchCode(transfer.recordHash)), content.verifyCode)
        assertEquals(7, content.verifyCode.length)
        assertFalse(content.offline)
        assertEquals(RECORD_SYNCED, content.footer)
        assertEquals("COUNTERSIGNED", stamp.word)
        assertEquals("13:02", stamp.time)
        assertEquals("WEI MING", stamp.line)
    }

    @Test fun registerRecordIsNumberOneWithNoFrom() {
        val (content, stamp) = receipt(register)
        assertEquals(1, content.recordNo)
        assertNull(content.followsNo)
        assertNull(content.from)
        assertEquals("Siti Aminah", content.to.name)
        assertNull(content.to.key)
        assertEquals("REGISTERED BY THE SERVER", content.signatures)
        assertTrue(content.signaturesOk)
        assertFalse(content.offline)
        assertEquals("REGISTERED", stamp.word)
        assertEquals("IN STORE", stamp.line)
        assertEquals("06.11.2026", content.date)
    }

    @Test fun unknownReceiverKeyLeavesTheKeyLineOut() {
        val withoutWei = roster.copy(devices = devices.take(2))
        val (content, _) = receipt(transfer, withoutWei)
        assertEquals("KEY ${devices[1].keyFp}", content.from?.key)
        assertEquals("Tan Wei Ming", content.to.name)
        assertNull(content.to.key)
        assertEquals("SIGNER NOT ON THIS PHONE", content.signatures)
        assertFalse(content.signaturesOk)
    }

    @Test fun tamperedRecordIsInvalid() {
        val (content, _) = receipt(transfer.copy(sealNo = "S-99999"))
        assertEquals("SIGNATURE INVALID", content.signatures)
        assertFalse(content.signaturesOk)
    }

    @Test fun unsyncedRecordIsOfflineAndNotSynced() {
        val waiting = transfer.copy(serverSeq = null, receivedAt = null, syncStatus = SyncMark.WAITING)
        val (content, _) = receipt(waiting)
        assertTrue(content.offline)
        assertEquals(RECORD_NOT_SYNCED, content.footer)
    }

    @Test fun lateArrivalIsOfflineButSynced() {
        val late = transfer.synced(seq = 3, receivedAt = transfer.acceptedMs!! + 61_000)
        val (content, _) = receipt(late)
        assertTrue(content.offline)
        assertEquals(RECORD_SYNCED, content.footer)
        assertFalse(receipt(transfer.synced(seq = 3, receivedAt = transfer.acceptedMs!! + 60_000)).first.offline)
    }

    @Test fun blankHashPrintsNoHashOrCode() {
        val (content, _) = receipt(transfer.copy(recordHash = ""))
        assertNull(content.hash)
        assertEquals("", content.verifyCode)
    }
}
