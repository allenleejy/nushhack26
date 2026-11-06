package com.nushhack26.app.crypto

import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.ResolutionEntity
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.ServerStatus
import com.nushhack26.app.data.local.SyncMark

enum class CustodyStatus { InStore, Out, Overdue, Disputed, Unknown }

/** Who holds a camera, as far as the records on this phone show. */
data class CustodyState(
    val item: ItemEntity,
    val status: CustodyStatus,
    /** The holder's person id; null when Disputed or Unknown. */
    val holderId: String? = null,
    val head: HandoverEntity? = null,
    val dueMs: Long? = null,
    /** When the head record was accepted (the receiver's phone time). */
    val lastSignedMs: Long? = null,
    /** True when every record on the path to the head has synced. */
    val allSynced: Boolean = true,
    /** At a fork with no resolution: the first record of each branch. */
    val forkBranches: List<HandoverEntity> = emptyList(),
)

/**
 * Walks the camera's chain from its register record to the head (docs/sync.md A.6).
 * [serverView] uses only records that have synced: the safety officer's "as the server
 * knows it". Card 01.
 */
fun deriveCustody(
    item: ItemEntity,
    handovers: List<HandoverEntity>,
    resolutions: List<ResolutionEntity>,
    nowMs: Long,
    serverView: Boolean = false,
): CustodyState {
    val records = handovers.filter {
        it.itemId == item.id && !it.hiddenByTamper && it.syncStatus != SyncMark.REJECTED &&
            it.status != ServerStatus.OVERRULED && it.status != ServerStatus.ORPHAN &&
            (!serverView || it.serverSeq != null)
    }.distinctBy { it.id }
    val byId = records.associateBy { it.id }
    val children = records.groupBy { it.prevId }
    val choices = resolutions.filter {
        it.itemId == item.id && it.syncStatus != SyncMark.REJECTED && (!serverView || it.serverSeq != null)
    }
    var head = records.firstOrNull { it.kind == HandoverKind.REGISTER && it.prevId == null }
        ?: return CustodyState(item, CustodyStatus.Unknown)
    var allSynced = head.serverSeq != null
    val visited = mutableSetOf<String>()

    fun branchContains(branch: HandoverEntity, chosenId: String): Boolean {
        var cursor = byId[chosenId]
        val seen = mutableSetOf<String>()
        while (cursor != null && seen.add(cursor.id)) {
            if (cursor.id == branch.id) return true
            cursor = byId[cursor.prevId]
        }
        return false
    }

    while (visited.add(head.id)) {
        val next = children[head.id].orEmpty().sortedBy { it.id }
        if (next.isEmpty()) {
            val status = when (head.kind) {
                HandoverKind.REGISTER, HandoverKind.RETURN -> CustodyStatus.InStore
                HandoverKind.ISSUE, HandoverKind.TRANSFER ->
                    if (head.dueMs?.let { it < nowMs } == true) CustodyStatus.Overdue else CustodyStatus.Out
                else -> CustodyStatus.Unknown
            }
            return CustodyState(item, status, if (status == CustodyStatus.Unknown) null else head.toPersonId,
                head, head.dueMs, head.acceptedMs, allSynced)
        }
        val child = if (next.size == 1) next.single() else {
            val selected = next.filter { branch -> choices.any { branchContains(branch, it.chosenHandoverId) } }
            if (selected.size != 1) {
                return CustodyState(item, CustodyStatus.Disputed, head = head, dueMs = head.dueMs,
                    lastSignedMs = head.acceptedMs, allSynced = allSynced, forkBranches = next)
            }
            selected.single()
        }
        head = child
        allSynced = allSynced && head.serverSeq != null
    }
    return CustodyState(item, CustodyStatus.Unknown, allSynced = allSynced)
}
