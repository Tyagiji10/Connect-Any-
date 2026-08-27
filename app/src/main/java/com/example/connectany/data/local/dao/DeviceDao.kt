package com.example.connectany.data.local.dao

import androidx.room.*
import com.example.connectany.data.local.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface DeviceDao {
    @Query("SELECT * FROM devices")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices")
    suspend fun getAllDevicesSync(): List<DeviceEntity>

    @Query("SELECT * FROM devices WHERE macAddress = :macAddress")
    suspend fun getDeviceByMac(macAddress: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity): Long

    @Delete
    suspend fun deleteDevice(device: DeviceEntity): Int
}
