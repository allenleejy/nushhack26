package com.nushhack26.app.screenshots

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.nushhack26.app.ui.incoming.IncomingOfferBannerContent
import com.nushhack26.app.ui.incoming.IncomingOfferUi
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class IncomingScreenshotTest {
    private val now = 1_794_466_800_000L
    private val offer = IncomingOfferUi("o1", "Siti Aminah", "C-12", now + 161_000, itemName = "Kestrel")

    @Test
    fun banner() = captureScreen("Incoming_R0_banner") {
        Column {
            IncomingOfferBannerContent(offer, now)
            Text("My custody", style = TenetType.title, modifier = Modifier.padding(TenetDimens.space4))
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun bannerNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Incoming_R0_banner_360dp_large_text") {
            IncomingOfferBannerContent(offer.copy(giverName = "Tan Wei Ming", itemCode = "C-07", itemName = ""), now)
        }
    }
}
