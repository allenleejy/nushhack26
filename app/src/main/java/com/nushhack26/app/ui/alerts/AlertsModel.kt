package com.nushhack26.app.ui.alerts

import com.nushhack26.app.crypto.CustodyStatus
import com.nushhack26.app.crypto.Roster
import com.nushhack26.app.crypto.deriveCustody
import com.nushhack26.app.data.local.AlertEntity
import com.nushhack26.app.data.local.AlertKind
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.ResolutionEntity
import com.nushhack26.app.data.local.ServerStatus
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.repo.SafetyData
import com.nushhack26.app.data.repo.checkSignatures
import com.nushhack26.app.data.repo.deliveredBy
import com.nushhack26.app.data.repo.handoverNumber
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.components.formatTime
import com.nushhack26.app.ui.store.phrase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

const val SIGNATURES_VALID_TEXT = "both signatures valid on this phone"
const val SIGNATURE_INVALID_TEXT = "a signature failed the check on this phone, so this isn't proof against them"
const val OVERDUE_CAVEAT = "newer handovers may not have synced"
const val SEVERITY_WAIT_MS = 5 * 60_000L

/** One card on the Alerts list (each `itemCode` is the camera as a sentence names it: "Kestrel (C-12)"), most serious first: fork, overdue, seal mismatch, refused, waiting, resolved. */
sealed interface AlertCard {
    val key: String

    data class Fork(
        val alertId: String,
        val itemCode: String,
        val personName: String,
        val sentence: String,
        val signaturesValid: Boolean,
        val meta: String,
        /** "Kestrel", for the short button label. */
        val itemName: String = itemCode,
    ) : AlertCard { override val key get() = "fork-$alertId" }

    data class Overdue(val itemId: String, val itemCode: String, val sentence: String) : AlertCard {
        override val key get() = "overdue-$itemId"
    }

    data class SealMismatch(val alertId: String, val itemId: String?, val itemCode: String, val sentence: String, val meta: String) : AlertCard {
        override val key get() = "seal-$alertId"
    }

    data class Refused(val alertId: String, val itemId: String?, val sentence: String, val meta: String) : AlertCard {
        override val key get() = "refused-$alertId"
    }

    data class Waiting(val alertId: String, val itemId: String?, val itemCode: String, val sentence: String) : AlertCard {
        override val key get() = "waiting-$alertId"
    }

    data class Resolved(val alertId: String, val itemCode: String, val text: String, val notSynced: Boolean) : AlertCard {
        override val key get() = "resolved-$alertId"
    }
}

data class AlertsUiState(
    val loading: Boolean = true,
    val cards: List<AlertCard> = emptyList(),
    val status: SyncStatus = SyncStatus(),
)

/** "Can't reach the server. Showing what synced at 14:02." */
fun alertsOfflineText(lastSyncAt: Long?): String =
    if (lastSyncAt == null) "Can't reach the server. Nothing has synced to this phone yet."
    else "Can't reach the server. Showing what synced at ${formatTime(lastSyncAt)}."

/** The decision that settles [alert], if any; a refused decision doesn't count. */
fun resolutionFor(alert: AlertEntity, resolutions: List<ResolutionEntity>): ResolutionEntity? =
    resolutions.firstOrNull { it.id == alert.resolutionId }
        ?: resolutions.firstOrNull { it.alertId == alert.id && it.syncStatus != SyncMark.REJECTED }

