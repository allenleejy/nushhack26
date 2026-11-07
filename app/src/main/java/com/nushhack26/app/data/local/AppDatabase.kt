package com.nushhack26.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The phone's copy of the data. Bump [version] whenever an entity changes, and add a
 * [Migration] for it: a destructive upgrade also wipes the identity, so every phone would
 * have to enrol again. Destructive migration is only the fallback for a missing step.
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
    version = 5,
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
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        /**
         * Camera names (card 24). Keeps the identity, the outbox and every record, so phones stay
         * enrolled. The names arrive on the next pull, which starts again from 0: rows pulled
         * before the app knew about names won't change on the server again.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `items` ADD COLUMN `name` TEXT NOT NULL DEFAULT ''")
                db.execSQL("DELETE FROM `sync_state` WHERE `key` = '${SyncKeys.LAST_SERVER_SEQ}'")
            }
        }
    }
}
