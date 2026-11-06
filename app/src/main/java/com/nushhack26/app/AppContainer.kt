package com.nushhack26.app

import android.content.Context
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.remote.SessionManager
import com.nushhack26.app.data.remote.SupabaseConfig
import com.nushhack26.app.data.remote.createSupabase
import com.nushhack26.app.sync.NetworkMonitor
import com.nushhack26.app.sync.SyncScheduler
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.sync.syncStatusFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The one place that creates the database, the Supabase client, the sync scheduler and the repositories. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.create(appContext) }

    val networkMonitor = NetworkMonitor(appContext)
    val isOnline: StateFlow<Boolean> =
        networkMonitor.isOnline.stateIn(scope, SharingStarted.Eagerly, networkMonitor.currentlyOnline())

    val serverConfigured: Boolean = SupabaseConfig.isConfigured
    val supabase = createSupabase()
    val session = SessionManager(supabase, isOnline, scope)

    val syncScheduler = SyncScheduler(appContext)

    val syncStatus: StateFlow<SyncStatus> by lazy {
        syncStatusFlow(isOnline, database.outboxDao(), database.syncStateDao())
            .stateIn(scope, SharingStarted.Eagerly, SyncStatus(isOnline = isOnline.value))
    }

    // Repositories go here, one per feature (plan job).

    fun start() {
        session.start()
        syncScheduler.schedulePeriodic()
        syncScheduler.syncNow()
        // Sync again each time the network comes back.
        scope.launch {
            isOnline.drop(1).filter { it }.collect { syncScheduler.syncNow() }
        }
    }

    /** Wipes the phone's copy of the data. The sign-in is kept, so the next sync pulls everything again. */
    suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }
}
