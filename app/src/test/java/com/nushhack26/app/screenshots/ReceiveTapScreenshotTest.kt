package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.crypto.CheckLine
import com.nushhack26.app.nearby.TapAvailability
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

/** Card 19: RECEIVE as a tab, tap first. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class ReceiveTapScreenshotTest {
    private fun at(h: Int, m: Int) = LocalDateTime.of(2026, 11, 7, h, m).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val summary = HandoverSummary(
        itemCode = "C-12", sourceSerial = "IR-0457", isotope = "Ir-192", sealNo = "S-40117",
        giverName = "Siti Aminah", giverRole = "store_officer",
        receiverName = "Ravi Kumar", receiverRole = "radiographer",
        kind = "issue", dueMs = at(14, 31), itemName = "Kestrel",
    )

    private val green = listOf(
        CheckLine(true, "Signed by Siti Aminah · key 3F:A9:12:7C"),
        CheckLine(true, "Siti Aminah holds C-12"),
        CheckLine(true, "Intended recipient"),
        CheckLine(true, "Valid for 0:41"),
        CheckLine(true, "Not used before"),
        CheckLine(true, "Authorised · radiographer · valid to 30.06.2027"),
    )

    private val tab = ReceiveUiState(step = ReceiveStep.ScanOffer, asTab = true, nowMs = at(13, 2))
    private val inFlow = tab.copy(summary = summary, linked = true)

    @Test
    fun tapWaiting() = captureScreen("ReceiveTap_R1_tap") { ReceiveContent(tab) }

    @Test
    fun nfcOff() = captureScreen("ReceiveTap_R1_nfc_off") { ReceiveContent(tab.copy(tapAvailability = TapAvailability.NfcOff)) }

    @Test
    fun qrMode() = captureScreen("ReceiveTap_R1_qr_mode") { ReceiveContent(tab.copy(qrMode = true, tapReady = true)) }

    @Test
    fun checksPassed() = captureScreen("ReceiveTap_R2_passed") {
        ReceiveContent(inFlow.copy(step = ReceiveStep.Checks, lines = green, passed = true))
    }

    @Test
    fun acceptWithName() = captureScreen("ReceiveTap_R4_accept_name") { ReceiveContent(inFlow.copy(step = ReceiveStep.Accept)) }

    @Test
    fun done() = captureScreen("ReceiveTap_done") {
        ReceiveContent(
            inFlow.copy(
                step = ReceiveStep.Done, linked = false, sentTap = true, matchCode = "48 21 07",
                signedAtMs = at(13, 2), recordSync = SyncChipState.Waiting,
            ),
        )
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun tapWaitingSmallLargeFont() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("ReceiveTap_R1_tap_360dp_1.3x") { ReceiveContent(tab.copy(tapAvailability = TapAvailability.BluetoothOff)) }
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun doneSmallLargeFont() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("ReceiveTap_done_360dp_1.3x") {
            ReceiveContent(inFlow.copy(step = ReceiveStep.Done, linked = false, matchCode = "48 21 07", signedAtMs = at(13, 2)))
        }
    }
}
