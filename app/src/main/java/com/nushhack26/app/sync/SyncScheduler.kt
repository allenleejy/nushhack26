package com.nushhack26.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Runs [SyncWorker] every 15 minutes, and once more whenever something asks for a sync. */
class SyncScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    private val needsNetwork = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(needsNetwork)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /**
     * Sync as soon as there's a network: after each local change, when the network
     * returns, when Tenet comes on screen, and from "Sync now". A new request replaces the
     * one before it, so it never waits behind a failed sync's growing retry delay. Replacing
     * a sync that's running is safe: the server skips ops it has already applied.
     */
    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(needsNetwork)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(ONE_TIME_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val PERIODIC_WORK = "sync-periodic"
        const val ONE_TIME_WORK = "sync-now"
        private const val BACKOFF_SECONDS = 10L
    }
}
