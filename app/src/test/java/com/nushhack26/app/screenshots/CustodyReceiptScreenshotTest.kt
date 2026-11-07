package com.nushhack26.app.screenshots

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.nushhack26.app.ui.components.CustodyReceipt
import com.nushhack26.app.ui.components.ReceiptContent
import com.nushhack26.app.ui.components.ReceiptParty
import com.nushhack26.app.ui.components.ReceiptStamp
import com.nushhack26.app.ui.components.ReceiptStyle
import com.nushhack26.app.ui.theme.Tenet
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Draws [content] still, in its final state: printed, stamped, no wave, no haptics. */
@Composable
fun ReceiptStill(content: @Composable () -> Unit) = CompositionLocalProvider(LocalInspectionMode provides true, content = content)

private const val TALL = "w412dp-h1400dp-xxhdpi"

private val giverDone = ReceiptContent(
    recordNo = 5,
    followsNo = 4,
    date = "07.11.2026",
    signedTime = "13:02",
    itemName = "Kestrel",
    itemCode = "C-12",
    source = "IR-0457 · Ir-192",
    sealNo = "S-40117",
    from = ReceiptParty("Ravi Kumar"),
    to = ReceiptParty("Tan Wei Ming"),
    returnBy = "19:00",
    heldBy = "Tan Wei Ming",
    verifyCode = "482 107",
)

private val receiverDone = giverDone.copy(heldBy = "YOU")

private val record = giverDone.copy(
    from = ReceiptParty("Ravi Kumar", key = "KEY 3F9A · C21D"),
    to = ReceiptParty("Tan Wei Ming", key = "KEY 81B0 · 7E44"),
    via = "TAP · NEARBY",
    signatures = "2 OF 2 VALID",
    hash = "9c4e1a7f0b3d52e8a61f4c09d7b2e3a5c8f1046e9d2b7a3c5e8f01d4b6a92c37",
    offline = true,
    footer = "Signed on both phones. Checked by the server at 13:04.",
)

private val handedOver = ReceiptStamp("Handed over", "13:02", "To Wei Ming")
private val received = ReceiptStamp("Received", "13:02", "From Ravi")

/** Card 29: the torn custody receipt on G5 and R Done, and the record view's printed receipt, in their final state. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class CustodyReceiptScreenshotTest {
    @Test
    fun doneGiver() = captureReceipt("CustodyReceipt_done_giver") { CustodyReceipt(giverDone, handedOver, ReceiptStyle.Done) }

    @Test
    @Config(qualifiers = "+night")
    fun doneGiverDark() = captureReceipt("CustodyReceipt_done_giver_dark") { CustodyReceipt(giverDone, handedOver, ReceiptStyle.Done) }

    @Test
    fun doneReceiver() = captureReceipt("CustodyReceipt_done_receiver") { CustodyReceipt(receiverDone, received, ReceiptStyle.Done) }

    @Test
    @Config(qualifiers = "+night")
    fun doneReceiverDark() = captureReceipt("CustodyReceipt_done_receiver_dark") {
        CustodyReceipt(receiverDone, received, ReceiptStyle.Done)
    }

    @Test
    @Config(qualifiers = TALL)
    fun record() = captureReceipt("CustodyReceipt_record") { CustodyReceipt(record, handedOver, ReceiptStyle.Record) }

    @Test
    @Config(qualifiers = "w412dp-h1400dp-night-xxhdpi")
    fun recordDark() = captureReceipt("CustodyReceipt_record_dark") { CustodyReceipt(record, handedOver, ReceiptStyle.Record) }

    @Test
    @Config(qualifiers = "w360dp-h1200dp-xxhdpi")
    fun doneLongNameNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureReceipt("CustodyReceipt_done_long_360dp_1.3x") {
            CustodyReceipt(
                giverDone.copy(
                    from = ReceiptParty("Muhammad Hafiz bin Abdul Rahman"),
                    to = ReceiptParty("Alexandra Tan Wei Ming-Rodrigues"),
                    heldBy = "Alexandra Tan Wei Ming-Rodrigues",
                ),
                ReceiptStamp("Handed over", "13:02", "To Alexandra"),
                ReceiptStyle.Done,
            )
        }
    }
}

private fun captureReceipt(name: String, content: @Composable () -> Unit) = captureScreen(name) {
    ReceiptStill {
        Column(Modifier.fillMaxSize().background(Tenet.colors.bg000).padding(16.dp)) { content() }
    }
}
