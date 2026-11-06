package com.nushhack26.app.sync

import com.nushhack26.app.data.remote.AlertRow
import com.nushhack26.app.data.remote.DeviceRow
import com.nushhack26.app.data.remote.HandoverRow
import com.nushhack26.app.data.remote.ItemRow
import com.nushhack26.app.data.remote.MissingReportRow
import com.nushhack26.app.data.remote.OpResult
import com.nushhack26.app.data.remote.OpResultKind
import com.nushhack26.app.data.remote.PersonRow
import com.nushhack26.app.data.remote.PullPage
import com.nushhack26.app.data.remote.PullRows
import com.nushhack26.app.data.remote.PushOp
import com.nushhack26.app.data.remote.ResolutionRow
import com.nushhack26.app.data.remote.ServerJson
import com.nushhack26.app.data.remote.SyncApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.IOException
import java.util.UUID

/**
 * A server in memory that follows the same rules as `sync_push` and `sync_pull`
 * (docs/sync.md sections C and D). The record-hash check is a hook, [hashOk], because it
 * needs card 01's Canonical; by default every hash passes.
 */
class FakeSyncServer : SyncApi {
    val people = linkedMapOf<String, PersonRow>()
    val devices = linkedMapOf<String, DeviceRow>()
    val items = linkedMapOf<String, ItemRow>()
    val handovers = linkedMapOf<String, HandoverRow>()
    val alerts = linkedMapOf<String, AlertRow>()
    val resolutions = linkedMapOf<String, ResolutionRow>()
    val missingReports = linkedMapOf<String, MissingReportRow>()

    private val applied = mutableMapOf<String, OpResult>()
    private val appliedHash = mutableMapOf<String, String?>()
    private var seq = 0L

    /** Card 01 can plug in Canonical.recordHash here. */
    var hashOk: (HandoverRow) -> Boolean = { true }

    /** Applies the next push, then throws as if the response was lost on the way back. */
    var dropNextResponse = false

    /** Throws before applying anything, as if there were no network. */
    var offline = false

    var pushCalls = 0
        private set

    // --- Seeding, as supabase/seed.sql does ---------------------------------------------

    fun addPerson(row: PersonRow) { people[row.id] = row.copy(serverSeq = ++seq) }
    fun addDevice(row: DeviceRow) { devices[row.id] = row.copy(serverSeq = ++seq) }
    fun addItem(row: ItemRow) { items[row.id] = row.copy(serverSeq = ++seq) }
    fun addHandover(row: HandoverRow) { handovers[row.id] = row.copy(serverSeq = ++seq) }

    // --- SyncApi -----------------------------------------------------------------------

    override suspend fun push(ops: List<PushOp>): List<OpResult> {
        if (offline) throw IOException("offline")
        pushCalls++
        val results = ops.map { op ->
            runCatching { applyOp(op) }.getOrElse { reject(op.opId, op.table, op.payload, "server error: ${it.message}") }
        }
        if (dropNextResponse) {
            dropNextResponse = false
            throw IOException("connection dropped after the server applied the batch")
        }
        return results
    }

    override suspend fun pull(since: Long): PullPage {
        if (offline) throw IOException("offline")
        val limit = 2_000
        data class Row(val seq: Long, val add: (PullRows) -> PullRows)
        val rows = buildList {
            people.values.filter { (it.serverSeq ?: 0) > since }.forEach { r -> add(Row(r.serverSeq!!) { it.copy(people = it.people + r) }) }
            devices.values.filter { (it.serverSeq ?: 0) > since }.forEach { r -> add(Row(r.serverSeq!!) { it.copy(devices = it.devices + r) }) }
            items.values.filter { (it.serverSeq ?: 0) > since }.forEach { r -> add(Row(r.serverSeq!!) { it.copy(items = it.items + r) }) }
            handovers.values.filter { (it.serverSeq ?: 0) > since }.forEach { r -> add(Row(r.serverSeq!!) { it.copy(handovers = it.handovers + r) }) }
            alerts.values.filter { (it.serverSeq ?: 0) > since }.forEach { r -> add(Row(r.serverSeq!!) { it.copy(alerts = it.alerts + r) }) }
            resolutions.values.filter { (it.serverSeq ?: 0) > since }.forEach { r -> add(Row(r.serverSeq!!) { it.copy(resolutions = it.resolutions + r) }) }
            missingReports.values.filter { (it.serverSeq ?: 0) > since }.forEach { r -> add(Row(r.serverSeq!!) { it.copy(missingReports = it.missingReports + r) }) }
        }.sortedBy { it.seq }
        val page = rows.take(limit)
        return PullPage(
            maxSeq = page.lastOrNull()?.seq ?: since,
            more = rows.size > limit,
            rows = page.fold(PullRows()) { acc, row -> row.add(acc) },
        )
    }

    // --- The rules (docs/sync.md section C) ---------------------------------------------

