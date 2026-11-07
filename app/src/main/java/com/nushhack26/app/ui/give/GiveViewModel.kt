package com.nushhack26.app.ui.give

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.auth.AccessRules
import com.nushhack26.app.crypto.Canonical
import com.nushhack26.app.crypto.DeviceKeys
import com.nushhack26.app.crypto.HandoverBuilder
import com.nushhack26.app.crypto.OfferChecker
import com.nushhack26.app.crypto.QrPayloads
import com.nushhack26.app.crypto.Roster
import com.nushhack26.app.crypto.ScannedCode
import com.nushhack26.app.crypto.deriveCustody
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.PendingOfferEntity
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.ResolutionEntity
import com.nushhack26.app.data.local.Roles
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.remote.RelayStatus
import com.nushhack26.app.data.remote.RelayStatusKind
import com.nushhack26.app.data.remote.handoverFromJson
import com.nushhack26.app.data.repo.GiveRepository
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.data.repo.IdentityRepository
import com.nushhack26.app.data.repo.RelayOutcome
import com.nushhack26.app.data.repo.RelayRepository
import com.nushhack26.app.nearby.GiverTapEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** [Sent] is G3-online: the offer went through the relay and the phone waits for the acceptance. */
enum class GiveStep { Choose, Review, Sent, Offer, Scan, Done }

/** What one relay status check means for G3-online (docs/sync.md A.10). */
sealed interface RelayStep {
    data object Wait : RelayStep
    data class Accepted(val acceptance: String) : RelayStep
    data object Declined : RelayStep
    data object Gone : RelayStep
    data object LostNetwork : RelayStep
}

/** [nullsInARow] counts this check: three unanswered checks in a row mean the network is gone. */
fun relayStep(status: RelayStatus?, nullsInARow: Int): RelayStep = when (status?.status) {
    null -> if (nullsInARow >= 3) RelayStep.LostNetwork else RelayStep.Wait
    RelayStatusKind.ACCEPTED -> status.acceptance?.takeIf { it.isNotEmpty() }?.let { RelayStep.Accepted(it) } ?: RelayStep.Wait
    RelayStatusKind.DECLINED -> RelayStep.Declined
    RelayStatusKind.CANCELLED, RelayStatusKind.UNKNOWN -> RelayStep.Gone
    else -> RelayStep.Wait
}

/** The due-time chips (issue only). A clock time already passed today means tomorrow. */
enum class DueChip(val label: String) {
    Plus90s("+90 s"), Plus5Min("+5 min"), At1700("17:00"), At1900("19:00");

    fun dueMs(nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): Long = when (this) {
        Plus90s -> nowMs + 90_000
        Plus5Min -> nowMs + 5 * 60_000
        At1700 -> clock(nowMs, 17, zone)
        At1900 -> clock(nowMs, 19, zone)
    }

    private fun clock(nowMs: Long, hour: Int, zone: ZoneId): Long {
        val now = Instant.ofEpochMilli(nowMs).atZone(zone)
        var at = ZonedDateTime.of(now.toLocalDate(), LocalTime.of(hour, 0), zone)
        if (!at.isAfter(now)) at = at.plusDays(1)
        return at.toInstant().toEpochMilli()
    }
}

/** A person in the picker: greyed out with [text] when [selectable] is false (docs/sync.md A.8). */
data class ReceiverOption(
    val id: String,
    val name: String,
    val role: String,
    val group: String,
    val selectable: Boolean,
    val text: String,
)

data class OfferView(
    val payload: String,
    val expiresMs: Long,
)

data class DoneView(
    val recordNo: Int,
    val matchCode: String,
    val acceptedMs: Long,
    val synced: Boolean,
    /** The acceptance came through the server (A.10), not a scanned QR. */
    val viaServer: Boolean = false,
    /** The acceptance came back over the tap link (A.11). */
    val viaTap: Boolean = false,
)

/** Who and what, shown on G2, G3 and the receipt. */
data class HandoverSummary(
    val kind: String,
    val giverName: String,
    val giverRole: String,
    val receiverName: String,
    val receiverRole: String,
    val itemCode: String,
    val itemLine: String,
    val sealNo: String,
    val dueMs: Long?,
)

