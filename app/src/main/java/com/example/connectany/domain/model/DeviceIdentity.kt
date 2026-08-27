package com.example.connectany.domain.model

data class DeviceIdentity(
    val localId: String, // ConnectAny-generated stable UUID
    val platformAddress: String?, // optional / privacy constrained
    val normalizedName: String,
    val deviceType: DeviceType,
    val profileHints: Set<ProfileType>
)

enum class DeviceType {
    EARBUDS, HEADPHONES, SPEAKER, WATCH, CAR_AUDIO, KEYBOARD, CONTROLLER, LAPTOP, OTHER
}
