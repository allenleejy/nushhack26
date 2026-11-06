package com.nushhack26.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {
    @Insert
    suspend fun insert(entry: OutboxEntry)

    /** Oldest first, the order the server must apply them in. */
    @Query("SELECT * FROM outbox ORDER BY createdAt, opId LIMIT :limit")
    suspend fun nextBatch(limit: Int): List<OutboxEntry>

    @Query("SELECT * FROM outbox ORDER BY createdAt, opId")
    fun observeAll(): Flow<List<OutboxEntry>>

    @Query("SELECT COUNT(*) FROM outbox")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM outbox WHERE opId IN (:opIds)")
    suspend fun delete(opIds: List<String>)

    @Query("UPDATE outbox SET attempts = attempts + 1, lastError = :error WHERE opId = :opId")
    suspend fun markFailed(opId: String, error: String)
}
