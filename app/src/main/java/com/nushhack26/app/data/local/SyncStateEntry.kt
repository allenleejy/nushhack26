package com.nushhack26.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Small key/value facts about sync, such as the last server_seq pulled and the last sync time. */
@Entity(tableName = "sync_state")
data class SyncStateEntry(
    @PrimaryKey val key: String,
    val value: String,
)

object SyncKeys {
    const val LAST_SYNC_AT = "last_sync_at"
    const val LAST_ERROR = "last_error"
    const val LAST_SERVER_SEQ = "last_server_seq"
}

@Dao
interface SyncStateDao {
    @Upsert
    suspend fun put(entry: SyncStateEntry)

    @Query("SELECT value FROM sync_state WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM sync_state WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    @Query("DELETE FROM sync_state WHERE `key` = :key")
    suspend fun remove(key: String)
}

suspend fun SyncStateDao.put(key: String, value: String) = put(SyncStateEntry(key, value))
