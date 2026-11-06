package com.nushhack26.app.screenshots

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.components.DangerButton
import com.nushhack26.app.ui.components.DiamondVariant
import com.nushhack26.app.ui.components.HandoverRole
import com.nushhack26.app.ui.components.HandoverStepper
import com.nushhack26.app.ui.components.HazardDiamond
import com.nushhack26.app.ui.components.PrimaryButton
import com.nushhack26.app.ui.components.QrPanel
import com.nushhack26.app.ui.components.SealCheckRow
import com.nushhack26.app.ui.components.SecondaryButton
import com.nushhack26.app.ui.components.SyncChip
import com.nushhack26.app.ui.components.SyncChipState
import com.nushhack26.app.ui.components.TextLinkButton
import com.nushhack26.app.ui.components.EmptyState
import com.nushhack26.app.ui.components.ListDivider
import com.nushhack26.app.ui.components.ListRow
import com.nushhack26.app.ui.components.RowLeading
import com.nushhack26.app.ui.components.SquareList
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The shared Tenet components (ui/components), rendered once so cards can see what they reuse. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class ComponentsScreenshotTest {
    @Test
    fun controls() = captureScreen("Components_controls", SyncStatus(isOnline = false, pendingCount = 3, lastSyncAt = 0L)) {
        Gallery {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SyncChip(SyncChipState.Synced)
                SyncChip(SyncChipState.Waiting, count = 3)
                SyncChip(SyncChipState.Conflict)
                SyncChip(SyncChipState.Offline)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HazardDiamond()
                HazardDiamond(variant = DiamondVariant.Filled)
                HazardDiamond(variant = DiamondVariant.Alert)
            }
            HandoverStepper(HandoverRole.Receiver, currentStep = 2)
            SealCheckRow(matches = true, itemCode = "C-12", scannedSeal = "S-40117", expectedSeal = "S-40117")
            SealCheckRow(matches = false, itemCode = "C-12", scannedSeal = "S-40999", expectedSeal = "S-40117")
            SecondaryButton("Scan seal", onClick = {})
            DangerButton("Report missing", onClick = {})
            TextLinkButton("View chain", onClick = {})
            PrimaryButton("Sign", onClick = {}, enabled = false, disabledReason = "Scan seal first")
            PrimaryButton("Sign and show QR", onClick = {})
        }
    }

    @Test
    fun qrPanel() = captureScreen("Components_qr_panel") {
        Gallery {
            HandoverStepper(HandoverRole.Giver, currentStep = 2)
            QrPanel(
                payload = "CS1O:" + "x".repeat(900),
                caption = "Offer · C-12",
                instruction = "Ask Wei Ming to scan this code.",
            )
        }
    }

    @Test
    fun listsAndEmpty() = captureScreen("Components_lists") {
        Gallery {
            SquareList {
                ListRow("Wei Ming", supporting = "Radiographer · authorised", leading = RowLeading.Initials("WM"), onClick = {})
                ListDivider()
                ListRow("Jun Lim", supporting = "Radiographer · authorised", leading = RowLeading.Initials("JL"), selected = true)
                ListDivider()
                ListRow("Ravi Kumar", supporting = "Registration expired", leading = RowLeading.Initials("RK"), blocked = true)
                ListDivider()
                ListRow("C-07 · IR-0391", supporting = "In store", leading = RowLeading.SealedSource, onClick = {}) {
                    SyncChip(SyncChipState.Synced)
                }
            }
            EmptyState(
                title = "No items in your custody",
                message = "Items signed over to you appear here. To receive one, scan the giver's offer.",
                actionLabel = "Scan an offer",
                onAction = {},
            )
        }
    }

    @Composable
    private fun Gallery(content: @Composable () -> Unit) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) { content() }
    }
}
