package com.nushhack26.app.data.repo

import com.nushhack26.app.data.local.AppDatabase
import com.nushhack26.app.data.local.OutboxDao
import com.nushhack26.app.data.local.OutboxEntry
import com.nushhack26.app.data.local.OutboxKind
import com.nushhack26.app.crypto.Roster
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

object ServerTables {
    const val DEVICES = "devices"
    const val HANDOVERS = "handovers"
    const val RESOLUTIONS = "resolutions"
    const val MISSING_REPORTS = "missing_reports"
}

/**
 * Queues a new record for the server. Call it inside the same Room transaction that saves
 * the record. The op id is the record's own id, so two phones delivering the same record
 * send the same op and the server keeps one (docs/sync.md D.1).
 */
suspend fun OutboxDao.enqueueInsert(table: String, recordId: String, payload: String, now: Long = System.currentTimeMillis()) =
    insert(
        OutboxEntry(
            opId = recordId,
            tableName = table,
            recordId = recordId,
            kind = OutboxKind.INSERT,
            payload = payload,
            baseVersion = null,
            createdAt = now,
        ),
    )

/** Everyone and every device key, as last synced. Includes inactive people, so old signatures still check. */
fun AppDatabase.observeRoster(): Flow<Roster> =
    combine(peopleDao().observeAll(), deviceDao().observeAll()) { people, devices -> Roster(people, devices) }

suspend fun AppDatabase.roster(): Roster = Roster(peopleDao().all(), deviceDao().all())
