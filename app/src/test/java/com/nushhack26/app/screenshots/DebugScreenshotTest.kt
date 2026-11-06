package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.nearby.TapAvailability
import com.nushhack26.app.ui.debug.DebugContent
import com.nushhack26.app.ui.debug.DebugUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class DebugScreenshotTest {
    @Test fun tapTest() = captureScreen("Debug_tap_test") {
        DebugContent(DebugUiState(tapAvailability = TapAvailability.Ready, tapServing = true,
            tapLog = listOf("Tapped", "Linked", "Acceptance(CS1A:test-123)")))
    }

    @Test @Config(qualifiers = "w360dp-h915dp-xxhdpi") fun narrowTapTest() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Debug_tap_narrow") {
            DebugContent(DebugUiState(tapAvailability = TapAvailability.BluetoothOff,
                tapLog = listOf("Link timed out", "Send: false")))
        }
    }

    @Test fun receiverResult() = captureScreen("Debug_tap_receiver") {
        DebugContent(DebugUiState(tapAvailability = TapAvailability.Ready,
            tapPayload = "CS1T:{\"o\":\"CS1O:test-123\",\"k\":\"AAECAwQFBgcICQoLDA0ODw\"}",
            tapLog = listOf("Tap to read: 120 ms", "Tap to linked: 750 ms", "Send: true")))
    }
}
