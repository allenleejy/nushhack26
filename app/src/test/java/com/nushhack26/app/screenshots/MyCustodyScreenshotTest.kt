package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.crypto.CustodyStatus
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.ui.mycustody.HeldCamera
import com.nushhack26.app.ui.mycustody.MyCustodyContent
import com.nushhack26.app.ui.mycustody.MyCustodyUiState
import com.nushhack26.app.ui.mycustody.PendingOfferRow
import com.nushhack26.app.ui.mycustody.ReceiptRow
import com.nushhack26.app.ui.store.LOCAL_DB_ERROR
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val out = HeldCamera(
    "i1", "C-12", "IR-0457 · Ir-192", "S-40117", CustodyStatus.Out,
    dueMs = SAMPLE_NOW + 21_480_000, startMs = SAMPLE_NOW - 7_200_000, synced = true, name = "Kestrel",
)
private val overdue = HeldCamera(
    "i2", "C-07", "IR-0391 · Ir-192", "S-40102", CustodyStatus.Overdue,
    dueMs = SAMPLE_NOW - 240_000, startMs = SAMPLE_NOW - 90_240, synced = false, name = "Osprey",
)
private val receipts = listOf(
    ReceiptRow("h9", "C-12", HandoverKind.TRANSFER, "Tan Wei Ming", SAMPLE_NOW - 1_800_000, "S-40117", "48 21 07", SyncMark.WAITING, name = "Kestrel"),
    ReceiptRow("h8", "C-04", HandoverKind.RETURN, "Siti Aminah", SAMPLE_NOW - 86_400_000, "S-40299", "11 93 52", SyncMark.SYNCED),
)
private val pending = listOf(PendingOfferRow("i5", "C-15", "Tan Wei Ming", SAMPLE_NOW + 42_000, name = "Heron"))

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class MyCustodyScreenshotTest {
    @Test
    fun holding() = captureScreen("MyCustody_holding") {
        MyCustodyContent(MyCustodyUiState.Ready(SAMPLE_NOW, listOf(out), receipts = receipts))
    }

    @Test
    fun overdue() = captureScreen("MyCustody_overdue") {
        MyCustodyContent(MyCustodyUiState.Ready(SAMPLE_NOW, listOf(overdue, out)))
    }

    @Test
    fun disputed() = captureScreen("MyCustody_disputed") {
        MyCustodyContent(MyCustodyUiState.Ready(SAMPLE_NOW, listOf(out.copy(itemId = "i3", code = "C-15", name = "Heron", status = CustodyStatus.Disputed), out)))
    }

    @Test
    fun pendingOffer() = captureScreen("MyCustody_pending_offer") {
        MyCustodyContent(MyCustodyUiState.Ready(SAMPLE_NOW, listOf(out), pending, receipts))
    }

    @Test
    fun empty() = captureScreen("MyCustody_empty") {
        MyCustodyContent(MyCustodyUiState.Ready(SAMPLE_NOW, emptyList(), receipts = receipts))
    }

    @Test
    fun loading() = captureScreen("MyCustody_loading") { MyCustodyContent(MyCustodyUiState.Loading) }

    @Test
    fun error() = captureScreen("MyCustody_error") { MyCustodyContent(MyCustodyUiState.Error(LOCAL_DB_ERROR)) }

    @Test
    @Config(qualifiers = NARROW_CUSTODY)
    fun narrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("MyCustody_360dp_font130") {
            MyCustodyContent(MyCustodyUiState.Ready(SAMPLE_NOW, listOf(overdue), pending))
        }
    }
}
