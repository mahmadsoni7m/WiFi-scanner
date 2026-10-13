package com.wifiscanner.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Version 1. When the schema changes, bump [version], add a Migration object and register it with
 * addMigrations(...) below. Exported schemas are written to app/schemas/ during the build.
 */
@Database(
    entities = [
        ScanHistoryEntity::class,
        SpeedResultEntity::class,
        SavedNetworkEntity::class,
        SecurityCheckEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun speedResultDao(): SpeedResultDao
    abstract fun savedNetworkDao(): SavedNetworkDao
    abstract fun securityCheckDao(): SecurityCheckDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "wifiscanner.db")
                .build()
    }
}
