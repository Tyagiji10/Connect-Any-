package com.example.connectany.domain.popup

import com.example.connectany.domain.model.ConnectionState
import com.example.connectany.domain.model.NormalizedConnectionEvent
import com.example.connectany.domain.health.RuntimeHealthMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class PopupDecisionEngine @Inject constructor(
    private val healthMonitor: RuntimeHealthMonitor
) {
    private val _currentPopup = MutableStateFlow<NormalizedConnectionEvent?>(null)
    val currentPopup: StateFlow<NormalizedConnectionEvent?> = _currentPopup.asStateFlow()

    fun evaluate(event: NormalizedConnectionEvent) {
        val health = healthMonitor.healthState.value
        if (!health.hasOverlayPermission) {
            // Cannot show popup, fallback to notification
            // TODO: dispatch to notification gateway
            return
        }

        if (event.state == ConnectionState.TRIGGERED) {
            _currentPopup.value = event
        } else if (event.state == ConnectionState.READY || event.state == ConnectionState.DISCONNECTED) {
            // Clean up
            _currentPopup.value = null
        }
    }
}