    private fun applyOp(op: PushOp): OpResult {
        applied[op.opId]?.let { prior ->
            val hash = op.payload.str("record_hash")
            if (op.table == "handovers" && appliedHash[op.opId] != null && appliedHash[op.opId] != hash) {
                addAlert(AlertRow(UUID.randomUUID().toString(), "rejected", reason = "conflicting copy of the same record", opTable = op.table, opPayload = op.payload))
                return OpResult(op.opId, OpResultKind.REJECTED, "conflicting copy of the same record")
            }
            return OpResult(op.opId, OpResultKind.DUPLICATE, prior.reason, prior.status)
        }
        if (op.kind != "insert") return reject(op.opId, op.table, op.payload, "only inserts are allowed")
        return when (op.table) {
            "handovers" -> applyHandover(op.opId, op.payload)
            "devices" -> applyDevice(op.opId, op.payload)
            "resolutions" -> applyResolution(op.opId, op.payload)
            "missing_reports" -> applyMissingReport(op.opId, op.payload)
            else -> reject(op.opId, op.table, op.payload, "unknown table")
        }
    }

    private fun applyHandover(opId: String, p: JsonObject): OpResult {
        val h = runCatching { ServerJson.decodeFromJsonElement(HandoverRow.serializer(), p) }.getOrNull()
            ?: return reject(opId, "handovers", p, "malformed record")
        if (h.prevId == null || h.kind !in setOf("issue", "transfer", "return")) return reject(opId, "handovers", p, "malformed record")
        if (h.itemId !in items) return reject(opId, "handovers", p, "unknown camera")
        if (!hashOk(h)) return reject(opId, "handovers", p, "hash mismatch")
        if (h.nonce == null || handovers.values.any { it.nonce == h.nonce && it.id != h.id }) return reject(opId, "handovers", p, "replayed offer")
        if (devices[h.fromDeviceId]?.personId != h.fromPersonId || devices[h.toDeviceId]?.personId != h.toPersonId) {
            return reject(opId, "handovers", p, "unknown signer")
        }
        val fromRole = people[h.fromPersonId]?.role
        val toRole = people[h.toPersonId]?.role
        val rolesFit = (h.kind == "issue" && fromRole == "store_officer" && toRole == "radiographer") ||
            (h.kind == "transfer" && fromRole == "radiographer" && toRole == "radiographer") ||
            (h.kind == "return" && fromRole == "radiographer" && toRole == "store_officer")
        if (!rolesFit) return reject(opId, "handovers", p, "not authorised")
        val prev = handovers[h.prevId]
        if (prev != null && prev.toPersonId != h.fromPersonId) return reject(opId, "handovers", p, "giver was not the holder")
        if (prev != null && prev.recordHash != h.prevHash) return reject(opId, "handovers", p, "broken link")

        handovers[h.id] = h.copy(status = "orphan", statusReason = null, serverSeq = ++seq)
        val status = settle(h.id)
        val stored = handovers.getValue(h.id)
        val result = OpResult(
            opId,
            if (status == "accepted") OpResultKind.APPLIED else OpResultKind.CONFLICT,
            if (status == "accepted") null else stored.statusReason,
            status,
            stored.serverSeq,
        )
        applied[opId] = result
        appliedHash[opId] = h.recordHash
        return result
    }

    /** Mirrors `_settle_handover`: where a stored record fits, its status, and its alerts. */
    private fun settle(id: String): String {
        val h = handovers.getValue(id)
        val item = items.getValue(h.itemId)
        val prev = handovers[h.prevId]
        if (prev == null) {
            setStatus(id, "orphan", "waiting for an earlier record")
            if (alerts.values.none { it.kind == "orphan" && it.closedAt == null && id in it.handoverIds }) {
                addAlert(AlertRow(UUID.randomUUID().toString(), "orphan", h.itemId, h.fromPersonId, listOf(id), reason = "Waiting for an earlier record"))
            }
            return "orphan"
        }
        alerts.values.filter { it.kind == "orphan" && it.closedAt == null && id in it.handoverIds }
            .forEach { alerts[it.id] = it.copy(closedAt = "2026-11-06T14:15:00Z", serverSeq = ++seq) }

        if (prev.toPersonId != h.fromPersonId || prev.recordHash != h.prevHash) {
            val reason = if (prev.toPersonId != h.fromPersonId) "giver was not the holder" else "broken link"
            setStatus(id, "orphan", reason)
            addAlert(AlertRow(UUID.randomUUID().toString(), "rejected", h.itemId, h.fromPersonId, listOf(id), reason = reason))
            return "orphan"
        }

        val (status, reason) = when {
            prev.status == "orphan" -> "orphan" to "waiting for an earlier record"
            prev.status == "forked" || prev.status == "overruled" -> "forked" to "follows a disputed record"
            handovers.values.any { it.prevId == prev.id && it.id != id && it.status == "accepted" } ->
                "forked" to "second handover after the same record"
            h.sealNo != item.sealNo -> {
                addAlert(AlertRow(UUID.randomUUID().toString(), "seal_mismatch", h.itemId, h.toPersonId, listOf(id), reason = "${item.code} accepted with seal ${h.sealNo} (expected ${item.sealNo})"))
                "accepted" to "seal mismatch"
            }
            else -> "accepted" to null
        }
        setStatus(id, status, reason)

        if (status == "forked") {
            val siblings = handovers.values.filter { it.prevId == prev.id }.sortedBy { it.serverSeq }.map { it.id }
            val open = alerts.values.firstOrNull {
                it.kind == "fork" && it.closedAt == null && it.itemId == h.itemId &&
                    (it.handoverIds.any(siblings::contains) || prev.id in it.handoverIds)
            }
            if (open == null) {
                val giver = people[h.fromPersonId]?.name
                addAlert(AlertRow(UUID.randomUUID().toString(), "fork", h.itemId, h.fromPersonId, siblings, forkPrevId = prev.id, reason = "$giver signed two handovers of ${item.code} after the same record"))
            } else {
                alerts[open.id] = open.copy(handoverIds = (open.handoverIds + id).distinct(), serverSeq = ++seq)
            }
        }

        if (status != "orphan") {
            handovers.values.filter { it.prevId == id && it.status == "orphan" }.sortedBy { it.serverSeq }.forEach { settle(it.id) }
        }
        return status
    }

