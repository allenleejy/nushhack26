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
    "i1", "CAM-01", "Ir-192 · IR-7714", "S-40213", CustodyStatus.Out,
    dueMs = SAMPLE_NOW + 21_480_000, startMs = SAMPLE_NOW - 7_200_000, synced = true,
)
private val overdue = HeldCamera(
    "i2", "CAM-02", "Ir-192 · IR-7722", "S-40288", CustodyStatus.Overdue,
    dueMs = SAMPLE_NOW - 240_000, startMs = SAMPLE_NOW - 90_240, synced = false,
)
private val receipts = listOf(
    ReceiptRow("h9", "CAM-05", HandoverKind.TRANSFER, "Daniel Lim", SAMPLE_NOW - 1_800_000, "S-40301", "48 21 07", SyncMark.WAITING),
    ReceiptRow("h8", "CAM-04", HandoverKind.RETURN, "Store officer", SAMPLE_NOW - 86_400_000, "S-40299", "11 93 52", SyncMark.SYNCED),
)
private val pending = listOf(PendingOfferRow("i5", "CAM-05", "Daniel Lim", SAMPLE_NOW + 42_000))

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
        MyCustodyContent(MyCustodyUiState.Ready(SAMPLE_NOW, listOf(out.copy(itemId = "i3", code = "CAM-03", status = CustodyStatus.Disputed), out)))
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
