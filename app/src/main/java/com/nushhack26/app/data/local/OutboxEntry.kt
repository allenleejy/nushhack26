package com.nushhack26.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OutboxKind { INSERT, UPDATE, DELETE }

/**
 * One change waiting to go to the server. A repository writes it in the same Room
 * transaction as the record itself; only the sync engine reads it.
 */
@Entity(tableName = "outbox")
data class OutboxEntry(
    /** Made on the phone. The server skips an op id it has already applied, so retries are safe. */
    @PrimaryKey val opId: String,
    val tableName: String,
    val recordId: String,
    val kind: OutboxKind,
    /** The record as JSON, in the server's column names. */
    val payload: String,
    /** The record version this edit started from; null for inserts. */
    val baseVersion: Long?,
    val createdAt: Long,
    val attempts: Int = 0,
    val lastError: String? = null,
)
