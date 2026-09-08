package com.example.connectany.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.connectany.data.local.entity.BatteryLogEntity

@Dao
@JvmSuppressWildcards
interface BatteryLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: BatteryLogEntity): Long

    @Query("SELECT * FROM battery_logs WHERE macAddress = :macAddress ORDER BY timestampMs ASC")
    suspend fun getLogsForDevice(macAddress: String): List<BatteryLogEntity>

    @Query("SELECT * FROM battery_logs WHERE macAddress = :macAddress ORDER BY timestampMs DESC LIMIT 1")
    suspend fun getLatestLogForDevice(macAddress: String): BatteryLogEntity?

    @Query("DELETE FROM battery_logs WHERE macAddress = :macAddress")
    suspend fun clearLogsForDevice(macAddress: String): Int
    
    // Auto-cleanup old logs (e.g., keep only last 24 hours). 
    // We can run this occasionally.
    @Query("DELETE FROM battery_logs WHERE timestampMs < :olderThanMs")
    suspend fun deleteLogsOlderThan(olderThanMs: Long): Int
}
