package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.data.local.DeviceEntity
import com.nushhack26.app.data.repo.DeviceListing
import com.nushhack26.app.data.repo.RelayOutcome
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.devices.DevicesContent
import com.nushhack26.app.ui.devices.DevicesUiState
import com.nushhack26.app.ui.devices.WithdrawSheetContent
import com.nushhack26.app.ui.devices.deviceRows
import com.nushhack26.app.ui.devices.withdrawSnack
import com.nushhack26.app.ui.enrol.EnrolContent
import com.nushhack26.app.ui.enrol.EnrolUiState
import com.nushhack26.app.data.repo.RevocationRepository
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class DevicesScreenshotTest {
    private val at = SampleSafety::at
    private fun phone(id: String, personId: String, fp: String, security: String, enrolled: Long, withdrawn: Long? = null) =
        DeviceEntity(id, personId, "pub-$id", fp, security, performedAt = enrolled, revokedAtMs = withdrawn)

    private fun listings(raviWithdrawn: Long? = null) = listOf(
        DeviceListing(phone("d-tan", SampleSafety.drTan.id, "9C:01:44:E2", "strongbox", at(7, 45)), SampleSafety.drTan),
        DeviceListing(phone("d-ravi", SampleSafety.ravi.id, "3F:A9:12:7C", "tee", at(7, 58), raviWithdrawn), SampleSafety.ravi),
        DeviceListing(phone("d-siti", SampleSafety.siti.id, "B7:2E:90:15", "tee", at(7, 52)), SampleSafety.siti),
    )

    private val online = SyncStatus(lastSyncAt = at(14, 38))
    private val offline = SyncStatus(isOnline = false, lastSyncAt = at(14, 38))

    private fun state(status: SyncStatus = online, raviWithdrawn: Long? = null) =
        DevicesUiState(loading = false, rows = deviceRows(listings(raviWithdrawn), myDeviceId = "d-tan"), status = status)

    @Test
    fun list() = captureScreen("Devices_list", online) { DevicesContent(state()) }

    @Test
    fun withdrawn() = captureScreen("Devices_withdrawn", online) {
        val s = state(raviWithdrawn = at(14, 40))
        DevicesContent(s.copy(snack = withdrawSnack(RelayOutcome.Sent, s.rows[1], at(14, 40), online = true)))
    }

    @Test
    fun offline() = captureScreen("Devices_offline", offline) { DevicesContent(state(offline, raviWithdrawn = at(14, 40))) }

    @Test
    fun confirmSheet() = captureScreen("Devices_confirm_sheet", online) {
        val s = state()
        WithdrawSheetContent(s.copy(confirming = s.rows[1]))
    }

    @Test
    fun refused() = captureScreen("Devices_refused", online) {
        val s = state()
        DevicesContent(s.copy(snack = withdrawSnack(RelayOutcome.Refused("not a safety officer"), s.rows[1], at(14, 40), online = true)))
    }

    @Test
    fun empty() = captureScreen("Devices_empty", online) { DevicesContent(DevicesUiState(loading = false, status = online)) }

    @Test
    fun loading() = captureScreen("Devices_loading") { DevicesContent(DevicesUiState()) }

    @Test
    fun error() = captureScreen("Devices_error") {
        DevicesContent(DevicesUiState(loading = false, error = "Couldn't read the phones on this device: disk I/O error"))
    }

    @Test
    fun enrolWithdrawnNotice() = captureScreen("Devices_enrol_withdrawn_notice", online) {
        EnrolContent(EnrolUiState(withdrawnNotice = RevocationRepository.WITHDRAWN_NOTICE))
    }

    @Test
    @Config(qualifiers = NARROW)
    fun narrowLargeFont() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Devices_narrow_1_3x", offline) { DevicesContent(state(offline, raviWithdrawn = at(14, 40))) }
    }

    @Test
    @Config(qualifiers = NARROW)
    fun narrowLargeFontSheet() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Devices_confirm_sheet_narrow_1_3x", online) {
            val s = state()
            WithdrawSheetContent(s.copy(confirming = s.rows[1]))
        }
    }
}
