package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.ui.lock.LockContent
import com.nushhack26.app.ui.lock.LockUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class LockScreenshotTest {
    private val weiMing = LockUiState(name = "Tan Wei Ming", staffNo = "R-2234", lease = validLease)

    @Test
    fun normal() = captureScreen("Lock_normal") { LockContent(weiMing) }

    @Test
    fun error() = captureScreen("Lock_error") { LockContent(weiMing.copy(error = "Fingerprint cancelled. Tenet stays locked.")) }

    @Test
    fun keyMismatch() = captureScreen("Lock_key_mismatch") {
        LockContent(weiMing.copy(error = "This phone's key doesn't match its enrolment. Clear the app's storage and enrol again."))
    }

    @Test
    fun leaseExpired() = captureScreen("Lock_lease_expired") { LockContent(weiMing.copy(lease = expiredLease)) }

    @Test
    @Config(qualifiers = NARROW)
    fun narrowLargeFont() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Lock_narrow_1_3x") {
            LockContent(weiMing.copy(lease = expiredLease, error = "Fingerprint cancelled. Tenet stays locked."))
        }
    }
}
