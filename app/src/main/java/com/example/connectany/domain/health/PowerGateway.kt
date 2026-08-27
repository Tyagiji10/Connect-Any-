package com.example.connectany.domain.health

interface PowerGateway {
    fun isIgnoringBatteryOptimizations(): Boolean
    fun requestIgnoreBatteryOptimizations()
}
