package com.example.connectany.domain

import com.example.connectany.domain.model.ConnectionState
import com.example.connectany.domain.model.NormalizedConnectionEvent
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow

interface ConnectionStateMachine {
    val currentState: StateFlow<ConnectionState>
    val latestEvent: StateFlow<NormalizedConnectionEvent?>
    val popupEvents: Flow<NormalizedConnectionEvent>

    fun processEvent(event: NormalizedConnectionEvent)
    
    /**
     * Immediately queues a preview event for the overlay service.
     */
    fun forcePreview(event: NormalizedConnectionEvent)
    
    /**
     * Updates the battery level of the latest event in-place without re-triggering
     * a popup. Used when battery info arrives asynchronously after the initial connect.
     */
    fun updateBatteryLevel(address: String, level: Int)
    
    /**
     * Gets the latest cached battery level for the given device address.
     */
    fun getBatteryLevel(address: String): Int?
}
