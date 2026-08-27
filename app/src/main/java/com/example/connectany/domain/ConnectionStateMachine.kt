package com.example.connectany.domain

import com.example.connectany.domain.model.ConnectionState
import com.example.connectany.domain.model.NormalizedConnectionEvent
import kotlinx.coroutines.flow.StateFlow

interface ConnectionStateMachine {
    val currentState: StateFlow<ConnectionState>
    val latestEvent: StateFlow<NormalizedConnectionEvent?>
    fun processEvent(event: NormalizedConnectionEvent)
}
