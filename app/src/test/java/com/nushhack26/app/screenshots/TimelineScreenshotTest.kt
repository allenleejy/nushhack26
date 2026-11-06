package com.nushhack26.app.screenshots

import android.app.Application
import com.nushhack26.app.crypto.CustodyStatus
import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.ui.store.LOCAL_DB_ERROR
import com.nushhack26.app.ui.timeline.EntryLook
import com.nushhack26.app.ui.timeline.KeyWithdrawn
import com.nushhack26.app.ui.timeline.SigCheck
import com.nushhack26.app.ui.timeline.TimelineContent
import com.nushhack26.app.ui.timeline.TimelineEntry
import com.nushhack26.app.ui.timeline.TimelineHeader
import com.nushhack26.app.ui.timeline.TimelineNode
import com.nushhack26.app.ui.timeline.TimelineUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private fun entry(id: String, kind: String, title: String, detail: String, minutesAgo: Long, look: EntryLook = EntryLook.Normal, sync: String = SyncMark.SYNCED, sig: SigCheck = SigCheck.Valid, reason: String? = null) =
    TimelineEntry(
        id, kind, title, detail, SAMPLE_NOW - minutesAgo * 60_000,
        dueMs = if (kind == HandoverKind.REGISTER || kind == HandoverKind.RETURN) null else SAMPLE_NOW + 21_480_000,
        sealNo = if (kind == HandoverKind.REGISTER) null else "S-40213",
        matchCode = if (kind == HandoverKind.REGISTER) "" else "48 21 07",
        sig = sig, syncStatus = sync, look = look, reason = reason,
    )

private val register = entry("r", HandoverKind.REGISTER, "Registered to Store officer", "Store register", 600, sig = SigCheck.Unsigned)
private val issue = entry("a", HandoverKind.ISSUE, "Issued to Priya Nair", "From Store officer", 120)
private val transfer = entry("b", HandoverKind.TRANSFER, "Handed to Daniel Lim", "From Priya Nair", 30, sync = SyncMark.WAITING)

private const val LOCAL_FORK = "This phone holds two handovers of CAM-01 from Priya Nair after H1. Only one can be right. The safety officer will decide once this phone syncs."

private val header = TimelineHeader("CAM-01", "Ir-192 · IR-7714", "S-40213", CustodyStatus.Out, "Daniel Lim", SAMPLE_NOW + 21_480_000)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = PHONE)
class TimelineScreenshotTest {
    @Test
    fun chain() = captureScreen("Timeline_chain") {
        TimelineContent(
            TimelineUiState.Ready(
                SAMPLE_NOW, header,
                listOf(
                    TimelineNode.Record(register), TimelineNode.Record(issue), TimelineNode.Record(transfer),
                    TimelineNode.Record(entry("x", HandoverKind.TRANSFER, "Handed to Wei Ling", "From Daniel Lim", 10, EntryLook.Rejected, SyncMark.REJECTED, reason = "The seal number doesn't match the register.")),
                ),
            ),
        )
    }

