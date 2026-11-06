package com.nushhack26.app.screenshots

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nushhack26.app.ui.components.TapPhase
import com.nushhack26.app.ui.components.TapPulse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The tap moment (card 20), one still frame per phase, in Daylight and Field. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class TapPulseScreenshotTest {
    @Test fun waiting() = capture("TapPulse_waiting", TapPhase.Waiting)
    @Test fun contact() = capture("TapPulse_contact", TapPhase.Contact)
    @Test fun linked() = capture("TapPulse_linked", TapPhase.Linked)

    @Test @Config(qualifiers = "+night") fun waitingField() = capture("TapPulse_waiting_field", TapPhase.Waiting)
    @Test @Config(qualifiers = "+night") fun contactField() = capture("TapPulse_contact_field", TapPhase.Contact)
    @Test @Config(qualifiers = "+night") fun linkedField() = capture("TapPulse_linked_field", TapPhase.Linked)

    @Test
    @Config(qualifiers = NARROW)
    fun waitingNarrow() {
        RuntimeEnvironment.setFontScale(1.3f)
        capture("TapPulse_waiting_narrow", TapPhase.Waiting)
    }

    private fun capture(name: String, phase: TapPhase) = captureScreen(name) { Pulse(phase) }

    @Composable
    private fun Pulse(phase: TapPhase) {
        val linked = phase == TapPhase.Linked
        TapPulse(
            phase = phase,
            title = if (linked) "LINKED" else "HOLD TO WEI MING'S PHONE",
            subtitle = if (linked) "Wei Ming is scanning the seal and signing." else "Back to back, until both buzz.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 48.dp),
        )
    }
}
