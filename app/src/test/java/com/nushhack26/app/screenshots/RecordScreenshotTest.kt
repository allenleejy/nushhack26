package com.nushhack26.app.screenshots

import android.app.Application
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import com.github.takahirom.roborazzi.captureRoboImage
import com.nushhack26.app.ui.components.ReceiptContent
import com.nushhack26.app.ui.components.ReceiptParty
import com.nushhack26.app.ui.components.ReceiptStamp
import com.nushhack26.app.ui.record.RECORD_NOT_SYNCED
import com.nushhack26.app.ui.record.RECORD_SYNCED
import com.nushhack26.app.ui.record.RecordContent
import com.nushhack26.app.ui.record.RecordUiState
import com.nushhack26.app.ui.theme.TenetTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val signed = RecordUiState.Loaded(
    ReceiptContent(
        recordNo = 5, followsNo = 4, date = "07.11.2026", signedTime = "13:02",
        itemName = "Kestrel", itemCode = "C-12", source = "IR-0457 · Ir-192", sealNo = "S-40117",
        from = ReceiptParty("Ravi Kumar", "KEY 3F:A9:12:7C"), to = ReceiptParty("Tan Wei Ming", "KEY 8B:04:E1:5D"),
        returnBy = "19:00", heldBy = "Tan Wei Ming", signatures = "2 OF 2 VERIFIED",
        hash = "4f2a91c077e30b1d5e6a8c0f2b9d4e7a1c3f5b8d0e2a4c6f8b1d3e5a7c9f0b2d",
        verifyCode = "482 107", footer = RECORD_SYNCED,
    ),
    ReceiptStamp("COUNTERSIGNED", "13:02", "WEI MING"),
)

private val longNamesOffline = RecordUiState.Loaded(
    signed.content.copy(
        from = ReceiptParty("Muhammad Hafiz bin Abdul Rahman", "KEY 3F:A9:12:7C"),
        to = ReceiptParty("Dr Tan Mei Ling Josephine Wong", null),
        heldBy = "Dr Tan Mei Ling Josephine Wong",
        signatures = "SIGNER NOT ON THIS PHONE", signaturesOk = false,
        offline = true, footer = RECORD_NOT_SYNCED,
    ),
    ReceiptStamp("COUNTERSIGNED", "13:02", "DR TAN"),
)

private val register = RecordUiState.Loaded(
    signed.content.copy(
        recordNo = 1, followsNo = null, signedTime = "08:30", sealNo = null, from = null,
        to = ReceiptParty("Siti Aminah"), returnBy = null, heldBy = "Siti Aminah",
        signatures = "REGISTERED BY THE SERVER", hash = "9c1e04b7a2d35f86e0b4c7a91d2f3e5b6a8c0d1e2f3a4b5c6d7e8f9a0b1c2d3e",
        verifyCode = "655 012",
    ),
    ReceiptStamp("REGISTERED", "08:30", "IN STORE"),
)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class RecordScreenshotTest {
    private fun capture(name: String, state: RecordUiState) = captureScreen(name) {
        CompositionLocalProvider(LocalInspectionMode provides true) { RecordContent(state) }
    }

    @Test fun loaded() = capture("Record_loaded", signed)

    @Test fun loadedNight() = captureRoboImage("build/outputs/roborazzi/Record_loaded_night.png") {
        TenetTheme(darkTheme = true) {
            Surface { CompositionLocalProvider(LocalInspectionMode provides true) { RecordContent(signed) } }
        }
    }

    @Test fun register() = capture("Record_register", register)

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun narrowLargeFont() {
        RuntimeEnvironment.setFontScale(1.3f)
        capture("Record_360dp_font130", longNamesOffline)
    }

    @Test fun notFound() = capture("Record_not_found", RecordUiState.NotFound)

    @Test fun loading() = capture("Record_loading", RecordUiState.Loading)
}
