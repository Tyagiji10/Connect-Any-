package com.example.connectany.domain

import com.example.connectany.core.time.TimeProvider
import com.example.connectany.domain.model.ConnectionState
import com.example.connectany.domain.model.NormalizedConnectionEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

class ConnectionStateMachineImpl @Inject constructor(
    private val timeProvider: TimeProvider
) : ConnectionStateMachine {

    private val _currentState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val currentState: StateFlow<ConnectionState> = _currentState.asStateFlow()

    private val _latestEvent = MutableStateFlow<NormalizedConnectionEvent?>(null)
    override val latestEvent: StateFlow<NormalizedConnectionEvent?> = _latestEvent.asStateFlow()

    private var lastTriggeredTime = 0L
    private val cooldownMs = 3000L // 3s cooldown as requested
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun processEvent(event: NormalizedConnectionEvent) {
        val now = timeProvider.elapsedRealtime()

        when (event.state) {
            ConnectionState.CONNECTING -> {
                _currentState.value = ConnectionState.CONNECTING
            }
            ConnectionState.CONNECTED -> {
                // Trigger if we are past the cooldown, or not recently triggered
                if (now - lastTriggeredTime >= cooldownMs) {
                    _currentState.value = ConnectionState.CONNECTED
                    _latestEvent.value = event
                    
                    scope.launch {
                        _currentState.value = ConnectionState.TRIGGERED
                        lastTriggeredTime = timeProvider.elapsedRealtime()
                        delay(200) // Allow UI to collect
                        _currentState.value = ConnectionState.COOLDOWN
                        delay(cooldownMs)
                        _currentState.value = ConnectionState.READY
                        _latestEvent.value = null
                    }
                }
            }
            ConnectionState.DISCONNECTED -> {
                if (now - lastTriggeredTime >= cooldownMs) {
                    _currentState.value = ConnectionState.DISCONNECTED
                    _latestEvent.value = event
                    
                    scope.launch {
                        _currentState.value = ConnectionState.TRIGGERED
                        lastTriggeredTime = timeProvider.elapsedRealtime()
                        delay(200)
                        _currentState.value = ConnectionState.COOLDOWN
                        delay(cooldownMs)
                        _currentState.value = ConnectionState.READY
                        _latestEvent.value = null
                    }
                }
            }
            else -> {}
        }
    }
}
