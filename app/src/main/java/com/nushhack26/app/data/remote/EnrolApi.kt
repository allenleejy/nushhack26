package com.nushhack26.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

// Enrolment (docs/sync.md C.2 and D.4): the one call that isn't the outbox, because it
// needs the network anyway and the server must check the code before the phone gets access.

object EnrolResultKind {
    const val APPLIED = "applied"
    const val DUPLICATE = "duplicate"
    const val REJECTED = "rejected"
    const val LOCKED = "locked"
}

@Serializable
data class EnrolDevice(
    val id: String,
    @SerialName("public_key") val publicKey: String,
    @SerialName("key_fp") val keyFp: String,
    @SerialName("key_security") val keySecurity: String,
    @SerialName("app_version") val appVersion: String,
    @SerialName("performed_at") val performedAt: String,
)

@Serializable
data class EnrolResponse(
    val result: String,
    val reason: String? = null,
    val person: PersonRow? = null,
    val device: DeviceRow? = null,
)

interface EnrolApi {
    /** `enrol_device(p_staff_no, p_code, p_device)`. Throws [NotReadyException] or a network error when it can't reach the server. */
    suspend fun enrol(staffNo: String, code: String, device: EnrolDevice): EnrolResponse
}

class SupabaseEnrolApi(private val client: SupabaseClient?) : EnrolApi {
    override suspend fun enrol(staffNo: String, code: String, device: EnrolDevice): EnrolResponse {
        val supabase = client ?: throw NotReadyException("Server not configured")
        if (supabase.auth.currentSessionOrNull() == null) throw NotReadyException("Not signed in yet")
        val parameters = buildJsonObject {
            put("p_staff_no", staffNo)
            put("p_code", code)
            put("p_device", ServerJson.encodeToJsonElement(device))
        }
        val result = supabase.postgrest.rpc("enrol_device", parameters)
        return ServerJson.decodeFromString(EnrolResponse.serializer(), result.data)
    }
}
