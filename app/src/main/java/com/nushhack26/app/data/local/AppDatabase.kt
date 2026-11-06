package com.nushhack26.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The phone's copy of the data. Bump [version] whenever an entity changes: until the
 * freeze, migrations are destructive, so phones sync before installing a new APK.
 */
@Database(
    entities = [OutboxEntry::class, SyncStateEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun outboxDao(): OutboxDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
