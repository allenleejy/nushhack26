package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.crypto.CustodyStatus
import com.nushhack26.app.ui.store.LOCAL_DB_ERROR
import com.nushhack26.app.ui.store.StoreContent
import com.nushhack26.app.ui.store.StoreRow
import com.nushhack26.app.ui.store.StoreUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Fixed times so the screenshots don't change from run to run. */
const val SAMPLE_NOW = 1_762_430_400_000L

val sampleStoreRows = listOf(
    StoreRow("i3", "C-15", "SE-0112 · Se-75", CustodyStatus.Disputed, "Priya Nair", SAMPLE_NOW + 3_600_000, synced = true, name = "Heron"),
    StoreRow("i2", "C-12", "IR-0457 · Ir-192", CustodyStatus.Overdue, "Daniel Lim", SAMPLE_NOW - 240_000, synced = false, name = "Kestrel"),
    StoreRow("i1", "C-07", "IR-0391 · Ir-192", CustodyStatus.Out, "Priya Nair", SAMPLE_NOW + 21_480_000, synced = true, name = "Osprey"),
    StoreRow("i4", "C-21", "IR-7714 · Ir-192", CustodyStatus.InStore, "Store officer", null, synced = true),
)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class StoreScreenshotTest {
    @Test
    fun store() = captureScreen("Store_rows") {
        StoreContent(StoreUiState.Ready(SAMPLE_NOW, sampleStoreRows), readOnly = false)
    }

    @Test
    fun readOnly() = captureScreen("Store_read_only") {
        StoreContent(StoreUiState.Ready(SAMPLE_NOW, sampleStoreRows), readOnly = true)
    }

    @Test
    fun empty() = captureScreen("Store_empty") {
        StoreContent(StoreUiState.Ready(SAMPLE_NOW, emptyList()), readOnly = false)
    }

    @Test
    fun loading() = captureScreen("Store_loading") { StoreContent(StoreUiState.Loading, readOnly = false) }

    @Test
    fun error() = captureScreen("Store_error") { StoreContent(StoreUiState.Error(LOCAL_DB_ERROR), readOnly = false) }

    @Test
    @Config(qualifiers = NARROW_CUSTODY)
    fun narrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Store_360dp_font130") {
            StoreContent(StoreUiState.Ready(SAMPLE_NOW, sampleStoreRows), readOnly = false)
        }
    }
}

/** The smallest phone we support; pair with font scale 1.3. */
const val NARROW_CUSTODY = "w360dp-h780dp-xxhdpi"
