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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Card 23: the role/validity line under the name — valid, lapsed and store officer. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class MeAccessScreenshotTest {
    private val base = MeUiState(
        serverConfigured = true,
        userId = "3f2a9c1e-0000-4000-8000-000000000000",
        identity = Identity("p-ravi", "d-1", "Ravi Kumar", Roles.RADIOGRAPHER),
        person = PersonEntity("p-ravi", "Ravi Kumar", Roles.RADIOGRAPHER, "R-2210", "RW-DEMO-1452", authorisedUntilMs = END_OF_JUNE_2027),
        lease = validLease,
        key = KeyInfo("pk", "3F:A9:12:7C", "tee"),
        lockApplies = true,
        sync = SyncStatus(isOnline = true, pendingCount = 0, lastSyncAt = CONFIRMED_AT),
        now = NOW,
        version = "r07",
        debugBuild = false,
    )

    @Test
    fun valid() = captureScreen("MeAccess_valid") { MeContent(base) }

    @Test
    fun lapsed() = captureScreen("MeAccess_lapsed") {
        MeContent(
            base.copy(
                identity = Identity("p-jun", "d-2", "Jun Lim", Roles.RADIOGRAPHER),
                person = PersonEntity("p-jun", "Jun Lim", Roles.RADIOGRAPHER, "R-2251", authorisedUntilMs = localMs(2026, 11, 3, 23, 59)),
            ),
        )
    }

    @Test
    fun storeOfficer() = captureScreen("MeAccess_store_officer") {
        MeContent(
            base.copy(
                identity = Identity("p-siti", "d-3", "Siti Aminah", Roles.STORE_OFFICER),
                person = PersonEntity("p-siti", "Siti Aminah", Roles.STORE_OFFICER, "S-1021"),
            ),
        )
    }
}
