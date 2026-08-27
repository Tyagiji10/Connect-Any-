package com.example.connectany.domain.model

data class NormalizedConnectionEvent(
    val deviceIdentity: DeviceIdentity?,
    val profileType: ProfileType,
    val state: ConnectionState,
    val timestampMs: Long,
    val rawAddress: String,
    val batteryLevel: Int? = null
)
