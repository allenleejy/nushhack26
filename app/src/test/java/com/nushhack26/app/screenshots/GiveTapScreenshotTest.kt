package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.auth.AccessRules
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.Roles
import com.nushhack26.app.nearby.TapAvailability
import com.nushhack26.app.ui.give.DoneView
import com.nushhack26.app.ui.give.GiveContent
import com.nushhack26.app.ui.give.GiveStep
import com.nushhack26.app.ui.give.GiveUiState
import com.nushhack26.app.ui.give.HandoverSummary
import com.nushhack26.app.ui.give.OfferView
import com.nushhack26.app.ui.give.ReceiverOption
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Card 18: G3 tap first, the QR fallback, and G5's parcel label. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class GiveTapScreenshotTest {
    private val now = 1_762_400_000_000L
    private val item = ItemEntity("item-c12", "C-12", "IR-0457", "Ir-192", "S-40117", "Main store", name = "Kestrel")

    private val summary = HandoverSummary(
        kind = HandoverKind.ISSUE,
        giverName = "Siti Aliyah",
        giverRole = AccessRules.roleName(Roles.STORE_OFFICER),
        receiverName = "Tan Wei Ming",
        receiverRole = AccessRules.roleName(Roles.RADIOGRAPHER),
        itemCode = "C-12",
        itemLine = "C-12 · sealed source IR-0457 (Ir-192) · seal S-40117",
        sealNo = "S-40117",
        dueMs = now + 3 * 3_600_000,
    )

    private val offerState = GiveUiState(
        loading = false,
        step = GiveStep.Offer,
        item = item,
        isIssue = true,
        receivers = listOf(ReceiverOption("p-wei", "Tan Wei Ming", Roles.RADIOGRAPHER, "Issue to", true, "Tan Wei Ming · authorised")),
        selectedReceiverId = "p-wei",
        summary = summary,
        offer = OfferView(payload = "CS1O:" + "A".repeat(400), expiresMs = now + 58_000),
        tapReady = true,
        nowMs = now,
    )

    private val doneState = offerState.copy(
        step = GiveStep.Done,
        done = DoneView(recordNo = 3, matchCode = "48 21 07", acceptedMs = now, synced = false, viaTap = true),
    )

    @Test
    fun waiting() = captureScreen("GiveTap_G3_waiting") { GiveContent(offerState) }

    @Test
    fun linked() = captureScreen("GiveTap_G3_linked") { GiveContent(offerState.copy(tapped = true)) }

    @Test
    fun nfcOff() = captureScreen("GiveTap_G3_nfc_off") {
        GiveContent(offerState.copy(tapReady = false), tap = TapAvailability.NfcOff)
    }

    @Test
    fun qrMode() = captureScreen("GiveTap_G3_qr_mode") { GiveContent(offerState, qrMode = true) }

    @Test
    fun done() = captureScreen("GiveTap_G5_done") { GiveContent(doneState) }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun waitingNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("GiveTap_G3_waiting_360dp_1.3x") { GiveContent(offerState) }
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun doneNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("GiveTap_G5_done_360dp_1.3x") { GiveContent(doneState) }
    }
}
