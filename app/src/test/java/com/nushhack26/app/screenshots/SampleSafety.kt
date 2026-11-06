package com.nushhack26.app.screenshots

import com.nushhack26.app.crypto.Canonical
import com.nushhack26.app.crypto.Roster
import com.nushhack26.app.crypto.Signatures
import com.nushhack26.app.data.local.AlertEntity
import com.nushhack26.app.data.local.AlertKind
import com.nushhack26.app.data.local.DeviceEntity
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.ResolutionEntity
import com.nushhack26.app.data.local.Roles
import com.nushhack26.app.data.local.ServerStatus
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.repo.SafetyData
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Base64

/** The demo's C-12 fork (docs/spec.md, seeded demo data), signed with real keys so the phone's check passes. */
object SampleSafety {
    fun at(hour: Int, minute: Int): Long =
        LocalDate.of(2026, 11, 7).atTime(LocalTime.of(hour, minute)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun keys(): KeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    private fun b64(bytes: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    private fun sign(k: KeyPair, text: String) = Signature.getInstance("SHA256withECDSA").run {
        initSign(k.private); update(text.toByteArray()); b64(sign())
    }

    val siti = PersonEntity("p-siti", "Siti Aminah", Roles.STORE_OFFICER, "S-001")
    val ravi = PersonEntity("p-ravi", "Ravi Kumar", Roles.RADIOGRAPHER, "R-002")
    val weiMing = PersonEntity("p-wm", "Tan Wei Ming", Roles.RADIOGRAPHER, "R-003")
    val drTan = PersonEntity("p-tan", "Dr Tan Mei Ling", Roles.RSO, "O-001")
    val jun = PersonEntity("p-jun", "Jun Lim", Roles.RADIOGRAPHER, "R-004")
    private val keyPairs = listOf(siti, ravi, weiMing, drTan, jun).associate { it.id to keys() }
    private val devices = keyPairs.map { (person, k) ->
        val pub = b64(k.public.encoded)
        DeviceEntity("d-$person", person, pub, Signatures.keyFp(pub), "tee", performedAt = 0, createdBy = "u-$person", serverSeq = 1, syncStatus = SyncMark.SYNCED)
    }
    val item = ItemEntity("i-c12", "C-12", "SRC-4471", "Ir-192", "S-40117", "Main store")

    private fun signed(h: HandoverEntity): HandoverEntity {
        val giver = h.fromPersonId?.let { keyPairs.getValue(it) }
        val withGiver = h.copy(giverSig = giver?.let { sign(it, Canonical.offerString(h)) })
        val withReceiver = withGiver.copy(receiverSig = sign(keyPairs.getValue(h.toPersonId), Canonical.acceptanceString(withGiver)))
        return withReceiver.copy(recordHash = Canonical.recordHash(withReceiver))
    }

    private fun link(id: String, kind: String, prev: HandoverEntity?, from: PersonEntity?, to: PersonEntity, accepted: Long, due: Long?, seq: Long, status: String = ServerStatus.ACCEPTED) =
        signed(
            HandoverEntity(
                id = id, itemId = item.id, kind = kind, prevId = prev?.id, prevHash = prev?.recordHash,
                fromPersonId = from?.id, fromDeviceId = from?.let { "d-${it.id}" }, toPersonId = to.id, toDeviceId = "d-${to.id}",
                sealNo = item.sealNo, dueMs = due, nonce = "n-$id", offeredMs = accepted - 30_000, expiresMs = accepted + 60_000,
                acceptedMs = accepted,
            ),
        ).copy(status = status, serverSeq = seq, receivedAt = accepted + 3 * 60_000, createdBy = "u-${to.id}", syncStatus = SyncMark.SYNCED)

    val h0 = link("h0", HandoverKind.REGISTER, null, null, siti, at(8, 0), null, 1)
    val h1 = link("h1", HandoverKind.ISSUE, h0, siti, ravi, at(9, 0), at(14, 20), 2)
    val h2 = link("h2", HandoverKind.TRANSFER, h1, ravi, weiMing, at(14, 30), at(14, 31), 3)
    val h2b = link("h2b", HandoverKind.RETURN, h1, ravi, siti, at(14, 31), null, 4, ServerStatus.FORKED)

    val fork = AlertEntity(
        id = "a-fork", kind = AlertKind.FORK, itemId = item.id, personId = ravi.id, handoverIds = listOf("h2", "h2b"),
        forkPrevId = "h1", reason = "second handover after the same record", performedAt = at(14, 35), serverSeq = 10,
    )
    val refusedLapsed = AlertEntity(
        id = "a-lapsed", kind = AlertKind.REJECTED, itemId = item.id, personId = siti.id, reason = "not authorised: lapsed",
        opTable = "handovers", opPayload = """{"item_id":"i-c12","from_person_id":"p-siti","to_person_id":"p-jun"}""",
        performedAt = at(13, 50), serverSeq = 8,
    )
    val refusedDevice = AlertEntity(
        id = "a-device", kind = AlertKind.REJECTED, personId = jun.id, reason = "devices are added by enrol_device only",
        opTable = "devices", opPayload = """{"person_id":"p-jun"}""", performedAt = at(13, 40), serverSeq = 7,
    )
    val sealAlert = AlertEntity(
        id = "a-seal", kind = AlertKind.SEAL_MISMATCH, itemId = item.id, handoverIds = listOf("h-seal"),
        reason = "seal mismatch", performedAt = at(12, 10), serverSeq = 6,
    )
    /** Stands in for the seal-mismatch record; kept out of the synced chain so C-12 custody stays the demo's. */
    private val hSeal = h1.copy(id = "h-seal", sealNo = "S-40999", serverSeq = null)

    val roster = Roster(listOf(siti, ravi, weiMing, drTan, jun), devices, listOf(item))

    fun data(alerts: List<AlertEntity>, resolutions: List<ResolutionEntity> = emptyList(), tamperH2b: Boolean = false) = SafetyData(
        alerts = alerts,
        items = listOf(item),
        handovers = listOf(h0, h1, h2, if (tamperH2b) h2b.copy(receiverSig = h2.receiverSig) else h2b, hSeal),
        resolutions = resolutions,
        roster = roster,
    )

    fun resolution(synced: Boolean) = ResolutionEntity(
        id = "r1", alertId = fork.id, itemId = item.id, chosenHandoverId = h2.id, decidedByPersonId = drTan.id,
        decidedByDeviceId = "d-${drTan.id}", decidedMs = at(14, 34), sig = "sig",
        serverSeq = if (synced) 11 else null, syncStatus = if (synced) SyncMark.SYNCED else SyncMark.WAITING,
    )
}
