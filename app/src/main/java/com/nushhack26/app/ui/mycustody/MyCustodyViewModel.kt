package com.nushhack26.app.ui.mycustody

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.crypto.Canonical
import com.nushhack26.app.crypto.CustodyStatus
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.PendingOfferEntity
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.repo.CustodyBoard
import com.nushhack26.app.data.repo.CustodyRepository
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.data.repo.IdentityRepository
import com.nushhack26.app.ui.store.LOCAL_DB_ERROR
import com.nushhack26.app.ui.store.cameraPhrase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** A camera I hold. [startMs] is when it became mine, for the countdown bar. */
data class HeldCamera(
    val itemId: String,
    val code: String,
    val source: String,
    val sealNo: String,
    val status: CustodyStatus,
    val dueMs: Long?,
    val startMs: Long?,
    val synced: Boolean,
    val name: String = "",
) {
    val displayName: String get() = name.ifBlank { code }
    val phrase: String get() = cameraPhrase(name, code)
}

/** An offer I made that's waiting for the receiver's acceptance code. */
data class PendingOfferRow(val itemId: String, val code: String, val toName: String, val expiresMs: Long, val name: String = "") {
    val phrase: String get() = cameraPhrase(name, code)
}

/** A handover I gave: the receiver signed for the camera. */
data class ReceiptRow(
    val handoverId: String,
    val code: String,
    val kind: String,
    val toName: String,
    val acceptedMs: Long?,
    val sealNo: String?,
    val matchCode: String,
    val syncStatus: String,
    val name: String = "",
) {
    val displayName: String get() = name.ifBlank { code }
    val phrase: String get() = cameraPhrase(name, code)
}

sealed interface MyCustodyUiState {
    data object Loading : MyCustodyUiState
    data class Error(val message: String) : MyCustodyUiState
    data class Ready(
        val nowMs: Long,
        val held: List<HeldCamera>,
        val pending: List<PendingOfferRow> = emptyList(),
        val receipts: List<ReceiptRow> = emptyList(),
    ) : MyCustodyUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class MyCustodyViewModel(repository: CustodyRepository, identity: IdentityRepository) : ViewModel() {
    val state: StateFlow<MyCustodyUiState> = identity.identity
        .flatMapLatest<Identity?, MyCustodyUiState> { me ->
            if (me == null) flowOf(MyCustodyUiState.Ready(System.currentTimeMillis(), emptyList())) else observe(repository, me)
        }
        .catch { emit(MyCustodyUiState.Error(LOCAL_DB_ERROR)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyCustodyUiState.Loading)

    private fun observe(repository: CustodyRepository, me: Identity): Flow<MyCustodyUiState> =
        combine(repository.observeBoard(), repository.observeMyPendingOffers()) { board, offers ->
            myCustody(board, offers, me.personId)
        }
}

fun myCustody(board: CustodyBoard, offers: List<PendingOfferEntity>, personId: String): MyCustodyUiState.Ready {
    val held = board.states
        .filter { s ->
            (s.holderId == personId && (s.status == CustodyStatus.Out || s.status == CustodyStatus.Overdue)) ||
                (s.status == CustodyStatus.Disputed && s.forkBranches.any { it.toPersonId == personId || it.fromPersonId == personId })
        }
        .map { s ->
            HeldCamera(
                itemId = s.item.id, code = s.item.code, source = "${s.item.sourceSerial} · ${s.item.isotope}",
                sealNo = s.item.sealNo, status = s.status, dueMs = s.dueMs, startMs = issueStart(s.head, board.handovers),
                synced = s.allSynced, name = s.item.name,
            )
        }
        .sortedWith(compareBy({ it.status != CustodyStatus.Disputed }, { it.dueMs ?: Long.MAX_VALUE }, { it.code }))
    val item = { itemId: String -> board.roster.items.firstOrNull { it.id == itemId } ?: board.states.firstOrNull { it.item.id == itemId }?.item }
    val code = { itemId: String -> item(itemId)?.code ?: itemId }
    val itemName = { itemId: String -> item(itemId)?.name.orEmpty() }
    val name = { id: String? -> board.roster.person(id)?.name ?: "Unknown person" }
    val pending = offers
        .filter { it.expiresMs > board.nowMs }
        .map { PendingOfferRow(it.itemId, code(it.itemId), name(it.toPersonId), it.expiresMs, itemName(it.itemId)) }
    val receipts = CustodyRepository.receiptsOf(board.handovers, personId).map { h ->
        ReceiptRow(
            handoverId = h.id, code = code(h.itemId), kind = h.kind, toName = name(h.toPersonId),
            acceptedMs = h.acceptedMs, sealNo = h.sealNo,
            matchCode = runCatching { Canonical.matchCode(h.recordHash) }.getOrDefault(""),
            syncStatus = h.syncStatus, name = itemName(h.itemId),
        )
    }
    return MyCustodyUiState.Ready(board.nowMs, held, pending, receipts)
}

/** When the loan began: the issue that set the due time (a transfer copies it, so walk back). */
private fun issueStart(head: HandoverEntity?, all: List<HandoverEntity>): Long? {
    var record = head
    var guard = 0
    while (record != null && record.kind == HandoverKind.TRANSFER && guard++ < 1_000) {
        record = all.firstOrNull { it.id == record?.prevId }
    }
    return (record ?: head)?.let { it.acceptedMs ?: it.offeredMs }
}

val ReceiptRow.isSynced get() = syncStatus == SyncMark.SYNCED
