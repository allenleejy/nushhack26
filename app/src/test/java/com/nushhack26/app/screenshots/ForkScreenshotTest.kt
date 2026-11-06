package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.auth.AccessLease
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.fork.DecisionSheetContent
import com.nushhack26.app.ui.fork.ForkContent
import com.nushhack26.app.ui.fork.ForkInput
import com.nushhack26.app.ui.fork.buildForkState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class ForkScreenshotTest {
    private val s = SampleSafety
    private val online = SyncStatus(isOnline = true, lastSyncAt = s.at(14, 35))
    private val lease = AccessLease(confirmedAt = s.at(14, 35), validUntil = s.at(14, 35) + 72 * 3_600_000L, expired = false)

    private fun state(
        input: ForkInput = ForkInput(),
        me: com.nushhack26.app.data.local.PersonEntity = s.drTan,
        lease: AccessLease = this.lease,
        synced: Boolean? = null,
    ) = buildForkState(s.fork.id, s.fork, s.data(listOf(s.fork), listOfNotNull(synced?.let(s::resolution))), me, lease, input)

    @Test
    fun safetyOfficer() = captureScreen("Fork_rso", online) { ForkContent(state(ForkInput(selectedId = "h2", note = "Wei Ming has it on site"))) }

    @Test
    fun noChoiceYet() = captureScreen("Fork_no_choice", online) { ForkContent(state()) }

    @Test
    fun leaseExpired() = captureScreen("Fork_lease_expired", SyncStatus(isOnline = false, lastSyncAt = s.at(9, 0))) {
        ForkContent(state(ForkInput(selectedId = "h2"), lease = lease.copy(expired = true)))
    }

    @Test
    fun radiographer() = captureScreen("Fork_radiographer", online) { ForkContent(state(me = s.ravi)) }

    @Test
    fun confirmSheet() = captureScreen("Fork_confirm_sheet", online) {
        DecisionSheetContent(state(ForkInput(selectedId = "h2", note = "Wei Ming has it on site", confirming = true)))
    }

    @Test
    fun decidedOffline() = captureScreen("Fork_decided_offline", SyncStatus(isOnline = false, pendingCount = 1, lastSyncAt = s.at(14, 2))) {
        ForkContent(state(synced = false))
    }

    @Test
    fun resolved() = captureScreen("Fork_resolved", online) { ForkContent(state(synced = true)) }
}
