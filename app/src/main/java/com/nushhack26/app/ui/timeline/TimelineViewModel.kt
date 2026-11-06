package com.nushhack26.app.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.crypto.Canonical
import com.nushhack26.app.crypto.CustodyStatus
import com.nushhack26.app.crypto.Roster
import com.nushhack26.app.crypto.Signatures
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.ResolutionEntity
import com.nushhack26.app.data.local.ServerStatus
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.repo.CustodyRepository
import com.nushhack26.app.data.repo.ItemChain
import com.nushhack26.app.ui.store.LOCAL_DB_ERROR
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Whether a record's signatures check out against the roster's public keys. */
enum class SigCheck { Valid, Invalid, UnknownKey, Unsigned }

/** How a record is drawn: normal, struck out by a supervisor, refused by the server, or cut off from the chain. */
enum class EntryLook { Normal, Overruled, Rejected, Orphan }

data class TimelineEntry(
    val id: String,
    val kind: String,
    val title: String,
    val detail: String,
    val timeMs: Long?,
    val dueMs: Long?,
    val sealNo: String?,
    val matchCode: String,
    val sig: SigCheck,
    val syncStatus: String,
    val look: EntryLook,
    val reason: String? = null,
)

/** A row of the timeline: one record, a fork with its branches side by side, or a supervisor's decision. */
sealed interface TimelineNode {
    data class Record(val entry: TimelineEntry) : TimelineNode
    data class Fork(val branches: List<List<TimelineEntry>>) : TimelineNode
    data class Resolution(val chosenTitle: String, val by: String, val note: String, val timeMs: Long, val sig: SigCheck, val synced: Boolean) : TimelineNode
}

data class TimelineHeader(
    val code: String,
    val source: String,
    val sealNo: String,
    val status: CustodyStatus,
    val holderName: String?,
    val dueMs: Long?,
)

sealed interface TimelineUiState {
    data object Loading : TimelineUiState
    data class Error(val message: String) : TimelineUiState
    data object NotFound : TimelineUiState
    data class Ready(val nowMs: Long, val header: TimelineHeader, val nodes: List<TimelineNode>) : TimelineUiState
}

