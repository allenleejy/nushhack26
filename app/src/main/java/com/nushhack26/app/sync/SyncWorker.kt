package com.nushhack26.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nushhack26.app.App
import com.nushhack26.app.data.local.SyncKeys
import com.nushhack26.app.data.local.put
import com.nushhack26.app.data.remote.NotReadyException
import kotlinx.coroutines.CancellationException

/**
 * Runs one sync, retrying failures with the scheduler's backoff.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as App).container
        val syncState = container.database.syncStateDao()
        return try {
            container.syncEngine.run()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: NotReadyException) {
            Result.retry()
        } catch (e: Exception) {
            syncState.put(SyncKeys.LAST_ERROR, e.message ?: e::class.simpleName.orEmpty())
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 8
    }
}
