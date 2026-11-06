package com.nushhack26.app

import android.content.Context
import com.nushhack26.app.auth.AppLock
import com.nushhack26.app.auth.Unlocker
import com.nushhack26.app.crypto.DeviceKeys
import com.nushhack26.app.crypto.deviceKeys
import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.local.put
import com.nushhack26.app.data.remote.SessionManager
import com.nushhack26.app.data.remote.SupabaseEnrolApi
import com.nushhack26.app.data.remote.SupabaseConfig
import com.nushhack26.app.data.remote.SupabaseOfferRelayApi
import com.nushhack26.app.data.remote.SupabaseSyncApi
import com.nushhack26.app.data.remote.createSupabase
import com.nushhack26.app.data.repo.CustodyRepository
import com.nushhack26.app.data.repo.EnrolRepository
import com.nushhack26.app.data.repo.GiveRepository
import com.nushhack26.app.data.repo.IdentityRepository
import com.nushhack26.app.data.repo.ReceiveRepository
import com.nushhack26.app.data.repo.RelayRepository
import com.nushhack26.app.data.repo.SafetyRepository
import com.nushhack26.app.nearby.TapLink
import com.nushhack26.app.sync.NetworkMonitor
import com.nushhack26.app.sync.SyncEngine
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
    val syncEngine: SyncEngine by lazy { SyncEngine(database, SupabaseSyncApi(supabase)) }

    val syncStatus: StateFlow<SyncStatus> by lazy {
        syncStatusFlow(isOnline, database.outboxDao(), database.syncStateDao())
            .stateIn(scope, SharingStarted.Eagerly, SyncStatus(isOnline = isOnline.value))
    }

    /** This phone's signing key (card 01). */
    val deviceKeys: DeviceKeys by lazy { deviceKeys(appContext) }

    val identity: IdentityRepository by lazy { IdentityRepository(database.syncStateDao()) }

    /** The app lock and the offline unlock (docs/sync.md A.7). */
    val appLock = AppLock(appContext)
    val unlocker: Unlocker by lazy { Unlocker({ deviceKeys }, identity, appLock) }

    // Repositories, one per feature. Each save also asks for a sync.
    private val requestSync: () -> Unit = { syncScheduler.syncNow() }
    val giveRepository by lazy { GiveRepository(database, identity, requestSync) }
    val receiveRepository by lazy { ReceiveRepository(database, requestSync) }
    val custodyRepository by lazy { CustodyRepository(database) }
    val enrolRepository by lazy { EnrolRepository(database, identity, SupabaseEnrolApi(supabase), appLock, requestSync) }
    val safetyRepository by lazy { SafetyRepository(database, requestSync) }

    /** The online handover: the server carries the offer and acceptance strings (docs/sync.md A.10). */
    val offerRelay by lazy { RelayRepository(SupabaseOfferRelayApi(supabase), isOnline, identity, scope) }

    /** Tap to hand over offline: NFC pairs the phones, Nearby Connections links them (docs/sync.md A.11). */
    val tapLink by lazy { TapLink(appContext, scope) }

    fun start() {
        appLock.start()
        session.start()
        syncScheduler.schedulePeriodic()
        syncScheduler.syncNow()
        // Sync again each time the network comes back.
        scope.launch {
            isOnline.drop(1).filter { it }.collect { syncScheduler.syncNow() }
        }
    }

    /**
     * Wipes the phone's copy of the data. The sign-in, the signing key and the enrolment are
     * kept, so the phone stays the same person, can still unlock offline, and the next sync
     * pulls everything again.
     */
    suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        val syncState = database.syncStateDao()
        val kept = IdentityRepository.KEPT_KEYS.mapNotNull { key -> syncState.get(key)?.let { key to it } }
        database.clearAllTables()
        kept.forEach { (key, value) -> syncState.put(key, value) }
        syncScheduler.syncNow()
    }
}
