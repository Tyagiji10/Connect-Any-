package com.example.connectany.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "battery_logs",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["macAddress"],
            childColumns = ["macAddress"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["macAddress"])]
)
data class BatteryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val macAddress: String,
    val timestampMs: Long,
    val batteryLevel: Int
)
