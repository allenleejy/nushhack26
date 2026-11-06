package com.nushhack26.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** [SyncApi] over the Supabase RPCs `sync_push` and `sync_pull`. */
class SupabaseSyncApi(private val client: SupabaseClient?) : SyncApi {
    private fun readyClient(): SupabaseClient {
        val ready = client ?: throw NotReadyException("No Supabase client configured")
        if (ready.auth.currentSessionOrNull() == null) throw NotReadyException("No Supabase session yet")
        return ready
    }

    override suspend fun push(ops: List<PushOp>): List<OpResult> =
        readyClient().postgrest.rpc("sync_push", buildJsonObject {
            put("ops", ServerJson.encodeToJsonElement(ListSerializer(PushOp.serializer()), ops))
        }).decodeAs()

    override suspend fun pull(since: Long): PullPage =
        readyClient().postgrest.rpc("sync_pull", buildJsonObject { put("since", since) }).decodeAs()
}
