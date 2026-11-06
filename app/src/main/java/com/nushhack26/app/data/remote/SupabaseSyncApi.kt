package com.nushhack26.app.data.remote

import io.github.jan.supabase.SupabaseClient

/** [SyncApi] over the Supabase RPCs `sync_push` and `sync_pull`. Card 02 fills this in. */
class SupabaseSyncApi(private val client: SupabaseClient?) : SyncApi {
    override suspend fun push(ops: List<PushOp>): List<OpResult> = TODO("card 02")

    override suspend fun pull(since: Long): PullPage = TODO("card 02")
}
