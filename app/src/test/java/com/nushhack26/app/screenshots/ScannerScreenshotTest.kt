package com.nushhack26.app.screenshots

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.components.CameraPermissionWindow
import com.nushhack26.app.ui.components.QrScanner
import com.nushhack26.app.ui.components.ScanStatus
import com.nushhack26.app.ui.components.ScannerInstrument
import com.nushhack26.app.ui.components.SyncStatusBar
import com.nushhack26.app.ui.theme.TenetTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class ScannerScreenshotTest {

    private fun capture(name: String, content: @Composable () -> Unit) {
        for ((theme, dark) in listOf("field" to true, "daylight" to false)) {
            captureRoboImage("build/outputs/roborazzi/Scanner_${name}_$theme.png") {
                TenetTheme(darkTheme = dark) {
                    Surface {
                        CompositionLocalProvider(LocalInspectionMode provides true) {
                            Column {
                                SyncStatusBar(SyncStatus())
                                Column(Modifier.padding(16.dp)) { content() }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Instrument(title: String, status: ScanStatus, hint: String) = ScannerInstrument(
        title = title,
        status = status,
        hint = hint,
        torchOn = false,
        onToggleTorch = {},
        onPaste = {},
        modifier = Modifier.fillMaxWidth(),
    ) {}

    @Test
    fun searching() = capture("searching") {
        QrScanner(
            onCode = {},
            title = "Scanner · Offer",
            hint = "Point at the giver's code",
            onToggleTorch = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }

    @Test
    fun locked() = capture("locked") {
        Instrument("Scanner · Seal", ScanStatus.LOCKED, "Scan the seal on camera C-12")
    }

    @Test
    fun rejected() = capture("rejected") {
        Instrument("Scanner · Seal", ScanStatus.REJECTED, "Scan the seal on camera C-12")
    }

    @Test
    fun noPermission() = capture("no_permission") {
        ScannerInstrument(
            title = "Scanner · Acceptance",
            status = ScanStatus.SEARCHING,
            hint = "Point at Tan Wei Ming's code",
            torchOn = false,
            onToggleTorch = null,
            onPaste = {},
            modifier = Modifier.fillMaxWidth(),
            showSights = false,
        ) {
            CameraPermissionWindow(refused = false, onAllow = {}, onOpenSettings = {})
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun narrowLargeFont() {
        RuntimeEnvironment.setFontScale(1.3f)
        capture("360dp_font130") {
            Instrument("Scanner · Acceptance", ScanStatus.SEARCHING, "Point at Tan Wei Ming's code")
        }
    }
}
