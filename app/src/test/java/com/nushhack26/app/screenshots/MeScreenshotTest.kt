package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.ui.me.MeContent
import com.nushhack26.app.ui.me.MeUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class MeScreenshotTest {
    @Test
    fun signedIn() = captureScreen("Me_signed_in") {
        MeContent(MeUiState(serverConfigured = true, userId = "3f2a9c1e-0000-4000-8000-000000000000", version = "r01 (release)"))
    }

    @Test
    fun notConfigured() = captureScreen("Me_not_configured") {
        MeContent(MeUiState(serverConfigured = false, userId = null, version = "r01 (debug)"))
    }
}
