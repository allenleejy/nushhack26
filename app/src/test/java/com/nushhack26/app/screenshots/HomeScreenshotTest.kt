package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.home.HomeContent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class HomeScreenshotTest {
    @Test
    fun online() = captureScreen("Home_online", SyncStatus(lastSyncAt = 0L)) { HomeContent() }

    @Test
    fun offlineWithChangesWaiting() =
        captureScreen("Home_offline", SyncStatus(isOnline = false, pendingCount = 3)) { HomeContent() }
}
