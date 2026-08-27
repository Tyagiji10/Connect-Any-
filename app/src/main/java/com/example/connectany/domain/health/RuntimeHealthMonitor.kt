package com.example.connectany.domain.health

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class RuntimeHealthState(
    val hasOverlayPermission: Boolean = false,
    val hasBluetoothConnectPermission: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val isReady: Boolean = false
)

class RuntimeHealthMonitor @Inject constructor(
    private val powerGateway: PowerGateway
) {
    private val _healthState = MutableStateFlow(RuntimeHealthState())
    val healthState: StateFlow<RuntimeHealthState> = _healthState.asStateFlow()

    fun updateOverlayPermission(granted: Boolean) {
        _healthState.value = _healthState.value.copy(hasOverlayPermission = granted)
        checkReadiness()
    }

    fun updateBluetoothPermission(granted: Boolean) {
        _healthState.value = _healthState.value.copy(hasBluetoothConnectPermission = granted)
        checkReadiness()
    }
    
    fun refreshPowerState() {
        val ignoring = powerGateway.isIgnoringBatteryOptimizations()
        _healthState.value = _healthState.value.copy(isIgnoringBatteryOptimizations = ignoring)
        checkReadiness()
    }

    private fun checkReadiness() {
        val state = _healthState.value
        val ready = state.hasOverlayPermission && state.hasBluetoothConnectPermission
        _healthState.value = state.copy(isReady = ready)
    }
}