    @Test
    fun fork() = captureScreen("Timeline_fork") {
        TimelineContent(
            TimelineUiState.Ready(
                SAMPLE_NOW, header.copy(status = CustodyStatus.Disputed, holderName = null, dueMs = null),
                listOf(
                    TimelineNode.Record(register), TimelineNode.Record(issue),
                    TimelineNode.Fork(
                        listOf(
                            listOf(entry("b", HandoverKind.TRANSFER, "Handed to Daniel Lim", "From Priya Nair", 30)),
                            listOf(entry("c", HandoverKind.TRANSFER, "Handed to Wei Ling", "From Priya Nair", 28, sync = SyncMark.WAITING, sig = SigCheck.UnknownKey)),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun resolved() = captureScreen("Timeline_resolved") {
        TimelineContent(
            TimelineUiState.Ready(
                SAMPLE_NOW, header,
                listOf(
                    TimelineNode.Record(register), TimelineNode.Record(issue),
                    TimelineNode.Fork(
                        listOf(
                            listOf(entry("b", HandoverKind.TRANSFER, "Handed to Daniel Lim", "From Priya Nair", 30)),
                            listOf(entry("c", HandoverKind.TRANSFER, "Handed to Wei Ling", "From Priya Nair", 28, EntryLook.Overruled)),
                        ),
                    ),
                    TimelineNode.Resolution("Priya Nair to Daniel Lim", "Safety officer", "Daniel has the camera on site.", SAMPLE_NOW - 600_000, SigCheck.Valid, synced = true),
                ),
            ),
        )
    }

    @Test
    fun localFork() = captureScreen("Timeline_local_fork") {
        TimelineContent(
            TimelineUiState.Ready(
                SAMPLE_NOW, header.copy(status = CustodyStatus.Disputed, holderName = null, dueMs = null),
                listOf(
                    TimelineNode.Record(register), TimelineNode.Record(issue),
                    TimelineNode.LocalFork(LOCAL_FORK),
                    TimelineNode.Fork(
                        listOf(
                            listOf(entry("b", HandoverKind.TRANSFER, "Handed to Daniel Lim", "From Priya Nair", 30, sync = SyncMark.WAITING).copy(keyWithdrawn = KeyWithdrawn(SAMPLE_NOW - 300_000, afterSigning = true))),
                            listOf(entry("c", HandoverKind.RETURN, "Returned to Store officer", "From Priya Nair", 20, sync = SyncMark.WAITING)),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun keyWithdrawn() = captureScreen("Timeline_key_withdrawn") {
        TimelineContent(
            TimelineUiState.Ready(
                SAMPLE_NOW, header,
                listOf(
                    TimelineNode.Record(register),
                    TimelineNode.Record(issue.copy(keyWithdrawn = KeyWithdrawn(SAMPLE_NOW - 300_000, afterSigning = true))),
                    TimelineNode.Record(transfer.copy(keyWithdrawn = KeyWithdrawn(SAMPLE_NOW - 300_000, afterSigning = true))),
                    TimelineNode.Record(entry("x", HandoverKind.TRANSFER, "Handed to Wei Ling", "From Daniel Lim", 2, sync = SyncMark.WAITING).copy(keyWithdrawn = KeyWithdrawn(SAMPLE_NOW - 300_000, afterSigning = false))),
                ),
            ),
        )
    }

    @Test
    fun notFound() = captureScreen("Timeline_not_found") { TimelineContent(TimelineUiState.NotFound) }

    @Test
    fun loading() = captureScreen("Timeline_loading") { TimelineContent(TimelineUiState.Loading) }

    @Test
    fun error() = captureScreen("Timeline_error") { TimelineContent(TimelineUiState.Error(LOCAL_DB_ERROR)) }

    @Test
    @Config(qualifiers = NARROW_CUSTODY)
    fun narrowLargeText() {
        RuntimeEnvironment.setFontScale(1.3f)
        captureScreen("Timeline_360dp_font130") {
            TimelineContent(
                TimelineUiState.Ready(
                    SAMPLE_NOW, header.copy(status = CustodyStatus.Disputed),
                    listOf(
                        TimelineNode.Record(issue.copy(keyWithdrawn = KeyWithdrawn(SAMPLE_NOW - 300_000, afterSigning = true))),
                        TimelineNode.LocalFork(LOCAL_FORK),
                        TimelineNode.Fork(listOf(listOf(transfer.copy(keyWithdrawn = KeyWithdrawn(SAMPLE_NOW - 300_000, afterSigning = true))), listOf(entry("c", HandoverKind.TRANSFER, "Handed to Wei Ling", "From Priya Nair", 28).copy(keyWithdrawn = KeyWithdrawn(SAMPLE_NOW - 300_000, afterSigning = false))))),
                    ),
                ),
            )
        }
    }
}
