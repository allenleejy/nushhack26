package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.ui.components.SyncChipState
import com.nushhack26.app.ui.receive.HandoverSummary
import com.nushhack26.app.ui.receive.ReceiveContent
import com.nushhack26.app.ui.receive.ReceiveStep
import com.nushhack26.app.ui.receive.ReceiveUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.ZoneId

/** Card 29: R Done's torn custody receipt, HELD BY YOU and stamped RECEIVED, and the cube on a relayed offer. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class ReceiveReceiptScreenshotTest {
    private fun at(h: Int, m: Int) = LocalDateTime.of(2026, 11, 7, h, m).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val summary = HandoverSummary(
        itemCode = "C-12", sourceSerial = "IR-0457", isotope = "Ir-192", sealNo = "S-40117",
        giverName = "Ravi Kumar", giverRole = "radiographer",
        receiverName = "Tan Wei Ming", receiverRole = "radiographer",
        kind = "transfer", dueMs = at(19, 0), itemName = "Kestrel",
    )

    private val done = ReceiveUiState(
        step = ReceiveStep.Done, summary = summary, nowMs = at(13, 2), signedAtMs = at(13, 2),
        matchCode = "48 21 07", recordNo = 5, recordSync = SyncChipState.Waiting, sentTap = true,
    )

    @Test
    fun done() = captureScreen("ReceiveReceipt_R_done") { ReceiptStill { ReceiveContent(done) } }

    @Test
    @Config(qualifiers = "+night")
    fun doneDark() = captureScreen("ReceiveReceipt_R_done_dark") { ReceiptStill { ReceiveContent(done) } }

    @Test
    fun doneSynced() = captureScreen("ReceiveReceipt_R_done_synced") {
        ReceiptStill { ReceiveContent(done.copy(recordSync = SyncChipState.Synced)) }
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun doneNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("ReceiveReceipt_R_done_360dp_1.3x") { ReceiptStill { ReceiveContent(done) } }
    }

    @Test
    fun relayedPulse() = captureScreen("ReceiveReceipt_R_relayed_pulse") {
        ReceiveContent(
            ReceiveUiState(
                step = ReceiveStep.Checking, relayed = true, tapContact = true, nowMs = at(13, 1),
                summary = summary.copy(giverName = "Siti Aminah", giverRole = "store_officer", kind = "issue"),
            ),
        )
    }

    @Test
    fun tapContactKnownItem() = captureScreen("ReceiveReceipt_R1_tap_known_item") {
        ReceiveContent(ReceiveUiState(step = ReceiveStep.Checks, tapContact = true, summary = summary, nowMs = at(13, 1)))
    }
}