    private fun applyDevice(opId: String, p: JsonObject): OpResult {
        val d = runCatching { ServerJson.decodeFromJsonElement(DeviceRow.serializer(), p) }.getOrNull()
            ?: return reject(opId, "devices", p, "malformed record")
        if (d.keySecurity !in setOf("strongbox", "tee", "software")) return reject(opId, "devices", p, "malformed record")
        if (people[d.personId]?.active != true) return reject(opId, "devices", p, "unknown person")
        devices[d.id] = d.copy(serverSeq = ++seq)
        return record(opId, OpResult(opId, OpResultKind.APPLIED, serverSeq = seq))
    }

    private fun applyResolution(opId: String, p: JsonObject): OpResult {
        val r = runCatching { ServerJson.decodeFromJsonElement(ResolutionRow.serializer(), p) }.getOrNull()
            ?: return reject(opId, "resolutions", p, "malformed record")
        if (people[r.decidedByPersonId]?.role != "rso") return reject(opId, "resolutions", p, "not a safety officer")
        if (devices[r.decidedByDeviceId]?.personId != r.decidedByPersonId) return reject(opId, "resolutions", p, "unknown signer")
        val alert = alerts[r.alertId]
        if (alert == null || alert.kind != "fork") return reject(opId, "resolutions", p, "not a fork alert")
        if (alert.closedAt != null) return reject(opId, "resolutions", p, "already resolved")
        if (r.chosenHandoverId !in alert.handoverIds) return reject(opId, "resolutions", p, "not part of this fork")

        resolutions[r.id] = r.copy(serverSeq = ++seq)

        // The chosen branch starts at the child of the fork point that leads to the chosen record.
        var root = r.chosenHandoverId
        while (true) {
            val parent = handovers[root]?.prevId ?: break
            if (parent == alert.forkPrevId) break
            root = parent
        }
        fun branch(start: String): List<String> =
            listOf(start) + handovers.values.filter { it.prevId == start }.flatMap { branch(it.id) }
        handovers.values.filter { it.prevId == alert.forkPrevId }.forEach { child ->
            val chosen = child.id == root
            branch(child.id).forEach { id ->
                val h = handovers.getValue(id)
                if (h.status != "orphan") {
                    setStatus(id, if (chosen) "accepted" else "overruled", if (chosen) h.statusReason else "overruled by the safety officer")
                }
            }
        }
        alerts[alert.id] = alert.copy(resolutionId = r.id, closedAt = "2026-11-06T14:15:00Z", serverSeq = ++seq)
        return record(opId, OpResult(opId, OpResultKind.APPLIED, serverSeq = resolutions.getValue(r.id).serverSeq))
    }

    private fun applyMissingReport(opId: String, p: JsonObject): OpResult {
        val m = runCatching { ServerJson.decodeFromJsonElement(MissingReportRow.serializer(), p) }.getOrNull()
            ?: return reject(opId, "missing_reports", p, "malformed record")
        missingReports[m.id] = m.copy(serverSeq = ++seq)
        return record(opId, OpResult(opId, OpResultKind.APPLIED, serverSeq = seq))
    }

    // --- Helpers -----------------------------------------------------------------------

    private fun setStatus(id: String, status: String, reason: String?) {
        handovers[id] = handovers.getValue(id).copy(status = status, statusReason = reason, serverSeq = ++seq)
    }

    private fun addAlert(row: AlertRow) {
        alerts[row.id] = row.copy(serverSeq = ++seq)
    }

    private fun record(opId: String, result: OpResult): OpResult {
        applied[opId] = result
        return result
    }

    private fun reject(opId: String, table: String, payload: JsonObject, reason: String): OpResult {
        val itemId = payload.str("item_id")?.takeIf { it in items }
        val personId = (payload.str("from_person_id") ?: payload.str("decided_by_person_id"))?.takeIf { it in people }
        addAlert(AlertRow(UUID.randomUUID().toString(), "rejected", itemId, personId, reason = reason, opTable = table, opPayload = payload))
        val result = OpResult(opId, OpResultKind.REJECTED, reason)
        applied[opId] = result
        appliedHash[opId] = payload.str("record_hash")
        return result
    }
}

private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
