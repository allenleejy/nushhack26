package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.outbox.OutboxContent
import com.nushhack26.app.ui.outbox.OutboxRowUi
import com.nushhack26.app.ui.outbox.OutboxUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class OutboxScreenshotTest {
    private val lastSync = SampleSafety.at(14, 2)
    private val rows = listOf(
        OutboxRowUi("op-1", "Handover · C-12 · Ravi → Wei Ming · signed 14:30", "Tried 2 times · queued 14:30", "Last error: Unable to resolve host", handoverId = "h-1"),
        OutboxRowUi("op-2", "Decision · C-12 · with Wei Ming · signed 14:34", "Not sent yet · queued 14:34"),
    )

    @Test
    fun waitingOffline() {
        val status = SyncStatus(isOnline = false, pendingCount = 2, lastSyncAt = lastSync)
        captureScreen("Outbox_waiting_offline", status) { OutboxContent(OutboxUiState(loading = false, rows = rows, status = status)) }
    }

    @Test
    fun waitingOnline() {
        val status = SyncStatus(isOnline = true, pendingCount = 2, lastSyncAt = lastSync, lastError = "Unable to resolve host")
        captureScreen("Outbox_waiting_online", status) { OutboxContent(OutboxUiState(loading = false, rows = rows, status = status)) }
    }

    @Test
    fun empty() {
        val status = SyncStatus(isOnline = true, pendingCount = 0, lastSyncAt = lastSync)
        captureScreen("Outbox_empty", status) { OutboxContent(OutboxUiState(loading = false, rows = emptyList(), status = status)) }
    }
}
