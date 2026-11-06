package com.nushhack26.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// Screens read these as Flows through the repositories. The sync engine uses the
// plain suspend reads and the upserts.

@Dao
interface PeopleDao {
    @Query("SELECT * FROM people WHERE active = 1 ORDER BY name")
    fun observeActive(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people ORDER BY name")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people")
    suspend fun all(): List<PersonEntity>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun get(id: String): PersonEntity?

    @Upsert
    suspend fun upsertAll(people: List<PersonEntity>)
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices")
    fun observeAll(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices")
    suspend fun all(): List<DeviceEntity>

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun get(id: String): DeviceEntity?

    @Insert
    suspend fun insert(device: DeviceEntity)

    @Upsert
    suspend fun upsertAll(devices: List<DeviceEntity>)

    @Query("UPDATE devices SET syncStatus = :syncStatus, syncReason = :reason WHERE id = :id")
    suspend fun updateSync(id: String, syncStatus: String, reason: String?)
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY code")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observe(id: String): Flow<ItemEntity?>

    @Query("SELECT * FROM items")
    suspend fun all(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: String): ItemEntity?

    @Upsert
    suspend fun upsertAll(items: List<ItemEntity>)
}

@Dao
interface HandoverDao {
    @Query("SELECT * FROM handovers")
    fun observeAll(): Flow<List<HandoverEntity>>

    @Query("SELECT * FROM handovers WHERE itemId = :itemId")
    fun observeForItem(itemId: String): Flow<List<HandoverEntity>>

    @Query("SELECT * FROM handovers")
    suspend fun all(): List<HandoverEntity>

    @Query("SELECT * FROM handovers WHERE itemId = :itemId")
    suspend fun forItem(itemId: String): List<HandoverEntity>

    @Query("SELECT * FROM handovers WHERE id = :id")
    suspend fun get(id: String): HandoverEntity?

    @Query("SELECT * FROM handovers WHERE nonce = :nonce LIMIT 1")
    suspend fun byNonce(nonce: String): HandoverEntity?

    @Insert
    suspend fun insert(handover: HandoverEntity)

    @Upsert
    suspend fun upsert(handover: HandoverEntity)

    @Upsert
    suspend fun upsertAll(handovers: List<HandoverEntity>)

    @Query("UPDATE handovers SET syncStatus = :syncStatus, syncReason = :reason, status = COALESCE(:status, status) WHERE id = :id")
    suspend fun updateSync(id: String, syncStatus: String, reason: String?, status: String?)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY serverSeq DESC")
    fun observeAll(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE id = :id")
    fun observe(id: String): Flow<AlertEntity?>

    @Query("SELECT * FROM alerts WHERE id = :id")
    suspend fun get(id: String): AlertEntity?

    @Upsert
    suspend fun upsertAll(alerts: List<AlertEntity>)
}

@Dao
interface ResolutionDao {
    @Query("SELECT * FROM resolutions")
    fun observeAll(): Flow<List<ResolutionEntity>>

    @Query("SELECT * FROM resolutions")
    suspend fun all(): List<ResolutionEntity>

    @Query("SELECT * FROM resolutions WHERE id = :id")
    suspend fun get(id: String): ResolutionEntity?

    @Insert
    suspend fun insert(resolution: ResolutionEntity)

    @Upsert
    suspend fun upsertAll(resolutions: List<ResolutionEntity>)

    @Query("UPDATE resolutions SET syncStatus = :syncStatus, syncReason = :reason WHERE id = :id")
    suspend fun updateSync(id: String, syncStatus: String, reason: String?)
}

@Dao
interface MissingReportDao {
    @Query("SELECT * FROM missing_reports ORDER BY reportedMs DESC")
    fun observeAll(): Flow<List<MissingReportEntity>>

    @Query("SELECT * FROM missing_reports WHERE id = :id")
    suspend fun get(id: String): MissingReportEntity?

    @Insert
    suspend fun insert(report: MissingReportEntity)

    @Upsert
    suspend fun upsertAll(reports: List<MissingReportEntity>)

    @Query("UPDATE missing_reports SET syncStatus = :syncStatus, syncReason = :reason WHERE id = :id")
    suspend fun updateSync(id: String, syncStatus: String, reason: String?)
}

@Dao
interface PendingOfferDao {
    @Query("SELECT * FROM pending_offers ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PendingOfferEntity>>

    @Query("SELECT * FROM pending_offers WHERE itemId = :itemId ORDER BY createdAt DESC LIMIT 1")
    fun observeLatestForItem(itemId: String): Flow<PendingOfferEntity?>

    @Query("SELECT * FROM pending_offers WHERE id = :id")
    suspend fun get(id: String): PendingOfferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(offer: PendingOfferEntity)

    @Query("DELETE FROM pending_offers WHERE id = :id")
    suspend fun delete(id: String)
}
