package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.crypto.CheckLine
import com.nushhack26.app.ui.components.SyncChipState
import com.nushhack26.app.ui.receive.HandoverSummary
import com.nushhack26.app.ui.receive.ReceiveContent
import com.nushhack26.app.ui.receive.ReceiveStep
import com.nushhack26.app.ui.receive.ReceiveUiState
import com.nushhack26.app.ui.receive.SealResult
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class ReceiveScreenshotTest {
    private fun at(h: Int, m: Int) = LocalDateTime.of(2026, 11, 7, h, m).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val summary = HandoverSummary(
        itemCode = "C-12", sourceSerial = "IR-0457", isotope = "Ir-192", sealNo = "S-40117",
        giverName = "Siti Aminah", giverRole = "store_officer",
        receiverName = "Ravi Kumar", receiverRole = "radiographer",
        kind = "issue", dueMs = at(14, 31),
    )

    private val green = listOf(
        CheckLine(true, "Signed by Siti Aminah · key on file"),
        CheckLine(true, "Siti Aminah holds C-12"),
        CheckLine(true, "Made out to you"),
        CheckLine(true, "Valid for 0:41"),
        CheckLine(true, "Not used before"),
        CheckLine(true, "Authorised · radiographer · valid to 30 Jun 2027"),
    )

    private val base = ReceiveUiState(summary = summary, nowMs = at(13, 2))

    @Test
    fun scanOffer() = captureScreen("Receive_R1_scan_offer") {
        ReceiveContent(base.copy(step = ReceiveStep.ScanOffer, summary = null))
    }

    @Test
    fun notHandoverCode() = captureScreen("Receive_R1_not_handover_code") {
        ReceiveContent(base.copy(step = ReceiveStep.ScanOffer, summary = null, message = "Not a handover code."))
    }

    @Test
    fun noRoster() = captureScreen("Receive_R1_no_roster") {
        ReceiveContent(base.copy(step = ReceiveStep.ScanOffer, summary = null, rosterEmpty = true))
    }

    @Test
    fun checking() = captureScreen("Receive_R2_checking") {
        ReceiveContent(base.copy(step = ReceiveStep.Checking))
    }

    @Test
    fun checksPassed() = captureScreen("Receive_R2_checks_passed") {
        ReceiveContent(base.copy(step = ReceiveStep.Checks, lines = green, passed = true))
    }

    @Test
    fun refusedCopy() = captureScreen("Receive_R2_refused_copy") {
        val lines = green.toMutableList().apply { this[4] = CheckLine(false, "Already used on this phone") }
        ReceiveContent(base.copy(step = ReceiveStep.Checks, lines = lines, passed = false, looksLikeCopy = true))
    }

    @Test
    fun refusedLapsed() = captureScreen("Receive_R2_refused_lapsed") {
        val lines = green.toMutableList().apply {
            this[2] = CheckLine(false, "Made out to Tan Wei Ming, not you")
            this[5] = CheckLine(false, "You aren't authorised to hold sealed sources (lapsed 3 Nov)")
        }
        ReceiveContent(base.copy(step = ReceiveStep.Checks, lines = lines, passed = false))
    }

    @Test
    fun scanSeal() = captureScreen("Receive_R3_scan_seal") {
        ReceiveContent(base.copy(step = ReceiveStep.ScanSeal))
    }

    @Test
    fun sealMatches() = captureScreen("Receive_R3_seal_matches") {
        ReceiveContent(base.copy(step = ReceiveStep.ScanSeal, seal = SealResult(true, "S-40117", "S-40117", "Seal S-40117 matches C-12")))
    }

    @Test
    fun sealMismatch() = captureScreen("Receive_R3_seal_mismatch") {
        val text = "Seal S-40999 doesn't match C-12 (S-40117). Don't accept this camera."
        ReceiveContent(base.copy(step = ReceiveStep.ScanSeal, seal = SealResult(false, "S-40999", "S-40117", text)))
    }

    @Test
    fun accept() = captureScreen("Receive_R4_accept") {
        ReceiveContent(base.copy(step = ReceiveStep.Accept))
    }

    @Test
    fun acceptCancelled() = captureScreen("Receive_R4_fingerprint_cancelled") {
        ReceiveContent(base.copy(step = ReceiveStep.Accept, message = "Fingerprint cancelled. Nothing was signed. Siti still holds C-12."))
    }

    @Test
    fun leaseExpired() = captureScreen("Receive_R4_lease_expired") {
        ReceiveContent(base.copy(step = ReceiveStep.Accept, leaseExpired = true))
    }

    @Test
    fun showAcceptance() = captureScreen("Receive_R5_acceptance_qr") {
        ReceiveContent(base.copy(step = ReceiveStep.ShowAcceptance, acceptanceQr = "CS1A:demo-acceptance-payload", matchCode = "48 21 07"))
    }

    @Test
    fun done() = captureScreen("Receive_R5_done") {
        ReceiveContent(base.copy(step = ReceiveStep.Done, matchCode = "48 21 07", recordSync = SyncChipState.Waiting))
    }

    @Test
    fun unavailable() = captureScreen("Receive_not_enrolled") {
        ReceiveContent(ReceiveUiState(ReceiveStep.Unavailable, message = "Enrol this phone before receiving a camera."))
    }

    private val relayed = base.copy(relayed = true)

    @Test
    fun relayedChecksPassed() = captureScreen("Receive_online_R2_checks_with_decline") {
        ReceiveContent(relayed.copy(step = ReceiveStep.Checks, lines = green.toMutableList().apply { this[3] = CheckLine(true, "Valid for 2:41") }, passed = true))
    }

    @Test
    fun relayedAccept() = captureScreen("Receive_online_R4_accept_with_decline") {
        ReceiveContent(relayed.copy(step = ReceiveStep.Accept))
    }

    @Test
    fun relayedNotOpen() = captureScreen("Receive_online_offer_not_open") {
        ReceiveContent(ReceiveUiState(ReceiveStep.Unavailable, relayed = true, message = "This offer is no longer open. Ask Siti to send it again."))
    }

    @Test
    fun relayedDeclined() = captureScreen("Receive_online_declined") {
        ReceiveContent(relayed.copy(step = ReceiveStep.Declined, declinedText = "Declined. Siti still holds C-12."))
    }

    @Test
    fun relayedSending() = captureScreen("Receive_online_sending") {
        ReceiveContent(relayed.copy(step = ReceiveStep.Sending, matchCode = "48 21 07"))
    }

    @Test
    fun relayedSent() = captureScreen("Receive_online_R5_sent") {
        ReceiveContent(relayed.copy(step = ReceiveStep.Done, sentOnline = true, matchCode = "48 21 07"))
    }

    @Test
    fun relayedFallbackQr() = captureScreen("Receive_online_R5_fallback_qr") {
        ReceiveContent(
            relayed.copy(
                step = ReceiveStep.ShowAcceptance, acceptanceQr = "CS1A:demo-acceptance-payload", matchCode = "48 21 07",
                fallbackText = "Couldn't reach the server. Show this to Siti instead.",
            ),
        )
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun relayedSentNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Receive_online_R5_sent_360dp_large_text") {
            ReceiveContent(relayed.copy(step = ReceiveStep.Done, sentOnline = true, matchCode = "48 21 07"))
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun relayedSealNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Receive_online_R3_seal_360dp_large_text") {
            ReceiveContent(relayed.copy(step = ReceiveStep.ScanSeal, seal = SealResult(true, "S-40117", "S-40117", "Seal S-40117 matches C-12")))
        }
    }
    @Test
    fun scanOfferTap() = captureScreen("Receive_R1_tap") {
        ReceiveContent(base.copy(step = ReceiveStep.ScanOffer, summary = null, tapReady = true))
    }

    @Test
    fun tappedChecksLinked() = captureScreen("Receive_tap_R2_linked") {
        ReceiveContent(base.copy(step = ReceiveStep.Checks, lines = green, passed = true, linked = true))
    }

    @Test
    fun tappedSent() = captureScreen("Receive_tap_R5_sent") {
        ReceiveContent(base.copy(step = ReceiveStep.Done, sentTap = true, matchCode = "48 21 07"))
    }

    @Test
    fun tappedFallbackQr() = captureScreen("Receive_tap_R5_fallback_qr") {
        ReceiveContent(
            base.copy(
                step = ReceiveStep.ShowAcceptance, acceptanceQr = "CS1A:demo-acceptance-payload", matchCode = "48 21 07",
                fallbackText = "Couldn't reach Siti's phone. Show this instead.",
            ),
        )
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun tappedSentNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Receive_tap_R5_sent_360dp_large_text") {
            ReceiveContent(base.copy(step = ReceiveStep.Done, sentTap = true, matchCode = "48 21 07"))
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun tappedSealNarrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Receive_tap_R3_linked_360dp_large_text") {
            ReceiveContent(base.copy(step = ReceiveStep.ScanSeal, linked = true, seal = SealResult(true, "S-40117", "S-40117", "Seal S-40117 matches C-12")))
        }
    }
}
