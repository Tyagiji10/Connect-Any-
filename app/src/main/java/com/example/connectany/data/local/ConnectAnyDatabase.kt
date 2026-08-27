package com.example.connectany.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.connectany.data.local.dao.DeviceDao
import com.example.connectany.data.local.entity.DeviceEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE devices ADD COLUMN autoLaunchPackage TEXT")
        db.execSQL("ALTER TABLE devices ADD COLUMN smartVolumeLevel INTEGER")
    }
}

@Database(entities = [DeviceEntity::class], version = 5, exportSchema = true)
abstract class ConnectAnyDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
}
