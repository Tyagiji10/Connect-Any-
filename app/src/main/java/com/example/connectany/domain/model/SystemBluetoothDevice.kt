package com.example.connectany.domain.model

data class SystemBluetoothDevice(
    val stableKey: String, // MAC Address
    val name: String?,
    val deviceType: DeviceType,
    val isBonded: Boolean = true,
    val isConnected: Boolean
)
