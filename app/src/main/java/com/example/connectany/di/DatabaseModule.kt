package com.example.connectany.di

import android.content.Context
import androidx.room.Room
import com.example.connectany.data.local.ConnectAnyDatabase
import com.example.connectany.data.local.dao.DeviceDao
import com.example.connectany.data.local.dao.BatteryLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ConnectAnyDatabase {
        return Room.databaseBuilder(
            context,
            ConnectAnyDatabase::class.java,
            "connect_any_db"
        )
        .addMigrations(
            com.example.connectany.data.local.MIGRATION_4_5,
            com.example.connectany.data.local.MIGRATION_5_6,
            com.example.connectany.data.local.MIGRATION_6_7
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideDeviceDao(db: ConnectAnyDatabase): DeviceDao = db.deviceDao()

    @Provides
    fun provideBatteryLogDao(db: ConnectAnyDatabase): BatteryLogDao = db.batteryLogDao()
}
