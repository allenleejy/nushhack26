package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.alerts.AlertsContent
import com.nushhack26.app.ui.alerts.AlertsUiState
import com.nushhack26.app.ui.alerts.buildAlertCards
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class AlertsScreenshotTest {
    private val s = SampleSafety
    private val now = s.at(14, 35)
    private val online = SyncStatus(isOnline = true, lastSyncAt = s.at(14, 35))

    @Test
    fun forkAndOthers() {
        val cards = buildAlertCards(s.data(listOf(s.fork, s.sealAlert, s.refusedLapsed, s.refusedDevice)), now)
        captureScreen("Alerts_fork", online) { AlertsContent(AlertsUiState(loading = false, cards = cards, status = online)) }
    }

    @Test
    fun invalidSignature() {
        val cards = buildAlertCards(s.data(listOf(s.fork), tamperH2b = true), now)
        captureScreen("Alerts_invalid_signature", online) { AlertsContent(AlertsUiState(loading = false, cards = cards, status = online)) }
    }

    @Test
    fun offline() {
        val status = SyncStatus(isOnline = false, lastSyncAt = s.at(14, 2))
        val cards = buildAlertCards(s.data(listOf(s.refusedLapsed)), now)
        captureScreen("Alerts_offline", status) { AlertsContent(AlertsUiState(loading = false, cards = cards, status = status)) }
    }

    @Test
    fun resolved() {
        val cards = buildAlertCards(s.data(listOf(s.fork), listOf(s.resolution(synced = false))), now)
        captureScreen("Alerts_resolved", online) { AlertsContent(AlertsUiState(loading = false, cards = cards, status = online)) }
    }

    @Test
    fun empty() {
        val data = s.data(emptyList()).let { it.copy(handovers = listOf(s.h0)) }
        captureScreen("Alerts_empty", online) { AlertsContent(AlertsUiState(loading = false, cards = buildAlertCards(data, now), status = online)) }
    }

    @Test
    fun loading() = captureScreen("Alerts_loading", SyncStatus(lastSyncAt = null)) {
        AlertsContent(AlertsUiState(loading = true, status = SyncStatus(lastSyncAt = null)))
    }
}
