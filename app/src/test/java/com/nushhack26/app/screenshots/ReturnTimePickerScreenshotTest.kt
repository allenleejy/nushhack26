package com.nushhack26.app.screenshots

import android.app.Application
import androidx.compose.material3.Surface
import com.github.takahirom.roborazzi.captureRoboImage
import com.nushhack26.app.ui.components.ReturnTimeSheet
import com.nushhack26.app.ui.theme.TenetTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** The return time sheet (card 31): 18:45 picked at 14:30, in both themes, and at 360 dp with 1.3× font. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class ReturnTimePickerScreenshotTest {
    private val zone = ZoneId.of("Asia/Singapore")
    private val now = ZonedDateTime.of(2026, 11, 7, 14, 30, 0, 0, zone).toInstant().toEpochMilli()

    private fun capture(name: String, time: LocalTime = LocalTime.of(18, 45)) {
        for ((theme, dark) in listOf("field" to true, "daylight" to false)) {
            captureRoboImage("build/outputs/roborazzi/ReturnTimePicker_${name}_$theme.png") {
                TenetTheme(darkTheme = dark) {
                    Surface { ReturnTimeSheet(time, now, onConfirm = {}, onDismiss = {}, zone = zone) }
                }
            }
        }
    }

    @Test fun today() = capture("today")

    @Test fun tomorrow() = capture("tomorrow", LocalTime.of(11, 35))

    @Test
    @Config(qualifiers = NARROW_CUSTODY)
    fun narrow() {
        RuntimeEnvironment.setFontScale(1.3f)
        capture("narrow")
    }
}
