package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.crypto.KeyInfo
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.Roles
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.me.MeContent
import com.nushhack26.app.ui.me.MeUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class MeScreenshotTest {
    private val ravi = MeUiState(
        serverConfigured = true,
        userId = "3f2a9c1e-0000-4000-8000-000000000000",
        identity = Identity("p-ravi", "d-1", "Ravi Kumar", Roles.RADIOGRAPHER),
        person = PersonEntity("p-ravi", "Ravi Kumar", Roles.RADIOGRAPHER, "R-2210", "RW-DEMO-1452", authorisedUntilMs = END_OF_JUNE_2027),
        lease = validLease,
        key = KeyInfo("pk", "3F:A9:12:7C", "tee"),
        lockApplies = true,
        sync = SyncStatus(isOnline = true, pendingCount = 2, lastSyncAt = CONFIRMED_AT),
        now = NOW,
        version = "r07",
        debugBuild = false,
    )

    private val drTan = ravi.copy(
        identity = Identity("p-tan", "d-2", "Dr Tan Mei Ling", Roles.RSO),
        person = PersonEntity("p-tan", "Dr Tan Mei Ling", Roles.RSO, "O-3001"),
        sync = SyncStatus(isOnline = true, pendingCount = 0, lastSyncAt = CONFIRMED_AT),
    )

    @Test
    fun radiographer() = captureScreen("Me_radiographer") { MeContent(ravi) }

    @Test
    fun safetyOfficer() = captureScreen("Me_safety_officer") { MeContent(drTan) }

    @Test
    fun leaseExpired() = captureScreen("Me_lease_expired", status = SyncStatus(isOnline = false, pendingCount = 2, lastSyncAt = CONFIRMED_AT)) {
        MeContent(ravi.copy(lease = expiredLease, sync = SyncStatus(isOnline = false, pendingCount = 2, lastSyncAt = CONFIRMED_AT)))
    }

    @Test
    fun testKeyNoLock() = captureScreen("Me_test_key") {
        MeContent(
            ravi.copy(
                key = KeyInfo("pk", "3F:A9:12:7C", "software"),
                lockApplies = false,
                userId = null,
                sync = SyncStatus(lastError = "Sign-in failed: network unreachable"),
                debugBuild = true,
            ),
        )
    }

    @Test
    fun notConfigured() = captureScreen("Me_not_configured") {
        MeContent(ravi.copy(serverConfigured = false, userId = null, person = null, debugBuild = true))
    }

    @Test
    fun loading() = captureScreen("Me_loading") { MeContent(MeUiState(loading = true)) }

    @Test
    @Config(qualifiers = NARROW)
    fun narrowLargeFont() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Me_narrow_1_3x") { MeContent(ravi.copy(lease = expiredLease)) }
    }
}
