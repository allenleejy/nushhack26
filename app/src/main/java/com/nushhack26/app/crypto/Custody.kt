package com.nushhack26.app.crypto

import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.ResolutionEntity

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
): CustodyState = TODO("card 01")
