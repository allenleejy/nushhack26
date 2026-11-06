package com.nushhack26.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Device revocation (docs/sync.md A.12 and D.6). Online only, never queued.

@Serializable
data class RevokeResult(
    /** `revoked`, `duplicate` or `rejected`. */
    val result: String,
    val reason: String? = null,
    /** The updated devices row, to save in Room straight away. */
    val device: DeviceRow? = null,
)

@Serializable
data class DeviceStatus(
    val revoked: Boolean = false,
    @SerialName("revoked_at") val revokedAt: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
)

interface DeviceAdminApi {
    /** `revoke_device(p_device_id)`: the safety officer withdraws a phone. */
    suspend fun revoke(deviceId: String): RevokeResult

    /** `device_status()`: has the device this session enrolled been withdrawn? Works when no longer enrolled. */
    suspend fun status(): DeviceStatus
}

class SupabaseDeviceAdminApi(private val client: SupabaseClient?) : DeviceAdminApi {
    private fun readyClient(): SupabaseClient {
        val ready = client ?: throw NotReadyException("Server not configured")
        if (ready.auth.currentSessionOrNull() == null) throw NotReadyException("Not signed in yet")
        return ready
    }

    override suspend fun revoke(deviceId: String): RevokeResult = ServerJson.decodeFromString(
        RevokeResult.serializer(),
        readyClient().postgrest.rpc("revoke_device", buildJsonObject { put("p_device_id", deviceId) }).data,
    )

    override suspend fun status(): DeviceStatus = ServerJson.decodeFromString(
        DeviceStatus.serializer(),
        readyClient().postgrest.rpc("device_status").data,
    )
}
