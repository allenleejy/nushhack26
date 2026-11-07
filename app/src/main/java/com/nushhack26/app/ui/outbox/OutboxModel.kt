package com.nushhack26.app.ui.outbox

import com.nushhack26.app.data.local.OutboxEntry
import com.nushhack26.app.data.repo.OutboxData
import com.nushhack26.app.data.repo.ServerTables
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.alerts.shortName
import com.nushhack26.app.ui.alerts.shortNameOf
import com.nushhack26.app.ui.components.formatTime

/** One waiting record: what it carries, how often it's been tried, and the last error in words. */
data class OutboxRowUi(
    val opId: String,
    val title: String,
    val supporting: String,
    val error: String? = null,
    /** Set on handover rows: tapping opens that record's receipt (card 30). */
    val handoverId: String? = null,
)

data class OutboxUiState(
    val loading: Boolean = true,
    val rows: List<OutboxRowUi> = emptyList(),
    val status: SyncStatus = SyncStatus(),
)

fun outboxRows(data: OutboxData): List<OutboxRowUi> = data.entries.map { describe(it, data) }

private fun describe(entry: OutboxEntry, data: OutboxData): OutboxRowUi {
    val roster = data.roster
    fun code(itemId: String?) = data.items.firstOrNull { it.id == itemId }?.code ?: "Unknown item"
    val title = when (entry.tableName) {
        ServerTables.HANDOVERS -> data.handovers.firstOrNull { it.id == entry.recordId }?.let { h ->
            val giver = h.fromPersonId?.let(roster::shortNameOf) ?: "Store"
            val signed = h.acceptedMs?.let { " · signed ${formatTime(it)}" }.orEmpty()
            "Handover · ${code(h.itemId)} · $giver → ${roster.shortNameOf(h.toPersonId)}$signed"
        }
        ServerTables.RESOLUTIONS -> data.resolutions.firstOrNull { it.id == entry.recordId }?.let { r ->
            val chosen = data.handovers.firstOrNull { it.id == r.chosenHandoverId }
            val with = chosen?.let { " · with ${roster.shortNameOf(it.toPersonId)}" }.orEmpty()
            "Decision · ${code(r.itemId)}$with · signed ${formatTime(r.decidedMs)}"
        }
        ServerTables.MISSING_REPORTS -> data.missingReports.firstOrNull { it.id == entry.recordId }?.let { m ->
            "Missing report · ${code(m.itemId)} · ${roster.shortNameOf(m.reportedByPersonId)} · ${formatTime(m.reportedMs)}"
        }
        ServerTables.DEVICES -> roster.device(entry.recordId)?.let { d ->
            "Phone key · ${roster.person(d.personId)?.name?.let(::shortName) ?: "Unknown"} · ${d.keyFp}"
        }
        else -> null
    } ?: "${entry.tableName.replace('_', ' ')} · ${entry.recordId.take(8)}"
    val tries = when (entry.attempts) {
        0 -> "Not sent yet"
        1 -> "Tried once"
        else -> "Tried ${entry.attempts} times"
    }
    return OutboxRowUi(
        opId = entry.opId,
        title = title,
        supporting = "$tries · queued ${formatTime(entry.createdAt)}",
        error = entry.lastError?.let { "Last error: $it" },
        handoverId = entry.recordId.takeIf { entry.tableName == ServerTables.HANDOVERS },
    )
}