sealed interface GiveSnack {
    data class Problem(val title: String, val message: String) : GiveSnack
    data class Info(val message: String) : GiveSnack
    data class Confirmation(val title: String, val facts: String) : GiveSnack
}

data class GiveUiState(
    val loading: Boolean = true,
    val step: GiveStep = GiveStep.Choose,
    /** "You don't hold this camera" or "No one on the roster can receive it. Sync once." */
    val empty: String? = null,
    val item: ItemEntity? = null,
    val isIssue: Boolean = false,
    val receivers: List<ReceiverOption> = emptyList(),
    val selectedReceiverId: String? = null,
    val due: DueChip? = null,
    val pickerOpen: Boolean = false,
    val summary: HandoverSummary? = null,
    val leaseExpired: Boolean = false,
    val signing: Boolean = false,
    /** Decided at G2: online sends the offer through the server, offline shows the QR. */
    val online: Boolean = false,
    val offer: OfferView? = null,
    /** G3-online: the receiver declined. */
    val declined: Boolean = false,
    /** G3-online: the relayed acceptance failed the giver's checks. */
    val relayError: String? = null,
    val scanError: String? = null,
    /** G3: NFC and Bluetooth are on with the permissions, so the tap line shows. Set by the screen. */
    val tapReady: Boolean = false,
    /** G3: the receiver's phone read the offer by tap; this phone finishes when the acceptance arrives. */
    val tapped: Boolean = false,
    /** G3: the acceptance that came over the tap link failed the giver's checks. */
    val tapError: String? = null,
    val done: DoneView? = null,
    val snack: GiveSnack? = null,
    val nowMs: Long = 0L,
) {
    val selectedReceiver: ReceiverOption? get() = receivers.firstOrNull { it.id == selectedReceiverId }
    val receiverFirstName: String get() = summary?.receiverName?.let(::callName) ?: selectedReceiver?.name?.let(::callName).orEmpty()
    val canReview: Boolean get() = selectedReceiver?.selectable == true && (!isIssue || due != null)
    val offerExpired: Boolean get() = offer != null && nowMs > offer.expiresMs
    val secondsLeft: Long get() = offer?.let { ((it.expiresMs - nowMs).coerceAtLeast(0) + 999) / 1_000 } ?: 0
}

sealed interface GiveAction {
    data object OpenPicker : GiveAction
    data object ClosePicker : GiveAction
    data class SelectReceiver(val id: String) : GiveAction
    data class SelectDue(val chip: DueChip) : GiveAction
    data object Review : GiveAction
    data object BackToChoose : GiveAction
    data object Sign : GiveAction
    data object ScanAcceptance : GiveAction
    data object ShowQrInstead : GiveAction
    data object ChooseSomeoneElse : GiveAction
    data object BackToOffer : GiveAction
    data object CancelOffer : GiveAction
    data object SignNewOffer : GiveAction
    data object CopyCode : GiveAction
    data class CodeScanned(val text: String) : GiveAction
    data object DismissSnack : GiveAction
    data object Finish : GiveAction
}

/** The name colleagues use: "Ravi" for Ravi Kumar, "Wei Ming" for Tan Wei Ming. */
fun callName(fullName: String): String {
    val words = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.dropWhile { it == "Dr" }
    return when {
        words.isEmpty() -> fullName
        words.size >= 3 -> words.drop(1).joinToString(" ")
        else -> words.first()
    }
}

private data class GiveData(
    val item: ItemEntity?,
    val chain: List<HandoverEntity>,
    val resolutions: List<ResolutionEntity>,
    val roster: Roster,
    val pending: PendingOfferEntity?,
    val me: Identity?,
)

