package com.nushhack26.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// The two server calls (docs/sync.md section D). The sync engine talks to the server only
// through this interface, so the tests can swap in a fake server with the same rules.

object OpResultKind {
    const val APPLIED = "applied"
    const val DUPLICATE = "duplicate"
    const val CONFLICT = "conflict"
    const val REJECTED = "rejected"
}

@Serializable
data class PushOp(
    @SerialName("op_id") val opId: String,
    val table: String,
    val kind: String = "insert",
    val payload: JsonObject,
)

@Serializable
data class OpResult(
    @SerialName("op_id") val opId: String,
    val result: String,
    val reason: String? = null,
    val status: String? = null,
    @SerialName("server_seq") val serverSeq: Long? = null,
)

@Serializable
data class PullRows(
    val people: List<PersonRow> = emptyList(),
    val devices: List<DeviceRow> = emptyList(),
    val items: List<ItemRow> = emptyList(),
    val handovers: List<HandoverRow> = emptyList(),
    val alerts: List<AlertRow> = emptyList(),
    val resolutions: List<ResolutionRow> = emptyList(),
    @SerialName("missing_reports") val missingReports: List<MissingReportRow> = emptyList(),
)

@Serializable
data class PullPage(
    @SerialName("max_seq") val maxSeq: Long,
    val more: Boolean = false,
    val rows: PullRows = PullRows(),
)

interface SyncApi {
    /** `sync_push(ops)`: one result per op, in order. */
    suspend fun push(ops: List<PushOp>): List<OpResult>

    /** `sync_pull(since)`: every row with server_seq after [since], up to 2,000. */
    suspend fun pull(since: Long): PullPage
}

/** No server config or no session yet. The worker retries later; nothing is lost. */
class NotReadyException(message: String) : Exception(message)
