package com.nushhack26.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The phone's copy of the data. Bump [version] whenever an entity changes: until the
 * freeze, migrations are destructive, so phones sync before installing a new APK.
 */
@Database(
    entities = [
        OutboxEntry::class,
        SyncStateEntry::class,
        PersonEntity::class,
        DeviceEntity::class,
        ItemEntity::class,
        HandoverEntity::class,
        AlertEntity::class,
        ResolutionEntity::class,
        MissingReportEntity::class,
        PendingOfferEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun outboxDao(): OutboxDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun peopleDao(): PeopleDao
    abstract fun deviceDao(): DeviceDao
    abstract fun itemDao(): ItemDao
    abstract fun handoverDao(): HandoverDao
    abstract fun alertDao(): AlertDao
    abstract fun resolutionDao(): ResolutionDao
    abstract fun missingReportDao(): MissingReportDao
    abstract fun pendingOfferDao(): PendingOfferDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