private data class FlowState(
    val step: GiveStep = GiveStep.Choose,
    val selectedReceiverId: String? = null,
    val due: DueChip? = null,
    val pickerOpen: Boolean = false,
    val signing: Boolean = false,
    val leaseExpired: Boolean = false,
    val online: Boolean = false,
    /** The offer was posted to the relay, so cancelling it also closes it there. */
    val relayed: Boolean = false,
    val declined: Boolean = false,
    val relayError: String? = null,
    val viaServer: Boolean = false,
    val tapped: Boolean = false,
    val tapError: String? = null,
    val viaTap: Boolean = false,
    /** The signed offer being shown or waiting for its acceptance. */
    val offer: HandoverEntity? = null,
    val proof: HandoverEntity? = null,
    val scanError: String? = null,
    val completed: HandoverEntity? = null,
    val snack: GiveSnack? = null,
    val resumeChecked: Boolean = false,
)

class GiveViewModel(
    private val itemId: String,
    private val repository: GiveRepository,
    private val identity: IdentityRepository,
    private val deviceKeys: () -> DeviceKeys,
    private val relay: RelayRepository,
    private val isOnline: StateFlow<Boolean>,
    private val serverConfigured: Boolean,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val flowState = MutableStateFlow(FlowState())

    /** Set while a handover is being completed, so the relay, the sync and a scan can't finish it twice. */
    private var finishing = false

    private val ticker = flow {
        while (true) {
            emit(clock())
            delay(1_000)
        }
    }

    private val data = combine(
        repository.observeItem(itemId),
        repository.observeChain(itemId),
        repository.observeResolutions(),
        repository.observeRoster(),
        repository.observePendingOffer(itemId),
        identity.identity,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        GiveData(
            item = values[0] as ItemEntity?,
            chain = values[1] as List<HandoverEntity>,
            resolutions = values[2] as List<ResolutionEntity>,
            roster = values[3] as Roster,
            pending = values[4] as PendingOfferEntity?,
            me = values[5] as Identity?,
        )
    }

    val state: StateFlow<GiveUiState> = combine(data, flowState, ticker) { d, f, now -> build(d, f, now) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GiveUiState())

    init {
        viewModelScope.launch {
            // A pending offer left unscanned reopens at G4 (scan the acceptance).
            val d = data.first()
            val pending = d.pending
            flowState.update { f ->
                if (f.resumeChecked || pending == null || f.offer != null) {
                    f.copy(resumeChecked = true)
                } else {
                    val offer = runCatching { handoverFromJson(pending.offerJson) }.getOrNull()
                    if (offer == null) f.copy(resumeChecked = true)
                    else f.copy(
                        resumeChecked = true,
                        step = GiveStep.Scan,
                        offer = offer,
                        proof = pending.proofId?.let { id -> d.chain.firstOrNull { it.id == id } },
                        selectedReceiverId = offer.toPersonId,
                    )
                }
            }
            flowState.update { it.copy(leaseExpired = identity.leaseNow(clock()).expired) }
            flowState.value.offer?.takeIf { flowState.value.step == GiveStep.Scan }?.let { resumeRelayed(it) }
        }
    }

    /** A resumed offer may have gone through the relay: reopen G3-online, or finish if it was accepted. */
    private suspend fun resumeRelayed(offer: HandoverEntity) {
        val status = relay.status(offer.id) ?: return
        val f = flowState.value
        if (f.step != GiveStep.Scan || f.offer?.id != offer.id) return
        when (val next = relayStep(status, 0)) {
            is RelayStep.Accepted -> finishFromLink(offer, next.acceptance, viaTap = false)
            else -> if (status.status == RelayStatusKind.OPEN) {
                flowState.update { it.copy(step = GiveStep.Sent, relayed = true, declined = false, relayError = null) }
            }
        }
    }

    fun onAction(action: GiveAction) {
        when (action) {
            GiveAction.OpenPicker -> flowState.update { it.copy(pickerOpen = true) }
            GiveAction.ClosePicker -> flowState.update { it.copy(pickerOpen = false) }
            is GiveAction.SelectReceiver -> flowState.update { it.copy(selectedReceiverId = action.id, pickerOpen = false) }
            is GiveAction.SelectDue -> flowState.update { it.copy(due = action.chip) }
            GiveAction.Review -> if (state.value.canReview) {
                viewModelScope.launch {
                    val expired = identity.leaseNow(clock()).expired
                    val online = isOnline.value && serverConfigured
                    flowState.update { it.copy(step = GiveStep.Review, leaseExpired = expired, online = online, snack = null) }
                }
            }
            GiveAction.BackToChoose -> flowState.update { it.copy(step = GiveStep.Choose, snack = null) }
            GiveAction.ScanAcceptance -> flowState.update { it.copy(step = GiveStep.Scan, scanError = null, tapped = false, tapError = null) }
            GiveAction.BackToOffer -> flowState.update { it.copy(step = GiveStep.Offer, scanError = null) }
            GiveAction.ShowQrInstead -> flowState.update { it.copy(step = GiveStep.Offer, relayError = null) }
            GiveAction.ChooseSomeoneElse -> discardOffer(null, toStep = GiveStep.Choose)
            GiveAction.CancelOffer -> discardOffer(GiveSnack.Info("Offer cancelled. Nothing was handed over."))
            GiveAction.SignNewOffer -> discardOffer(null)
            is GiveAction.CodeScanned -> onCodeScanned(action.text)
            GiveAction.DismissSnack -> flowState.update { it.copy(snack = null) }
            GiveAction.Sign, GiveAction.CopyCode, GiveAction.Finish -> Unit // Handled by the screen.
        }
    }

    /** G3 tap (docs/sync.md A.11): what the tap link reports while this phone serves the offer. */
    fun onTapEvent(event: GiverTapEvent) {
        when (event) {
            GiverTapEvent.Tapped -> flowState.update { if (it.step == GiveStep.Offer) it.copy(tapped = true, tapError = null) else it }
            GiverTapEvent.Linked -> Unit
            is GiverTapEvent.Acceptance -> {
                val f = flowState.value
                val offer = f.offer ?: return
                if (f.step != GiveStep.Offer) return
                viewModelScope.launch { finishFromLink(offer, event.text, viaTap = true) }
            }
            // The QR stays the way through, so a dropped link just brings the tap line back.
            is GiverTapEvent.Failed -> flowState.update { it.copy(tapped = false) }
        }
    }

    /** Before the fingerprint prompt: the subtitle to show, or null if this phone can't sign now. */
    suspend fun prepareSign(): String? {
        val expired = identity.leaseNow(clock()).expired
        flowState.update { it.copy(leaseExpired = expired) }
        val s = state.value
        if (expired || s.signing || s.summary == null) return null
        flowState.update { it.copy(signing = true, snack = null) }
        return "${s.summary.giverName} → ${s.summary.receiverName} · ${s.summary.itemCode}"
    }

    fun signCancelled() {
        flowState.update {
            it.copy(signing = false, snack = GiveSnack.Problem("Fingerprint cancelled.", "Nothing was handed over."))
        }
    }

    /** After the fingerprint: build, sign and save the offer, then show its QR (G3). */
    suspend fun signAndShow() {
        val result = runCatching {
            val d = data.first()
            val f = flowState.value
            val me = requireNotNull(d.me) { "This phone isn't enrolled" }
            val item = requireNotNull(d.item) { "Unknown camera" }
            val receiver = requireNotNull(d.roster.person(f.selectedReceiverId)) { "Choose who receives it" }
            val now = clock()
            val custody = deriveCustody(item, d.chain, d.resolutions, now)
            val head = custody.head?.takeIf { custody.holderId == me.personId } ?: error("You don't hold this camera")
            val giver = d.roster.person(me.personId) ?: PersonEntity(me.personId, me.name, me.role, staffNo = "")
            check(AccessRules.checkPick(giver, receiver, now).ok) { AccessRules.checkPick(giver, receiver, now).text }
            val newOffer = HandoverBuilder.newOffer(head, me, receiver, f.due?.dueMs(now), now)
            // The expiry is inside the signed string, so online or QR is fixed before signing.
            val unsigned = if (f.online) newOffer.copy(expiresMs = Math.addExact(now, RelayRepository.RELAYED_OFFER_VALID_MS)) else newOffer
            val signature = withContext(Dispatchers.Default) { deviceKeys().sign(Canonical.offerString(unsigned)) }
            val offer = HandoverBuilder.withGiverSig(unsigned, signature)
            val proof = head.takeIf { it.kind != HandoverKind.REGISTER }
            val payload = QrPayloads.offer(offer, proof) // Fails here, before saving, if the code can't be made.
            repository.savePendingOffer(offer, proof?.id)
            val sent = f.online &&
                relay.post(offer.id, offer.itemId, offer.toPersonId, payload, offer.expiresMs ?: 0L) == RelayOutcome.Sent
            Triple(offer, proof, sent)
        }
        result.onSuccess { (offer, proof, sent) ->
            flowState.update {
                it.copy(
                    signing = false, step = if (sent) GiveStep.Sent else GiveStep.Offer, offer = offer, proof = proof,
                    relayed = sent, declined = false, relayError = null, scanError = null, tapped = false, tapError = null,
                )
            }
        }.onFailure { e ->
            flowState.update {
                it.copy(signing = false, snack = GiveSnack.Problem("Couldn't sign.", e.message ?: "Nothing was handed over."))
            }
        }
    }

    private fun onCodeScanned(text: String) {
        val f = flowState.value
        val offer = f.offer ?: return
        if (f.step != GiveStep.Scan) return
        viewModelScope.launch {
            val d = data.first()
            val name = d.roster.person(offer.toPersonId)?.name?.let(::callName) ?: "the receiver"
            val code = QrPayloads.parse(text)
            if (code !is ScannedCode.Acceptance) {
                flowState.update { it.copy(scanError = "Not a handover code.") }
                return@launch
            }
            val check = OfferChecker.checkAcceptance(code, offer, d.roster)
            if (!check.ok) {
                val message = if (check.text.startsWith("Unknown signer") || check.text.contains("was withdrawn")) check.text else "This isn't $name's acceptance of this offer."
                flowState.update { it.copy(scanError = message) }
                return@launch
            }
            if (finishing) return@launch
            finishing = true
            val record = HandoverBuilder.fromAcceptance(offer, code)
            repository.completeHandover(record)
            showDone(record, d, viaServer = false)
        }
    }

    /**
     * G3-online, while the screen is visible: checks the relay every 2 s, watches Room for the
     * receiver's record arriving by sync, and falls back to the QR when the network goes.
     */
    suspend fun watchRelay() = coroutineScope {
        val offer = flowState.value.offer ?: return@coroutineScope
        fun waiting() = flowState.value.let {
            it.step == GiveStep.Sent && it.offer?.id == offer.id && !it.declined && it.relayError == null && !finishing
        }
        launch {
            val arrived = repository.handover(offer.id).filterNotNull().first()
            if (waiting()) viewModelScope.launch { finishFromSync(offer, arrived) }
        }
        launch {
            isOnline.first { !it }
            if (waiting()) lostNetwork()
        }
        var nulls = 0
        while (waiting()) {
            val status = relay.status(offer.id)
            nulls = if (status == null) nulls + 1 else 0
            if (!waiting()) break
            when (val next = relayStep(status, nulls)) {
                RelayStep.Wait -> Unit
                is RelayStep.Accepted -> viewModelScope.launch { finishFromLink(offer, next.acceptance, viaTap = false) }.join()
                RelayStep.Declined -> flowState.update { it.copy(declined = true) }
                RelayStep.Gone -> discardOffer(GiveSnack.Info(NO_LONGER_OPEN), toStep = GiveStep.Choose, closeRelay = false)
                RelayStep.LostNetwork -> lostNetwork()
            }
            if (waiting()) delay(RelayRepository.POLL_MS)
        }
        coroutineContext.cancelChildren()
    }

    private fun lostNetwork() {
        flowState.update { it.copy(step = GiveStep.Offer, snack = GiveSnack.Info(LOST_NETWORK)) }
    }

    /** A relayed or tapped `CS1A:` string gets the same checks as a scanned one (G4). */
    private suspend fun finishFromLink(offer: HandoverEntity, acceptance: String, viaTap: Boolean) {
        if (finishing) return
        finishing = true
        val d = data.first()
        val name = d.roster.person(offer.toPersonId)?.name?.let(::callName) ?: "the receiver"
        val code = QrPayloads.parse(acceptance)
        val check = (code as? ScannedCode.Acceptance)?.let { OfferChecker.checkAcceptance(it, offer, d.roster) }
        if (code !is ScannedCode.Acceptance || check?.ok != true) {
            finishing = false
            val message = when {
                check == null -> "Not a handover code."
                check.text.startsWith("Unknown signer") || check.text.contains("was withdrawn") -> check.text
                else -> "This isn't $name's acceptance of this offer."
            }
            flowState.update { if (viaTap) it.copy(tapError = message, tapped = false) else it.copy(relayError = message) }
            return
        }
        val record = HandoverBuilder.fromAcceptance(offer, code)
        repository.completeHandover(record)
        showDone(record, d, viaServer = !viaTap, viaTap = viaTap)
    }

    /** The receiver's record reached this phone by sync before the relay answered. */
    private suspend fun finishFromSync(offer: HandoverEntity, record: HandoverEntity) {
        if (finishing) return
        finishing = true
        repository.cancelPendingOffer(offer.id)
        showDone(record, data.first(), viaServer = true)
    }

    private fun showDone(record: HandoverEntity, d: GiveData, viaServer: Boolean, viaTap: Boolean = false) {
        val facts = listOfNotNull(
            d.item?.code,
            "Holder: ${d.roster.person(record.toPersonId)?.name?.let(::callName) ?: "the receiver"}",
            record.dueMs?.let { "Return by ${hhmm(it)}" },
        ).joinToString(" · ")
        flowState.update {
            it.copy(
                step = GiveStep.Done, completed = record, scanError = null, relayError = null, viaServer = viaServer,
                viaTap = viaTap, tapped = false, tapError = null,
                snack = GiveSnack.Confirmation(confirmationTitle(record.kind), facts),
            )
        }
    }

    private fun discardOffer(snack: GiveSnack?, toStep: GiveStep = GiveStep.Review, closeRelay: Boolean = true) {
        val f = flowState.value
        val offer = f.offer
        // Best effort: an offer the relay never hears about still lapses at its expiry.
        if (offer != null && f.relayed && closeRelay) viewModelScope.launch { relay.close(offer.id) }
        viewModelScope.launch {
            if (offer != null) repository.cancelPendingOffer(offer.id)
            flowState.update {
                it.copy(
                    step = toStep, offer = null, proof = null, scanError = null, snack = snack,
                    relayed = false, declined = false, relayError = null, tapped = false, tapError = null,
                    // Back at G2, decide online or QR again: the network may have changed.
                    online = if (toStep == GiveStep.Review) isOnline.value && serverConfigured else it.online,
                    selectedReceiverId = if (toStep == GiveStep.Choose) null else it.selectedReceiverId,
                )
            }
        }
    }

    private fun build(d: GiveData, f: FlowState, now: Long): GiveUiState {
        val item = d.item
        val me = d.me
        if (!f.resumeChecked || me == null) return GiveUiState(loading = true, nowMs = now)
        if (item == null) return GiveUiState(loading = false, empty = NOT_HOLDER, nowMs = now)

        val giver = d.roster.person(me.personId) ?: PersonEntity(me.personId, me.name, me.role, staffNo = "")
        val receivers = receiversFor(giver, d.roster.people, now)
        val custody = deriveCustody(item, d.chain, d.resolutions, now)
        val holds = custody.holderId == me.personId
        val inFlow = f.step == GiveStep.Sent || f.step == GiveStep.Offer || f.step == GiveStep.Scan || f.step == GiveStep.Done
        val empty = when {
            inFlow -> null
            !holds -> NOT_HOLDER
            receivers.isEmpty() -> NO_RECEIVERS
            else -> null
        }

        val offer = f.completed ?: f.offer
        val receiverId = offer?.toPersonId ?: f.selectedReceiverId
        val receiver = d.roster.person(receiverId)
        val kind = offer?.kind ?: receiver?.let { AccessRules.kindFor(me.role, it.role) }
        val summary = if (receiver != null && kind != null) {
            HandoverSummary(
                kind = kind,
                giverName = giver.name,
                giverRole = AccessRules.roleName(giver.role),
                receiverName = receiver.name,
                receiverRole = AccessRules.roleName(receiver.role),
                itemCode = item.code,
                itemLine = "${item.code} · sealed source ${item.sourceSerial} (${item.isotope}) · seal ${item.sealNo}",
                sealNo = item.sealNo,
                dueMs = offer?.dueMs ?: when (kind) {
                    HandoverKind.ISSUE -> f.due?.dueMs(now)
                    HandoverKind.TRANSFER -> custody.head?.dueMs
                    else -> null
                },
            )
        } else null

        val done = f.completed?.let { record ->
            val stored = d.chain.firstOrNull { it.id == record.id }
            DoneView(
                recordNo = recordNumber(record, d.chain),
                matchCode = Canonical.matchCode(record.recordHash),
                acceptedMs = record.acceptedMs ?: now,
                synced = stored != null && !stored.tamperHidden && (stored.syncStatus == SyncMark.SYNCED || stored.serverSeq != null),
                viaServer = f.viaServer,
                viaTap = f.viaTap,
            )
        }

        return GiveUiState(
            loading = false,
            step = f.step,
            empty = empty,
            item = item,
            isIssue = me.role == Roles.STORE_OFFICER,
            receivers = receivers,
            selectedReceiverId = f.selectedReceiverId,
            due = f.due,
            pickerOpen = f.pickerOpen,
            summary = summary,
            leaseExpired = f.leaseExpired,
            signing = f.signing,
            online = f.online,
            declined = f.declined,
            relayError = f.relayError,
            offer = f.offer?.takeIf { f.completed == null }?.let { o ->
                runCatching { OfferView(QrPayloads.offer(o, f.proof), o.expiresMs ?: 0L) }.getOrNull()
            },
            scanError = f.scanError,
            tapped = f.tapped,
            tapError = f.tapError,
            done = done,
            snack = f.snack,
            nowMs = now,
        )
    }

    companion object {
        const val NOT_HOLDER = "You don't hold this camera"
        const val NO_RECEIVERS = "No one on the roster can receive it. Sync once."
        const val NO_LONGER_OPEN = "This offer is no longer open."
        const val LOST_NETWORK = "Lost the network. Show the QR instead."

        /** G1's picker: everyone of a receiving role, the ones who may not take it greyed out with the reason. */
        fun receiversFor(giver: PersonEntity, people: List<PersonEntity>, now: Long): List<ReceiverOption> {
            val groups = when (giver.role) {
                Roles.STORE_OFFICER -> listOf(Roles.RADIOGRAPHER to "Issue to")
                Roles.RADIOGRAPHER -> listOf(Roles.RADIOGRAPHER to "Hand to a colleague", Roles.STORE_OFFICER to "Return to store")
                else -> emptyList()
            }
            return groups.flatMap { (role, group) ->
                people.filter { it.role == role && it.id != giver.id }
                    .sortedWith(compareBy({ !AccessRules.checkPick(giver, it, now).ok }, { it.name }))
                    .map { person ->
                        val check = AccessRules.checkPick(giver, person, now)
                        ReceiverOption(person.id, person.name, person.role, group, check.ok, check.text)
                    }
            }
        }

        fun confirmationTitle(kind: String): String = when (kind) {
            HandoverKind.ISSUE -> "Custody issue successful."
            HandoverKind.RETURN -> "Custody return successful."
            else -> "Custody transfer successful."
        }

        /** The record's place in the chain, the register record being #1. */
        fun recordNumber(record: HandoverEntity, chain: List<HandoverEntity>): Int {
            val byId = chain.associateBy { it.id }
            var n = 1
            var prev = record.prevId
            val seen = mutableSetOf<String>()
            while (prev != null && seen.add(prev)) {
                n++
                prev = byId[prev]?.prevId
            }
            return n
        }

        fun hhmm(ms: Long): String = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(ms))
    }
}
