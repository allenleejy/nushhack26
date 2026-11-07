package com.nushhack26.app.screenshots

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.components.SyncStatusBar
import com.nushhack26.app.ui.components.TapPhase
import com.nushhack26.app.ui.components.TapPulse
import com.nushhack26.app.ui.components.TapRole
import com.nushhack26.app.ui.theme.TenetTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The tap moment as the sealed cube and the portal (card 28), still frames in Field and Daylight. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class TapPulseScreenshotTest {
    @Test fun giverWaiting() = capture("giver_waiting", TapPhase.Waiting, TapRole.Giver)
    @Test fun giverLinked() = capture("giver_linked", TapPhase.Linked, TapRole.Giver)
    @Test fun receiverWaiting() = capture("receiver_waiting", TapPhase.Waiting, TapRole.Receiver)
    /** Still Contact on the receiver jumps to the end state: the cube landed, the circle gone. */
    @Test fun receiverLanded() = capture("receiver_landed", TapPhase.Contact, TapRole.Receiver)

    @Test
    @Config(qualifiers = NARROW)
    fun giverWaitingNarrow() {
        RuntimeEnvironment.setFontScale(1.3f)
        capture("giver_waiting_narrow", TapPhase.Waiting, TapRole.Giver)
    }

    private fun capture(name: String, phase: TapPhase, role: TapRole) {
        for ((theme, dark) in listOf("field" to true, "daylight" to false)) {
            captureRoboImage("build/outputs/roborazzi/TapPulse_${name}_$theme.png") {
                TenetTheme(darkTheme = dark) {
                    Surface {
                        Column {
                            SyncStatusBar(SyncStatus())
                            Pulse(phase, role)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Pulse(phase: TapPhase, role: TapRole) {
        val linked = phase == TapPhase.Linked
        TapPulse(
            phase = phase,
            title = when {
                linked -> "Linked"
                role == TapRole.Receiver -> "Hold to the giver's phone"
                else -> "Hold to Wei Ming's phone"
            },
            subtitle = if (linked) "Waiting for Wei Ming to scan the seal and sign." else "Back to back, until both buzz.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 48.dp),
            role = role,
        )
    }
}
