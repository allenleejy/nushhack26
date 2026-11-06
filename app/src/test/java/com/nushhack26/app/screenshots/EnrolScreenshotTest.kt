package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.enrol.EnrolContent
import com.nushhack26.app.ui.enrol.EnrolUiState
import com.nushhack26.app.ui.enrol.EnrolViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class EnrolScreenshotTest {
    @Test
    fun empty() = captureScreen("Enrol_empty") { EnrolContent(EnrolUiState()) }

    @Test
    fun wrongCode() = captureScreen("Enrol_wrong_code") {
        EnrolContent(EnrolUiState(staffNo = "R-2210", code = "000000", wrongCode = true, error = EnrolViewModel.wrongCodeText("R-2210")))
    }

    @Test
    fun offline() = captureScreen("Enrol_offline", status = SyncStatus(isOnline = false)) {
        EnrolContent(EnrolUiState(staffNo = "R-2210", code = "552917", error = EnrolViewModel.NO_CONNECTION))
    }

    @Test
    fun checking() = captureScreen("Enrol_checking") { EnrolContent(EnrolUiState(staffNo = "R-2210", code = "552917", busy = true)) }

    @Test
    fun enrolled() = captureScreen("Enrol_enrolled") {
        EnrolContent(
            EnrolUiState(staffNo = "R-2210", code = "552917", enrolled = EnrolViewModel.enrolledLine("Ravi Kumar", "radiographer", "3F:A9:12:7C")),
        )
    }

    @Test
    @Config(qualifiers = NARROW)
    fun narrowLargeFont() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Enrol_narrow_1_3x") {
            EnrolContent(EnrolUiState(staffNo = "R-2210", code = "552917", error = EnrolViewModel.LOCKED))
        }
    }
}