class TimelineViewModel(repository: CustodyRepository, itemId: String) : ViewModel() {
    val state: StateFlow<TimelineUiState> = repository.observeItemChain(itemId)
        .map<ItemChain, TimelineUiState> { buildTimeline(it) }
        .catch { emit(TimelineUiState.Error(LOCAL_DB_ERROR)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState.Loading)
}

/** The chain in linked order (register first, then each record's children), never by phone clocks. */
fun buildTimeline(chain: ItemChain): TimelineUiState {
    val item = chain.item ?: return TimelineUiState.NotFound
    val state = chain.state
    val roster = chain.roster
    val records = chain.handovers.filter { !it.hiddenByTamper }
    val byId = records.associateBy { it.id }
    val children = records.filter { it.prevId != null }.groupBy { it.prevId }
    val nodes = mutableListOf<TimelineNode>()
    val placed = mutableSetOf<String>()
    fun entry(h: HandoverEntity): TimelineEntry {
        placed += h.id
        return toEntry(h, byId[h.prevId], roster)
    }

    fun branchFrom(start: HandoverEntity): List<TimelineEntry> {
        val out = mutableListOf<TimelineEntry>()
        var at: HandoverEntity? = start
        while (at != null && at.id !in placed) {
            out += entry(at)
            // A fork inside a branch is rare; keep its older child here and leave the rest as orphans.
            at = children[at.id].orEmpty().sortedBy { it.serverSeq ?: Long.MAX_VALUE }.firstOrNull()
        }
        return out
    }

    var at: HandoverEntity? = records.filter { it.prevId == null }.minByOrNull { it.serverSeq ?: Long.MAX_VALUE }
    while (at != null && at.id !in placed) {
        nodes += TimelineNode.Record(entry(at))
        val next = children[at.id].orEmpty().sortedBy { it.serverSeq ?: Long.MAX_VALUE }
        at = when {
            next.isEmpty() -> null
            next.size == 1 -> next.single()
            else -> {
                nodes += TimelineNode.Fork(next.map { branchFrom(it) })
                chain.resolutions.sortedBy { it.decidedMs }.forEach { nodes += resolutionNode(it, byId, roster) }
                null
            }
        }
    }
    // Records that don't link to the chain on this phone yet.
    records.filter { it.id !in placed }
        .sortedBy { it.serverSeq ?: it.offeredMs ?: Long.MAX_VALUE }
        .forEach { nodes += TimelineNode.Record(entry(it).copy(look = lookOf(it, orphan = true))) }

    val header = TimelineHeader(
        code = item.code, source = "${item.isotope} · ${item.sourceSerial}", sealNo = item.sealNo,
        status = state?.status ?: CustodyStatus.Unknown,
        holderName = state?.holderId?.let { roster.person(it)?.name ?: "Unknown person" },
        dueMs = state?.dueMs,
    )
    return TimelineUiState.Ready(chain.nowMs, header, nodes)
}

private fun toEntry(h: HandoverEntity, parent: HandoverEntity?, roster: Roster): TimelineEntry {
    val to = roster.person(h.toPersonId)?.name ?: "Unknown person"
    val from = roster.person(h.fromPersonId)?.name ?: "Unknown person"
    val title = when (h.kind) {
        HandoverKind.REGISTER -> "Registered to $to"
        HandoverKind.ISSUE -> "Issued to $to"
        HandoverKind.TRANSFER -> "Handed to $to"
        HandoverKind.RETURN -> "Returned to $to"
        else -> "Handed to $to"
    }
    return TimelineEntry(
        id = h.id, kind = h.kind, title = title,
        detail = if (h.kind == HandoverKind.REGISTER) "Store register" else "From $from",
        timeMs = h.acceptedMs ?: h.receivedAt ?: h.offeredMs,
        dueMs = h.dueMs, sealNo = h.sealNo,
        matchCode = runCatching { Canonical.matchCode(h.recordHash) }.getOrDefault(""),
        sig = checkSignatures(h, parent, roster),
        syncStatus = h.syncStatus,
        look = lookOf(h, orphan = false),
        reason = h.syncReason ?: h.statusReason,
    )
}

private fun lookOf(h: HandoverEntity, orphan: Boolean): EntryLook = when {
    h.syncStatus == SyncMark.REJECTED -> EntryLook.Rejected
    h.status == ServerStatus.OVERRULED -> EntryLook.Overruled
    orphan || h.status == ServerStatus.ORPHAN -> EntryLook.Orphan
    else -> EntryLook.Normal
}

/** Both signatures, the record hash and the link to the record before, checked on this phone. */
fun checkSignatures(h: HandoverEntity, parent: HandoverEntity?, roster: Roster): SigCheck {
    if (h.kind == HandoverKind.REGISTER) {
        return if (h.recordHash.isEmpty() || h.recordHash == Canonical.registerHash(h.id, h.itemId, h.toPersonId)) SigCheck.Unsigned else SigCheck.Invalid
    }
    val giverSig = h.giverSig
    val receiverSig = h.receiverSig
    if (giverSig == null || receiverSig == null) return SigCheck.Unsigned
    val giver = roster.deviceOf(h.fromPersonId, h.fromDeviceId) ?: return SigCheck.UnknownKey
    val receiver = roster.deviceOf(h.toPersonId, h.toDeviceId) ?: return SigCheck.UnknownKey
    val ok = Signatures.verify(giver.publicKey, Canonical.offerString(h), giverSig) &&
        Signatures.verify(receiver.publicKey, Canonical.acceptanceString(h), receiverSig) &&
        h.recordHash == Canonical.recordHash(h) &&
        (parent == null || parent.recordHash == h.prevHash)
    return if (ok) SigCheck.Valid else SigCheck.Invalid
}

private fun resolutionNode(r: ResolutionEntity, byId: Map<String, HandoverEntity>, roster: Roster): TimelineNode.Resolution {
    val chosen = byId[r.chosenHandoverId]
    val device = roster.deviceOf(r.decidedByPersonId, r.decidedByDeviceId)
    val sig = when {
        device == null -> SigCheck.UnknownKey
        Signatures.verify(device.publicKey, Canonical.resolutionString(r), r.sig) -> SigCheck.Valid
        else -> SigCheck.Invalid
    }
    return TimelineNode.Resolution(
        chosenTitle = chosen?.let { "${roster.person(it.fromPersonId)?.name ?: "Unknown"} to ${roster.person(it.toPersonId)?.name ?: "Unknown"}" } ?: "A record not on this phone",
        by = roster.person(r.decidedByPersonId)?.name ?: "Unknown person",
        note = r.note, timeMs = r.decidedMs, sig = sig, synced = r.syncStatus == SyncMark.SYNCED,
    )
}