fun buildAlertCards(data: SafetyData, nowMs: Long): List<AlertCard> {
    val roster = data.roster
    fun code(itemId: String?) = data.items.firstOrNull { it.id == itemId }?.phrase ?: "Unknown item"
    fun detected(alert: AlertEntity) = alert.performedAt?.let { "Detected ${formatTime(it)}" } ?: "Detected by the server"

    val forks = mutableListOf<AlertCard>()
    val resolved = mutableListOf<AlertCard>()
    data.alerts.filter { it.kind == AlertKind.FORK }.sortedByDescending { it.performedAt ?: 0 }.forEach { alert ->
        val resolution = resolutionFor(alert, data.resolutions)
        if (resolution != null || alert.closedAt != null) {
            val by = resolution?.let { "Resolved by ${roster.shortNameOf(it.decidedByPersonId)} · ${formatTime(it.decidedMs)}" }
                ?: "Resolved · ${formatTime(alert.closedAt ?: nowMs)}"
            resolved += AlertCard.Resolved(alert.id, code(alert.itemId), by, notSynced = resolution?.syncStatus == SyncMark.WAITING)
        } else {
            val records = alert.handoverIds.map { id -> data.handovers.firstOrNull { it.id == id } }
            val valid = records.isNotEmpty() && records.all { it != null && checkSignatures(it, roster).allOk }
            val after = handoverNumber(alert.forkPrevId, data.handovers)?.let { "H$it" } ?: "registration"
            val name = roster.nameOf(alert.personId)
            val itemCode = code(alert.itemId)
            forks += AlertCard.Fork(
                alertId = alert.id,
                itemCode = itemCode,
                personName = name,
                sentence = if (!valid) "One record has an invalid signature. It isn't proof against $name." else "$name signed two handovers of $itemCode after $after · " +
                    if (valid) SIGNATURES_VALID_TEXT else SIGNATURE_INVALID_TEXT,
                signaturesValid = valid,
                meta = "$itemCode · ${detected(alert)}",
                itemName = data.items.firstOrNull { it.id == alert.itemId }?.displayName ?: itemCode,
            )
        }
    }

    val overdue = data.items.mapNotNull { item -> overdueCard(item.id, item.phrase, data, nowMs) }

    val seals = data.alerts.filter { it.kind == AlertKind.SEAL_MISMATCH && it.closedAt == null }.map { alert ->
        val h = data.handovers.firstOrNull { it.id in alert.handoverIds }
        val item = data.items.firstOrNull { it.id == alert.itemId }
        val sentence = if (h?.sealNo != null && item != null) {
            "${item.phrase} accepted by ${roster.nameOf(h.toPersonId)} with seal ${h.sealNo} (expected ${item.sealNo})"
        } else "Seal mismatch on ${code(alert.itemId)}: ${alert.reason}"
        AlertCard.SealMismatch(alert.id, alert.itemId, code(alert.itemId), sentence, detected(alert))
    }

    val refused = data.alerts.filter { it.kind == AlertKind.REJECTED && it.closedAt == null }.map { alert ->
        AlertCard.Refused(alert.id, alert.itemId, refusedSentence(alert, roster, data.handovers, ::code), detected(alert))
    }

    val waiting = data.alerts.filter {
        it.kind == AlertKind.ORPHAN && it.closedAt == null && nowMs - (it.performedAt ?: 0) > SEVERITY_WAIT_MS
    }.map { alert ->
        val itemCode = code(alert.itemId)
        AlertCard.Waiting(alert.id, alert.itemId, itemCode, "$itemCode: a record is waiting for an earlier record to reach the server.")
    }

    return forks + overdue + seals + refused + waiting + resolved
}

/** Overdue as the server knows it, from synced records only (docs/spec.md O1). */
private fun overdueCard(itemId: String, itemCode: String, data: SafetyData, nowMs: Long): AlertCard.Overdue? {
    val item = data.items.first { it.id == itemId }
    val custody = deriveCustody(item, data.handovers, data.resolutions, nowMs, serverView = true)
    val head: HandoverEntity = when (custody.status) {
        CustodyStatus.Overdue -> custody.head
        CustodyStatus.Disputed -> custody.forkBranches
            .flatMap { branchHeads(it, data.handovers) }
            .filter { (it.kind == HandoverKind.ISSUE || it.kind == HandoverKind.TRANSFER) && (it.dueMs ?: Long.MAX_VALUE) < nowMs }
            .maxByOrNull { it.acceptedMs ?: 0 }
        else -> null
    } ?: return null
    val due = head.dueMs ?: return null
    val signed = head.acceptedMs?.let { ", signed ${formatTime(it)}" }.orEmpty()
    return AlertCard.Overdue(
        itemId = itemId,
        itemCode = itemCode,
        sentence = "$itemCode overdue ${formatDuration(nowMs - due)} · last holder the server knows: " +
            "${data.roster.nameOf(head.toPersonId)}$signed · $OVERDUE_CAVEAT",
    )
}

