package com.example.connectany.domain.model

data class ConnectAnyDeviceUiModel(
    val deviceKey: String, // Typically MAC address
    val deviceName: String, // Friendly name
    val systemDeviceType: DeviceType, // Mapped type
    val paired: Boolean, // Always true if from bonded devices
    val connectionState: ConnectionState,
    val connectanyEnabled: Boolean,
    val customImageUri: String?,
    val popupStyle: String?,
    val autoLaunchPackage: String?,
    val smartVolumeLevel: Int?
)
