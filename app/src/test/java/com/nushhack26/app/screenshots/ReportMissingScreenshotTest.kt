package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.data.local.MissingReportEntity
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.missing.MissingInput
import com.nushhack26.app.ui.missing.NOT_ENROLLED_ERROR
import com.nushhack26.app.ui.missing.ReportMissingContent
import com.nushhack26.app.ui.missing.ReportMissingUiState
import com.nushhack26.app.ui.missing.SAVE_ERROR
import com.nushhack26.app.ui.missing.buildReportMissingState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class ReportMissingScreenshotTest {
    private val s = SampleSafety
    private val online = SyncStatus(isOnline = true, lastSyncAt = s.at(14, 50))
    private val offline = SyncStatus(isOnline = false, pendingCount = 1, lastSyncAt = s.at(14, 40))
    private val chain = listOf(s.h0, s.h1, s.h2)
    private val note = "Last seen on site B, level 3, around 14:40"

    private fun report(id: String, status: String, by: String = s.weiMing.id, reason: String? = null) = MissingReportEntity(
        id = id, itemId = s.item.id, reportedByPersonId = by, reportedByDeviceId = "d-$by", lastHandoverId = s.h2.id,
        note = note, reportedMs = s.at(14, 52), syncStatus = status, syncReason = reason,
    )

    private fun state(
        reports: List<MissingReportEntity> = emptyList(),
        input: MissingInput = MissingInput(),
        handovers: List<com.nushhack26.app.data.local.HandoverEntity> = chain,
    ) = buildReportMissingState(s.item, handovers, s.roster, reports, input)

    private fun sent(status: String, reason: String? = null): ReportMissingUiState {
        val r = report("r-new", status, reason = reason)
        return state(listOf(r), MissingInput(note = note, sentId = r.id, sentMs = r.reportedMs))
    }

    @Test
    fun lastSignedHolderIsWeiMing() {
        val ready = state() as ReportMissingUiState.Ready
        assertEquals("Tan Wei Ming", ready.holder?.name)
        assertEquals(s.h2.id, ready.lastHandoverId)
        val inStore = state(handovers = listOf(s.h0)) as ReportMissingUiState.Ready
        assertEquals("Siti Aminah", inStore.holder?.name)
        assertEquals(s.h0.id, inStore.lastHandoverId)
    }

    @Test
    fun form() = captureScreen("ReportMissing_form", online) { ReportMissingContent(state(input = MissingInput(note = note))) }

    @Test
    fun formEmpty() = captureScreen("ReportMissing_form_empty", online) { ReportMissingContent(state()) }

    @Test
    fun inStore() = captureScreen("ReportMissing_in_store", online) { ReportMissingContent(state(handovers = listOf(s.h0))) }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    fun formNarrowLargeFont() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("ReportMissing_form_360_font130", online) { ReportMissingContent(state(input = MissingInput(note = note))) }
    }

    @Test
    fun alreadyReported() = captureScreen("ReportMissing_already_reported", online) {
        ReportMissingContent(state(listOf(report("r-old", SyncMark.SYNCED))))
    }

    @Test
    fun saveError() = captureScreen("ReportMissing_save_error", online) {
        ReportMissingContent(state(input = MissingInput(note = note, error = SAVE_ERROR)))
    }

    @Test
    fun queued() = captureScreen("ReportMissing_queued", offline) { ReportMissingContent(sent(SyncMark.WAITING)) }

    @Test
    fun sentOk() = captureScreen("ReportMissing_sent", online) { ReportMissingContent(sent(SyncMark.SYNCED)) }

    @Test
    fun rejected() = captureScreen("ReportMissing_rejected", online) { ReportMissingContent(sent(SyncMark.REJECTED, "device withdrawn")) }

    @Test
    fun notFound() = captureScreen("ReportMissing_not_found", online) {
        ReportMissingContent(buildReportMissingState(null, emptyList(), s.roster, emptyList()))
    }

    @Test
    fun notEnrolled() = captureScreen("ReportMissing_not_enrolled", online) { ReportMissingContent(ReportMissingUiState.Error(NOT_ENROLLED_ERROR)) }

    @Test
    fun loading() = captureScreen("ReportMissing_loading", online) { ReportMissingContent(ReportMissingUiState.Loading) }
}
