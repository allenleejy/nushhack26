package com.nushhack26.app.ui.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.auth.AccessRules
import com.nushhack26.app.crypto.Canonical
import com.nushhack26.app.crypto.CheckLine
import com.nushhack26.app.crypto.DeviceKeys
import com.nushhack26.app.crypto.HandoverBuilder
import com.nushhack26.app.crypto.OfferChecker
import com.nushhack26.app.crypto.QrPayloads
import com.nushhack26.app.crypto.Roster
import com.nushhack26.app.crypto.ScannedCode
import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.data.repo.IdentityRepository
import com.nushhack26.app.data.repo.ReceiveRepository
import com.nushhack26.app.data.repo.RelayOutcome
import com.nushhack26.app.data.repo.RelayRepository
import com.nushhack26.app.ui.components.SignResult
import com.nushhack26.app.ui.components.SyncChipState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The receiver's flow, R1–R5 in docs/spec.md section 3. Sending and Declined are the relayed offer's (card 15). */
enum class ReceiveStep { Loading, Unavailable, ScanOffer, Checking, Checks, ScanSeal, Accept, Sending, ShowAcceptance, Done, Declined }

/** What the who-and-what card shows. */
data class HandoverSummary(
    val itemCode: String,
    val sourceSerial: String,
    val isotope: String,
    val sealNo: String,
    val giverName: String,
    val giverRole: String,
    val receiverName: String,
    val receiverRole: String,
    val kind: String,
    val dueMs: Long?,
) {
    val giverShortName: String get() = shortName(giverName)
}

data class SealResult(val matches: Boolean, val scanned: String, val expected: String, val text: String)

data class ReceiveUiState(
    val step: ReceiveStep = ReceiveStep.Loading,
    /** The error for this step: "Not a handover code.", fingerprint cancelled, and so on. */
    val message: String? = null,
    val torchOn: Boolean = false,
    /** This phone has no device keys yet, so it can't check any offer. */
    val rosterEmpty: Boolean = false,
    val lines: List<CheckLine> = emptyList(),
    val revealedLines: Int = Int.MAX_VALUE,
    val passed: Boolean = false,
    val looksLikeCopy: Boolean = false,
    val summary: HandoverSummary? = null,
    val seal: SealResult? = null,
    val leaseExpired: Boolean = false,
    val signing: Boolean = false,
    val acceptanceQr: String? = null,
    val matchCode: String? = null,
    val recordSync: SyncChipState = SyncChipState.Waiting,
    val nowMs: Long = 0L,
    /** Opened from the R0 banner: the offer came through the server, not a scanned QR. */
    val relayed: Boolean = false,
    /** Declining: waiting for the server to close the offer. */
    val declining: Boolean = false,
    /** R5-online: the acceptance went back to the giver through the server. */
    val sentOnline: Boolean = false,
    /** R5 after a failed send: "Couldn't reach the server. Show this to Siti instead." */
    val fallbackText: String? = null,
    /** The Declined screen's lines. */
    val declinedText: String? = null,
) {
    val refusedHeader: String
        get() = if (looksLikeCopy) "This is a copy. It can't be used." else "This handover can't be accepted."
}

sealed interface ReceiveAction {
    data object ToggleTorch : ReceiveAction
    data class OfferScanned(val text: String) : ReceiveAction
    data object ScanAnyway : ReceiveAction
    data object ScanAgain : ReceiveAction
    data object ScanSeal : ReceiveAction
    data class SealScanned(val text: String) : ReceiveAction
    data object Next : ReceiveAction
    data object Accept : ReceiveAction
    data object Done : ReceiveAction
    data object Decline : ReceiveAction
    data object Close : ReceiveAction
}

