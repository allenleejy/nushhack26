package com.nushhack26.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// The online handover relay (docs/sync.md A.10 and D.5). The server carries the strings the
// QR codes carry offline. Online only, never queued: if a call fails, the phone shows the QR.

object RelayStatusKind {
    const val OPEN = "open"
    const val ACCEPTED = "accepted"
    const val DECLINED = "declined"
    const val CANCELLED = "cancelled"
    const val UNKNOWN = "unknown"
}

/** An offer made out to this phone's person, waiting on the server. */
@Serializable
data class RelayedOffer(
    val id: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("from_person_id") val fromPersonId: String,
    /** The same `CS1O:` string the offer QR shows. */
    val offer: String,
    @SerialName("expires_ms") val expiresMs: Long,
)

@Serializable
data class RelayStatus(
    val status: String,
    /** The same `CS1A:` string the acceptance QR shows, once the receiver accepted. */
    val acceptance: String? = null,
)

@Serializable
data class RelayResult(
    val result: String,
    val reason: String? = null,
    val status: String? = null,
)

interface OfferRelayApi {
    /** `relay_offer`: result `posted`, `duplicate` or `rejected`. */
    suspend fun post(id: String, itemId: String, toPersonId: String, offer: String, expiresMs: Long): RelayResult

    /** `relay_inbox`: open offers made out to me. */
    suspend fun inbox(): List<RelayedOffer>

    /** `relay_accept`: result `accepted`, `duplicate` or `rejected`. */
    suspend fun accept(id: String, acceptance: String): RelayResult

    /** `relay_close`: the receiver declines, or the giver cancels. */
    suspend fun close(id: String): RelayResult

    /** `relay_status`: where the offer stands, with the acceptance once there is one. */
    suspend fun status(id: String): RelayStatus
}

class SupabaseOfferRelayApi(private val client: SupabaseClient?) : OfferRelayApi {
    private fun readyClient(): SupabaseClient {
        val ready = client ?: throw NotReadyException("Server not configured")
        if (ready.auth.currentSessionOrNull() == null) throw NotReadyException("Not signed in yet")
        return ready
    }

    private suspend fun call(function: String, parameters: JsonObject = buildJsonObject {}): String =
        readyClient().postgrest.rpc(function, parameters).data

    override suspend fun post(id: String, itemId: String, toPersonId: String, offer: String, expiresMs: Long): RelayResult =
        ServerJson.decodeFromString(RelayResult.serializer(), call("relay_offer", buildJsonObject {
            put("p_id", id)
            put("p_item_id", itemId)
            put("p_to_person_id", toPersonId)
            put("p_offer", offer)
            put("p_expires_ms", expiresMs)
        }))

    override suspend fun inbox(): List<RelayedOffer> =
        ServerJson.decodeFromString(ListSerializer(RelayedOffer.serializer()), call("relay_inbox"))

    override suspend fun accept(id: String, acceptance: String): RelayResult =
        ServerJson.decodeFromString(RelayResult.serializer(), call("relay_accept", buildJsonObject {
            put("p_id", id)
            put("p_acceptance", acceptance)
        }))

    override suspend fun close(id: String): RelayResult =
        ServerJson.decodeFromString(RelayResult.serializer(), call("relay_close", buildJsonObject { put("p_id", id) }))

    override suspend fun status(id: String): RelayStatus =
        ServerJson.decodeFromString(RelayStatus.serializer(), call("relay_status", buildJsonObject { put("p_id", id) }))
}
