package com.example.connectany.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.connectany.data.local.dao.DeviceDao
import com.example.connectany.data.local.entity.DeviceEntity
import com.example.connectany.data.local.dao.BatteryLogDao
import com.example.connectany.data.local.entity.BatteryLogEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE devices ADD COLUMN autoLaunchPackage TEXT")
        db.execSQL("ALTER TABLE devices ADD COLUMN smartVolumeLevel INTEGER")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE devices ADD COLUMN showGlow INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `battery_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `macAddress` TEXT NOT NULL, 
                `timestampMs` INTEGER NOT NULL, 
                `batteryLevel` INTEGER NOT NULL, 
                FOREIGN KEY(`macAddress`) REFERENCES `devices`(`macAddress`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_battery_logs_macAddress` ON `battery_logs` (`macAddress`)")
    }
}

@Database(entities = [DeviceEntity::class, BatteryLogEntity::class], version = 7, exportSchema = true)
abstract class ConnectAnyDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun batteryLogDao(): BatteryLogDao
}
