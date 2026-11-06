package com.nushhack26.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nushhack26.app.App
import com.nushhack26.app.data.local.SyncKeys
import com.nushhack26.app.data.local.put

/**
 * Runs one sync. For now it only records when it last ran; the sync engine card
 * fills in the push of the outbox and the pull of server changes.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val syncState = (applicationContext as App).container.database.syncStateDao()
        return try {
            syncState.put(SyncKeys.LAST_SYNC_AT, System.currentTimeMillis().toString())
            syncState.remove(SyncKeys.LAST_ERROR)
            Result.success()
        } catch (e: Exception) {
            syncState.put(SyncKeys.LAST_ERROR, e.message ?: e::class.simpleName.orEmpty())
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 8
    }
}
