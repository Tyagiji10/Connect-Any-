package com.example.connectany.domain.model

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    TRIGGERED,
    COOLDOWN,
    READY
}
