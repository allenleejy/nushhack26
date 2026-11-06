package com.nushhack26.app.sync

import com.nushhack26.app.data.remote.DeviceRow
import com.nushhack26.app.data.remote.HandoverRow
import com.nushhack26.app.data.remote.ItemRow
import com.nushhack26.app.data.remote.PersonRow
import com.nushhack26.app.data.remote.PushOp
import com.nushhack26.app.data.remote.ResolutionRow
import com.nushhack26.app.data.remote.ServerJson
import kotlinx.serialization.json.jsonObject

/** The demo's people, devices and camera C-12 (docs/spec.md section 8), with fake keys and hashes. */
object Demo {
    const val SITI = "a0000000-0000-4000-8000-000000000001"
    const val RAVI = "a0000000-0000-4000-8000-000000000002"
    const val WEI_MING = "a0000000-0000-4000-8000-000000000003"
    const val JUN = "a0000000-0000-4000-8000-000000000004"
    const val DR_TAN = "a0000000-0000-4000-8000-000000000005"

    const val SITI_PHONE = "e0000000-0000-4000-8000-000000000001"
    const val RAVI_PHONE = "e0000000-0000-4000-8000-000000000002"
    const val WEI_MING_PHONE = "e0000000-0000-4000-8000-000000000003"
    const val JUN_PHONE = "e0000000-0000-4000-8000-000000000004"
    const val DR_TAN_PHONE = "e0000000-0000-4000-8000-000000000005"

    const val C12 = "b0000000-0000-4000-8000-000000000012"
    const val C12_SEAL = "S-40117"

    val people = listOf(
        PersonRow(SITI, "Siti Aminah", "store_officer", "S-1021"),
        PersonRow(RAVI, "Ravi Kumar", "radiographer", "R-2210", "RW-DEMO-1452"),
        PersonRow(WEI_MING, "Tan Wei Ming", "radiographer", "R-2234", "RW-DEMO-1478"),
        PersonRow(JUN, "Jun Lim", "radiographer", "R-2251", "RW-DEMO-1503"),
        PersonRow(DR_TAN, "Dr Tan Mei Ling", "rso", "O-3001"),
    )

    val devices = listOf(
        DeviceRow(SITI_PHONE, SITI, "pk-siti", "00:00:00:01", "software"),
        DeviceRow(RAVI_PHONE, RAVI, "pk-ravi", "00:00:00:02", "software"),
        DeviceRow(WEI_MING_PHONE, WEI_MING, "pk-weiming", "00:00:00:03", "software"),
        DeviceRow(JUN_PHONE, JUN, "pk-jun", "00:00:00:04", "software"),
        DeviceRow(DR_TAN_PHONE, DR_TAN, "pk-drtan", "00:00:00:05", "software"),
    )

    val c12 = ItemRow(C12, "C-12", "IR-0457", "Ir-192", C12_SEAL, "Tuas Store 1")

    val c12Register = HandoverRow(
        id = "c0000000-0000-4000-8000-000000000012",
        itemId = C12,
        kind = "register",
        toPersonId = SITI,
        recordHash = "hash-register-c12",
        status = "accepted",
    )

    private val phoneOf = mapOf(SITI to SITI_PHONE, RAVI to RAVI_PHONE, WEI_MING to WEI_MING_PHONE, JUN to JUN_PHONE)

    /** A complete handover that follows [prev]. Hashes are fake but linked, which the fake server checks. */
    fun handover(
        id: String,
        prev: HandoverRow,
        from: String,
        to: String,
        kind: String,
        nonce: String = "nonce-$id",
        seal: String = C12_SEAL,
        acceptedMs: Long = 1_794_466_830_000,
        dueMs: Long? = 1_794_470_400_000,
    ) = HandoverRow(
        id = id,
        itemId = prev.itemId,
        kind = kind,
        prevId = prev.id,
        prevHash = prev.recordHash,
        fromPersonId = from,
        fromDeviceId = phoneOf.getValue(from),
        toPersonId = to,
        toDeviceId = phoneOf.getValue(to),
        sealNo = seal,
        dueMs = if (kind == "return") null else dueMs,
        nonce = nonce,
        offeredMs = acceptedMs - 30_000,
        expiresMs = acceptedMs + 30_000,
        acceptedMs = acceptedMs,
        giverSig = "giver-sig-$id",
        receiverSig = "receiver-sig-$id",
        recordHash = "hash-$id",
    )

    fun resolution(id: String, alertId: String, chosen: String) = ResolutionRow(
        id = id,
        alertId = alertId,
        itemId = C12,
        chosenHandoverId = chosen,
        decidedByPersonId = DR_TAN,
        decidedByDeviceId = DR_TAN_PHONE,
        note = "Seal checked by the store",
        decidedMs = 1_794_467_000_000,
        sig = "rso-sig-$id",
    )
}

fun FakeSyncServer.seedDemo() {
    Demo.people.forEach(::addPerson)
    Demo.devices.forEach(::addDevice)
    addItem(Demo.c12)
    addHandover(Demo.c12Register)
}

fun HandoverRow.op() = PushOp(id, "handovers", payload = ServerJson.encodeToJsonElement(HandoverRow.serializer(), this).jsonObject)

fun ResolutionRow.op() = PushOp(id, "resolutions", payload = ServerJson.encodeToJsonElement(ResolutionRow.serializer(), this).jsonObject)
