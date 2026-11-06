package com.nushhack26.app.ui.fork

import com.nushhack26.app.auth.AccessLease
import com.nushhack26.app.auth.AccessRules
import com.nushhack26.app.data.local.AlertEntity
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.repo.SafetyData
import com.nushhack26.app.data.repo.checkSignatures
import com.nushhack26.app.data.repo.deliveredBy
import com.nushhack26.app.data.repo.handoverNumber
import com.nushhack26.app.ui.alerts.SIGNATURES_VALID_TEXT
import com.nushhack26.app.ui.alerts.SIGNATURE_INVALID_TEXT
import com.nushhack26.app.ui.alerts.nameOf
import com.nushhack26.app.ui.alerts.resolutionFor
import com.nushhack26.app.ui.alerts.shortName
import com.nushhack26.app.ui.alerts.shortNameOf
import com.nushhack26.app.ui.components.formatTime

const val DECISION_WAITING_TEXT = "Decision signed · not synced yet"

/** One signature on a branch: who signed, their key's fingerprint, and whether it checked out on this phone. */
data class SignatureLineUi(val who: String, val keyFp: String?, val ok: Boolean)

data class BranchUi(
    val handoverId: String,
    /** H2, H2b. */
    val label: String,
    /** Ravi → Wei Ming. */
    val title: String,
    val receiverName: String,
    val signedText: String,
    val deliveredText: String,
    val sealText: String?,
    val sealOk: Boolean,
    val giver: SignatureLineUi,
    val receiver: SignatureLineUi,
    /** CHOSEN or OVERRULED once decided; nothing before, so neither branch looks favoured. */
    val statusText: String?,
)

data class ForkUiState(
    val loading: Boolean = true,
    /** The alert or its records haven't reached this phone. */
    val missing: Boolean = false,
    val alertId: String = "",
    val itemId: String = "",
    val itemCode: String = "",
    val headline: String = "",
    val evidence: String = "",
    val branches: List<BranchUi> = emptyList(),
    /** AccessRules.maySignDecision: only an active safety officer sees Sign decision. */
    val canDecide: Boolean = false,
    val leaseExpired: Boolean = false,
    /** "Resolved by Dr Tan · 14:34" or [DECISION_WAITING_TEXT] once decided. */
    val decision: String? = null,
    val selectedId: String? = null,
    val note: String = "",
    val confirming: Boolean = false,
    val signing: Boolean = false,
    val error: String? = null,
) {
    val selected: BranchUi? get() = branches.firstOrNull { it.handoverId == selectedId }
}

/** What the person has done on the screen so far. */
data class ForkInput(
    val selectedId: String? = null,
    val note: String = "",
    val confirming: Boolean = false,
    val signing: Boolean = false,
    val error: String? = null,
)

fun buildForkState(
    alertId: String,
    alert: AlertEntity?,
    data: SafetyData,
    me: PersonEntity?,
    lease: AccessLease?,
    input: ForkInput,
): ForkUiState {
    val roster = data.roster
    val records = alert?.handoverIds.orEmpty().mapNotNull { id -> data.handovers.firstOrNull { it.id == id } }
    if (alert == null || records.isEmpty()) return ForkUiState(loading = false, missing = true, alertId = alertId)
    val item = data.items.firstOrNull { it.id == alert.itemId }
    val itemCode = item?.code ?: "Unknown item"
    val resolution = resolutionFor(alert, data.resolutions)
    val number = records.firstNotNullOfOrNull { handoverNumber(it.id, data.handovers) }
    val after = handoverNumber(alert.forkPrevId, data.handovers)?.let { "H$it" } ?: "registration"
    val ordered = records.sortedWith(compareBy({ it.receivedAt ?: Long.MAX_VALUE }, { it.id }))
    var allOk = true
    val branches = ordered.mapIndexed { index, h ->
        val check = checkSignatures(h, roster)
        allOk = allOk && check.allOk
        val giverName = h.fromPersonId?.let(roster::nameOf) ?: "The store"
        val receiverName = roster.nameOf(h.toPersonId)
        val phone = deliveredBy(h.createdBy, roster)?.let { "Delivered by ${shortName(it.name)}'s phone" } ?: "Delivered by a phone not on the roster"
        val sealOk = item != null && h.sealNo == item.sealNo
        BranchUi(
            handoverId = h.id,
            label = "H${number ?: "?"}" + if (index == 0) "" else ('a' + index).toString(),
            title = "${h.fromPersonId?.let(roster::shortNameOf) ?: "Store"} → ${roster.shortNameOf(h.toPersonId)}",
            receiverName = receiverName,
            signedText = h.acceptedMs?.let { "Signed ${formatTime(it)}" } ?: "Not signed",
            deliveredText = phone + h.receivedAt?.let { " at ${formatTime(it)}" }.orEmpty(),
            sealText = h.sealNo?.let { if (sealOk) "Seal $it" else "Seal $it · expected ${item?.sealNo ?: "unknown"}" },
            sealOk = sealOk,
            giver = SignatureLineUi(giverName, check.giverKeyFp, check.giverOk),
            receiver = SignatureLineUi(receiverName, check.receiverKeyFp, check.receiverOk),
            statusText = when {
                resolution != null && resolution.chosenHandoverId == h.id -> "Chosen"
                resolution != null -> "Overruled"
                else -> null
            },
        )
    }
    val giver = roster.nameOf(alert.personId ?: records.first().fromPersonId)
    val decision = when {
        resolution != null && resolution.syncStatus == SyncMark.WAITING -> DECISION_WAITING_TEXT
        resolution != null -> "Resolved by ${roster.shortNameOf(resolution.decidedByPersonId)} · ${formatTime(resolution.decidedMs)}"
        alert.closedAt != null -> "Resolved · ${formatTime(alert.closedAt)}"
        else -> null
    }
    return ForkUiState(
        loading = false,
        alertId = alert.id,
        itemId = alert.itemId.orEmpty(),
        itemCode = itemCode,
        headline = "$itemCode · fork after $after",
        evidence = "$giver signed two handovers of $itemCode that both follow $after · " +
            if (allOk) SIGNATURES_VALID_TEXT else SIGNATURE_INVALID_TEXT,
        branches = branches,
        canDecide = me != null && AccessRules.maySignDecision(me),
        leaseExpired = lease?.expired == true,
        decision = decision,
        selectedId = input.selectedId?.takeIf { id -> branches.any { it.handoverId == id } },
        note = input.note,
        confirming = input.confirming,
        signing = input.signing,
        error = input.error,
    )
}