/** Follows a branch's synced records to every record at its end. */
private fun branchHeads(start: HandoverEntity, handovers: List<HandoverEntity>): List<HandoverEntity> {
    val synced = handovers.filter {
        it.itemId == start.itemId && it.serverSeq != null && !it.tamperHidden &&
            it.status != ServerStatus.OVERRULED && it.status != ServerStatus.ORPHAN
    }
    val children = synced.groupBy { it.prevId }
    val heads = mutableListOf<HandoverEntity>()
    val seen = mutableSetOf<String>()
    val stack = ArrayDeque(listOf(start))
    while (stack.isNotEmpty()) {
        val h = stack.removeLast()
        if (!seen.add(h.id)) continue
        val next = children[h.id].orEmpty()
        if (next.isEmpty()) heads += h else next.forEach(stack::addLast)
    }
    return heads
}

/** The server's refusal reason (docs/sync.md D.3), in words. */
fun refusedSentence(alert: AlertEntity, roster: Roster, handovers: List<HandoverEntity>, code: (String?) -> String): String {
    val payload = alert.opPayload?.let { runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull() }
    fun field(name: String) = (payload?.get(name) as? JsonPrimitive)?.contentOrNull
    val itemCode = code(alert.itemId ?: field("item_id"))
    val record = handovers.firstOrNull { it.id in alert.handoverIds }
    val to = (field("to_person_id") ?: record?.toPersonId)?.let(roster::nameOf)
    val from = (field("from_person_id") ?: record?.fromPersonId ?: alert.personId)?.let(roster::nameOf)
    val phone = deliveredBy(field("created_by"), roster)?.let { " from ${shortName(it.name)}'s phone" }.orEmpty()
    val reason = alert.reason
    return when {
        reason.startsWith("not authorised: lapsed") -> "Refused: $itemCode to ${to ?: "someone"}, whose authorisation had lapsed"
        reason.startsWith("not authorised: inactive") -> "Refused: $itemCode to ${to ?: "someone"}, who is no longer on the active roster"
        reason.startsWith("not authorised") -> "Refused: $itemCode from ${from ?: "the store"} to ${to ?: "someone"}: their roles don't allow this handover"
        reason.startsWith("devices are added by enrol_device only") ->
            "Refused: a phone key for ${field("person_id")?.let(roster::nameOf) ?: "someone"}$phone · devices are added by enrol_device only"
        reason.startsWith("replayed offer") -> "Refused: a replayed offer for $itemCode$phone. The offer had already been used."
        reason.startsWith("wrong code") -> "Refused: $itemCode$phone, because the six-digit code didn't match"
        reason.startsWith("hash mismatch") -> "Refused: a $itemCode record$phone doesn't match its own signatures"
        reason.startsWith("unknown signer") -> "Refused: a $itemCode record$phone signed by a key that isn't on the roster"
        reason.startsWith("giver was not the holder") -> "Refused: $itemCode from ${from ?: "someone"}, who wasn't holding it"
        reason.startsWith("broken link") -> "Refused: a $itemCode record$phone that doesn't link to the record before it"
        reason.startsWith("conflicting copy") -> "Refused: a conflicting copy of a $itemCode record$phone"
        reason.startsWith("malformed device") -> "Refused: a phone key$phone that isn't valid"
        reason.startsWith("not a safety officer") -> "Refused: a decision on $itemCode by someone who isn't a safety officer"
        reason.startsWith("already resolved") -> "Refused: a second decision on $itemCode; the fork was already resolved"
        reason.startsWith("not part of this fork") -> "Refused: a decision on $itemCode that picks a record outside the fork"
        reason.startsWith("not a fork alert") -> "Refused: a decision on $itemCode for an alert that isn't an open fork"
        reason.startsWith("unknown camera") -> "Refused: a record$phone for a camera that isn't in the register"
        reason.startsWith("unknown person") -> "Refused: a $itemCode record$phone naming someone who isn't on the roster"
        reason.startsWith("device revoked") -> {
            val signers = listOfNotNull(field("from_device_id") ?: record?.fromDeviceId, field("to_device_id") ?: record?.toDeviceId)
                .mapNotNull(roster::device)
            val withdrawn = signers.firstOrNull { it.revokedAtMs != null } ?: signers.firstOrNull()
            "Refused: a $itemCode record signed by ${withdrawn?.let { roster.nameOf(it.personId) } ?: from ?: "someone"}'s withdrawn phone"
        }
        reason.startsWith("malformed record") -> "Refused: a $itemCode record$phone that couldn't be read"
        else -> "Refused: $itemCode$phone · $reason"
    }
}

