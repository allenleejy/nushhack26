package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.auth.AccessRules
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.ItemEntity
import com.nushhack26.app.data.local.Roles
import com.nushhack26.app.ui.give.DoneView
import com.nushhack26.app.ui.give.DueChip
import com.nushhack26.app.ui.give.GiveContent
import com.nushhack26.app.ui.give.GiveSnack
import com.nushhack26.app.ui.give.GiveStep
import com.nushhack26.app.ui.give.GiveUiState
import com.nushhack26.app.ui.give.GiveViewModel
import com.nushhack26.app.ui.give.HandoverSummary
import com.nushhack26.app.ui.give.OfferView
import com.nushhack26.app.ui.give.ReceiverList
import com.nushhack26.app.ui.give.ReceiverOption
import com.nushhack26.app.ui.give.SheetBody
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class GiveScreenshotTest {
    private val now = 1_762_400_000_000L
    private val item = ItemEntity("item-c12", "C-12", "IR-4471", "Ir-192", "S-0912", "Main store")

    private val transferReceivers = listOf(
        ReceiverOption("p-wei", "Tan Wei Ming", Roles.RADIOGRAPHER, "Hand to a colleague", true, "Tan Wei Ming · authorised"),
        ReceiverOption("p-jun", "Jun Lim", Roles.RADIOGRAPHER, "Hand to a colleague", false, "Jun Lim · authorisation lapsed 3 Nov"),
        ReceiverOption("p-siti", "Siti Aliyah", Roles.STORE_OFFICER, "Return to store", true, "Siti Aliyah · authorised"),
    )

    private val issueReceivers = listOf(
        ReceiverOption("p-ravi", "Ravi Kumar", Roles.RADIOGRAPHER, "Issue to", true, "Ravi Kumar · authorised"),
        ReceiverOption("p-wei", "Tan Wei Ming", Roles.RADIOGRAPHER, "Issue to", true, "Tan Wei Ming · authorised"),
        ReceiverOption("p-jun", "Jun Lim", Roles.RADIOGRAPHER, "Issue to", false, "Jun Lim · authorisation lapsed 3 Nov"),
    )

    private val transfer = HandoverSummary(
        kind = HandoverKind.TRANSFER,
        giverName = "Ravi Kumar",
        giverRole = AccessRules.roleName(Roles.RADIOGRAPHER),
        receiverName = "Tan Wei Ming",
        receiverRole = AccessRules.roleName(Roles.RADIOGRAPHER),
        itemCode = "C-12",
        itemLine = "C-12 · sealed source IR-4471 (Ir-192) · seal S-0912",
        sealNo = "S-0912",
        dueMs = now + 3 * 3_600_000,
    )

    private val base = GiveUiState(
        loading = false,
        item = item,
        receivers = transferReceivers,
        nowMs = now,
    )

    private val offer = OfferView(payload = "CS1O:" + "A".repeat(400), expiresMs = now + 47_000)

    @Test
    fun chooseIssue() = captureScreen("Give_G1_choose_issue") {
        GiveContent(base.copy(isIssue = true, receivers = issueReceivers, selectedReceiverId = "p-ravi", due = DueChip.At1700))
    }

    @Test
    fun chooseTransferNothingPicked() = captureScreen("Give_G1_choose_transfer") {
        GiveContent(base)
    }

    @Test
    fun receiverPicker() = captureScreen("Give_G1_picker") {
        val state = base.copy(selectedReceiverId = "p-wei")
        SheetBody(title = "Hand C-12 to", note = "2 can receive") { ReceiverList(state) {} }
    }

    @Test
    fun review() = captureScreen("Give_G2_review") {
        GiveContent(base.copy(step = GiveStep.Review, selectedReceiverId = "p-wei", summary = transfer))
    }

    @Test
    fun reviewFingerprintCancelled() = captureScreen("Give_G2_cancelled") {
        GiveContent(
            base.copy(
                step = GiveStep.Review, selectedReceiverId = "p-wei", summary = transfer,
                snack = GiveSnack.Problem("Fingerprint cancelled.", "Nothing was handed over."),
            ),
        )
    }

    @Test
    fun reviewLeaseExpired() = captureScreen("Give_G2_lease_expired") {
        GiveContent(base.copy(step = GiveStep.Review, selectedReceiverId = "p-wei", summary = transfer, leaseExpired = true))
    }

    @Test
    fun reviewOnline() = captureScreen("Give_G2_review_online") {
        GiveContent(base.copy(step = GiveStep.Review, selectedReceiverId = "p-wei", summary = transfer, online = true))
    }

    private val relayedOffer = OfferView(payload = "CS1O:" + "A".repeat(400), expiresMs = now + 178_000)

    @Test
    fun sentOnline() = captureScreen("Give_G3_online_sent") {
        GiveContent(base.copy(step = GiveStep.Sent, selectedReceiverId = "p-wei", summary = transfer, offer = relayedOffer, online = true))
    }

    @Test
    fun sentDeclined() = captureScreen("Give_G3_online_declined") {
        GiveContent(base.copy(step = GiveStep.Sent, selectedReceiverId = "p-wei", summary = transfer, offer = relayedOffer, declined = true))
    }

    @Test
    fun sentAcceptanceFailed() = captureScreen("Give_G3_online_error") {
        GiveContent(
            base.copy(
                step = GiveStep.Sent, selectedReceiverId = "p-wei", summary = transfer, offer = relayedOffer,
                relayError = "This isn't Wei Ming's acceptance of this offer.",
            ),
        )
    }

    @Test
    fun lostNetwork() = captureScreen("Give_G3_lost_network") {
        GiveContent(
            base.copy(
                step = GiveStep.Offer, selectedReceiverId = "p-wei", summary = transfer, offer = relayedOffer,
                snack = GiveSnack.Info(GiveViewModel.LOST_NETWORK),
            ),
        )
    }

    @Test
    fun doneOnline() = captureScreen("Give_G5_done_online") {
        GiveContent(
            base.copy(
                step = GiveStep.Done, selectedReceiverId = "p-wei", summary = transfer,
                done = DoneView(recordNo = 3, matchCode = "48 21 07", acceptedMs = now, synced = false, viaServer = true),
            ),
        )
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun sentNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Give_G3_online_360dp_1.3x") {
            GiveContent(base.copy(step = GiveStep.Sent, selectedReceiverId = "p-wei", summary = transfer, offer = relayedOffer, online = true))
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun reviewOnlineNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Give_G2_online_360dp_1.3x") {
            GiveContent(base.copy(step = GiveStep.Review, selectedReceiverId = "p-wei", summary = transfer, online = true))
        }
    }

    @Test
    fun offerQr() = captureScreen("Give_G3_offer") {
        GiveContent(base.copy(step = GiveStep.Offer, selectedReceiverId = "p-wei", summary = transfer, offer = offer))
    }

    @Test
    fun offerExpired() = captureScreen("Give_G3_expired") {
        GiveContent(base.copy(step = GiveStep.Offer, selectedReceiverId = "p-wei", summary = transfer, offer = offer, nowMs = now + 61_000))
    }

    @Test
    fun scanWrongCode() = captureScreen("Give_G4_scan_error") {
        GiveContent(
            base.copy(
                step = GiveStep.Scan, selectedReceiverId = "p-wei", summary = transfer, offer = offer,
                scanError = "This isn't Wei Ming's acceptance of this offer.",
            ),
        )
    }

    @Test
    fun done() = captureScreen("Give_G5_done") {
        GiveContent(
            base.copy(
                step = GiveStep.Done, selectedReceiverId = "p-wei", summary = transfer,
                done = DoneView(recordNo = 3, matchCode = "48 21 07", acceptedMs = now, synced = false),
                snack = GiveSnack.Confirmation(GiveViewModel.confirmationTitle(HandoverKind.TRANSFER), "C-12 · Holder: Wei Ming"),
            ),
        )
    }

    @Test
    fun notHolder() = captureScreen("Give_empty_not_holder") {
        GiveContent(GiveUiState(loading = false, empty = GiveViewModel.NOT_HOLDER))
    }

    @Test
    fun loading() = captureScreen("Give_loading") {
        GiveContent(GiveUiState())
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun offerNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Give_G3_offer_360dp_1.3x") {
            GiveContent(base.copy(step = GiveStep.Offer, selectedReceiverId = "p-wei", summary = transfer, offer = offer))
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun doneNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Give_G5_done_360dp_1.3x") {
            GiveContent(
                base.copy(
                    step = GiveStep.Done, selectedReceiverId = "p-wei", summary = transfer,
                    done = DoneView(recordNo = 3, matchCode = "48 21 07", acceptedMs = now, synced = true),
                ),
            )
        }
    }
    @Test
    fun offerTap() = captureScreen("Give_G3_tap_offer") {
        GiveContent(base.copy(step = GiveStep.Offer, selectedReceiverId = "p-wei", summary = transfer, offer = offer, tapReady = true))
    }

    @Test
    fun offerTapped() = captureScreen("Give_G3_tapped") {
        GiveContent(base.copy(step = GiveStep.Offer, selectedReceiverId = "p-wei", summary = transfer, offer = offer, tapReady = true, tapped = true))
    }

    @Test
    fun offerTapError() = captureScreen("Give_G3_tap_error") {
        GiveContent(
            base.copy(
                step = GiveStep.Offer, selectedReceiverId = "p-wei", summary = transfer, offer = offer, tapReady = true,
                tapError = "This isn't Wei Ming's acceptance of this offer.",
            ),
        )
    }

    @Test
    fun doneTapped() = captureScreen("Give_G5_done_tapped") {
        GiveContent(
            base.copy(
                step = GiveStep.Done, selectedReceiverId = "p-wei", summary = transfer,
                done = DoneView(recordNo = 3, matchCode = "48 21 07", acceptedMs = now, synced = false, viaTap = true),
            ),
        )
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun offerTappedNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Give_G3_tapped_360dp_1.3x") {
            GiveContent(base.copy(step = GiveStep.Offer, selectedReceiverId = "p-wei", summary = transfer, offer = offer, tapReady = true, tapped = true))
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun offerTapNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Give_G3_tap_offer_360dp_1.3x") {
            GiveContent(base.copy(step = GiveStep.Offer, selectedReceiverId = "p-wei", summary = transfer, offer = offer, tapReady = true))
        }
    }
}
