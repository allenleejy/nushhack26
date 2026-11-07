package com.nushhack26.app.ui.missing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.auth.AccessRules
import com.nushhack26.app.crypto.Roster
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.MissingReportEntity
import com.nushhack26.app.data.local.ServerStatus
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.repo.CustodyRepository
import com.nushhack26.app.data.repo.IdentityRepository
import com.nushhack26.app.data.repo.IdentityState
import com.nushhack26.app.data.repo.SafetyRepository
import com.nushhack26.app.data.repo.handoverNumber
import com.nushhack26.app.ui.store.LOCAL_DB_ERROR
import com.nushhack26.app.ui.store.clockText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

const val NOTE_MAX = 280
const val SAVE_ERROR = "Couldn't save the report. Try again."
const val NOT_ENROLLED_ERROR = "This phone isn't enrolled. Enrol it before reporting a camera missing."

data class MissingCamera(val code: String, val name: String, val source: String, val sealNo: String) {
    val displayName: String get() = name.ifBlank { code }
}

/** The receiver of the newest record both people signed, or the store holder on the register record. */
data class LastHolder(val name: String, val role: String, val detail: String)

/** A report this phone already holds for the camera: "Reported missing by Wei Ming · 14:52". */
data class EarlierReport(val id: String, val byName: String, val reportedMs: Long)

/** The report just sent from this screen, with its sync mark as Room has it. */
data class SentReport(val reportedMs: Long, val syncStatus: String, val reason: String?)

data class MissingInput(
    val note: String = "",
    val sending: Boolean = false,
    val error: String? = null,
    val sentId: String? = null,
    val sentMs: Long? = null,
)

sealed interface ReportMissingUiState {
    data object Loading : ReportMissingUiState
    data object NotFound : ReportMissingUiState
    data class Error(val message: String) : ReportMissingUiState
    data class Ready(
        val camera: MissingCamera,
        val holder: LastHolder?,
        val lastHandoverId: String?,
        val earlier: List<EarlierReport> = emptyList(),
        val note: String = "",
        val sending: Boolean = false,
        val error: String? = null,
        val sent: SentReport? = null,
    ) : ReportMissingUiState
}

sealed interface ReportMissingAction {
    data class Note(val text: String) : ReportMissingAction
    data object Send : ReportMissingAction
    data object Done : ReportMissingAction
}

class ReportMissingViewModel(
    private val itemId: String,
    custody: CustodyRepository,
    private val safety: SafetyRepository,
    private val identity: IdentityRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val input = MutableStateFlow(MissingInput())

    private val records = combine(
        custody.observeItems().map { items -> items.firstOrNull { it.id == itemId } },
        custody.observeChain(itemId),
        custody.observeRoster(),
    ) { item, chain, roster -> Triple(item, chain, roster) }

    val state: StateFlow<ReportMissingUiState> = combine(
        records,
        safety.observeOutbox().map { data -> data.missingReports.filter { it.itemId == itemId } },
        identity.state,
        input,
    ) { (item, chain, roster), reports, me, input ->
        if (me is IdentityState.NotEnrolled) ReportMissingUiState.Error(NOT_ENROLLED_ERROR)
        else buildReportMissingState(item, chain, roster, reports, input)
    }
        .catch { emit(ReportMissingUiState.Error(LOCAL_DB_ERROR)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportMissingUiState.Loading)

    fun setNote(text: String) = input.update { it.copy(note = text.take(NOTE_MAX), error = null) }

    /** Saves the report with a phone-made id and its outbox entry; it syncs when the network allows. */
    fun send() {
        val ready = state.value as? ReportMissingUiState.Ready ?: return
        if (ready.sending || ready.sent != null) return
        input.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            val me = identity.current() ?: run {
                input.update { it.copy(sending = false, error = NOT_ENROLLED_ERROR) }
                return@launch
            }
            val report = MissingReportEntity(
                id = UUID.randomUUID().toString(),
                itemId = itemId,
                reportedByPersonId = me.personId,
                reportedByDeviceId = me.deviceId,
                lastHandoverId = ready.lastHandoverId,
                note = ready.note.trim(),
                reportedMs = clock(),
            )
            runCatching { safety.saveMissingReport(report) }
                .onSuccess { input.update { it.copy(sending = false, sentId = report.id, sentMs = report.reportedMs) } }
                .onFailure { input.update { it.copy(sending = false, error = SAVE_ERROR) } }
        }
    }
}

fun buildReportMissingState(
    item: ItemEntity?,
    chain: List<HandoverEntity>,
    roster: Roster,
    reports: List<MissingReportEntity>,
    input: MissingInput = MissingInput(),
): ReportMissingUiState {
    if (item == null) return ReportMissingUiState.NotFound
    val records = chain.filter { it.itemId == item.id && !it.tamperHidden && it.syncStatus != SyncMark.REJECTED }
    val last = lastSignedRecord(records)
    val holder = last?.let { h ->
        val person = roster.person(h.toPersonId)
        val name = person?.name ?: "Unknown person"
        LastHolder(
            name = name,
            role = person?.role?.let { AccessRules.roleName(it).replaceFirstChar(Char::uppercase) } ?: "Not on this phone's roster",
            detail = if (h.kind == HandoverKind.REGISTER) {
                "In store with $name · register record"
            } else {
                listOfNotNull(h.acceptedMs?.let { "Signed ${clockText(it)}" }, handoverNumber(h.id, chain)?.let { "H$it" }).joinToString(" · ")
            },
        )
    }
    val sentRow = input.sentId?.let { id -> reports.firstOrNull { it.id == id } }
    val sent = input.sentId?.let {
        SentReport(
            reportedMs = sentRow?.reportedMs ?: input.sentMs ?: 0L,
            syncStatus = sentRow?.syncStatus ?: SyncMark.WAITING,
            reason = sentRow?.syncReason,
        )
    }
    val earlier = reports.filter { it.id != input.sentId }
        .sortedByDescending { it.reportedMs }
        .map { EarlierReport(it.id, roster.person(it.reportedByPersonId)?.name ?: "Unknown person", it.reportedMs) }
    return ReportMissingUiState.Ready(
        camera = MissingCamera(item.code, item.name, "${item.sourceSerial} · ${item.isotope}", item.sealNo),
        holder = holder,
        lastHandoverId = last?.id,
        earlier = earlier,
        note = input.note,
        sending = input.sending,
        error = input.error,
        sent = sent,
    )
}

/**
 * The newest record both people signed, by its place in the chain (not phone clocks), skipping
 * overruled branches. With only the register record, that record.
 */
private fun lastSignedRecord(records: List<HandoverEntity>): HandoverEntity? {
    val signed = records.filter {
        it.kind != HandoverKind.REGISTER && it.acceptedMs != null && it.receiverSig != null && it.status != ServerStatus.OVERRULED
    }
    return signed.maxWithOrNull(
        compareBy<HandoverEntity> { handoverNumber(it.id, records) ?: 0 }
            .thenBy { it.serverSeq ?: Long.MAX_VALUE }
            .thenBy { it.acceptedMs },
    ) ?: records.firstOrNull { it.kind == HandoverKind.REGISTER }
}
