package com.nushhack26.app.screenshots

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.nushhack26.app.ui.components.ParcelField
import com.nushhack26.app.ui.components.ParcelLabel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Card 22: the parcel label on the Done screens, drawn in its final state (the stamp landed). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class ParcelLabelScreenshotTest {
    @Test
    fun received() = captureLabel("ParcelLabel_received") { Received() }

    @Test
    @Config(qualifiers = "+night")
    fun receivedDark() = captureLabel("ParcelLabel_received_dark") { Received() }

    @Test
    fun handedOver() = captureLabel("ParcelLabel_handed_over") { HandedOver() }

    @Test
    @Config(qualifiers = "+night")
    fun handedOverDark() = captureLabel("ParcelLabel_handed_over_dark") { HandedOver() }

    @Test
    @Config(qualifiers = NARROW_CUSTODY)
    fun longNameNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureLabel("ParcelLabel_long_360dp_font130") {
            ParcelLabel(
                itemName = "Kestrel",
                itemCode = "C-12",
                fields = listOf(
                    ParcelField("From", "Muhammad Hafiz bin Abdul Rahman"),
                    ParcelField("To", "Alexandra Tan Wei Ming-Rodrigues"),
                    ParcelField("Seal", "S-40117"),
                ),
                stampWord = "Received",
                stampTime = "13:02",
                stampLine = "From Muhammad",
            )
        }
    }
}

private val receivedFields = listOf(
    ParcelField("From", "Ravi Kumar"),
    ParcelField("To", "Tan Wei Ming"),
    ParcelField("Seal", "S-40117"),
    ParcelField("Return by", "07.11.2026 18:00"),
)

@Composable
private fun Received() = ParcelLabel(
    itemName = "Kestrel",
    itemCode = "C-12",
    fields = receivedFields,
    stampWord = "Received",
    stampTime = "13:02",
    stampLine = "From Ravi",
)

@Composable
private fun HandedOver() = ParcelLabel(
    itemName = "Kestrel",
    itemCode = "C-12",
    fields = receivedFields,
    stampWord = "Handed over",
    stampTime = "13:02",
    stampLine = "To Wei Ming",
)

private fun captureLabel(name: String, content: @Composable () -> Unit) = captureScreen(name) {
    CompositionLocalProvider(LocalInspectionMode provides true) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}