class ReceiveViewModel(
    private val repo: ReceiveRepository,
    private val identityRepo: IdentityRepository,
    private val deviceKeys: () -> DeviceKeys,
    private val clock: () -> Long = System::currentTimeMillis,
    private val relay: RelayRepository? = null,
    private val relayedOfferId: String? = null,
    /** Outlives this screen, so a refused offer still gets closed after the person leaves. */
    private val appScope: CoroutineScope? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(ReceiveUiState())
    val state: StateFlow<ReceiveUiState> = _state.asStateFlow()

    private var identity: Identity? = null
    private var me: PersonEntity? = null
    private var roster = Roster()
    private var local: List<HandoverEntity> = emptyList()
    private var offer: ScannedCode.Offer? = null
    private var item: ItemEntity? = null
    private var giver: PersonEntity? = null
    private var scannedSeal: String? = null
    private var ticker: Job? = null
    private var relayClosed = false

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val id = identityRepo.current()
        if (id == null) {
            _state.value = ReceiveUiState(ReceiveStep.Unavailable, message = "Enrol this phone before receiving a camera.")
            return
        }
        identity = id
        roster = withContext(Dispatchers.IO) { repo.roster() }
        me = roster.person(id.personId)
        if (relayedOfferId != null) {
            loadRelayed(relayedOfferId)
            return
        }
        _state.value = ReceiveUiState(ReceiveStep.ScanOffer, rosterEmpty = roster.devices.isEmpty(), nowMs = clock())
    }

    /** R0 → R2: the offer string from the server goes down the same path as a scanned one. R1 is skipped. */
    private suspend fun loadRelayed(offerId: String) {
        _state.value = ReceiveUiState(ReceiveStep.Checking, relayed = true, nowMs = clock())
        val found = relay?.incomingOffer(offerId)
        val scanned = found?.let { QrPayloads.parse(it.offer) as? ScannedCode.Offer }?.takeIf { it.offer.id == offerId }
        if (scanned == null) {
            if (found != null) closeRelayed()
            val giverId = found?.fromPersonId ?: repo.relayedGiver(offerId)
            val name = giverId?.let { roster.person(it)?.name ?: withContext(Dispatchers.IO) { repo.person(it) }?.name }
            _state.value = ReceiveUiState(
                ReceiveStep.Unavailable,
                relayed = true,
                message = if (found != null) "Not a handover code." else notOpenText(name),
            )
            return
        }
        startChecks(scanned)
    }

    fun onAction(action: ReceiveAction) {
        when (action) {
            ReceiveAction.ToggleTorch -> _state.update { it.copy(torchOn = !it.torchOn) }
            ReceiveAction.ScanAnyway -> _state.update { it.copy(rosterEmpty = false) }
            is ReceiveAction.OfferScanned -> onOfferScanned(action.text)
            ReceiveAction.ScanAgain -> restart()
            ReceiveAction.ScanSeal -> {
                ticker?.cancel()
                _state.update { it.copy(step = ReceiveStep.ScanSeal, message = null, seal = null) }
            }
            is ReceiveAction.SealScanned -> onSealScanned(action.text)
            ReceiveAction.Next -> viewModelScope.launch {
                val lease = identityRepo.leaseNow(clock())
                _state.update { it.copy(step = ReceiveStep.Accept, message = null, leaseExpired = lease.expired, nowMs = clock()) }
            }
            ReceiveAction.Done -> _state.update { it.copy(step = ReceiveStep.Done) }
            ReceiveAction.Decline -> decline()
            ReceiveAction.Accept, ReceiveAction.Close -> Unit
        }
    }

    private fun restart() {
        ticker?.cancel()
        offer = null
        item = null
        giver = null
        scannedSeal = null
        _state.update {
            ReceiveUiState(ReceiveStep.ScanOffer, torchOn = it.torchOn, rosterEmpty = roster.devices.isEmpty(), nowMs = clock())
        }
    }

    private fun onOfferScanned(text: String) {
        if (_state.value.step != ReceiveStep.ScanOffer) return
        val scanned = QrPayloads.parse(text) as? ScannedCode.Offer
        if (scanned == null) {
            _state.update { it.copy(message = "Not a handover code.") }
            return
        }
        _state.update { it.copy(step = ReceiveStep.Checking, message = null) }
        viewModelScope.launch { startChecks(scanned) }
    }

    private suspend fun startChecks(scanned: ScannedCode.Offer) {
        local = withContext(Dispatchers.IO) { repo.localHandovers() }
        roster = withContext(Dispatchers.IO) { repo.roster() }
        me = identity?.let { roster.person(it.personId) }
        offer = scanned
        item = roster.items.firstOrNull { it.id == scanned.offer.itemId }
        giver = roster.person(scanned.offer.fromPersonId)
        val result = check()
        if (result == null) {
            if (relayedOfferId != null) {
                closeRelayed()
                _state.update { it.copy(step = ReceiveStep.Unavailable, message = "Not a handover code.") }
            } else {
                _state.update { it.copy(step = ReceiveStep.ScanOffer, message = "Not a handover code.") }
            }
            return
        }
        // "Checking offline…" for a moment, then the lines one by one.
        delay(300)
        _state.update { it.copy(step = ReceiveStep.Checks, revealedLines = 0) }
        for (n in 1..result.lines.size) {
            delay(110)
            _state.update { it.copy(revealedLines = n) }
        }
        startTicker()
    }

    /** Runs the six checks now and puts them in the state. Null if the code isn't readable. */
    private suspend fun check(): CheckResult? {
        val scanned = offer ?: return null
        val id = identity ?: return null
        val now = clock()
        val result = withContext(Dispatchers.Default) {
            ReceiveChecks.run(scanned, id, me, giver, item, roster, local, now)
        } ?: return null
        _state.update {
            it.copy(
                lines = result.lines, passed = result.passed, looksLikeCopy = result.looksLikeCopy,
                summary = summary(scanned.offer), nowMs = now,
            )
        }
        // A refused relayed offer: nothing stored or sent, but the giver isn't left waiting.
        if (!result.passed) closeRelayed()
        return result
    }

    /** Best effort: tells the server this offer is finished here. It also lapses at its expiry. */
    private fun closeRelayed() {
        val id = relayedOfferId ?: return
        if (relayClosed) return
        relayClosed = true
        repo.markRelayedClosed(id)
        relay?.let { r -> (appScope ?: viewModelScope).launch { r.close(id) } }
    }

    /** R2–R4 Decline: close the offer on the server; the giver's phone says "<name> declined". */
    private fun decline() {
        val id = relayedOfferId ?: return
        val current = _state.value
        if (current.declining || current.signing || current.step !in DECLINABLE) return
        ticker?.cancel()
        _state.update { it.copy(declining = true, message = null) }
        viewModelScope.launch {
            relayClosed = true
            repo.markRelayedClosed(id)
            val outcome = relay?.close(id) ?: RelayOutcome.Unavailable("no relay")
            val who = giver?.name?.let(::shortName) ?: "The giver"
            val code = item?.code ?: "the camera"
            val text = buildString {
                append("Declined. $who still holds $code.")
                if (outcome !is RelayOutcome.Sent) append("\nCouldn't reach the server. The offer lapses on its own.")
            }
            _state.update { it.copy(step = ReceiveStep.Declined, declining = false, declinedText = text) }
        }
    }

    /** Keeps "Valid for 0:41" counting, and refuses the offer the moment it expires. */
    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive && _state.value.step == ReceiveStep.Checks) {
                delay(1_000)
                if (_state.value.step == ReceiveStep.Checks) check()
            }
        }
    }

    private fun onSealScanned(text: String) {
        val current = _state.value
        if (current.step != ReceiveStep.ScanSeal || current.seal?.matches == true) return
        val camera = item ?: return
        val seal = QrPayloads.parse(text) as? ScannedCode.Seal
        if (seal == null) {
            _state.update { it.copy(message = "Not a seal code. Scan the seal on camera ${camera.code}.") }
            return
        }
        val line = OfferChecker.checkSeal(seal, camera)
        scannedSeal = if (line.ok) seal.sealNo else null
        _state.update { it.copy(message = null, seal = SealResult(line.ok, seal.sealNo, camera.sealNo, line.text)) }
    }

    /**
     * Before the fingerprint prompt: the lease and the offer checks again, since time has passed
     * since the scan. False means don't show the prompt.
     */
    suspend fun prepareAccept(): Boolean {
        if (_state.value.step != ReceiveStep.Accept || _state.value.signing) return false
        if (scannedSeal == null) return false
        if (identityRepo.leaseNow(clock()).expired) {
            _state.update { it.copy(leaseExpired = true) }
            return false
        }
        val result = check() ?: return false
        if (!result.passed) {
            _state.update { it.copy(step = ReceiveStep.Checks, revealedLines = Int.MAX_VALUE) }
            return false
        }
        _state.update { it.copy(signing = true, message = null) }
        return true
    }

    /** After the fingerprint: sign the receiver's half, save both records, show the acceptance QR. */
    suspend fun finishAccept(result: SignResult) {
        val scanned = offer
        val id = identity
        val seal = scannedSeal
        if (scanned == null || id == null || seal == null) return
        if (result is SignResult.Cancelled) {
            _state.update { it.copy(signing = false, message = cancelledText()) }
            return
        }
        val record = runCatching {
            withContext(Dispatchers.Default) {
                val accepted = HandoverBuilder.acceptance(scanned.offer, id.deviceId, seal, clock())
                HandoverBuilder.complete(accepted, deviceKeys().sign(Canonical.acceptanceString(accepted)))
                    .copy(syncStatus = SyncMark.WAITING)
            }
        }.getOrElse {
            _state.update { it.copy(signing = false, message = cancelledText()) }
            return
        }
        withContext(Dispatchers.IO) { repo.saveAccepted(scanned.proof, record) }
        val acceptance = QrPayloads.acceptance(record)
        val matchCode = Canonical.matchCode(record.recordHash)
        val relayedId = relayedOfferId
        if (relayedId != null && relay != null) {
            relayClosed = true
            _state.update {
                it.copy(step = ReceiveStep.Sending, signing = false, message = null, acceptanceQr = acceptance, matchCode = matchCode)
            }
            val sent = relay.accept(relayedId, acceptance) is RelayOutcome.Sent
            val who = giver?.name?.let(::shortName) ?: "the giver"
            _state.update {
                if (sent) it.copy(step = ReceiveStep.Done, sentOnline = true)
                else it.copy(step = ReceiveStep.ShowAcceptance, fallbackText = "Couldn't reach the server. Show this to $who instead.")
            }
        } else {
            _state.update {
                it.copy(
                    step = ReceiveStep.ShowAcceptance, signing = false, message = null,
                    acceptanceQr = acceptance, matchCode = matchCode,
                )
            }
        }
        viewModelScope.launch {
            repo.observeHandover(record.itemId, record.id).collect { saved ->
                val chip = when (saved?.syncStatus) {
                    SyncMark.SYNCED -> SyncChipState.Synced
                    SyncMark.REJECTED -> SyncChipState.Conflict
                    else -> SyncChipState.Waiting
                }
                _state.update { it.copy(recordSync = chip) }
            }
        }
    }

    private fun notOpenText(giverName: String?): String =
        if (giverName == null) "This offer is no longer open. Ask the giver to send it again."
        else "This offer is no longer open. Ask ${shortName(giverName)} to send it again."

    private fun cancelledText(): String {
        val who = giver?.name?.let(::shortName) ?: "The giver"
        val code = item?.code ?: "this camera"
        return "Fingerprint cancelled. Nothing was signed. $who still holds $code."
    }

    private fun summary(h: HandoverEntity): HandoverSummary? {
        val camera = item ?: return null
        val receiver = me
        return HandoverSummary(
            itemCode = camera.code, sourceSerial = camera.sourceSerial, isotope = camera.isotope, sealNo = camera.sealNo,
            giverName = giver?.name ?: "Unknown signer", giverRole = giver?.role.orEmpty(),
            receiverName = receiver?.name ?: identity?.name.orEmpty(), receiverRole = receiver?.role ?: identity?.role.orEmpty(),
            kind = h.kind, dueMs = h.dueMs,
        )
    }
}

