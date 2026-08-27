package com.example.connectany.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val macAddress: String,
    val name: String,
    val deviceType: String, // HEADPHONES, EARBUDS, WATCH, SPEAKER, CAR_AUDIO, KEYBOARD, CONTROLLER, OTHER
    val imageUri: String?,
    val popupStyle: String, // DROP, GLASS, MAGNETIC
    val isEnabled: Boolean,
    val durationMs: Long,
    val vibration: Boolean = false,
    val showOnConnect: Boolean = true,
    val showOnDisconnect: Boolean = false,
    val showOnReconnect: Boolean = false,
    val showBattery: Boolean = true,
    val popupColor: Int = android.graphics.Color.DKGRAY,
    val playSound: Boolean = false,
    val autoLaunchPackage: String? = null,
    val smartVolumeLevel: Int? = null
)
