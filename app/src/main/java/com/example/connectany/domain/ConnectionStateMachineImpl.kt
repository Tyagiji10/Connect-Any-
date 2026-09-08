package com.example.connectany.domain

import com.example.connectany.core.time.TimeProvider
import com.example.connectany.domain.model.ConnectionState
import com.example.connectany.domain.model.NormalizedConnectionEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionStateMachineImpl @Inject constructor(
    private val timeProvider: TimeProvider
) : ConnectionStateMachine {

    private val _currentState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val currentState: StateFlow<ConnectionState> = _currentState.asStateFlow()

    private val _latestEvent = MutableStateFlow<NormalizedConnectionEvent?>(null)
    override val latestEvent: StateFlow<NormalizedConnectionEvent?> = _latestEvent.asStateFlow()

    // Unlimited channel acts as a perfect FIFO queue for popup events.
    private val _popupEvents = Channel<NormalizedConnectionEvent>(Channel.UNLIMITED)
    override val popupEvents: Flow<NormalizedConnectionEvent> = _popupEvents.receiveAsFlow()
    
    private val _batteryLevels = mutableMapOf<String, Int>()

    private val scope = CoroutineScope(Dispatchers.Default)

    override fun processEvent(event: NormalizedConnectionEvent) {
        when (event.state) {
            ConnectionState.CONNECTING -> {
                _currentState.value = ConnectionState.CONNECTING
            }
            ConnectionState.CONNECTED,
            ConnectionState.DISCONNECTED -> {
                _latestEvent.value = event
                _currentState.value = event.state
                
                // Send directly to the queue. The overlay service will process them sequentially.
                _popupEvents.trySend(event)
            }
            else -> {}
        }
    }

    override fun updateBatteryLevel(address: String, level: Int) {
        _batteryLevels[address] = level
        val current = _latestEvent.value ?: return
        if (current.rawAddress == address) {
            _latestEvent.value = current.copy(batteryLevel = level)
        }
    }
    
    override fun getBatteryLevel(address: String): Int? {
        return _batteryLevels[address]
    }

    override fun forcePreview(event: NormalizedConnectionEvent) {
        _latestEvent.value = event
        _popupEvents.trySend(event)
    }
}