private val DECLINABLE = setOf(ReceiveStep.Checks, ReceiveStep.ScanSeal, ReceiveStep.Accept)

data class CheckResult(val lines: List<CheckLine>, val passed: Boolean, val looksLikeCopy: Boolean)

/** R2: the five offer lines from [OfferChecker], then the authorisation line (docs/sync.md A.4 and A.8). Pure. */
object ReceiveChecks {
    fun run(
        offer: ScannedCode.Offer,
        identity: Identity,
        me: PersonEntity?,
        giver: PersonEntity?,
        item: ItemEntity?,
        roster: Roster,
        local: List<HandoverEntity>,
        nowMs: Long,
    ): CheckResult? {
        val check = OfferChecker.check(offer, identity, roster, local, nowMs)
        // Line 0 is "readable"; a code that parsed but isn't complete is "Not a handover code".
        if (!check.lines.first().ok) return null
        val access = if (me == null) {
            CheckLine(false, "You aren't on this phone's roster. Sync this phone before accepting.")
        } else {
            AccessRules.checkReceiver(giver, me, nowMs).let { CheckLine(it.ok, it.text) }
        }
        val lines = buildList {
            addAll(check.lines.drop(1))
            add(access)
            if (item == null) add(CheckLine(false, "Unknown camera. Sync this phone before accepting."))
        }
        val passed = lines.all { it.ok }
        return CheckResult(lines, passed, !passed && check.looksLikeCopy && access.ok && item != null)
    }
}

/** The name colleagues use: Siti, Ravi, Wei Ming, Dr Tan. */
fun shortName(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> name
        parts.first() == "Dr" && parts.size >= 2 -> "Dr ${parts[1]}"
        // Family name first (Tan Wei Ming): the given names are what colleagues use.
        parts.size >= 3 -> parts.drop(1).joinToString(" ")
        else -> parts.first()
    }
}
