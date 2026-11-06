package com.nushhack26.app.sync

import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.remote.SyncApi

/**
 * One sync: push the outbox, then pull everything new (docs/sync.md sections D and E).
 * Card 02 fills this in. Throws on a network failure so the worker retries with backoff.
 */
class SyncEngine(
    private val database: AppDatabase,
    private val api: SyncApi,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun run() {
        TODO("card 02")
    }
}
