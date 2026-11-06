package com.nushhack26.app.sync

import com.nushhack26.app.data.local.AppDatabase
import androidx.room.withTransaction
import com.nushhack26.app.data.local.SyncKeys
import com.nushhack26.app.data.local.SyncMark
import com.nushhack26.app.data.local.put
import com.nushhack26.app.data.remote.OpResultKind
import com.nushhack26.app.data.remote.PushOp
import com.nushhack26.app.data.remote.SyncApi
import com.nushhack26.app.data.remote.payloadObject
import com.nushhack26.app.data.remote.toEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One sync: push the outbox, then pull everything new (docs/sync.md sections D and E).
 * Throws on a network failure so the worker retries with backoff.
 */
class SyncEngine(
    private val database: AppDatabase,
    private val api: SyncApi,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    suspend fun run() = mutex.withLock {
        val state = database.syncStateDao()
        val outbox = database.outboxDao()
        try {
            while (true) {
                val batch = outbox.nextBatch(100)
                if (batch.isEmpty()) break
                try {
                    val results = api.push(batch.map {
                        PushOp(it.opId, it.tableName, it.kind.name.lowercase(), payloadObject(it.payload))
                    })
                    check(results.map { it.opId } == batch.map { it.opId }) { "Invalid push response" }
                    check(results.all { it.result in setOf(OpResultKind.APPLIED, OpResultKind.DUPLICATE, OpResultKind.CONFLICT, OpResultKind.REJECTED) }) { "Unknown push result" }
                    database.withTransaction {
                        batch.zip(results).forEach { (entry, result) ->
                            val mark = if (result.result == OpResultKind.REJECTED) SyncMark.REJECTED else SyncMark.SYNCED
                            val reason = if (mark == SyncMark.REJECTED) result.reason else null
                            when (entry.tableName) {
                                "devices" -> database.deviceDao().updateSync(entry.recordId, mark, reason)
                                "handovers" -> database.handoverDao().updateSync(entry.recordId, mark, reason,
                                    if (result.result == OpResultKind.DUPLICATE) null else result.status)
                                "resolutions" -> database.resolutionDao().updateSync(entry.recordId, mark, reason)
                                "missing_reports" -> database.missingReportDao().updateSync(entry.recordId, mark, reason)
                                else -> error("Unknown outbox table: ${entry.tableName}")
                            }
                        }
                        outbox.delete(batch.map { it.opId })
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    database.withTransaction { batch.forEach { outbox.markFailed(it.opId, e.message ?: e.toString()) } }
                    throw e
                }
            }
            var since = state.get(SyncKeys.LAST_SERVER_SEQ)?.toLong() ?: 0L
            var mismatch: String? = null
            do {
                val page = api.pull(since)
                check(page.maxSeq >= since && (!page.more || page.maxSeq > since)) { "Invalid pull cursor" }
                database.withTransaction {
                    val rows = page.rows
                    database.peopleDao().upsertAll(rows.people.map { it.toEntity() })
                    database.itemDao().upsertAll(rows.items.map { it.toEntity() })
                    database.deviceDao().upsertAll(rows.devices.map { row ->
                        val local = database.deviceDao().get(row.id)
                        val server = row.toEntity()
                        local?.copy(performedAt = server.performedAt, createdBy = server.createdBy, serverSeq = server.serverSeq) ?: server
                    })
                    database.handoverDao().upsertAll(rows.handovers.map { row ->
                        val local = database.handoverDao().get(row.id)
                        val server = row.toEntity()
                        if (local == null) server else {
                            val base = if (local.recordHash != row.recordHash) {
                                mismatch = "Server copy of ${row.id} differs from this phone's"
                                server.copy(origin = local.origin, hiddenByTamper = local.hiddenByTamper, syncStatus = local.syncStatus, syncReason = local.syncReason)
                            } else local
                            base.copy(status = server.status, statusReason = server.statusReason, serverSeq = server.serverSeq,
                                receivedAt = server.receivedAt, createdBy = server.createdBy)
                        }
                    })
                    database.alertDao().upsertAll(rows.alerts.map { it.toEntity() })
                    database.resolutionDao().upsertAll(rows.resolutions.map { row ->
                        val local = database.resolutionDao().get(row.id)
                        val server = row.toEntity()
                        local?.copy(createdBy = server.createdBy, serverSeq = server.serverSeq) ?: server
                    })
                    database.missingReportDao().upsertAll(rows.missingReports.map { row ->
                        val local = database.missingReportDao().get(row.id)
                        val server = row.toEntity()
                        local?.copy(serverSeq = server.serverSeq) ?: server
                    })
                    state.put(SyncKeys.LAST_SERVER_SEQ, page.maxSeq.toString())
                    mismatch?.let { state.put(SyncKeys.LAST_ERROR, it) }
                }
                since = page.maxSeq
            } while (page.more)
            state.put(SyncKeys.LAST_SYNC_AT, now().toString())
            if (mismatch == null) state.remove(SyncKeys.LAST_ERROR)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            state.put(SyncKeys.LAST_ERROR, e.message ?: e.toString())
            throw e
        }
    }
}
