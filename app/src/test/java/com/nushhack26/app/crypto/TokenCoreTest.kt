package com.nushhack26.app.crypto

import com.nushhack26.app.data.local.DeviceEntity
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.ResolutionEntity
import com.nushhack26.app.data.local.Roles
import com.nushhack26.app.data.local.ServerStatus
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.repo.Identity
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.UUID

class TokenCoreTest {
    private val siti = PersonEntity("a0000000-0000-4000-8000-000000000001", "Siti Aminah", Roles.STORE_OFFICER, "S-1021")
    private val ravi = PersonEntity("a0000000-0000-4000-8000-000000000002", "Ravi Kumar", Roles.RADIOGRAPHER, "R-2210")
    private val wei = PersonEntity("a0000000-0000-4000-8000-000000000003", "Tan Wei Ming", Roles.RADIOGRAPHER, "R-2234")
    private val item = ItemEntity("b0000000-0000-4000-8000-000000000012", "C-12", "IR-0457", "Ir-192", "S-40117", "Tuas Store 1")
    private val keys = List(3) { KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair() }
    private val people = listOf(siti, ravi, wei)
    private val devices = people.mapIndexed { index, person ->
        val publicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(keys[index].public.encoded)
        DeviceEntity("e0000000-0000-4000-8000-00000000000${index + 1}", person.id, publicKey, Signatures.keyFp(publicKey), "software", performedAt = 0)
    }
    private val roster = Roster(people, devices, listOf(item))
    private val now = 1_794_466_800_000L
    private val register = HandoverEntity(
        "c0000000-0000-4000-8000-000000000012", item.id, HandoverKind.REGISTER, toPersonId = siti.id,
        recordHash = "eb882341f1e56ea7f674e5bc2c65855ae8ba5fe5e460d255c40795c870a5ebc4",
        serverSeq = 1, syncStatus = SyncMark.SYNCED,
    )

    private fun identity(index: Int): Identity = people[index].let { Identity(it.id, devices[index].id, it.name, it.role) }
    private fun sign(index: Int, text: String): String = sign(keys[index], text)
    private fun sign(key: KeyPair, text: String): String = Signature.getInstance("SHA256withECDSA").run {
        initSign(key.private)
        update(text.toByteArray(Charsets.UTF_8))
        Base64.getUrlEncoder().withoutPadding().encodeToString(sign())
    }

    private fun offer(head: HandoverEntity = register, from: Int = 0, to: Int = 1, time: Long = now): HandoverEntity {
        val unsigned = HandoverBuilder.newOffer(head, identity(from), people[to], now + 3_600_000, time)
        return HandoverBuilder.withGiverSig(unsigned, sign(from, Canonical.offerString(unsigned)))
    }

    private fun complete(offer: HandoverEntity, receiver: Int): HandoverEntity {
        val accepted = HandoverBuilder.acceptance(offer, devices[receiver].id, item.sealNo, offer.offeredMs!! + 30_000)
        return HandoverBuilder.complete(accepted, sign(receiver, Canonical.acceptanceString(accepted)))
    }

    @Test fun canonicalVectorsAgreeWithSql() {
        assertEquals(register.recordHash, Canonical.registerHash(register.id, item.id, siti.id))
        val record = HandoverEntity(
            id = "d0000000-0000-4000-8000-000000000001", itemId = item.id, kind = "issue",
            prevId = register.id, prevHash = register.recordHash, fromPersonId = siti.id,
            fromDeviceId = devices[0].id, toPersonId = ravi.id, dueMs = 1_794_470_400_000,
            nonce = "AAECAwQFBgcICQoLDA0ODw", offeredMs = 1_794_466_800_000, expiresMs = 1_794_466_860_000,
            giverSig = "GIVERSIG", toDeviceId = devices[1].id, sealNo = item.sealNo,
            acceptedMs = 1_794_466_830_000, receiverSig = "RECEIVERSIG",
        )
        assertEquals("3ac4a29e2ac34f045ad932221b2c0e143bc32644546b768e9d406b79bd4a2993", Canonical.recordHash(record))
        assertEquals("96 52 14", Canonical.matchCode(Canonical.recordHash(record)))
        assertEquals("96 72 95", Canonical.matchCode("f".repeat(64)))
        val returned = record.copy(kind = "return", dueMs = null)
        assertTrue(Canonical.offerString(returned).contains("|${ravi.id}||AAECAwQFBgcICQoLDA0ODw|"))
    }

    @Test fun resolutionHashesTheUtf8Note() {
        val resolution = resolution("chosen").copy(note = "Seal checked · 安全")
        val hash = MessageDigest.getInstance("SHA-256").digest(resolution.note.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        assertEquals("CS1-RESOLVE|${resolution.id}|${resolution.alertId}|${item.id}|chosen|${siti.id}|${devices[0].id}|$hash|$now", Canonical.resolutionString(resolution))
        assertNotEquals(Canonical.resolutionString(resolution), Canonical.resolutionString(resolution.copy(note = "changed")))
    }

    @Test fun signaturesAndFingerprintUseSpkiAndDer() {
        val text = "offline · camera C-12"
        val signature = sign(0, text)
        assertTrue(Signatures.verify(devices[0].publicKey, text, signature))
        assertFalse(Signatures.verify(devices[0].publicKey, "$text!", signature))
        assertFalse(Signatures.verify(devices[1].publicKey, text, signature))
        assertFalse(Signatures.verify("bad key", text, signature))
        assertFalse(Signatures.verify(devices[0].publicKey, text, "not-DER"))
        val expected = MessageDigest.getInstance("SHA-256").digest(keys[0].public.encoded).take(4)
            .joinToString(":") { "%02X".format(it.toInt() and 255) }
        assertEquals(expected, Signatures.keyFp(devices[0].publicKey))
        assertFalse(signature.contains('='))
    }

    @Test fun changingAnySignedFieldBreaksItsSignature() {
        val h = complete(offer(), 1)
        val mutations = listOf(
            h.copy(id = UUID.randomUUID().toString()), h.copy(itemId = UUID.randomUUID().toString()),
            h.copy(kind = "transfer"), h.copy(prevId = null), h.copy(prevHash = null),
            h.copy(fromPersonId = wei.id), h.copy(fromDeviceId = devices[2].id), h.copy(toPersonId = wei.id),
            h.copy(dueMs = h.dueMs!! + 1), h.copy(nonce = "changed"), h.copy(offeredMs = now + 1), h.copy(expiresMs = now + 2),
        )
        for (changed in mutations) {
            assertFalse(Signatures.verify(devices[0].publicKey, Canonical.offerString(changed), h.giverSig!!))
            assertFalse(Signatures.verify(devices[1].publicKey, Canonical.acceptanceString(changed), h.receiverSig!!))
        }
        for (changed in listOf(h.copy(giverSig = "changed"), h.copy(toDeviceId = devices[2].id), h.copy(sealNo = "S-40999"), h.copy(acceptedMs = now))) {
            assertFalse(Signatures.verify(devices[1].publicKey, Canonical.acceptanceString(changed), h.receiverSig!!))
            assertNotEquals(h.recordHash, Canonical.recordHash(changed))
        }
        assertNotEquals(h.recordHash, Canonical.recordHash(h.copy(receiverSig = "changed")))
    }

    @Test fun builderFollowsRolesDueTimesAndSigningStages() {
        val issue = offer()
        assertEquals(HandoverKind.ISSUE, issue.kind)
        assertEquals(now + 60_000, issue.expiresMs)
        assertEquals(register.recordHash, issue.prevHash)
        assertEquals(16, Base64.getUrlDecoder().decode(issue.nonce).size)
        assertNotEquals(issue.id, offer().id)
        assertNotEquals(issue.nonce, offer().nonce)
        val signed = complete(issue, 1)
        val transfer = offer(signed, 1, 2)
        assertEquals(HandoverKind.TRANSFER, transfer.kind)
        assertEquals(signed.dueMs, transfer.dueMs)
        val returned = offer(signed, 1, 0)
        assertEquals(HandoverKind.RETURN, returned.kind)
        assertNull(returned.dueMs)
        val acceptance = QrPayloads.parse(QrPayloads.acceptance(signed)) as ScannedCode.Acceptance
        assertEquals(signed, HandoverBuilder.fromAcceptance(issue, acceptance))
        assertEquals(Canonical.recordHash(signed), signed.recordHash)
        assertThrows(IllegalArgumentException::class.java) { HandoverBuilder.newOffer(register, identity(1), siti, now, now) }
        assertThrows(IllegalArgumentException::class.java) { HandoverBuilder.fromAcceptance(issue, acceptance.copy(id = "wrong")) }
    }

    @Test fun offerQrWithRealSignedProofIsUnder1100AndRoundTrips() {
        repeat(20) {
            val proof = complete(offer(), 1)
            val transfer = offer(proof, 1, 2)
            val encoded = QrPayloads.offer(transfer, proof)
            assertTrue("QR length ${encoded.length}", encoded.length <= 1_100)
            assertEquals(ScannedCode.Offer(transfer, proof), QrPayloads.parse(encoded))
            val parsed = QrPayloads.parse(encoded) as ScannedCode.Offer
            assertTrue(OfferChecker.check(parsed, identity(2), roster, listOf(register), now).passed)
        }
        val issue = offer()
        assertEquals(ScannedCode.Offer(issue, null), QrPayloads.parse(QrPayloads.offer(issue, null)))
        assertEquals(ScannedCode.Seal(item.code, item.sealNo), QrPayloads.parse(QrPayloads.seal(item.code, item.sealNo)))
        val proof = complete(issue, 1)
        val returned = offer(proof, 1, 0)
        assertEquals(ScannedCode.Offer(returned, proof), QrPayloads.parse(QrPayloads.offer(returned, proof)))
    }

    @Test fun malformedCodesAreUnknown() {
        for (text in listOf("", "CS2O:{}", "CS1O:{}", "CS1O:[1]", "CS1O:{", "CS1A:{}", "CS1S:C-12", "CS1S::seal", "CS1S:C-12:bad|seal", "CS1O:" + "a".repeat(1_100))) {
            assertEquals(ScannedCode.Unknown(text), QrPayloads.parse(text))
        }
        val qr = QrPayloads.offer(offer(), null)
        assertTrue(QrPayloads.parse(qr.replace("\"k\":\"i\"", "\"k\":\"z\"")) is ScannedCode.Unknown)
        assertTrue(QrPayloads.parse(qr.replace("\"n\":\"", "\"n\":\"!")) is ScannedCode.Unknown)
    }

    @Test fun allSixOfferChecksAndCopyClassification() {
        val signed = offer()
        fun check(h: HandoverEntity = signed, local: List<HandoverEntity> = listOf(register), book: Roster = roster, time: Long = now) =
            OfferChecker.check(ScannedCode.Offer(h, null), identity(1), book, local, time)
        val ok = check()
        assertTrue(ok.passed)
        assertEquals(6, ok.lines.size)
        assertTrue(ok.lines.all { it.ok })
        assertFalse(ok.looksLikeCopy)
        assertTrue(ok.lines[1].text.startsWith("Signed by Siti Aminah · key "))
        assertEquals("Intended recipient", ok.lines[3].text)
        assertEquals("Valid for 1:00", ok.lines[4].text)
        val otherUnsigned = signed.copy(toPersonId = wei.id)
        val other = check(HandoverBuilder.withGiverSig(otherUnsigned, sign(0, Canonical.offerString(otherUnsigned))))
        assertEquals(listOf(3), other.lines.indices.filter { !other.lines[it].ok })
        assertEquals("Not intended recipient · for Tan Wei Ming", other.lines[3].text)
        assertTrue(other.looksLikeCopy)
        val expired = check(time = now + 100_000)
        assertEquals("Expired 0:40 ago", expired.lines[4].text)
        assertTrue(expired.looksLikeCopy)
        assertTrue(check(time = now - 15_000).passed)
        assertFalse(check(time = now - 15_001).passed)
        assertTrue(check(time = now + 75_000).passed)
        assertFalse(check(time = now + 75_001).passed)
        for (used in listOf(signed.copy(id = UUID.randomUUID().toString()), signed.copy(nonce = "another nonce"))) {
            val result = check(local = listOf(register, used))
            assertEquals("Already used on this phone", result.lines[5].text)
            assertTrue(result.looksLikeCopy)
        }
        val unknown = check(book = roster.copy(devices = emptyList()))
        assertFalse(unknown.lines[1].ok)
        assertEquals("Unknown signer. Sync this phone before accepting.", unknown.lines[1].text)
        assertFalse(unknown.looksLikeCopy)
        val badSignature = check(signed.copy(giverSig = "bad"))
        assertEquals("Signature doesn't match", badSignature.lines[1].text)
        assertFalse(badSignature.looksLikeCopy)
        val notHolder = check(local = listOf(register.copy(toPersonId = wei.id)))
        assertFalse(notHolder.lines[2].ok)
        assertFalse(notHolder.looksLikeCopy)
        assertFalse(check(local = emptyList()).passed)
        assertFalse(check(book = roster.copy(devices = listOf(devices[0].copy(personId = wei.id)))).passed)
    }

    @Test fun proofMustBeSignedLinkedAndForThisItemAndHolder() {
        val proof = complete(offer(), 1)
        val transfer = offer(proof, 1, 2)
        fun check(p: HandoverEntity = proof, records: List<HandoverEntity> = listOf(register)): OfferCheck =
            OfferChecker.check(ScannedCode.Offer(transfer, p), identity(2), roster, records, now)
        assertTrue(check().passed)
        assertTrue(check().lines[2].text.startsWith("Ravi Kumar holds C-12 · signed by Siti and Ravi at "))
        assertTrue(check(records = emptyList()).passed)
        for (p in listOf(proof.copy(recordHash = "bad"), proof.copy(giverSig = "bad"), proof.copy(receiverSig = "bad"),
            proof.copy(itemId = "other"), proof.copy(toPersonId = wei.id), proof.copy(id = "other"))) {
            assertFalse(check(p).lines[2].ok)
            assertFalse(check(p).looksLikeCopy)
        }
        assertFalse(check(records = listOf(register.copy(recordHash = "bad"))).lines[2].ok)
        assertFalse(check(records = listOf(register.copy(itemId = "other"))).lines[2].ok)
    }

    @Test fun sealsAndAcceptanceAreCheckedOffline() {
        assertEquals(CheckLine(true, "Seal S-40117 matches C-12"), OfferChecker.checkSeal(ScannedCode.Seal("C-12", "S-40117"), item))
        assertFalse(OfferChecker.checkSeal(ScannedCode.Seal("C-07", item.sealNo), item).ok)
        assertEquals("Seal S-40999 doesn't match C-12 (S-40117). Don't accept this camera.",
            OfferChecker.checkSeal(ScannedCode.Seal(item.code, "S-40999"), item).text)
        val offer = offer()
        val record = complete(offer, 1)
        val acceptance = QrPayloads.parse(QrPayloads.acceptance(record)) as ScannedCode.Acceptance
        assertTrue(OfferChecker.checkAcceptance(acceptance, offer, roster).ok)
        assertFalse(OfferChecker.checkAcceptance(acceptance.copy(id = "other"), offer, roster).ok)
        assertFalse(OfferChecker.checkAcceptance(acceptance.copy(toDeviceId = devices[2].id), offer, roster).ok)
        assertFalse(OfferChecker.checkAcceptance(acceptance.copy(sealNo = "wrong"), offer, roster).ok)
        assertFalse(OfferChecker.checkAcceptance(acceptance.copy(acceptedMs = now), offer, roster).ok)
        // The giver may scan this after expiry; the pending offer's signed times stay unchanged.
        assertTrue(OfferChecker.checkAcceptance(acceptance, offer, roster).ok)
    }

    @Test fun custodyInStoreOutOverdueAndReturned() {
        assertEquals(CustodyStatus.Unknown, deriveCustody(item, emptyList(), emptyList(), now).status)
        val stored = deriveCustody(item, listOf(register), emptyList(), now)
        assertEquals(CustodyStatus.InStore, stored.status)
        assertEquals(siti.id, stored.holderId)
        assertTrue(stored.allSynced)
        val issue = complete(offer(), 1)
        val out = deriveCustody(item, listOf(register, issue), emptyList(), now)
        assertEquals(CustodyStatus.Out, out.status)
        assertEquals(ravi.id, out.holderId)
        assertEquals(issue, out.head)
        assertEquals(issue.dueMs, out.dueMs)
        assertEquals(issue.acceptedMs, out.lastSignedMs)
        assertFalse(out.allSynced)
        assertEquals(CustodyStatus.Out, deriveCustody(item, listOf(register, issue), emptyList(), issue.dueMs!!).status)
        assertEquals(CustodyStatus.Overdue, deriveCustody(item, listOf(register, issue), emptyList(), issue.dueMs + 1).status)
        val returned = complete(offer(issue, 1, 0), 0)
        assertEquals(CustodyStatus.InStore, deriveCustody(item, listOf(returned, issue, register), emptyList(), now).status)
    }

    @Test fun excludedRecordsAndServerViewCannotChangeCustody() {
        val issue = complete(offer(), 1)
        for (excluded in listOf(issue.copy(hiddenByTamper = true), issue.copy(syncStatus = SyncMark.REJECTED),
            issue.copy(status = ServerStatus.OVERRULED), issue.copy(status = ServerStatus.ORPHAN), issue.copy(itemId = "other"))) {
            assertEquals(siti.id, deriveCustody(item, listOf(register, excluded), emptyList(), now).holderId)
        }
        assertEquals(siti.id, deriveCustody(item, listOf(register, issue), emptyList(), now, serverView = true).holderId)
        assertEquals(ravi.id, deriveCustody(item, listOf(register, issue.copy(serverSeq = 2)), emptyList(), now, serverView = true).holderId)
    }

    @Test fun forksAreDisputedUntilResolutionChoosesAChildOrDescendant() {
        val issue = complete(offer(), 1)
        val a = complete(offer(issue, 1, 2), 2)
        val b = complete(offer(issue, 1, 0), 0)
        val records = listOf(register, issue, a, b)
        val disputed = deriveCustody(item, records, emptyList(), now)
        assertEquals(CustodyStatus.Disputed, disputed.status)
        assertNull(disputed.holderId)
        assertEquals(issue, disputed.head)
        assertEquals(setOf(a, b), disputed.forkBranches.toSet())
        assertEquals(wei.id, deriveCustody(item, records, listOf(resolution(a.id)), now).holderId)
        assertEquals(siti.id, deriveCustody(item, records, listOf(resolution(b.id)), now).holderId)
        val descendant = complete(offer(a, 2, 0), 0)
        assertEquals(descendant, deriveCustody(item, records + descendant, listOf(resolution(descendant.id)), now).head)
        for (ignored in listOf(resolution("missing"), resolution(a.id).copy(itemId = "other"), resolution(a.id).copy(syncStatus = SyncMark.REJECTED))) {
            assertEquals(CustodyStatus.Disputed, deriveCustody(item, records, listOf(ignored), now).status)
        }
        val synced = records.mapIndexed { i, h -> h.copy(serverSeq = (i + 1).toLong()) }
        assertEquals(CustodyStatus.Disputed, deriveCustody(item, synced, listOf(resolution(a.id)), now, true).status)
        assertEquals(wei.id, deriveCustody(item, synced, listOf(resolution(a.id).copy(serverSeq = 10)), now, true).holderId)
        assertEquals(CustodyStatus.Disputed, deriveCustody(item, records, listOf(resolution(a.id), resolution(b.id)), now).status)
    }

    private fun resolution(chosen: String) = ResolutionEntity(
        UUID.randomUUID().toString(), UUID.randomUUID().toString(), item.id, chosen, siti.id, devices[0].id,
        decidedMs = now, sig = "test signature",
    )
}
